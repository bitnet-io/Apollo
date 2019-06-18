/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkState;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.HasHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerFileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs complete file downloading from peers
 *
 * @author alukin@gmail.com
 */
public class FileDownloader {

    @Vetoed
    public class Status {
        double completed = 0.0;
        int chunksTotal = 1; //init to 1 to avoid zero division
        int chunksReady = 0;
        List<String> peers = new ArrayList<>();
        FileDownloadDecision decision = FileDownloadDecision.NotReady;
        boolean isComplete(){
            return chunksReady==chunksTotal;
        }
    }

    public static final int DOWNLOAD_THREADS = 6;
    private String fileID;
    private FileDownloadInfo downloadInfo;
    private List<HasHashSum> goodPeers;
    private List<HasHashSum> badPeers;
    private final Status status = new Status();
    private static final Logger log = LoggerFactory.getLogger(FileDownloader.class);

    DownloadableFilesManager manager;

    ExecutorService executor;
    List<Future<Boolean>> runningDownloaders = new ArrayList<>();
    private javax.enterprise.event.Event<ShardPresentData> presentDataEvent;

    @Inject
    public FileDownloader(DownloadableFilesManager manager,
                          javax.enterprise.event.Event<ShardPresentData> presentDataEvent) {
        this.manager = manager;
        this.executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        this.presentDataEvent = presentDataEvent;
    }
    
    public void setFileId(String fileID){
      if(this.fileID==null){
        this.fileID=fileID;
      }else{
          throw new RuntimeException("Can not set new filed ID in FileDownloader, it is already set");
      }
    }
    
    public void startDownload() {
        this.fileID = fileID;
        CompletableFuture<Boolean> prepare;
        prepare = CompletableFuture.supplyAsync(() -> {
            status.decision = prepareForDownloading();
            Boolean res = (status.decision == FileDownloadDecision.AbsOK || status.decision == FileDownloadDecision.OK);
            return res;
        });
        
        prepare.thenAccept( r->{
            if(r){
                status.chunksTotal = downloadInfo.chunks.size();
                log.debug("Decision is OK: {}, statring chunks downloading",status.decision.name());
                download();
            }else{
                log.warn("Decision is not OK: {}, Chunks downloading is nopt started",status.decision.name());
            }                
        });

/*      // FIRE EVENT EXAMPLES
        //FIRE event when shard is PRESENT + ZIP is downloaded
        ShardPresentData shardPresentData = new ShardPresentData(fileID);
        presentDataEvent.select(literal(ShardPresentEventType.PRESENT)).fireAsync(shardPresentData);
        //FIRE event when shard is NOT PRESENT
        ShardPresentData shardPresentData = new ShardPresentData();
        presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored
*/
    }

    public Status getDownloadStatus() {
        status.completed = ((1.0D * status.chunksReady) / (1.0 * status.chunksTotal)) * 100.0;
        return status;
    }

    public FileDownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public FileDownloadDecision prepareForDownloading() {
        FileDownloadDecision res;
        Set<Peer> allPeers = getAllAvailablePeers();
        PeersList pl = new PeersList();
        allPeers.forEach((pi) -> {
            PeerFileInfo pfi = new PeerFileInfo(new PeerClient(pi), fileID);
            pl.add(pfi);
        });
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();
        if(pvdm.isNetworkUsable()){
            PeerFileInfo pfi = (PeerFileInfo)goodPeers.get(0);
            downloadInfo = pfi.getFdi();
        }
        return res;
    }

    private synchronized FileChunkInfo getNextEmptyChunk() {
        FileChunkInfo res = null;
        for (FileChunkInfo fci : downloadInfo.chunks) {
            if (fci.present.ordinal() < FileChunkState.DOWNLOAD_IN_PROCGRESS.ordinal()) {
                res = fci;
                break;
            }
        }
        if(res==null){ //NO more empty chunks. File is ready
            //TODO: fire event
        }
        return res;
    }

    private boolean doPeerDownload(PeerClient p) throws IOException {
        boolean res = true;
        FileChunkInfo fci = getNextEmptyChunk();
        ChunkedFileOps fops = new ChunkedFileOps(manager.mapFileIdToLocalPath(fileID));
        while (fci != null) {
            fci.present=FileChunkState.DOWNLOAD_IN_PROCGRESS;
            FileChunk fc = p.downloadChunk(fci);
            if(fc!=null){
                byte[] data = Base64.getDecoder().decode(fc.mime64data);
                fops.writeChunk(fc.info.offset, data, fc.info.crc);
                status.chunksReady++;
                fci.present = FileChunkState.SAVED;
            }else{
              fci.present=FileChunkState.PRESENT;
            }
            fci = getNextEmptyChunk();
        }
        return res;
    }

    public Status download() {
        int peerCount = 0;
        for (HasHashSum p : goodPeers) {
            Future<Boolean> dn_res = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    PeerFileInfo pfi = (PeerFileInfo) p;
                    return doPeerDownload(pfi.getPeerClient());
                }
            });
            runningDownloaders.add(dn_res);
            peerCount++;
            if (peerCount > DOWNLOAD_THREADS) {
                break;
            }
        }
        return status;
    }

    public Set<Peer> getAllAvailablePeers() {
        Set<Peer> res = new HashSet<>();
        Collection<? extends Peer> knownPeers = Peers.getAllPeers();
        res.addAll(knownPeers);
        return res;
    }
    
    @PreDestroy
    public void preDestroy(){
        if(executor!=null){
            //TODO: do we need to cancel tasks and threads?
            executor.shutdown();
        }
    }

    private AnnotationLiteral<ShardPresentEvent> literal(ShardPresentEventType shardPresentEventType) {
        return new ShardPresentEventBinding() {
            @Override
            public ShardPresentEventType value() {
                return shardPresentEventType;
            }
        };
    }

}

/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocket extends WebSocketAdapter {
    
    /** we use reference here to avoid memory leaks */
    private final SoftReference<Peer2PeerTransport> peerReference;

    /**
     * Our WebSocket message version
     */
    private static final int WS_VERSION = 1;
    /**
     * Negotiated WebSocket message version
     */
    private int version = WS_VERSION;
    /**
     * Compressed message flag
     */
    private static final int FLAG_COMPRESSED = 1;
    private long lastActivityTime;

    public PeerWebSocket(Peer2PeerTransport peer) {
        peerReference = new SoftReference<>(peer);
        lastActivityTime=System.currentTimeMillis();
    }
    
    String which(){
        String which="";
        Peer2PeerTransport p = peerReference.get();
        if(p!=null){
            which+=" at "+p.getHostWithPort();
        }else{
            Session s = getSession();
            if (s!=null){
                RemoteEndpoint r = s.getRemote();
                if(r!=null){
                    String addr = r.getInetSocketAddress().getAddress().getHostAddress();
                    which=addr+":"+r.getInetSocketAddress().getPort();
                }
            }
        }
        return which;
    }
    
    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        lastActivityTime=System.currentTimeMillis();
        log.debug("Peer: {} String received: \n{}",which(),message);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        log.trace("Peer: {} WebSocket error: {}",which(),cause);
        if(peerReference.get()==null){
            close();
        }
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);       
        log.trace("{} WebSocket connectded:", which());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        log.trace("Peer: {} WebSocket close: {}",which(),statusCode);
        Peer2PeerTransport p = peerReference.get();
        if(p!=null){
            p.onWebSocketClose(this);
        }else{
            log.debug("Closing orphaned websocket: {}",which());
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        super.onWebSocketBinary(payload, offset, len);
        lastActivityTime=System.currentTimeMillis();
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload, offset, len);
            version = Math.min(buf.getInt(), WS_VERSION);
            Long rqId = buf.getLong();
            int flags = buf.getInt();
            int length = buf.getInt();
            byte[] msgBytes = new byte[buf.remaining()];
            buf.get(msgBytes);
            if ((flags & FLAG_COMPRESSED) != 0) {
                ByteArrayInputStream inStream = new ByteArrayInputStream(msgBytes);
                try (GZIPInputStream gzipStream = new GZIPInputStream(inStream, 1024)) {
                    msgBytes = new byte[length];
                    int off = 0;
                    while (off < msgBytes.length) {
                        int count = gzipStream.read(msgBytes, off, msgBytes.length - off);
                        if (count < 0) {
                            throw new EOFException("End-of-data reading compressed data");
                        }
                        off += count;
                    }
                }
            }
            String message = new String(msgBytes, "UTF-8");
            Peer2PeerTransport p = peerReference.get();
            if(p!=null){
                p.onIncomingMessage(message,this,rqId);
            }else{
                log.warn("Peer reference is null on websocket incoming message, closing websocket:\n {}",message);
                close();
            }    

        } catch (IOException ex) {
            log.debug("Peer: {} IO Exception on message receiving: {}", which(), ex);
        }
    }

    /**
     * Sends websocket message
     * Must be synchronized because it is used from multiple threads
     * @param message message string
     * @param requestId if it is not null, it means it is request otherwise it is
     * response
     * @return requestId
     * @throws IOException
     */
    public  synchronized boolean send(String message, Long requestId) throws IOException {
        if(StringUtils.isBlank(message.trim())){
            log.warn("Empty request from us to {}",which());
            return false;
        }        
        byte[] requestBytes = message.getBytes("UTF-8");
        int requestLength = requestBytes.length;
        int flags = 0;
        if (Peers.isGzipEnabled && requestLength >= Peers.MIN_COMPRESS_SIZE) {
            flags |= FLAG_COMPRESSED;
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(requestLength);
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(outStream)) {
                gzipStream.write(requestBytes);
            }
            requestBytes = outStream.toByteArray();
        }
        ByteBuffer buf = ByteBuffer.allocate(requestBytes.length + 20);
        buf.putInt(version)
                .putLong(requestId)
                .putInt(flags)
                .putInt(requestLength)
                .put(requestBytes)
                .flip();
        if (buf.limit() > Peers.MAX_MESSAGE_SIZE) {
            throw new ProtocolException("POST request length exceeds max message size");
        }
        Session s =  getSession();
        if(s!=null){
           s.getRemote().sendBytes(buf);
        }else{
          throw new IOException("Websocket session is null for "+which())  ;
        }
        return true;
    }

    public void close(){
        Session s = getSession();
        if(s!=null){
            s.close(1001,"Disconnect"); //RFC 6455, Section 7.4.1
            try {
                s.disconnect();                
            } catch (IOException ex) {
                log.debug("Excetion on session disconnect to {}",which(),ex);
            }
        }
    }
    long getLastActivityTime() {
        return lastActivityTime;
    }
    public Peer2PeerTransport getTransport(){
        return peerReference.get();
    }
}

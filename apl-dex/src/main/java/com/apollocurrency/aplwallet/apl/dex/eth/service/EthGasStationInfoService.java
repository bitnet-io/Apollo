/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.eth.service;

import com.apollocurrency.aplwallet.apl.dex.eth.model.EthChainGasInfoImpl;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthStationGasInfo;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class EthGasStationInfoService {

    public EthGasInfo getEthPriceInfo() throws IOException {
        EthStationGasInfo ethGasInfo = null;
        HttpsURLConnection con = null;
        try {

            URL url = new URL(Constants.ETH_STATION_GAS_INFO_URL);
            SSLContext sc = null;
            try {
                sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(EthGasStationInfoService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyManagementException ex) {
                Logger.getLogger(EthGasStationInfoService.class.getName()).log(Level.SEVERE, null, ex);
            }

            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(sc.getSocketFactory());

            con.setRequestMethod("GET");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    ethGasInfo = new ObjectMapper()
                        .readerFor(EthStationGasInfo.class)
                        .readValue(reader);
                }
            }
        } finally {
            con.disconnect();
        }

        if (ethGasInfo.getSafeLowSpeedPrice().equals(0L)) {
            return null;
        }

        return ethGasInfo;
    }

    public EthGasInfo getEthChainPriceInfo() throws IOException {
        EthChainGasInfoImpl ethGasInfo = null;
        HttpsURLConnection con = null;
        try {
            URL url = new URL(Constants.ETH_CHAIN_GAS_INFO_URL);
            SSLContext sc = null;
            try {
                sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(EthGasStationInfoService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyManagementException ex) {
                Logger.getLogger(EthGasStationInfoService.class.getName()).log(Level.SEVERE, null, ex);
            }

            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(sc.getSocketFactory());

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    ethGasInfo = new ObjectMapper()
                        .readerFor(EthChainGasInfoImpl.class)
                        .readValue(reader);
                }
            }
        } finally {
            con.disconnect();
        }

        if (ethGasInfo.getSafeLowSpeedPrice().equals(0L)) {
            return null;
        }

        return ethGasInfo;
    }

}

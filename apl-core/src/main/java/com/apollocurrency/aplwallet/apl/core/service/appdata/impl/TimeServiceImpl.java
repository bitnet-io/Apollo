/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author al
 */
@Slf4j
@Singleton
public class TimeServiceImpl implements TimeService {

    private final NtpTime ntpTime;

    @Inject
    public TimeServiceImpl(NtpTime ntpTime) {
        this.ntpTime = ntpTime;
    }

    /**
     * Time since genesis block.
     *
     * @return int (time in seconds).
     */
    public int getEpochTime() {
        long ntpTime = this.ntpTime.getTime();
        int toEpochTime = Convert2.toEpochTime(ntpTime);
        log.trace("ntpTime : long = {}, toEpochTime = {}", ntpTime, toEpochTime);
        return toEpochTime;
    }


    /**
     * Returns current time in seconds
     * @return current time in seconds
     */
    @Override
    public long systemTime() {
        return ntpTime.getTime() / 1000;
    }

    /**
     * Returns current time in milliseconds
     * @return current time in milliseconds
     */
    @Override
    public long systemTimeMillis() {
        return ntpTime.getTime();
    }
}

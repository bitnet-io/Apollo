/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"enabled", "frequency", "digestAlgorithm"})
public class ShardingSettings {
    private static final int DEFAULT_SHARDING_FREQUENCY = 500_000;
    private static final boolean DEFAULT_SHARDING_ENABLED = false;
    private static final String DEFAULT_SHARDING_DIGEST_ALGORITHM = "SHA-256";

    private boolean enabled;
    private int frequency;
    private String digestAlgorithm; // it is not used in json configs actually

    public ShardingSettings(
        boolean enabled,
        int frequency,
        String digestAlgorithm /* it is not used in json configs actually */)
    {
        this.enabled = enabled;
        if (frequency > 0) {
            this.frequency = frequency;
        } else {
            this.frequency = DEFAULT_SHARDING_FREQUENCY;
        }
        this.digestAlgorithm = StringValidator.requireNonBlank(digestAlgorithm);
    }

    @JsonCreator
    public ShardingSettings(
        @JsonProperty("enabled") boolean enabled, @JsonProperty("frequency") int frequency) {
        this(enabled, frequency, DEFAULT_SHARDING_DIGEST_ALGORITHM);
    }

    public ShardingSettings(boolean enabled) {
        this(enabled, DEFAULT_SHARDING_FREQUENCY, DEFAULT_SHARDING_DIGEST_ALGORITHM);
    }

    public ShardingSettings(boolean enabled, String digestAlgorithm) {
        this(enabled, DEFAULT_SHARDING_FREQUENCY, digestAlgorithm);
    }

    public ShardingSettings() {
        this(DEFAULT_SHARDING_ENABLED);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public ShardingSettings copy() {
        return new ShardingSettings(enabled, frequency, digestAlgorithm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShardingSettings)) return false;
        ShardingSettings that = (ShardingSettings) o;
        return enabled == that.enabled &&
            frequency == that.frequency &&
            digestAlgorithm.equals(that.digestAlgorithm);
    }

    @Override
    public String toString() {
        return "ShardingSettings{" +
            "enabled=" + enabled +
            ", frequency=" + frequency +
            ", digestAlgorithm='" + digestAlgorithm + '\'' +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, frequency, digestAlgorithm);
    }
}

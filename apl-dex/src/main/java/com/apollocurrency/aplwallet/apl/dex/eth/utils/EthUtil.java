package com.apollocurrency.aplwallet.apl.dex.eth.utils;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthUnit;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Convert;

import javax.enterprise.inject.Vetoed;
import java.math.BigDecimal;
import java.math.BigInteger;

@Vetoed
public class EthUtil {

    private static final String ETH_ADDRESS_PATTERN = "^0x[0-9a-f]{40}$";

    private EthUtil() {
    }

    /**
     * Generate new account with random key.
     *
     * @return EthWallet
     */
    public static Credentials generateEthCredentials() {
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);

        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials cs = Credentials.create(ecKeyPair);

        return cs;
    }

    public static BigDecimal weiToEther(BigInteger wei) {
        return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
    }

    public static Long ethToGwei(BigDecimal ether) {
        return Convert.fromWei(etherToWei(ether).toString(), Convert.Unit.GWEI).longValue();
    }

    public static BigInteger etherToWei(BigDecimal ether) {
        return Convert.toWei(ether, Convert.Unit.ETHER).toBigInteger();
    }

    public static BigInteger gweiToWei(Long gwei) {
        return convertToWei(gwei, EthUnit.GWEI);
    }

    public static BigDecimal weiToGwei(BigDecimal wei) {
        return Convert.fromWei(wei, Convert.Unit.GWEI);
    }

    public static BigDecimal gweiToEth(Long gwei) {
        return weiToEther(gweiToWei(gwei));
    }

    public static BigDecimal atmToEth(Long apl) {
        return weiToEther(gweiToWei(atmToGwei(apl)));
    }

    public static Long atmToGwei(Long apl) {
        return apl * 10;
    }

    public static Long gweiToAtm(Long gwei) {
        return gwei / 10;
    }

    public static boolean isAddressValid(String address) {
        return StringUtils.isNotBlank(address) && address.toLowerCase().matches(ETH_ADDRESS_PATTERN);
    }

    public static BigInteger convertToWei(long amount, EthUnit unit) {
        return BigInteger.valueOf(amount).multiply(unit.i);
    }

}

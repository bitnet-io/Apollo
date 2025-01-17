/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.enterprise.inject.spi.CDI;

public final class VoteWeighting {

    private static AccountService accountService;
    private static AccountAssetService accountAssetService;
    private static AccountCurrencyService accountCurrencyService;
    private static AssetService assetService;
    private static CurrencyService currencyService;
    private final VotingModel votingModel;
    private final long holdingId; //either asset id or MS coin id
    private final long minBalance;
    private final MinBalanceModel minBalanceModel;

    public VoteWeighting(byte votingModel, long holdingId, long minBalance, byte minBalanceModel) {
        this.votingModel = VotingModel.get(votingModel);
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = MinBalanceModel.get(minBalanceModel);
    }

    public VoteWeighting(VotingModel votingModel, long holdingId, long minBalance, MinBalanceModel minBalanceModel) {
        this.votingModel = votingModel;
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = minBalanceModel;
    }

    private static AccountService lookupAccountService() {
        if (accountService == null) {
            accountService = CDI.current().select(AccountService.class).get();
        }
        return accountService;
    }

    private static AccountAssetService lookupAccountAssetService() {
        if (accountAssetService == null) {
            accountAssetService = CDI.current().select(AccountAssetService.class).get();
        }
        return accountAssetService;
    }

    private static AccountCurrencyService lookupAccountCurrencyService() {
        if (accountCurrencyService == null) {
            accountCurrencyService = CDI.current().select(AccountCurrencyService.class).get();
        }
        return accountCurrencyService;
    }

    public static AssetService lookupAssetService() {
        if (assetService == null) {
            assetService = CDI.current().select(AssetService.class).get();
        }
        return assetService;
    }

    public static CurrencyService lookupCurrencyService() {
        if (currencyService == null) {
            currencyService = CDI.current().select(CurrencyService.class).get();
        }
        return currencyService;
    }

    public VotingModel getVotingModel() {
        return votingModel;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public MinBalanceModel getMinBalanceModel() {
        return minBalanceModel;
    }

    public void validateStateIndependent() throws AplException.ValidationException {
        if (votingModel == null) {
            throw new AplException.NotValidException("Invalid voting model");
        }
        if (minBalanceModel == null) {
            throw new AplException.NotValidException("Invalid min balance model");
        }
        if ((votingModel == VotingModel.ASSET || votingModel == VotingModel.CURRENCY) && holdingId == 0) {
            throw new AplException.NotValidException("No holdingId provided");
        }
        if (minBalance < 0) {
            throw new AplException.NotValidException("Invalid minBalance " + minBalance);
        }
        if (minBalance > 0) {
            if (minBalanceModel == MinBalanceModel.NONE) {
                throw new AplException.NotValidException("Invalid min balance model " + minBalanceModel);
            }
            if (votingModel.getMinBalanceModel() != MinBalanceModel.NONE && votingModel.getMinBalanceModel() != minBalanceModel) {
                throw new AplException.NotValidException("Invalid min balance model: " + minBalanceModel + " for voting model " + votingModel);
            }
            if ((minBalanceModel == MinBalanceModel.ASSET || minBalanceModel == MinBalanceModel.CURRENCY) && holdingId == 0) {
                throw new AplException.NotValidException("No holdingId provided");
            }
        }
        if (minBalance == 0 && votingModel == VotingModel.ACCOUNT && holdingId != 0) {
            throw new AplException.NotValidException("HoldingId cannot be used in by account voting with no min balance");
        }
        if ((votingModel == VotingModel.ATM || minBalanceModel == MinBalanceModel.ATM) && holdingId != 0) {
            throw new AplException.NotValidException("HoldingId cannot be used in by balance voting or with min balance in ATM");
        }
        if ((!votingModel.acceptsVotes() || votingModel == VotingModel.HASH) && (holdingId != 0 || minBalance != 0 || minBalanceModel != MinBalanceModel.NONE)) {
            throw new AplException.NotValidException("With VotingModel " + votingModel + " no holdingId, minBalance, or minBalanceModel should be specified");
        }
    }

    public void validateStateDependent() throws AplException.ValidationException {
        lookupAssetService();
        if (votingModel == VotingModel.ASSET && assetService.getAsset(holdingId) == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(holdingId) + " not found");
        }
        if (minBalance > 0) {
            if (minBalanceModel == MinBalanceModel.ASSET && assetService.getAsset(holdingId) == null) {
                throw new AplException.NotCurrentlyValidException("Invalid min balance asset: " + Long.toUnsignedString(holdingId));
            }
            if (minBalanceModel == MinBalanceModel.CURRENCY && lookupCurrencyService().getCurrency(holdingId) == null) {
                throw new AplException.NotCurrentlyValidException("Invalid min balance currency: " + Long.toUnsignedString(holdingId));
            }
        }
    }

        public boolean isBalanceIndependent() {
            return (votingModel == VotingModel.ACCOUNT && minBalance == 0) || !votingModel.acceptsVotes() || votingModel == VotingModel.HASH;
        }

        public boolean acceptsVotes() {
            return votingModel.acceptsVotes();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VoteWeighting)) {
                return false;
            }
            VoteWeighting other = (VoteWeighting) o;
            return other.votingModel == this.votingModel
                && other.minBalanceModel == this.minBalanceModel
                && other.holdingId == this.holdingId
                && other.minBalance == this.minBalance;
        }

        @Override
        public int hashCode() {
            int hashCode = 17;
            hashCode = 31 * hashCode + Long.hashCode(holdingId);
            hashCode = 31 * hashCode + Long.hashCode(minBalance);
            hashCode = 31 * hashCode + minBalanceModel.hashCode();
            hashCode = 31 * hashCode + votingModel.hashCode();
            return hashCode;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("VoteWeighting{");
            sb.append("votingModel=").append(votingModel);
            sb.append(", holdingId=").append(holdingId);
            sb.append(", minBalance=").append(minBalance);
            sb.append(", minBalanceModel=").append(minBalanceModel);
            sb.append('}');
            return sb.toString();
        }

        public enum VotingModel {
            NONE(-1) {
                @Override
                public final boolean acceptsVotes() {
                    return false;
                }

                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    throw new UnsupportedOperationException("No voting possible for VotingModel.NONE");
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.NONE;
                }
            },
            ACCOUNT(0) {
                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    return (voteWeighting.minBalance == 0 || voteWeighting.minBalanceModel.getBalance(voteWeighting, voterId, height) >= voteWeighting.minBalance) ? 1 : 0;
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.NONE;
                }
            },
            ATM(1) {
                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    long atmBalance = lookupAccountService().getAccount(voterId, height).getBalanceATM();
                    return atmBalance >= voteWeighting.minBalance ? atmBalance : 0;
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.ATM;
                }
            },
            ASSET(2) {
                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    long atuBalance = lookupAccountAssetService().getAssetBalanceATU(voterId, voteWeighting.holdingId, height);
                    return atuBalance >= voteWeighting.minBalance ? atuBalance : 0;
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.ASSET;
                }
            },
            CURRENCY(3) {
                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    long units = lookupAccountCurrencyService().getCurrencyUnits(voterId, voteWeighting.holdingId, height);
                    return units >= voteWeighting.minBalance ? units : 0;
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.CURRENCY;
                }
            },
            TRANSACTION(4) {
                @Override
                public final boolean acceptsVotes() {
                    return false;
                }

                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    throw new UnsupportedOperationException("No voting possible for VotingModel.TRANSACTION");
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.NONE;
                }
            },
            HASH(5) {
                @Override
                public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                    return 1;
                }

                @Override
                public final MinBalanceModel getMinBalanceModel() {
                    return MinBalanceModel.NONE;
                }
            };

            private final byte code;

            VotingModel(int code) {
                this.code = (byte) code;
            }

            public static VotingModel get(byte code) {
                for (VotingModel votingModel : values()) {
                    if (votingModel.getCode() == code) {
                        return votingModel;
                    }
                }
                throw new IllegalArgumentException("Invalid votingModel " + code);
            }

            public byte getCode() {
                return code;
            }

            public abstract long calcWeight(VoteWeighting voteWeighting, long voterId, int height);

            public abstract MinBalanceModel getMinBalanceModel();

            public boolean acceptsVotes() {
                return true;
            }
        }

        public enum MinBalanceModel {
            NONE(0) {
                @Override
                public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                    throw new UnsupportedOperationException();
                }
            },
            ATM(1) {
                @Override
                public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                    return lookupAccountService().getAccount(voterId, height).getBalanceATM();
                }
            },
            ASSET(2) {
                @Override
                public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                    return lookupAccountAssetService().getAssetBalanceATU(voterId, voteWeighting.holdingId, height);
                }
            },
            CURRENCY(3) {
                @Override
                public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                    return lookupAccountCurrencyService().getCurrencyUnits(voterId, voteWeighting.holdingId, height);
                }
            };

            private final byte code;

            MinBalanceModel(int code) {
                this.code = (byte) code;
            }

            public static MinBalanceModel get(byte code) {
                for (MinBalanceModel minBalanceModel : values()) {
                    if (minBalanceModel.getCode() == code) {
                        return minBalanceModel;
                    }
                }
                throw new IllegalArgumentException("Invalid minBalanceModel " + code);
            }

            public byte getCode() {
                return code;
            }

            public abstract long getBalance(VoteWeighting voteWeighting, long voterId, int height);
        }
    }

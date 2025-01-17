/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

/*
  It's IMPORTANT
       the sharp symbol is used as a statement delimiter for StringTokenizer in the DbPopulator class.
  Cause the smart contract code use the semicolon as statement delimiter.
*/

TRUNCATE TABLE smc_mapping;
#

INSERT INTO smc_mapping
(`db_id`, `address`, `entry_key`, `name`, `object`, `height`, `latest`, `deleted`)
VALUES (1, 7307657537262705518, X'8F3F13CDBC4C2A8B668BB8C0ABE09B668F851F10FA39A49535F777919086D618', 'balances',
        '1234567890', 10, TRUE, FALSE)
;
#

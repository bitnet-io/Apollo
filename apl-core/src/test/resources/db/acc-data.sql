TRUNCATE TABLE account;
TRUNCATE TABLE account_asset;
TRUNCATE TABLE account_currency;
TRUNCATE TABLE account_guaranteed_balance;
TRUNCATE TABLE account_ledger;
TRUNCATE TABLE account_property;
TRUNCATE TABLE account_lease;

ALTER TABLE shuffling AUTO_INCREMENT = 230;
ALTER TABLE shuffling_participant AUTO_INCREMENT = 1070;
ALTER TABLE shuffling_data AUTO_INCREMENT = 1000;

ALTER TABLE account AUTO_INCREMENT = 150;
ALTER TABLE account_asset AUTO_INCREMENT = 28;
ALTER TABLE account_currency AUTO_INCREMENT = 57;
ALTER TABLE account_guaranteed_balance AUTO_INCREMENT = 1695307;
ALTER TABLE account_ledger AUTO_INCREMENT = 68;
ALTER TABLE account_property AUTO_INCREMENT = 10;
ALTER TABLE account_lease AUTO_INCREMENT = 7;


INSERT INTO account
(DB_ID  ,ID  	                        ,BALANCE  	             ,UNCONFIRMED_BALANCE  	    ,HAS_CONTROL_PHASING  	,FORGED_BALANCE  	,ACTIVE_LESSEE_ID  	,HEIGHT  	 ,LATEST, DELETED) values
(1       ,1739068987193023818           ,999990000000000     ,999990000000000       ,false                  ,0                  ,null               ,0           ,true, false),
(10      ,50                            ,555500000000         ,105500000000           ,false                  ,0                  ,null               ,100000     ,true, false),
(20      ,100                           ,100000000             ,100000000               ,false                  ,0                  ,50                 ,104595     ,true, false ),
(30      ,200                           ,250000000             ,200000000               ,false                  ,0                  ,null               ,104670     ,true , false),
(40      ,7821792282123976600     ,15025000000000      ,14725000000000        ,false                  ,0                  ,null               ,105000     ,true , false),
(50      ,9211698109297098287     ,25100000000000      ,22700000000000        ,false                  ,0                  ,null               ,106000     ,true , false),
(60     ,500                            ,77182383705332315  ,77182383705332315    ,false                  ,0                  ,50                 ,141839     ,false, false),
(70     ,500                            ,77216366305332315  ,77216366305332315    ,false                  ,0                  ,50                 ,141844     ,false, false),
(80     ,500                            ,77798522705332315  ,77798522705332315    ,false                  ,0                  ,null               ,141853     ,true, false),
(90     ,600                            ,40767800000000      ,40767800000000        ,false                  ,0                  ,null               ,141855     ,false, false),
(100    ,600                            ,41167700000000      ,41167700000000        ,false                  ,0                  ,null               ,141858     ,true, false),
(110    ,700                            ,2424711969422000   ,2424711969422000     ,false                  ,1150030000000  ,null               ,141860     ,true, false),
(120    ,800                            ,2424711869422000   ,2424711869422000     ,false                  ,1150030000000  ,null               ,141862     ,false, false),
(130    ,800                            ,2424711769422000   ,2424711769422000     ,false                  ,1150030000000  ,null               ,141864     ,false, false),
(140    ,800                            ,77200915499807515  ,77200915499807515    ,false                  ,0                  ,null               ,141866     ,false, true),
(150    ,800                            ,40367900000000      ,40367900000000        ,false                  ,0                  ,null               ,141868     ,false, true)
;

INSERT INTO account_asset
(DB_ID  , ACCOUNT_ID                , ASSET_ID              , QUANTITY              , UNCONFIRMED_QUANTITY, HEIGHT  , LATEST) VALUES
(2      , 100      , 10  , 8                     , 8                     , 42716 , true),
(3      , 110       , 10  , 2                     , 2                     , 42716 , true),
(4      , 120      , 20  , 1                     , 1                     , 74579 , true),
(7      , 130      , 30  , 10000000000000        , 10000000000000        , 103547, true),
(9      , 140       , 30  , 200000000000000       , 199690000000000       , 104313, true),
(11     , 150       , 40  , 100000000             , 0                     , 106009, true),
(15     , 160      , 50   , 1000000000            , 1000000000            , 115621, true),
(16     , 170      , 50   , 1000000000            , 1000000000            , 115621, true),
(17     , 180      , 50   , 1000000000            , 1000000000            , 115621, true),
(18     , 190      , 50   , 997000000000          , 997000000000          , 115625, true),
(21     , 200      , 60   , 50000                 , 1000                  , 135786, true),
(24     , 210      , 70   , 1                     , 1                     , 141149, true),
(26     , 220       , 80   , 1                     , 1                     , 157464, true),
(27     , 220       , 90   , 1                     , 1                     , 161462, true),
(28     , 230       , 100   , 1                     , 1                     , 163942, true)
;

INSERT INTO account_currency
(DB_ID, ACCOUNT_ID, CURRENCY_ID, UNITS, UNCONFIRMED_UNITS, HEIGHT, LATEST) VALUES
( 4, 100, 10, 2000000, 2000000, 9800, true),
( 5, 110, 10, 9899999998000000, 9899999998000000, 23208, true),
(14, 120, 20, 100, 100, 99999, true),
(18, 130, 20, 100, 100, 100237, true),
(23, 140, 20, 100, 100, 101515, true),
(25, 150, 20, 9800, 9800, 101976, true),
(28, 160, 20, 10000, 10000, 103064, true),
(33, 120, 30, 25000, 25000, 104087, true),
(39, 170, 40, 10000000000, 10000000000, 107363, true),
(41, 180, 50, 10000000000, 10000000000, 107380, true),
(42, 190, 60, 100000, 100000, 109087, true),
(47, 200, 20, 19979000, 19979000, 114982, true),
(48, 210, 20, 900, 900, 114982, true),
(56, 220, 70, 2000000000, 2000000000, 124550, true),
(57, 230, 80, 2000000000, 2000000000, 124607, true)
;

INSERT INTO account_guaranteed_balance
(DB_ID,     ACCOUNT_ID,   ADDITIONS,          HEIGHT) VALUES
(1695301, 100,          27044000000,     2502007),
(1695302, 100,          157452000000,    2502014),
(1695503, 200,          900000000,          2502060),
(1695304, 100,          64604000000,        2502265),
(1695305, 300,          100000000,          2502568),
(1695306, 300,          100000000,          2502600),
(1695307, 100,          100100000000,        2502845)
;

INSERT INTO account_ledger
(DB_ID, ACCOUNT_ID, EVENT_TYPE, EVENT_ID, HOLDING_TYPE, HOLDING_ID, `CHANGE`, BALANCE, BLOCK_ID, HEIGHT, `TIMESTAMP`) VALUES
(53, 110, 3, -7204505074792164093, 1, null, 250000000000000, 250000000000000, 4994769695807437270, 827, 1054211),
(54, 110, 50, 9218185695807163289, 1, null, -200000000, 249999800000000, -6084261423926609231, 836, 1054551),
(55, 120, 1, -6084261423926609231, 1, null, 200000000, 2692000001000000000, -6084261423926609231, 836, 1054551),
(56, 130, 50, -6534531925815509026, 1, null, -100000000, 249999500000000, -8049217029686801713, 837, 1054648),
(57, 130, 3, -6534531925815509026, 1, null, -100000000, 249999400000000, -8049217029686801713, 837, 1054648),
(58, 120, 1, -8049217029686801713, 1, null, 100000000, 2692000001100000000, -8049217029686801713, 837, 1054648),
(59, 120, 3, -6534531925815509026, 1, null, 100000000, 2692000001200000000, -8049217029686801713, 837, 1054648),
(60, 110, 50, 1936998860725150465, 1, null, -100000000, 249999700000000, 5690171646526982807, 838, 1054748),
(61, 110, 3, 1936998860725150465, 1, null, -2000000000, 249997700000000, 5690171646526982807, 838, 1054748),
(62, 120, 1, 5690171646526982807, 1, null, 100000000, 2692000001300000000, 5690171646526982807, 838, 1054748),
(63, 140, 3, 1936998860725150465, 1, null, 2000000000, 2000000000, 5690171646526982807, 838, 1054748),
(64, 110, 50, -2409079077163807920, 1, null, -100000000, 249997600000000, 4583712850787255153, 840, 1054915),
(65, 120, 1, 4583712850787255153, 1, null, 100000000, 2692000001400000000, 4583712850787255153, 840, 1054915),
(66, 120, 50, -5312761317760960087, 1, null, -100000000, 2692000001300000000, 7971792663971279902, 846, 1055410),
(67, 120, 3, -5312761317760960087, 1, null, -250000000000000, 2691750001300000000, 7971792663971279902, 846, 1055410),
(68, 120, 1, 7971792663971279902, 1, null, 100000000, 2691750001400000000, 7971792663971279902, 846, 1055410);

INSERT INTO account_property
(DB_ID, ID, RECIPIENT_ID, SETTER_ID, PROPERTY, VALUE, HEIGHT, LATEST) VALUES
(1, 10, 100, null, 'email', 'dchosrova@gmail.com', 94335, true),
(2, 20, 110, null, 'apollo', '1', 106420, true),
(3, 30, 120, null, 'Para cadastrar no blockchain', '1', 108618, true),
(4, 40, 130, null, 'Account', null, 108970, true),
(5, 50, 100, 160, 'Apollo', '1', 110754, true),
(6, 60, 150, null, '##$$%%alex747ander%%$$##', null, 113510, true),
(7, 70, 160, 100, 'Hide ip', '1', 117619, true),
(8, 80, 170, null, '10', null, 128755, true),
(10, 90, 100, null, 'mine', null, 134152, true);

INSERT INTO account_lease
(DB_ID, LESSOR_ID,  CURRENT_LEASING_HEIGHT_FROM, CURRENT_LEASING_HEIGHT_TO, CURRENT_LESSEE_ID, NEXT_LEASING_HEIGHT_FROM, NEXT_LEASING_HEIGHT_TO, NEXT_LESSEE_ID, HEIGHT, LATEST) VALUES
(1,      100,        10000,                       11000,                     10,                0,                       0,                      0,              10000,  true),
(2,      110,        10000,                       11000,                     10,                0,                       0,                      0,              10000,  true),
(3,      120,        10000,                       11000,                     20,                0,                       0,                      0,              10000,  true),
(4,      130,        8000,                        10000,                     30,                0,                       0,                      0,              8000,   true),
(5,      140,        8000,                        9000,                      40,                0,                       0,                      0,              7000,   false),
(6,      140,        9440,                        12440,                      50,                0,                       0,                      0,              8000,   true),
(7,      150,        9440,                        12440,                      50,                0,                       0,                      0,              8000,   true);

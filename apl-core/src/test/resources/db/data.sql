DELETE FROM PUBLIC.UPDATE_STATUS;
DELETE FROM PUBLIC.TRANSACTION;
DELETE FROM PUBLIC.BLOCK;
DELETE FROM PUBLIC.TWO_FACTOR_AUTH;
DELETE FROM PUBLIC.ACCOUNT;
DELETE FROM PUBLIC.VERSION;
delete from PUBLIC.transaction_shard_index;
delete from public.block_index;
delete from FTL.INDEXES;
delete from public.option;
delete from public.shard;
delete from referenced_transaction;
delete from phasing_poll;
delete from phasing_poll_result;
delete from phasing_poll_voter;
delete from phasing_vote;
delete from phasing_poll_linked_transaction;
delete from PUBLIC.GENESIS_PUBLIC_KEY;
delete from PUBLIC.PUBLIC_KEY;
delete from PUBLIC.SHARD_RECOVERY;
delete from PUBLIC.TAGGED_DATA_TIMESTAMP;
delete from PUBLIC.DATA_TAG;
delete from PUBLIC.TAGGED_DATA_EXTEND;
--delete from PUBLIC.TAGGED_DATA;

INSERT INTO PUBLIC.BLOCK
(DB_ID,         ID,                HEIGHT,      VERSION,   TIMESTAMP,  PREVIOUS_BLOCK_ID,  TOTAL_AMOUNT, TOTAL_FEE,   PAYLOAD_LENGTH,   PREVIOUS_BLOCK_HASH,                                                   CUMULATIVE_DIFFICULTY,  BASE_TARGET,    NEXT_BLOCK_ID,               GENERATION_SIGNATURE,                                                   BLOCK_SIGNATURE,                                                                                                                        PAYLOAD_HASH,                                                           GENERATOR_ID,       TIMEOUT) VALUES
(1	        ,-107868771406622438   ,0	        ,-1         ,0	     , null                 ,0	    ,0	                ,0          ,X'0000000000000000000000000000000000000000000000000000000000000000'	,X'00'	                ,5124095	    , 8235640967557025109		,X'bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5'	,X'00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'	,X'0000000000000000000000000000000000000000000000000000000000000000'	, 1739068987193023818	,0   ),
(104830	    ,-468651855371775066   ,1000	 	,3	        ,9200    , 9108206803338182346 	,0	    ,100000000	        ,1255       ,X'cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282'    ,X'026543d9a8161629'    ,9331842	    ,-1868632362992335764	    ,X'002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf'	,X'e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31'    ,X'37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0'	, 9211698109297098287   ,0   ),
(105015	    ,-7242168411665692630  ,1500	 	,3	        ,13800   ,-3475222224033883190  ,0	    ,100000000	        ,1257       ,X'cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0'    ,X'026601a7a1c313ca'    ,7069966	    , 5841487969085496907		,X'fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef'	,X'978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e'    ,X'2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6'	, 9211698109297098287   ,0   ),
(1641706	,-6746699668324916965  ,2000	 	,5	        ,18400   , 2069655134915376442	,0	    ,200000000	        ,207	    ,X'3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e'	,X'02dfb5187e88edab'	,23058430050	,-3540343645446911906		,X'dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b'	,X'4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4'	,X'18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99'	, 5564664969772495473	,1   ),
(1641707	,-3540343645446911906  ,2499	 	,6	        ,22998   ,-6746699668324916965	,0	    ,0	                ,0	        ,X'1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e'	,X'02dfb518ae37f5ac'	,23058430050	, 2729391131122928659		,X'facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3'	,X'f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-902424482979450876	,4   ),
(1641708	, 2729391131122928659  ,2998	 	,6	        ,28098   ,-3540343645446911906	,0	    ,0	                ,0	        ,X'5e485346362cdece52dada076459abf88a0ae128cac6870e108257a88543f09f'	,X'02dfb518dde6fdad'	,23058430050	, 1842732555539684628		,X'1fc63f083c3a49042c43c22a7e4d92aadac95c78d06ed21b8f9f0efd7c23b2a1'	,X'8cb8795d7a320e693e64ea67b47348f2f1099c3e7163311b59135aaff1a78a00542b9d4713928c265666997a4ce63b0c07585ce04464f8dfb2253f21f91bf22e'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 4363726829568989435	,3   ),
(1641709	, 1842732555539684628  ,3500	 	,4	        ,32200   , 2729391131122928659	,0	    ,200000000	        ,207	    ,X'130cafd7c5bee025885d0c6b58b2ddaaed71d2fa48423f552eb5828a423cc94b'	,X'02dfb5190d9605ae'	,23058430050	,-5580266015477525080		,X'a042a2accbb2600530a4df46db4eba105ac73f4491923fb1c34a6b9dd2619634'	,X'ee4e2ccd12b36ade6318b47246ddcad237a153da36ab9ea2498373a4687c35072f2a9d49925520b588cb16d0e5663f3d10e3adeee97dcbbb4137470e521b347c'	,X'9a8d7e4f2e83dc49351f9c3d72fabc5ecdc75f6eccc2b90f147ff5ec7d5068b2'	, 4363726829568989435	,0   ),
(1641710	,-5580266015477525080  ,5000	 	,6	        ,46000   , 1842732555539684628	,0	    ,0	                ,0	        ,X'149dfdfc7eb39219330d620a14fb0c2f02369abbda562bc4ab068e90c3cf11a4'	,X'02dfb5193d450daf'	,23058430050	, 6438949995368593549		,X'20feb26c8c34c22d55de747e3964acb3bc864326736949876d2b0594d15e87dd'	,X'a22f758567f0bd559ce2d821399da4f9ffdc4a694057d8b37045d2a9222be405f4311938e88a0b56418cbadcbea47dadabfc16e58f74e5dcd7a975d95dc17766'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-6535098620285495989	,7   ),
(1641711	, 6438949995368593549  ,8000	 	,4	        ,73600   ,-5580266015477525080	,0	    ,200000000	        ,207	    ,X'a81547db9fe98eb224d3cdc120f7305d3b829f162beb3bf719750e0cf48dbe9d'	,X'02dfb5196cf415b0'	,23058430050	, 7551185434952726924		,X'5b1bf463f202ec0d4ab42a9634976ed47b77c462d1de25e3fea3e8eaa8add8f6'	,X'992eacb8ac3bcbb7dbdbfcb637318adab190d4843b00da8961fd36ef60718f0f5acca4662cfdcf8447cc511d5e36ab4c321c185382f3577f0106c2bfb9f80ee6'	,X'8bdf98fbc4cfcf0b66dfaa688ce7ef9063f8b1748ee238c23e8209f071cfcee7'	, 6415509874415488619	,0   ),
(1641712	, 7551185434952726924  ,10000 	    ,6	        ,92000   , 6438949995368593549	,0	    ,0	                ,0	        ,X'8db872e0e7be5b59fb68ef26d84bfeb9df04f6a5b6f701fd1c88578bfcf48a84'	,X'02dfb5199ca31db1'	,23058430050	, 8306616486060836520		,X'1435d4b603b52d04dd0f8228f36dbd6f01e627a59370fa3e6a0f58a75b372621'	,X'5a8acc3cc947b76d42fa78938ed9ece33b91c5ca0bb7a1af6c92ec525e8bb6092babf03aee10bd965123fceb5afad63969e78991d8c6b2a6b4fc79cff8fe150d'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 7160808267188566436	,6   ),
(1641713	, 8306616486060836520  ,15000 	    ,6	        ,138000  , 7551185434952726924	,0	    ,0	                ,0	        ,X'8cf9752b2533cb6849ad83b275c40f7e61b204ac023f775847a60c2f1a9d3d79'	,X'02dfb519cc5225b2'	,23058430050	,-6206981717632723220		,X'5f1382ab768b8b000637d8a59d7415fd8d4b6d4edc00ca0a08aacf9caf8c9a06'	,X'6832f74bd4abe2a4b95755ff9e989133079e5215ae2111e590ea489353ce28078d094db3db077124ac541be9f4f7f09f5a36aac83c8c151dae0f09eb378033e1'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-3985647971895643754	,9   ),
(1641714	,-6206981717632723220  ,15456 	    ,4	        ,142195  , 8306616486060836520	,0	    ,200000000	        ,207	    ,X'a8460f09af074773186c58688eb29215a81d5b0b10fc9e5fc5275b2f39fd93bb'	,X'02dfb519fc012db3'	,23058430050	,-4166853316012435358		,X'df545469ed5a9405e0ff6efcdf468e61564776568c8b227f776f24c47206af46'	,X'3d1c22000eb41599cb12dfbfaa3980353fa84cdf99145d1fcc92886551044a0c0b388c539efa48414c21251e493e468d97a2df12be24e9a33dec4521fdb6c2eb'	,X'550dfe6da8732c1977c7545675f8dc163995aaba5533306b7a1f1b9364190dd3'	, 4749500066832760520	,0   ),
(1641715	,-4166853316012435358  ,104595 	    ,6	        ,962274  ,-6206981717632723220	,0	    ,0	                ,0	        ,X'ec562889035fdca9d59d9bdca460992c01c5286278104287a989834eeffcb83e'	,X'02dfb51a2bb035b4'	,23058430050	, 433871417191886464		,X'82e59d851fdf0d01ca1ee20df906009cd66885cc63e8314ebde80dc5e38987fa'	,X'202acda4d57f2a24212d265053241a07608de29a6dd8252994cf8be197765d02a585c676aca15e7f43a57d7747173d51435d9f2820da637ca8bc9cd1e536d761'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	, 3883484057046974168	,9   )
  ;

INSERT INTO PUBLIC.TRANSACTION
(DB_ID,     ID,                      HEIGHT,      BLOCK_ID,            BLOCK_TIMESTAMP,    DEADLINE, RECIPIENT_ID,     TRANSACTION_INDEX, AMOUNT,             FEE,            FULL_HASH,                                                                     SIGNATURE,                                                                                                                                  TIMESTAMP, TYPE, SUBTYPE, SENDER_ID,                REFERENCED_TRANSACTION_FULL_HASH,                                                        PHASED, VERSION, HAS_MESSAGE, HAS_ENCRYPTED_MESSAGE, HAS_PUBLIC_KEY_ANNOUNCEMENT, EC_BLOCK_HEIGHT,   EC_BLOCK_ID,            HAS_ENCRYPTTOSELF_MESSAGE, HAS_PRUNABLE_MESSAGE, HAS_PRUNABLE_ENCRYPTED_MESSAGE, HAS_PRUNABLE_ATTACHMENT, ATTACHMENT_BYTES) VALUES
  (150      ,3444674909301056677	  ,1000	     ,-468651855371775066       ,9200	       ,1440,	null	                ,0	        ,0	                ,2500000000000	,X'a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	      ,X'375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc'	    ,35073712	    ,5	   ,0	    ,9211698109297098287	 ,X'6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	                ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14399	        ,-5416619518547901377	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 X'01056673646673035145520500667364667301ae150000000000000000000000000000ae150000000000000000000000000000000000000000000001'),
  (175      ,2402544248051582903	  ,1000	     ,-468651855371775066       ,9200	       ,1440,	null	                ,0	        ,0	                ,1000000000000	,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'	      ,X'fc6f11f396aa20717c9191a1fb25fab0681512cc976c935db1563898aabad90ffc6ced28e1b8b3383d5abb55928bbb122a674dc066ab8b0cc585b9b4cdbd8fac'	    ,35075179	    ,2	   ,0	    ,9211698109297098287	 ,X'6500000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	                ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14405	        ,-2297016555338476945	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 X'01074d5941535345540b00666466736b64666c616c73102700000000000002'),
  (200      ,5373370077664349170	  ,1500	     ,-7242168411665692630      ,13800	       ,1440,	457571885748888948	    ,0	        ,100000000000000000	,100000000	    ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	      ,X'8afd3a91d0e3011e505e0353b1f7089c0d401672f8ed5d0ddc2107e0b130aa0bdd17f03b2d75eed8fcc645cda88b5c82ac1b621c142abad9b1bb95df517aa70c'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (500      ,-780794814210884355	  ,2000	     ,-6746699668324916965      ,18400	       ,1440,	6110033502865709882	    ,1	        ,100000000000000000	,100000000	    ,X'fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558'	      ,X'240b0a1ee9f63f5c3cb914b42584da1388b9d048a981f1651ac85dd12f12660c29782100c03cbe8491bdc831aa27f6fd3a546345b3da7860c56e6ba431550517'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	                ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (1000     ,-9128485677221760321	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -603599418476309001	    ,0	        ,100000000000000000	,100000000	    ,X'bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1'	      ,X'75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (1500     ,3746857886535243786	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -693728062313138401	    ,1	        ,100000000000000000	,100000000	    ,X'0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3'	      ,X'73a84f612f5957453b502ae8aeaa31bc2add030b1b9182624c66eb94d6377000462286b22ca7fcd6e13987292858b02b0b14ac4539b97df4bd3b14303797f11b'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (2000     ,5471926494854938613	  ,3500	     , 1842732555539684628      ,32200	       ,1440,  -3934231941937607328     ,2	        ,100000000000000000	,100000000	    ,X'f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691'	      ,X'98f5fc631ea607b47bf7888eb3253e0e696f5fd4bf26d6c698a9c69e1078ab0ff7afc6e76c5b1043f6ff00ecea2fed75c83dcbac754c195f29a61a6632010a39'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'0ad8cd666583ff333fc7b055930adb2997b5fffaaa2cf86fa360fe235311e9d3'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (2500     ,2083198303623116770	  ,8000	     , 6438949995368593549      ,73600	       ,1440,  -1017037002638468431     ,0	        ,100000000000000000	,100000000	    ,X'e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f'	      ,X'd24811bc4be2c7031196fd220639f1885c8e15c96e7672146c88c2eea25d8a0cd4e93b8e2324e2522e3aff14faa1ef811fc43a971fdbdb71f7ac0b5614e706cb'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'f57fe0d22730f04b01c5a131b52099356c899b29addb0476d835ea2de5cc5691'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (3000     ,808614188720864902	      ,8000	     , 6438949995368593549      ,73600	       ,1440,  -5803127966835594607     ,1	        ,100000000000000000	,100000000	    ,X'863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6'	      ,X'38484c6128b2707a81ea6f0c9f19663dbcd54358e644d56cfa2b33635f2d570f7b91c41820f8d1923e0afca5cb0e5785c76c2fd859e354c876a9640a75882aa2'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'e2f726e4d101e91c6ee735c9da0d55af7100c45263a0a6a0920c255a0f65b44f'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (3500     ,-2262365651675616510	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	2569665864951373924	    ,0	        ,100000000000000000	,100000000	    ,X'026bd4236d769ae022df97e248c6292aef1f403f5d5dcb74d787255344cf58e5'	      ,X'1a3ecfc672df4ae91b1bcf319cee962426cd3f65fac340a0e01ac27367646904fa8ccf22f0b0c93f84d00584fa3f7f5bd03933e08b3aa1295a9ebdd09a0c1654'	    ,35078473	    ,0	   ,0	    ,9211698109297098287	 ,X'863e0c0752c6380be76354bd861be0705711e0ee2bc0b84d9f0d71b5a4271af6'                   ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (4000     ,9145605905642517648	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	2230095012677269409	    ,1	        ,100000000000000000	,100000000	    ,X'9074899d1db8eb7e807f0d841973fdc8a84ab2742a4fb03d47b620f5e920e5fe'	      ,X'6ae95b4165ef53b335ac576a72d20d24464f57bd49dbdd76dd22f519caff3d0457d97769ae76d8496906e4f1ab5f7db30db73daea5db889d80e1ac0bd4b05257'	    ,35078473	    ,0	   ,0	    ,-8315839810807014152	 ,null	                                                                                ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (4500     ,-1536976186224925700     ,15456	 ,-6206981717632723220      ,142195    	   ,1440,	null	                ,2	        ,0	                ,100000000	    ,X'fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f'	      ,X'61a224ae2d8198bfcee91c83e449d6325a2caa974f6a477ab59d0072b9b7e50793575534ab29c7be7d3dbef46f5e9e206d0bf5801bebf06847a28aa16c6419a1'	    ,16758888	    ,8	   ,1	    ,9211698109297098287	 ,X'830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088'	                ,FALSE      ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,103874	        ,1281949812948953897	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,	                X'01054c494e5558035838360002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4'),
  (5000     ,-4081443370478530685	  ,15456	 ,-6206981717632723220      ,142195        ,1440,	null	                ,3	        ,0	                ,100000000	    ,X'830fc103e1cc5bc7a3dd989ad0d7a8c66307a9ef23f05d2a18b661ee0a464088'	      ,X'551f99bc4eceaae7c7007ac077ed163f4d95f8acc0119e38b726b5c8b494cf09c5059292de17efbc4ec14848e3944ecd0a5d0ca2591177266e04d426ce25a1c1'      ,16763004	    ,8	   ,0	    ,9211698109297098287	 ,null	                                                                                ,FALSE      ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,103950	        ,3234042379296483074	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,	                X'01054c494e555805414d4436340002a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c2150002b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea291105312e302e382000a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4')
;

INSERT INTO PUBLIC.UPDATE_STATUS (transaction_id, updated) VALUES (
  -1536976186224925700, TRUE
);
INSERT INTO PUBLIC.ACCOUNT (ID, BALANCE, UNCONFIRMED_BALANCE, FORGED_BALANCE, ACTIVE_LESSEE_ID, HEIGHT,LATEST)
VALUES (100, 100000000, 100000000, 0, null,	104595, true ),--1
 (200, 250000000, 200000000, 0, null,	104670, true );--2

INSERT INTO PUBLIC.TWO_FACTOR_AUTH (account, secret, confirmed) VALUES
(100, X'a3f312570b65671a7101', true),
(200, X'f3e0475e0db85a822037', false);
INSERT into PUBLIC.BLOCK_INDEX (shard_id, block_id, block_height) VALUES
(3, 3, 30),
(1, 1, 1),
(2, 2, 2)
;
INSERT into Public.TRANSACTION_SHARD_INDEX(transaction_id, partial_transaction_hash, block_id) VALUES
(100,X'cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e' ,3),
(101,X'2270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d' ,1),
(102,X'b96d5e9f64e51c597513717691eeeeaf18a26a864034f62c' ,1),
(103,X'cca5a1f825f9b918be00f35406f70b108b6656b299755558' ,1)
;
INSERT into PUBLIC.SHARD (shard_id, shard_hash, shard_height) VALUES
(1, X'8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176', 1),
(2, X'a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d', 2),
(3, X'931A8011F4BA1CDC0BCAE807032FE18B1E4F0B634F8DA6016E421D06C7E13693', 3)
;
INSERT into PUBLIC.REFERENCED_TRANSACTION (db_id, transaction_id, referenced_transaction_id, height) VALUES
(10     , 100                    , 101                  ,100    ),
(20     , 101                    , 102                  ,200    ),
(30     , 102                    , 103                  ,300    ),
(40     , 3444674909301056677    , 100                  ,1000   ),
(50     , 2402544248051582903    , 101                  ,1000   ),
(60     , 5373370077664349170    , 2402544248051582903  ,1500   ),
(70     , -780794814210884355    , 5373370077664349170  ,2000   ),
(80     , -9128485677221760321   , -780794814210884355  ,3500   ),
(90     , 3746857886535243786    , -9128485677221760321 ,3500   ),
(100    , 5471926494854938613    , 3746857886535243786  ,3500   ),
(110    , 2083198303623116770    , 5471926494854938613  ,8000   ),
(120    , 808614188720864902	 ,  2083198303623116770 ,8000   ),
(130    , -2262365651675616510   , 808614188720864902   ,15456  ),
(140    , -1536976186224925700   , 808614188720864902   ,15456  ),
;
INSERT INTO PUBLIC.PHASING_POLL (
DB_ID  	    ,ID  	            ,ACCOUNT_ID  	    ,WHITELIST_SIZE  	,FINISH_HEIGHT  	,VOTING_MODEL  	,QUORUM  	,MIN_BALANCE  	,HOLDING_ID  	,MIN_BALANCE_MODEL  ,HASHED_SECRET  ,ALGORITHM  	,HEIGHT) VALUES
(20	    , 808614188720864902	,9211698109297098287	        ,2	        ,10000	            ,0	        ,1	        ,null	        ,null	        ,0		            ,null           ,0	            ,8000),
(30	    , 2083198303623116770	,9211698109297098287	        ,0	        ,9500	            ,0	        ,1	        ,null	        ,null	        ,0		            ,null           ,0	            ,8000),
(40	    ,-4081443370478530685	,9211698109297098287	        ,0	        ,17000	            ,4	        ,3	        ,null	        ,null	        ,0		            ,null           ,0	            ,15456),
(50	    ,-1536976186224925700	,9211698109297098287	        ,1	        ,18000	            ,0	        ,3	        ,null	        ,null	        ,0		            ,null           ,0	            ,15456),
;
INSERT INTO PUBLIC.PHASING_POLL_RESULT
(DB_ID  	,ID  	          ,RESULT  	,APPROVED  	,HEIGHT  ) VALUES
(10	    ,100                	,1	    ,TRUE	    ,300     ),
(20	    ,3444674909301056677	,1	    ,TRUE	    ,1500   ),
(30	    ,808614188720864902 	,0	    ,TRUE	    ,9000   ),
(40	    ,2083198303623116770	,0	    ,FALSE	    ,9500   ),
;
INSERT into PUBLIC.PHASING_POLL_VOTER
(DB_ID  	,TRANSACTION_ID  	,VOTER_ID  	,HEIGHT) VALUES
(20   ,808614188720864902	, 5564664969772495473 ,8000  ),
(30   ,808614188720864902   , -8315839810807014152	 ,8000 ),
(40   ,-1536976186224925700   , -8315839810807014152	 ,15456 ),
(50   ,128   , 102	 ,15457 ),
(60   ,128   , 103	 ,15457 ),
(70   ,128   , 104	 ,15457 ),
;
INSERT into PUBLIC.PHASING_VOTE
(DB_ID  	,VOTE_ID  	                ,TRANSACTION_ID  	   ,VOTER_ID  	,HEIGHT) VALUES
(30         ,-2262365651675616510       ,808614188720864902   ,5564664969772495473 ,8500),
(40         ,9145605905642517648        ,808614188720864902   ,-8315839810807014152	 ,8999),
;
--001100100
INSERT into PUBLIC.PHASING_POLL_LINKED_TRANSACTION
(DB_ID  	,TRANSACTION_ID  	    ,LINKED_FULL_HASH  	,LINKED_TRANSACTION_ID  	,HEIGHT ) VALUES
(10         ,-4081443370478530685, X'6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e', 100                   , 15456),
(20         ,-4081443370478530685, X'fc23d4474d90abeae5dd6d599381a75a2a06e61f91ff2249067a10e6515d202f', -1536976186224925700  , 15456),
(30         ,-4081443370478530685, X'5ea0de6146ac28b8b64d4f7f1ccbd1c7b2e43397221ef7ed3fa10c4ec0581d43', -5176698353372716962  , 15456),
(40         ,100                 , X'b273e15c07bf99b5139e5753946e004d663e83b3eadb4c8dea699ee982573ef0',  -5361043843063909454 , 15457),
(50         ,100                 , X'faf20df37f7466857d33ddcd841d535fb5b216e93104ec663454210827c155ed',  -8834245526153202950 , 15457),
(60         ,200                 , X'3a0e1742d06078d5fd2b9f3b90cb2ea861406f0bebfb7c74366c40506a7c9bb1',  -3064593098847351238 , 15458),
;
INSERT into version values (267);
INSERT INTO FTL.INDEXES (schema, table, columns) VALUES('PUBLIC', 'CURRENCY', 'code,name,description');
INSERT INTO FTL.INDEXES (schema, table, columns) VALUES('PUBLIC', 'TAGGED_DATA', 'NAME,DESCRIPTION,TAGS');


INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (1, -8446737619314270165, '5E8D43FF197F8B554A59007F9E6F73E10BFF4DDA9906F8389D015F31D0ABC433', 1000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (2, -8446683063809540616, '25AE5107A806E561488394ED5B59916D61C2F0110182E67A1AAE19CD6BD86D0E', 1500, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (3, -8446656647637444484, '7AD7781E9FD7EFD304E516374E816E09356DB8439D8D577DA02A5B3EC55D6274', 2000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (4, -8446582139906358620, '58F5675BDE9DC62EC712988E84F336C1859CE672A065706C2D6B53F24809073A', 3500, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (5, -8446548496587389826, '42EDD0892C113954CC9EFD503E424E79D60D2EC94356F9C65BF7EA651F3A2E6A', 5000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (6, -8446473252322443609, '1AB245BBA7D381256999A016F40FE44EC7E3FE15EF598A3F4B089B4EBC40E973', 5000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (7, -8446384352342482748, '840544113081FD01265F6881DC4BF0627FD06E3A9D9ABF1F2FC65042AC03145D', 8000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (8, -8446347883943983778, '638EA31BC014E00B858D3EAC8CB5B1BED168EA8290B4CBAE6E6DE0498ABAD557', 8000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (9, -8446281322521085217, '2308A0680A8390ABD47F3AFE616F604047DCDD5A05E4EB1877DD9332CD56A057', 10000, true);
INSERT INTO PUBLIC.GENESIS_PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (10, -8446169427786754084, 'AF787EE65F2CE7355D10B3D69390BB48BBD47B725F2EB0C786F9D9E623A1AC51', 15000, true);


INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (1, 2728325718715804811, 'A3C4BF8B2CBB8863C3E30EB4590FB22839311A95CF1FD716C211AE38C7D47B33', 1000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (2, -5554610658583124986, '50D38876803FBB5EFFC9ED968245AA64A7CD217FD26DB6A60A0330E8FEF4BF61', 1500, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (3, 7995581942006468815, 'D36B2EC098A9349902213AB19957E544AD3D388A6593C902F5DCE2CC0556B73E', 2000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (4, 4961837675843750471, 'AECFD20DAE056E71AF7273F49994602BB038B35F2CFDD14DCF770725CADF570D', 3500, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (5, -8549596671240600704, 'F3B7EF980A1207D55B723144C87C30740FCB4F13134F8758FA03DC2B8B086916', 5000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (6, -960762611966502100, '2FD89F2823B313FFA81E707014733939E97354FA0F69210262BD7EB1FC1E2D28', 5000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (7, -6151359720024440589, 'D6896785B0D0A106B75B5268919B4D361EA32E53F2C6E93C277DB567FF7DB67D', 8000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (8, -6724281675870110558, '3BF91EC2D8ABE04B4BEDB7BF09140E31BA8B4195002D093C3BF32259FEA5A513', 8000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (9, 1344527020205736624, '5C6D82CD66A3328AA9A83C2FC2EC7724EB788A81E78ED67E2A979B127B9A887D', 10000, true);
INSERT INTO PUBLIC.PUBLIC_KEY (DB_ID, ACCOUNT_ID, PUBLIC_KEY, HEIGHT, LATEST) VALUES (10, -4013722529644937202, '6DFB3D4C9A0BC930B4700DC6C49881B71F5A48F38AFEC702BD8DE8D041CC9023', 15000, true);

INSERT INTO PUBLIC.SHARD_RECOVERY (SHARD_RECOVERY_ID, STATE, COLUMN_NAME, UPDATED) VALUES (1, 'INIT', NULL, CURRENT_TIMESTAMP());


INSERT into PUBLIC.TAGGED_DATA_TIMESTAMP
(DB_ID  	,ID  	             ,  TIMESTAMP  	 , HEIGHT , LATEST ) VALUES
(10         ,-780794814210884355 , 35078473      , 2000 , TRUE),
(20         ,-9128485677221760321, 35078473      , 3500, TRUE),
(30         ,3746857886535243786,  35078473      , 3500, TRUE),
;

INSERT into PUBLIC.DATA_TAG
(DB_ID  	,TAG      , TAG_COUNT  	,HEIGHT , LATEST) VALUES
(10         ,'abc',      1         , 1500, TRUE),
(20         ,'efd',      1         , 2000, FALSE),
(30         ,'xyz' ,     2         , 3500, FALSE),
(40         ,'trw' ,     1         , 3500, TRUE),
;

--INSERT into PUBLIC.TAGGED_DATA
--(DB_ID  	,ID  	             , ACCOUNT_ID  	        , NAME  ,      description  ,       data      ,  is_text   ,  block_timestamp ,  transaction_timestamp , HEIGHT ) VALUES
--(10         ,-780794814210884355 , 9211698109297098287  , 'tag1'  , 'tag1 descr'    ,   X'c11dd7986e'  ,   TRUE    ,          18400    ,        35078473        ,   2000 ),
--(20         ,-9128485677221760321, 9211698109297098287  , 'tag2'  , 'tag2 descr'    ,   X'c11d86986e'  ,   TRUE    ,          32200    ,        35078473        ,   3500 ),
--(30         ,3746857886535243786 , 9211698109297098287  , 'tag3'  , 'tag3 descr'    ,   X'c11d8344588e' ,   FALSE  ,          32200    ,      35078473        ,   3500 ),
--(40         ,2083198303623116770 , 9211698109297098287  , 'tag4'  , 'tag4 descr'    ,   X'c11d1234589e' ,   TRUE   ,          73600    ,      35078473        ,   3500),
--(50         ,808614188720864902 ,  9211698109297098287  , 'tag5'  , 'tag5 descr'    ,   X'c11d1234586e' ,   FALSE  ,          73600    ,      35078473        ,   8000),
--;

INSERT into PUBLIC.TAGGED_DATA_EXTEND
(DB_ID  	,ID  	             , EXTEND_ID  	,HEIGHT  , LATEST) VALUES
(10         ,-780794814210884355 ,   1          , 2000, FALSE),
(20         ,-9128485677221760321,   2          , 3500, FALSE),
(30         ,3746857886535243786 ,   3          , 3500, FALSE),
(40         ,2083198303623116770 ,   4          , 3500, FALSE),
(50         ,808614188720864902 ,  2083198303623116770 , 8000, TRUE),
;

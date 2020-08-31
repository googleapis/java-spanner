# Changelog

## [1.60.0](https://www.github.com/googleapis/java-spanner/compare/v1.59.0...v1.60.0) (2020-08-18)


### Features

* adds clirr check on pre-commit hook ([#388](https://www.github.com/googleapis/java-spanner/issues/388)) ([bd5c93f](https://www.github.com/googleapis/java-spanner/commit/bd5c93f045e06372b2235f3d350bade93bff2c24))
* include SQL statement in error message ([#355](https://www.github.com/googleapis/java-spanner/issues/355)) ([cc5ac48](https://www.github.com/googleapis/java-spanner/commit/cc5ac48232b6e4550b98d213c5877d6ec37b293f))


### Bug Fixes

* enables emulator tests ([#380](https://www.github.com/googleapis/java-spanner/issues/380)) ([f61c6d0](https://www.github.com/googleapis/java-spanner/commit/f61c6d0d332f15826499996a292acc7cbab267a7))
* remove custom timeout and retry settings ([#365](https://www.github.com/googleapis/java-spanner/issues/365)) ([f6afd21](https://www.github.com/googleapis/java-spanner/commit/f6afd213430d3f06d9a72c64a5c37172840fed0e))
* remove unused kokoro files ([#367](https://www.github.com/googleapis/java-spanner/issues/367)) ([6125c7d](https://www.github.com/googleapis/java-spanner/commit/6125c7d221c77f4c42497b72107627ee09312813))
* retry pdml transaction on EOS internal error ([#360](https://www.github.com/googleapis/java-spanner/issues/360)) ([a53d736](https://www.github.com/googleapis/java-spanner/commit/a53d7369bb2a8640ab42e409632b352decbdbf5e))
* sets the project for the integration tests ([#386](https://www.github.com/googleapis/java-spanner/issues/386)) ([c8fa458](https://www.github.com/googleapis/java-spanner/commit/c8fa458f5369a09c780ee38ecc09bd2562e8f987))


### Dependencies

* stop auto updates of commons-lang3 ([#362](https://www.github.com/googleapis/java-spanner/issues/362)) ([8f07ed6](https://www.github.com/googleapis/java-spanner/commit/8f07ed6b44f9c70f56b9ee2e4505c40385337ca7))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.8.6 ([#374](https://www.github.com/googleapis/java-spanner/issues/374)) ([6f47b8a](https://www.github.com/googleapis/java-spanner/commit/6f47b8a759643f772230df0c2e153338d44f70ce))
* update dependency org.openjdk.jmh:jmh-core to v1.24 ([#375](https://www.github.com/googleapis/java-spanner/issues/375)) ([94f568c](https://www.github.com/googleapis/java-spanner/commit/94f568cf731ba22cac7f0d898d7776a3cc2c178f))
* update dependency org.openjdk.jmh:jmh-core to v1.25 ([#382](https://www.github.com/googleapis/java-spanner/issues/382)) ([ec7888e](https://www.github.com/googleapis/java-spanner/commit/ec7888e1d62cf800bf6ad166d242e89443ddc7aa))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.25 ([#376](https://www.github.com/googleapis/java-spanner/issues/376)) ([8ffdc48](https://www.github.com/googleapis/java-spanner/commit/8ffdc481e15901f78eac592bd8d4bef33ac3378a))

## [1.59.0](https://www.github.com/googleapis/java-spanner/compare/v1.58.0...v1.59.0) (2020-07-16)


### Features

* add support for NUMERIC data type ([#193](https://www.github.com/googleapis/java-spanner/issues/193)) ([b38a91d](https://www.github.com/googleapis/java-spanner/commit/b38a91d8daac264b9dea327d6b31430d9599bd78))
* spanner NUMERIC type ([#349](https://www.github.com/googleapis/java-spanner/issues/349)) ([78c3192](https://www.github.com/googleapis/java-spanner/commit/78c3192266c474fc43277a8bf3f15caa968a0100))


### Bug Fixes

* check if emulator is running if env var is set ([#340](https://www.github.com/googleapis/java-spanner/issues/340)) ([597f501](https://www.github.com/googleapis/java-spanner/commit/597f501803e6d58717a6e3770e6fd3f34454e9a5))
* fix potential unnecessary transaction retry ([#337](https://www.github.com/googleapis/java-spanner/issues/337)) ([1a4f4fd](https://www.github.com/googleapis/java-spanner/commit/1a4f4fd675a1580c87ad1d53c650a20bd2ff4811)), closes [#327](https://www.github.com/googleapis/java-spanner/issues/327)
* respect PDML timeout when using streaming RPC ([#338](https://www.github.com/googleapis/java-spanner/issues/338)) ([d67f108](https://www.github.com/googleapis/java-spanner/commit/d67f108e86925c1296e695db8e78fa82e11fa4fa))
* runs sample tests in java 8 and java 11 ([#345](https://www.github.com/googleapis/java-spanner/issues/345)) ([b547e31](https://www.github.com/googleapis/java-spanner/commit/b547e31d095be3cf1646e0e9c07bfc467ecc3c22))
* set gRPC keep-alive to 120 seconds ([#339](https://www.github.com/googleapis/java-spanner/issues/339)) ([26be103](https://www.github.com/googleapis/java-spanner/commit/26be103da1117c4940550fad1672c66e6edfbdb3))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.8.3 ([#334](https://www.github.com/googleapis/java-spanner/issues/334)) ([45acd89](https://www.github.com/googleapis/java-spanner/commit/45acd8960c961d48e91a7b1546efa64d9e9ae576))
* update shared config to 0.9.2 ([#328](https://www.github.com/googleapis/java-spanner/issues/328)) ([75df62c](https://www.github.com/googleapis/java-spanner/commit/75df62c0176137fda1d0a9076b83be06f11228ce))

## [1.58.0](https://www.github.com/googleapis/java-spanner/compare/v1.57.0...v1.58.0) (2020-07-07)


### Features

* add async api ([#81](https://www.github.com/googleapis/java-spanner/issues/81)) ([462839b](https://www.github.com/googleapis/java-spanner/commit/462839b625e58e235581b8ba10b398e1d222eaaf))
* support setting compression option ([#192](https://www.github.com/googleapis/java-spanner/issues/192)) ([965e95e](https://www.github.com/googleapis/java-spanner/commit/965e95e70ccd9c62abd6513b0011aab136e48e26))


### Bug Fixes

* set default values for streaming retry ([#316](https://www.github.com/googleapis/java-spanner/issues/316)) ([543373b](https://www.github.com/googleapis/java-spanner/commit/543373b22336be72b10026fda9f0b55939ab94b4))


### Performance Improvements

* use streaming RPC for PDML ([#287](https://www.github.com/googleapis/java-spanner/issues/287)) ([df47c13](https://www.github.com/googleapis/java-spanner/commit/df47c13a4c00bdf5e6eafa01bbb64c12a96d7fb8))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.8.2 ([#315](https://www.github.com/googleapis/java-spanner/issues/315)) ([3d6fb9f](https://www.github.com/googleapis/java-spanner/commit/3d6fb9fd7dc6b2b5b2ff9935228701ac795c9167))

## [1.57.0](https://www.github.com/googleapis/java-spanner/compare/v1.56.0...v1.57.0) (2020-06-29)


### Features

* **deps:** adopt flatten plugin and google-cloud-shared-dependencies and update ExecutorProvider ([#302](https://www.github.com/googleapis/java-spanner/issues/302)) ([5aef6c3](https://www.github.com/googleapis/java-spanner/commit/5aef6c3f6d3e9564cb8728ad51718feb6b64475a))

## [1.56.0](https://www.github.com/googleapis/java-spanner/compare/v1.55.1...v1.56.0) (2020-06-17)


### Features

* add num_sessions_in_pool metric ([#128](https://www.github.com/googleapis/java-spanner/issues/128)) ([3a7a8ad](https://www.github.com/googleapis/java-spanner/commit/3a7a8ad79f1de3371d32a1298406990cb7bbf5be))


### Bug Fixes

* backend now supports optimizer version for DML ([#252](https://www.github.com/googleapis/java-spanner/issues/252)) ([24b986b](https://www.github.com/googleapis/java-spanner/commit/24b986b03a785f4c5ee978dcdc57f51687701e52))
* include an explicit version for javax-annotations-api ([#261](https://www.github.com/googleapis/java-spanner/issues/261)) ([e256d22](https://www.github.com/googleapis/java-spanner/commit/e256d22f33d5f091ea90ed81c0b0f8600beae96c))
* inconsistent json and yaml spanner configs ([#238](https://www.github.com/googleapis/java-spanner/issues/238)) ([627fdc1](https://www.github.com/googleapis/java-spanner/commit/627fdc13d64ab7b51934d4866ff753f7b08dabe4))
* test allowed a too old staleness ([#214](https://www.github.com/googleapis/java-spanner/issues/214)) ([f4fa6bf](https://www.github.com/googleapis/java-spanner/commit/f4fa6bfca4bb821cbda426c4cb7bf32f091a2913))
* use millis to prevent rounding errors ([#260](https://www.github.com/googleapis/java-spanner/issues/260)) ([22ed458](https://www.github.com/googleapis/java-spanner/commit/22ed45816098f5e50104935b66bc55297ea7f7b7))


### Dependencies

* include test-jar in bom ([#253](https://www.github.com/googleapis/java-spanner/issues/253)) ([4e86a37](https://www.github.com/googleapis/java-spanner/commit/4e86a374aacbcfc34d64809b7d9606f21176f6b9))
* update dependency org.json:json to v20200518 ([#239](https://www.github.com/googleapis/java-spanner/issues/239)) ([e3d7921](https://www.github.com/googleapis/java-spanner/commit/e3d79214ac4d6e72992acdddb7ddeb2148b1ae15))

### [1.55.1](https://www.github.com/googleapis/java-spanner/compare/v1.55.0...v1.55.1) (2020-05-21)


### Bug Fixes

* PDML retry settings were not applied for aborted tx ([#232](https://www.github.com/googleapis/java-spanner/issues/232)) ([308a465](https://www.github.com/googleapis/java-spanner/commit/308a465c768ba6e641c95d8c6efd214637266f50)), closes [#199](https://www.github.com/googleapis/java-spanner/issues/199)
* remove the need for any env var in all tests ([#235](https://www.github.com/googleapis/java-spanner/issues/235)) ([374fb40](https://www.github.com/googleapis/java-spanner/commit/374fb403306612330db58dfa5549205394a08e67))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.4.0 ([#224](https://www.github.com/googleapis/java-spanner/issues/224)) ([2cf04aa](https://www.github.com/googleapis/java-spanner/commit/2cf04aad7edc68baf5c296bda11f66c140abf669))

## [1.55.0](https://www.github.com/googleapis/java-spanner/compare/v1.54.0...v1.55.0) (2020-05-19)


### Features

* mark when a Spanner client is closed ([#198](https://www.github.com/googleapis/java-spanner/issues/198)) ([50cb174](https://www.github.com/googleapis/java-spanner/commit/50cb1744e7ede611758d3ff63b3df77a1d3682eb))


### Bug Fixes

* make it possible to override backups methods ([#195](https://www.github.com/googleapis/java-spanner/issues/195)) ([2d19c25](https://www.github.com/googleapis/java-spanner/commit/2d19c25ba32847d116194565e67e1b1276fcb9f8))
* Partitioned DML timeout was not always respected ([#203](https://www.github.com/googleapis/java-spanner/issues/203)) ([13cb37e](https://www.github.com/googleapis/java-spanner/commit/13cb37e55ddfd1ff4ec22b1dcdc20c4832eee444)), closes [#199](https://www.github.com/googleapis/java-spanner/issues/199)
* partitionedDml stub was not closed ([#213](https://www.github.com/googleapis/java-spanner/issues/213)) ([a2d9a33](https://www.github.com/googleapis/java-spanner/commit/a2d9a33fa31f7467fc2bfbef5a29c4b3f5aea7c8))
* reuse clientId for invalidated databases ([#206](https://www.github.com/googleapis/java-spanner/issues/206)) ([7b4490d](https://www.github.com/googleapis/java-spanner/commit/7b4490dfb61fbc81b5bd6be6c9a663b36b5ce402))
* use nanos to prevent truncation errors ([#204](https://www.github.com/googleapis/java-spanner/issues/204)) ([a608460](https://www.github.com/googleapis/java-spanner/commit/a60846043dc0ca47e1970d8ab99380b6d725c7a9)), closes [#200](https://www.github.com/googleapis/java-spanner/issues/200)


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.3.1 ([#190](https://www.github.com/googleapis/java-spanner/issues/190)) ([ad41a0d](https://www.github.com/googleapis/java-spanner/commit/ad41a0d4b0cc6a2c0ae0611c767652f64cfb2fb7))

## [1.54.0](https://www.github.com/googleapis/java-spanner/compare/v1.53.0...v1.54.0) (2020-05-05)


### Features

* **deps:** import shared-dependencies bom and use maven-flatten-plugin ([#172](https://www.github.com/googleapis/java-spanner/issues/172)) ([060a81a](https://www.github.com/googleapis/java-spanner/commit/060a81ac938ef644aefd8c90d026018107742141))


### Bug Fixes

* create filter in correct order ([#180](https://www.github.com/googleapis/java-spanner/issues/180)) ([d80428a](https://www.github.com/googleapis/java-spanner/commit/d80428a5b0291516b2298e2309de09b23e4c387d))
* remove error message checking ([#183](https://www.github.com/googleapis/java-spanner/issues/183)) ([b477322](https://www.github.com/googleapis/java-spanner/commit/b4773223dbeb682c2c8fa9c0a9dea31001dd94d6)), closes [#175](https://www.github.com/googleapis/java-spanner/issues/175)
* set resource type for database parameter of Backup ([#174](https://www.github.com/googleapis/java-spanner/issues/174)) ([bb4d7cf](https://www.github.com/googleapis/java-spanner/commit/bb4d7cf4a363cf4980e22be97d2b5e4267368a7d))
* stop preparing session on most errors ([#181](https://www.github.com/googleapis/java-spanner/issues/181)) ([d0e3d41](https://www.github.com/googleapis/java-spanner/commit/d0e3d41131a7480baee787654b7b9591efae5069)), closes [#177](https://www.github.com/googleapis/java-spanner/issues/177)

## [1.53.0](https://www.github.com/googleapis/java-spanner/compare/v1.52.0...v1.53.0) (2020-04-22)


### Features

* optimize maintainer to let sessions be GC'ed instead of deleted ([#135](https://www.github.com/googleapis/java-spanner/issues/135)) ([d65747c](https://www.github.com/googleapis/java-spanner/commit/d65747cbc704508f6f1bcef6eea53aa411d42ee2))


### Bug Fixes

* assign unique id's per test case ([#129](https://www.github.com/googleapis/java-spanner/issues/129)) ([a553b6d](https://www.github.com/googleapis/java-spanner/commit/a553b6d48c4f5ee2d0583e5b825d73a85f06216e))
* check for not null input for Id classes ([#159](https://www.github.com/googleapis/java-spanner/issues/159)) ([ecf5826](https://www.github.com/googleapis/java-spanner/commit/ecf582670818f32e85f534ec400d0b8d31cf9ca6)), closes [#145](https://www.github.com/googleapis/java-spanner/issues/145)
* clean up test instance if creation failed ([#162](https://www.github.com/googleapis/java-spanner/issues/162)) ([ff571e1](https://www.github.com/googleapis/java-spanner/commit/ff571e16a45fbce692d9bb172749ff15fafe7a9c))
* fix flaky test and remove warnings ([#153](https://www.github.com/googleapis/java-spanner/issues/153)) ([d534e35](https://www.github.com/googleapis/java-spanner/commit/d534e350346b0c9ab8057ede36bc3aac473c0b06)), closes [#146](https://www.github.com/googleapis/java-spanner/issues/146)
* increase test timeout and remove warnings ([#160](https://www.github.com/googleapis/java-spanner/issues/160)) ([63a6bd8](https://www.github.com/googleapis/java-spanner/commit/63a6bd8be08a56d002f58bc2cdb2856ad0dc5fa3)), closes [#158](https://www.github.com/googleapis/java-spanner/issues/158)
* retry non-idempotent long-running RPCs ([#141](https://www.github.com/googleapis/java-spanner/issues/141)) ([4669c02](https://www.github.com/googleapis/java-spanner/commit/4669c02a24e0f7b1d53c9edf5ab7b146b4116960))
* retry restore if blocked by pending restore ([#119](https://www.github.com/googleapis/java-spanner/issues/119)) ([220653d](https://www.github.com/googleapis/java-spanner/commit/220653d8e25c518d0df447bf777a7fcbf04a01ca)), closes [#118](https://www.github.com/googleapis/java-spanner/issues/118)
* StatementParser did not accept multiple query hints ([#170](https://www.github.com/googleapis/java-spanner/issues/170)) ([ef41a6e](https://www.github.com/googleapis/java-spanner/commit/ef41a6e503f218c00c16914aa9c1433d9b26db13)), closes [#163](https://www.github.com/googleapis/java-spanner/issues/163)
* wait for initialization to finish before test ([#161](https://www.github.com/googleapis/java-spanner/issues/161)) ([fe434ff](https://www.github.com/googleapis/java-spanner/commit/fe434ff7068b4b618e70379c224e1c5ab88f6ba1)), closes [#146](https://www.github.com/googleapis/java-spanner/issues/146)


### Performance Improvements

* increase sessions in the pool in batches ([#134](https://www.github.com/googleapis/java-spanner/issues/134)) ([9e5a1cd](https://www.github.com/googleapis/java-spanner/commit/9e5a1cdaacf71147b67681861f063c3276705f44))
* prepare sessions with r/w tx in-process ([#152](https://www.github.com/googleapis/java-spanner/issues/152)) ([2db27ce](https://www.github.com/googleapis/java-spanner/commit/2db27ce048efafaa3c28b097de33518747011465)), closes [#151](https://www.github.com/googleapis/java-spanner/issues/151)


### Dependencies

* update core dependencies ([#109](https://www.github.com/googleapis/java-spanner/issues/109)) ([5753f1f](https://www.github.com/googleapis/java-spanner/commit/5753f1f4fed83df87262404f7a7ba7eedcd366cb))
* update core dependencies ([#132](https://www.github.com/googleapis/java-spanner/issues/132)) ([77c1558](https://www.github.com/googleapis/java-spanner/commit/77c1558652ee00e529674ac3a2dcf3210ef049fa))
* update dependency com.google.api:api-common to v1.9.0 ([#127](https://www.github.com/googleapis/java-spanner/issues/127)) ([b2c744f](https://www.github.com/googleapis/java-spanner/commit/b2c744f01a4d5a8981df5ff900f3536c83265a61))
* update dependency com.google.guava:guava-bom to v29 ([#147](https://www.github.com/googleapis/java-spanner/issues/147)) ([3fe3ae0](https://www.github.com/googleapis/java-spanner/commit/3fe3ae02376af552564c93c766f562d6454b7ac1))
* update dependency io.grpc:grpc-bom to v1.29.0 ([#164](https://www.github.com/googleapis/java-spanner/issues/164)) ([2d2ce5c](https://www.github.com/googleapis/java-spanner/commit/2d2ce5ce4dc8f410ec671e542e144d47f39ab40b))
* update dependency org.threeten:threetenbp to v1.4.3 ([#120](https://www.github.com/googleapis/java-spanner/issues/120)) ([49d1abc](https://www.github.com/googleapis/java-spanner/commit/49d1abcb6c9c48762dcf0fe1466ab107bf67146b))

## [1.52.0](https://www.github.com/googleapis/java-spanner/compare/v1.51.0...v1.52.0) (2020-03-20)


### Features

* add backup support ([#100](https://www.github.com/googleapis/java-spanner/issues/100)) ([ed3874a](https://www.github.com/googleapis/java-spanner/commit/ed3874afcf55fe7381354e03dab3a3b97d7eb520))
* add Backups protos and APIs ([#97](https://www.github.com/googleapis/java-spanner/issues/97)) ([5643c22](https://www.github.com/googleapis/java-spanner/commit/5643c22a4531dac75b9fac5b128eb714a27920a0))


### Bug Fixes

* add client id to metrics to avoid collisions ([#117](https://www.github.com/googleapis/java-spanner/issues/117)) ([338e136](https://www.github.com/googleapis/java-spanner/commit/338e136508edc6745f9371e8a5d66638021bc8d7)), closes [#106](https://www.github.com/googleapis/java-spanner/issues/106)
* ignore added interface methods for generated code ([#101](https://www.github.com/googleapis/java-spanner/issues/101)) ([402cfa1](https://www.github.com/googleapis/java-spanner/commit/402cfa1e1e2994f7bb1b783cf823021b54fb175e)), closes [#99](https://www.github.com/googleapis/java-spanner/issues/99)
* use grpc 1.27.2 to prevent version conflicts ([#105](https://www.github.com/googleapis/java-spanner/issues/105)) ([37b7c88](https://www.github.com/googleapis/java-spanner/commit/37b7c8859e5f35d85bd14ef72662614fd185c020))


### Dependencies

* update core dependencies ([#94](https://www.github.com/googleapis/java-spanner/issues/94)) ([f3ca4c9](https://www.github.com/googleapis/java-spanner/commit/f3ca4c99c3d54f64c5eda11e4a4c076140fdbc6a))
* update opencensus.version to v0.26.0 ([#116](https://www.github.com/googleapis/java-spanner/issues/116)) ([1b8db0b](https://www.github.com/googleapis/java-spanner/commit/1b8db0b407429e02bb1e4c9af839afeed21dac5d))

## [1.51.0](https://www.github.com/googleapis/java-spanner/compare/v1.50.0...v1.51.0) (2020-03-13)


### Features

* add backend query options ([#90](https://www.github.com/googleapis/java-spanner/issues/90)) ([e96e172](https://www.github.com/googleapis/java-spanner/commit/e96e17246bee9691171b46857806d03d1f8e19b4))
* add QueryOptions proto ([#84](https://www.github.com/googleapis/java-spanner/issues/84)) ([eb8fc37](https://www.github.com/googleapis/java-spanner/commit/eb8fc375bbd766f25966aa565e266ed972bbe818))


### Bug Fixes

* never use credentials in combination with plain text ([#98](https://www.github.com/googleapis/java-spanner/issues/98)) ([7eb8d49](https://www.github.com/googleapis/java-spanner/commit/7eb8d49cd6c35d7f757cb89009ad16be601b77c3))


### Dependencies

* update dependency com.google.cloud:google-cloud-core-bom to v1.93.1 ([#91](https://www.github.com/googleapis/java-spanner/issues/91)) ([29d8db8](https://www.github.com/googleapis/java-spanner/commit/29d8db8cfc9d12824b9264d0fb870049a58a9a03))
* update dependency io.opencensus:opencensus-api to v0.25.0 ([#95](https://www.github.com/googleapis/java-spanner/issues/95)) ([57f5fd0](https://www.github.com/googleapis/java-spanner/commit/57f5fd0f3bee4b437f48b6a08ab3174f035c8cca))

## [1.50.0](https://www.github.com/googleapis/java-spanner/compare/v1.49.2...v1.50.0) (2020-02-28)


### Features

* add metrics to capture acquired and released sessions data ([#67](https://www.github.com/googleapis/java-spanner/issues/67)) ([94d0557](https://www.github.com/googleapis/java-spanner/commit/94d05575c37c7c7c7e9d7d3fbaea46c6d2eb6a4d))
* add session timeout metric ([#65](https://www.github.com/googleapis/java-spanner/issues/65)) ([8d84b53](https://www.github.com/googleapis/java-spanner/commit/8d84b53efd2d237e193b68bc36345d338b0cdf20))
* instrument Spanner client with OpenCensus metrics ([#54](https://www.github.com/googleapis/java-spanner/issues/54)) ([d9a00a8](https://www.github.com/googleapis/java-spanner/commit/d9a00a81c454ae793f9687d0e2de2bcc58d96502))


### Bug Fixes

* multiple calls to end of span ([#75](https://www.github.com/googleapis/java-spanner/issues/75)) ([3f32f51](https://www.github.com/googleapis/java-spanner/commit/3f32f51d70ceacbea02439c0f48ad057b10fb570))


### Dependencies

* update core dependencies ([#87](https://www.github.com/googleapis/java-spanner/issues/87)) ([b096651](https://www.github.com/googleapis/java-spanner/commit/b096651ddde940de9929600b31f78f965939139d))
* update dependency com.google.cloud:google-cloud-core-bom to v1.92.5 ([56742c9](https://www.github.com/googleapis/java-spanner/commit/56742c96ff30f444e18a8bbde94ca173123385be))
* update dependency com.google.http-client:google-http-client-bom to v1.34.2 ([#88](https://www.github.com/googleapis/java-spanner/issues/88)) ([628093d](https://www.github.com/googleapis/java-spanner/commit/628093d97877b912f6e4e706d22c2c24ba77a808))
* update dependency com.google.protobuf:protobuf-bom to v3.11.4 ([#77](https://www.github.com/googleapis/java-spanner/issues/77)) ([fb2c683](https://www.github.com/googleapis/java-spanner/commit/fb2c683cf195e7229fe3d61a3332c32298be2625))
* update dependency io.grpc:grpc-bom to v1.27.1 ([054b7e7](https://www.github.com/googleapis/java-spanner/commit/054b7e7091af6b61c7d2ad203688a65bcb18ed0c))
* update opencensus.version to v0.25.0 ([#70](https://www.github.com/googleapis/java-spanner/issues/70)) ([26a3eff](https://www.github.com/googleapis/java-spanner/commit/26a3eff44c7d1f36541440aa7d29fc1d3ae8a4d7))


### Documentation

* **regen:** update sample code to set total timeout, add API client header test ([#66](https://www.github.com/googleapis/java-spanner/issues/66)) ([1178958](https://www.github.com/googleapis/java-spanner/commit/1178958eaec5aa6ea80938ad91dfb0b1a688463d))

### [1.49.2](https://www.github.com/googleapis/java-spanner/compare/v1.49.1...v1.49.2) (2020-02-06)


### Bug Fixes

* stop sending RPCs on InstanceNotFound ([#61](https://www.github.com/googleapis/java-spanner/issues/61)) ([7618ac8](https://www.github.com/googleapis/java-spanner/commit/7618ac8bc32f7d3482bd4a0850be2bce71c33fc3)), closes [#60](https://www.github.com/googleapis/java-spanner/issues/60)
* use default retry settings for aborted tx ([#48](https://www.github.com/googleapis/java-spanner/issues/48)) ([6709552](https://www.github.com/googleapis/java-spanner/commit/6709552653f344537c209eef7f1e9e037a38e849))
* use resource type to identify type of error ([#57](https://www.github.com/googleapis/java-spanner/issues/57)) ([89c3e77](https://www.github.com/googleapis/java-spanner/commit/89c3e77b99b303576c83b2313fc54d8c0e075e18))
* use streaming retry settings for ResumableStreamIterator ([#49](https://www.github.com/googleapis/java-spanner/issues/49)) ([63b33e9](https://www.github.com/googleapis/java-spanner/commit/63b33e93e17303fe8f1fae01cfe44427178baf6c))


### Dependencies

* update core dependencies ([#59](https://www.github.com/googleapis/java-spanner/issues/59)) ([74b6b98](https://www.github.com/googleapis/java-spanner/commit/74b6b983ec275280572a5dcc49ececc94c4a4dce))

### [1.49.1](https://www.github.com/googleapis/java-spanner/compare/v1.49.0...v1.49.1) (2020-01-24)


### Bug Fixes

* stop sending RPCs to deleted database ([#34](https://www.github.com/googleapis/java-spanner/issues/34)) ([11e4a90](https://www.github.com/googleapis/java-spanner/commit/11e4a90e73af8a5baf9aa593daa6192520363398)), closes [#16](https://www.github.com/googleapis/java-spanner/issues/16)


### Performance Improvements

* close sessions async ([#24](https://www.github.com/googleapis/java-spanner/issues/24)) ([ab25087](https://www.github.com/googleapis/java-spanner/commit/ab250871cae51b3f496719d579db5bb6e263d5c3)), closes [#19](https://www.github.com/googleapis/java-spanner/issues/19)
* close sessions async revert revert ([#46](https://www.github.com/googleapis/java-spanner/issues/46)) ([c9864e5](https://www.github.com/googleapis/java-spanner/commit/c9864e58b14bb428e443bf958e7596a94199f629)), closes [#24](https://www.github.com/googleapis/java-spanner/issues/24) [#43](https://www.github.com/googleapis/java-spanner/issues/43) [#24](https://www.github.com/googleapis/java-spanner/issues/24)


### Reverts

* Revert "perf: close sessions async (#24)" (#43) ([809ed88](https://www.github.com/googleapis/java-spanner/commit/809ed8875d65362ef14d27c5382dfe4c1ad9aa1b)), closes [#24](https://www.github.com/googleapis/java-spanner/issues/24) [#43](https://www.github.com/googleapis/java-spanner/issues/43)

## [1.49.0](https://www.github.com/googleapis/java-spanner/compare/v1.48.0...v1.49.0) (2020-01-16)


### Features

* add support for CallCredentials ([#26](https://www.github.com/googleapis/java-spanner/issues/26)) ([1112357](https://www.github.com/googleapis/java-spanner/commit/1112357be1c5fb9c4abfba48989fe8217853876a)), closes [#18](https://www.github.com/googleapis/java-spanner/issues/18)


### Bug Fixes

* add keepalives to GRPC channel ([#11](https://www.github.com/googleapis/java-spanner/issues/11)) ([428a4a6](https://www.github.com/googleapis/java-spanner/commit/428a4a6d3c9e1536a80f1fa9f76f36fe1062a104))


### Dependencies

* mockito scope should be test ([#29](https://www.github.com/googleapis/java-spanner/issues/29)) ([9b0733d](https://www.github.com/googleapis/java-spanner/commit/9b0733d927237d8d16f507a1d0129ddb638df55a))
* update dependency com.google.truth:truth to v1.0.1 ([#35](https://www.github.com/googleapis/java-spanner/issues/35)) ([fa2b471](https://www.github.com/googleapis/java-spanner/commit/fa2b471884c3b805fd6aa56a38d7c1f98c4cb940))
* update dependency org.threeten:threetenbp to v1.4.1 ([c22c831](https://www.github.com/googleapis/java-spanner/commit/c22c831473dd0b18b71e1ea4d000cd34555a3a48))

## [1.48.0](https://www.github.com/googleapis/java-spanner/compare/1.47.0...v1.48.0) (2020-01-10)


### Features

* add public method to get gRPC status code ([#25](https://www.github.com/googleapis/java-spanner/issues/25)) ([2dbe3cf](https://www.github.com/googleapis/java-spanner/commit/2dbe3cf397357de09d24bb57e367bbe947e682f4)), closes [#14](https://www.github.com/googleapis/java-spanner/issues/14)
* make repo releasable, add parent/bom ([#4](https://www.github.com/googleapis/java-spanner/issues/4)) ([f0073ee](https://www.github.com/googleapis/java-spanner/commit/f0073ee8d0aa68161f3071e6a72af376a1db1731))


### Dependencies

* update dependency org.jacoco:jacoco-maven-plugin to v0.8.5 ([#7023](https://www.github.com/googleapis/java-spanner/issues/7023)) ([d8b6438](https://www.github.com/googleapis/java-spanner/commit/d8b6438aa3b881c1c9baff584a74813664be4df8))

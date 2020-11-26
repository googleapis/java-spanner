# Changelog

### [3.0.5](https://www.github.com/googleapis/java-spanner/compare/v3.0.4...v3.0.5) (2020-11-19)


### Bug Fixes

* delete stale sample databases ([#622](https://www.github.com/googleapis/java-spanner/issues/622)) ([7584baa](https://www.github.com/googleapis/java-spanner/commit/7584baa8b7051764f1055ddb1616069e7d591b64))
* does not generate codeowners ([#631](https://www.github.com/googleapis/java-spanner/issues/631)) ([9e133a9](https://www.github.com/googleapis/java-spanner/commit/9e133a972f648ee804f324bbf55163849cb478b8))
* query could hang transaction if ResultSet#next() is not called ([#643](https://www.github.com/googleapis/java-spanner/issues/643)) ([48f92e3](https://www.github.com/googleapis/java-spanner/commit/48f92e3d1b26644bde62a8d864cec96c3c71687d)), closes [#641](https://www.github.com/googleapis/java-spanner/issues/641)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.8 ([#644](https://www.github.com/googleapis/java-spanner/issues/644)) ([447a99b](https://www.github.com/googleapis/java-spanner/commit/447a99b9a6ccdfd3855505fca13e849fb9513943))

### [3.0.4](https://www.github.com/googleapis/java-spanner/compare/v3.0.3...v3.0.4) (2020-11-17)


### Reverts

* Revert "fix: skip failing backup tests for now" (#634) ([b22cd7d](https://www.github.com/googleapis/java-spanner/commit/b22cd7dfc377a0445534946af29500cee316e6b1)), closes [#634](https://www.github.com/googleapis/java-spanner/issues/634)

### [3.0.3](https://www.github.com/googleapis/java-spanner/compare/v3.0.2...v3.0.3) (2020-11-16)


### Dependencies

* update dependency org.json:json to v20201115 ([#624](https://www.github.com/googleapis/java-spanner/issues/624)) ([60e31d1](https://www.github.com/googleapis/java-spanner/commit/60e31d1947b6930ec030e1f3170dfbde62833b96))

### [3.0.2](https://www.github.com/googleapis/java-spanner/compare/v3.0.1...v3.0.2) (2020-11-13)


### Bug Fixes

* adds api spanner team as samples code owners ([#610](https://www.github.com/googleapis/java-spanner/issues/610)) ([35cc56c](https://www.github.com/googleapis/java-spanner/commit/35cc56c375615b26f522b7342916fd30ce826c2d))
* make enums in the Connection API public ([#579](https://www.github.com/googleapis/java-spanner/issues/579)) ([19b1629](https://www.github.com/googleapis/java-spanner/commit/19b1629450a8956b810e27e5d6ab8532dec75267)), closes [#253](https://www.github.com/googleapis/java-spanner/issues/253)
* session retry could cause infinite wait ([#616](https://www.github.com/googleapis/java-spanner/issues/616)) ([8a66d84](https://www.github.com/googleapis/java-spanner/commit/8a66d84edbdaeba6b021d962a9b1984a3d2f40df)), closes [#605](https://www.github.com/googleapis/java-spanner/issues/605)
* updates project / instance for samples tests ([#613](https://www.github.com/googleapis/java-spanner/issues/613)) ([2589e7d](https://www.github.com/googleapis/java-spanner/commit/2589e7d6f400a7b050c21f46a4ab1662baa1cdb7))


### Documentation

* add descriptions for connection URL properties ([#609](https://www.github.com/googleapis/java-spanner/issues/609)) ([34221d7](https://www.github.com/googleapis/java-spanner/commit/34221d7a889c131fb1f797a0f9434deee60d755b))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.7 ([#573](https://www.github.com/googleapis/java-spanner/issues/573)) ([5135e50](https://www.github.com/googleapis/java-spanner/commit/5135e50d21417ca9514b47bd1f7eaf3d2d1417ca))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.14.1 ([#567](https://www.github.com/googleapis/java-spanner/issues/567)) ([2e9c133](https://www.github.com/googleapis/java-spanner/commit/2e9c13346423a2e1e2798bec14a1dc8799203235))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.15.0 ([#614](https://www.github.com/googleapis/java-spanner/issues/614)) ([3fa7910](https://www.github.com/googleapis/java-spanner/commit/3fa7910c8e5089cff1c9ed645f160a9e0ddfc351))
* update dependency com.google.cloud:google-cloud-trace to v1.2.6 ([#574](https://www.github.com/googleapis/java-spanner/issues/574)) ([efabe0f](https://www.github.com/googleapis/java-spanner/commit/efabe0f44a5ec92ac07be3c3e964396b613099d1))

### [3.0.1](https://www.github.com/googleapis/java-spanner/compare/v3.0.0...v3.0.1) (2020-10-28)


### Bug Fixes

* adds assembly descriptor to snippets samples ([#559](https://www.github.com/googleapis/java-spanner/issues/559)) ([d4ae85c](https://www.github.com/googleapis/java-spanner/commit/d4ae85c91c2bda3f46cab8c9f7a4033ddd639c94))
* always delete all backups from an owned test instance ([#557](https://www.github.com/googleapis/java-spanner/issues/557)) ([ff571b0](https://www.github.com/googleapis/java-spanner/commit/ff571b01b9dffdda44a9bd322e04ff04b5b5c57a)), closes [#542](https://www.github.com/googleapis/java-spanner/issues/542)
* fixes the code of conduct document ([#541](https://www.github.com/googleapis/java-spanner/issues/541)) ([7b9d1db](https://www.github.com/googleapis/java-spanner/commit/7b9d1db28b7037d6b18df88f00b9213f2f6dab80))
* SessionNotFound was not retried for AsyncTransactionManager ([#552](https://www.github.com/googleapis/java-spanner/issues/552)) ([5969f83](https://www.github.com/googleapis/java-spanner/commit/5969f8313a4df6ece63ee8f14df98cbc8511f026))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.13.0 ([#521](https://www.github.com/googleapis/java-spanner/issues/521)) ([0f4c017](https://www.github.com/googleapis/java-spanner/commit/0f4c017f112478ffc7dd15b0b234a9c48cd55a6e))

## [3.0.0](https://www.github.com/googleapis/java-spanner/compare/v2.0.2...v3.0.0) (2020-10-23)


### ⚠ BREAKING CHANGES

* initialize should be protected (#536)
* async connection API (#392)

### Features

* adds options to the write operations ([#531](https://www.github.com/googleapis/java-spanner/issues/531)) ([659719d](https://www.github.com/googleapis/java-spanner/commit/659719deb5a18a87859bc174f5bde1e1147834d8))
* async connection API ([#392](https://www.github.com/googleapis/java-spanner/issues/392)) ([3dd0675](https://www.github.com/googleapis/java-spanner/commit/3dd0675d2d7882d40a6af1e12fda3b4617019870)), closes [#378](https://www.github.com/googleapis/java-spanner/issues/378)
* inline begin transaction ([#325](https://www.github.com/googleapis/java-spanner/issues/325)) ([d08d3de](https://www.github.com/googleapis/java-spanner/commit/d08d3debb6457548bb6b04335b7a2d2227369211)), closes [#515](https://www.github.com/googleapis/java-spanner/issues/515)


### Bug Fixes

* AsyncTransactionManager did not propagate statement errors ([#516](https://www.github.com/googleapis/java-spanner/issues/516)) ([4b8b845](https://www.github.com/googleapis/java-spanner/commit/4b8b8452589d63f6768b971a880a19bde80a9671)), closes [#514](https://www.github.com/googleapis/java-spanner/issues/514)
* AsyncTransactionManager should rollback on close ([#505](https://www.github.com/googleapis/java-spanner/issues/505)) ([c580df8](https://www.github.com/googleapis/java-spanner/commit/c580df8e1175bde293890c2a68e8816951c068d3)), closes [#504](https://www.github.com/googleapis/java-spanner/issues/504)
* close executor when closing pool ([#501](https://www.github.com/googleapis/java-spanner/issues/501)) ([2086746](https://www.github.com/googleapis/java-spanner/commit/208674632b20b37f51b828c1c4cc76c91154952b))
* fixes javadocs for Key ([#532](https://www.github.com/googleapis/java-spanner/issues/532)) ([768c19d](https://www.github.com/googleapis/java-spanner/commit/768c19dc1b9985f7823ec1e4ca92491936062f3b))
* fixes sample tests ([ed0665c](https://www.github.com/googleapis/java-spanner/commit/ed0665c71abbce57a28cb79531783145eccab1fb))
* ignores failing backup operations ([2ad0b7f](https://www.github.com/googleapis/java-spanner/commit/2ad0b7fc6d1369795702484181ee11ecf59a1f8b))
* increase visibility of #get() ([#486](https://www.github.com/googleapis/java-spanner/issues/486)) ([fa6d964](https://www.github.com/googleapis/java-spanner/commit/fa6d9641b7b2a5bb1d00de6b99b0f8bc157245d6))
* initialize should be protected ([#536](https://www.github.com/googleapis/java-spanner/issues/536)) ([5c4c8c5](https://www.github.com/googleapis/java-spanner/commit/5c4c8c58674490ba524b678b409b8b19184af02f))
* remove dependency on commons-lang ([#494](https://www.github.com/googleapis/java-spanner/issues/494)) ([c99294b](https://www.github.com/googleapis/java-spanner/commit/c99294beb43ce1bd67cc3d12e4104641efab6710))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2 ([#498](https://www.github.com/googleapis/java-spanner/issues/498)) ([3ab7348](https://www.github.com/googleapis/java-spanner/commit/3ab7348781e56384921d8287a5b5c0725dfed221))
* update dependency com.google.cloud:google-cloud-monitoring to v2.0.5 ([#525](https://www.github.com/googleapis/java-spanner/issues/525)) ([fb874ec](https://www.github.com/googleapis/java-spanner/commit/fb874ec2e1738d569d585d30825a6e9d3de96c66))
* update dependency com.google.cloud:google-cloud-monitoring to v2.0.6 ([#540](https://www.github.com/googleapis/java-spanner/issues/540)) ([ce3bed6](https://www.github.com/googleapis/java-spanner/commit/ce3bed6f5359224c37502331a9f776e29632d3a5))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.10.2 ([#500](https://www.github.com/googleapis/java-spanner/issues/500)) ([eb59929](https://www.github.com/googleapis/java-spanner/commit/eb5992949de326326a6bb02ec75b4a2a65a37b84))
* update dependency com.google.cloud:google-cloud-trace to v1.2.3 ([#496](https://www.github.com/googleapis/java-spanner/issues/496)) ([0595a80](https://www.github.com/googleapis/java-spanner/commit/0595a80d5a6bb09e62ce1b6d101a3a039896c7af))
* update dependency com.google.cloud:google-cloud-trace to v1.2.4 ([#526](https://www.github.com/googleapis/java-spanner/issues/526)) ([1020989](https://www.github.com/googleapis/java-spanner/commit/1020989e1ec1ad7f5185579da58d7a839167f05a))
* update dependency com.google.cloud:google-cloud-trace to v1.2.5 ([#539](https://www.github.com/googleapis/java-spanner/issues/539)) ([eddd6ad](https://www.github.com/googleapis/java-spanner/commit/eddd6ad4e5093ee21290b85f15fa432d071bae59))
* update dependency org.openjdk.jmh:jmh-core to v1.26 ([#506](https://www.github.com/googleapis/java-spanner/issues/506)) ([0f13c4c](https://www.github.com/googleapis/java-spanner/commit/0f13c4c5db37a736e391c002ed2456d78d04a090))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.26 ([#507](https://www.github.com/googleapis/java-spanner/issues/507)) ([600f397](https://www.github.com/googleapis/java-spanner/commit/600f397a37f1808eb387fa3c31be0be5bb076c77))
* update opencensus.version to v0.27.1 ([#497](https://www.github.com/googleapis/java-spanner/issues/497)) ([62fa39a](https://www.github.com/googleapis/java-spanner/commit/62fa39a2fbac6aa667073f16898e6861f0f5ec21))
* update opencensus.version to v0.28.1 ([#533](https://www.github.com/googleapis/java-spanner/issues/533)) ([777f5fc](https://www.github.com/googleapis/java-spanner/commit/777f5fc486de7a54801c9f3f82adca561388ebfe))
* update opencensus.version to v0.28.2 ([#538](https://www.github.com/googleapis/java-spanner/issues/538)) ([e1843ef](https://www.github.com/googleapis/java-spanner/commit/e1843ef38580fecb1f017330f3fa1447028607c7))

### [2.0.2](https://www.github.com/googleapis/java-spanner/compare/v2.0.1...v2.0.2) (2020-10-02)


### Bug Fixes

* improve numeric range checks ([#424](https://www.github.com/googleapis/java-spanner/issues/424)) ([9f26785](https://www.github.com/googleapis/java-spanner/commit/9f2678568be77e82c14632b1c7ffcaafb71e7679))
* ResultSet#close() should not throw exceptions from session creation ([#487](https://www.github.com/googleapis/java-spanner/issues/487)) ([60fb986](https://www.github.com/googleapis/java-spanner/commit/60fb986f8b758a65e20c5315faf85fc0a935d0cc))
* skip failing backup tests for now ([#463](https://www.github.com/googleapis/java-spanner/issues/463)) ([f037f2d](https://www.github.com/googleapis/java-spanner/commit/f037f2d28096cd173ba338a966fd16babe8c697e))
* use credentials key in pool ([#430](https://www.github.com/googleapis/java-spanner/issues/430)) ([28103fb](https://www.github.com/googleapis/java-spanner/commit/28103fb2d6e293d20399ecdfd680be67d9d62a1c))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.10.0 ([#453](https://www.github.com/googleapis/java-spanner/issues/453)) ([e05ee0e](https://www.github.com/googleapis/java-spanner/commit/e05ee0eaa16984393b60fc47f94412e560c36ff1))

### [2.0.1](https://www.github.com/googleapis/java-spanner/compare/v2.0.0...v2.0.1) (2020-09-18)


### Bug Fixes

* do not close delegate rs in callback runnable ([#425](https://www.github.com/googleapis/java-spanner/issues/425)) ([dce3ee7](https://www.github.com/googleapis/java-spanner/commit/dce3ee79664cc528415db08b3268d719ea720ded))
* re-adds method used in internal testing ([#438](https://www.github.com/googleapis/java-spanner/issues/438)) ([c36e41b](https://www.github.com/googleapis/java-spanner/commit/c36e41bfaaf8026d2f6601ed12bfaa0d7a4ea802))

## [2.0.0](https://www.github.com/googleapis/java-spanner/compare/v1.61.0...v2.0.0) (2020-09-16)


### ⚠ BREAKING CHANGES

* Remove Guava ImmutableList from API surface ([#411](https://www.github.com/googleapis/java-spanner/issues/411)) ([b35304e](https://www.github.com/googleapis/java-spanner/commit/b35304ede5c980c3c042b89247058cc5a4ab1488))

### Features

* add lazy initializer ([#423](https://www.github.com/googleapis/java-spanner/issues/423)) ([e8522b9](https://www.github.com/googleapis/java-spanner/commit/e8522b9955c4a19fa7d6297fd463e9d2521dff92))


### Bug Fixes

* fix aborted handling of batchUpdateAsync ([#421](https://www.github.com/googleapis/java-spanner/issues/421)) ([6154008](https://www.github.com/googleapis/java-spanner/commit/61540085c971d7885e4938b486e051a1ed9cf35f))
* uses old version of gax-grpc method ([#426](https://www.github.com/googleapis/java-spanner/issues/426)) ([fe6dc79](https://www.github.com/googleapis/java-spanner/commit/fe6dc796db6aa4c28832457ca54e6952a4b51c7e))


### Miscellaneous Chores

* ensure next release is major ([#428](https://www.github.com/googleapis/java-spanner/issues/428)) ([bdae120](https://www.github.com/googleapis/java-spanner/commit/bdae120fff807df760e7be2b34a559dc995adf7e))

## [1.61.0](https://www.github.com/googleapis/java-spanner/compare/v1.60.0...v1.61.0) (2020-09-09)


### Features

* Add experimental DirectPath support ([#396](https://www.github.com/googleapis/java-spanner/issues/396)) ([46264d1](https://www.github.com/googleapis/java-spanner/commit/46264d11529accde7b520638264732937b2feb03))
* support setting timeout per RPC ([#379](https://www.github.com/googleapis/java-spanner/issues/379)) ([5d115d4](https://www.github.com/googleapis/java-spanner/commit/5d115d49b988b3fc1c59ae41ee53d7c5a83b4d11)), closes [#378](https://www.github.com/googleapis/java-spanner/issues/378)


### Bug Fixes

* iterate over async result set in sync ([#416](https://www.github.com/googleapis/java-spanner/issues/416)) ([45d8419](https://www.github.com/googleapis/java-spanner/commit/45d8419250c904b2f785d6cc5abacf098e5781de))
* remove potential infinite loop in administrative requests ([#398](https://www.github.com/googleapis/java-spanner/issues/398)) ([81d2c76](https://www.github.com/googleapis/java-spanner/commit/81d2c7634edd30efd428846fdbc468aee5406ed5))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.9.0 ([#409](https://www.github.com/googleapis/java-spanner/issues/409)) ([ae43165](https://www.github.com/googleapis/java-spanner/commit/ae43165ba736e17b780ce128d97b9757039275c2))
* update dependency org.openjdk.jmh:jmh-core to v1.25.1 ([#399](https://www.github.com/googleapis/java-spanner/issues/399)) ([52fc363](https://www.github.com/googleapis/java-spanner/commit/52fc3638854116ab87b7e6bdd719134d3108229d))
* update dependency org.openjdk.jmh:jmh-core to v1.25.2 ([#412](https://www.github.com/googleapis/java-spanner/issues/412)) ([86d18cd](https://www.github.com/googleapis/java-spanner/commit/86d18cdcc2d3aa0771e3f331ebb50591ce811113))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.25.2 ([#400](https://www.github.com/googleapis/java-spanner/issues/400)) ([8a40a96](https://www.github.com/googleapis/java-spanner/commit/8a40a96123831ce992d18ecff6e699dbb7ffc82c))


### Documentation

* updates bom and spanner version in readme ([#415](https://www.github.com/googleapis/java-spanner/issues/415)) ([def7fdf](https://www.github.com/googleapis/java-spanner/commit/def7fdf9b11fc0f8e7bacd6be41875b6542f64d5))

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

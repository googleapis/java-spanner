# Changelog

## [6.8.0](https://www.github.com/googleapis/java-spanner/compare/v6.7.0...v6.8.0) (2021-06-29)


### Features

* add gRPC-GCP channel pool as an option ([#1227](https://www.github.com/googleapis/java-spanner/issues/1227)) ([1fa95a9](https://www.github.com/googleapis/java-spanner/commit/1fa95a9993ea8c7a5f943ab39eced4ced4cb87e7))
* spanner JSON type ([#1260](https://www.github.com/googleapis/java-spanner/issues/1260)) ([b2a56c6](https://www.github.com/googleapis/java-spanner/commit/b2a56c68695b6209e20f9f86d83d7c5a0f39c7a8))


### Bug Fixes

* Add `shopt -s nullglob` to dependencies script ([#1256](https://www.github.com/googleapis/java-spanner/issues/1256)) ([d1712f7](https://www.github.com/googleapis/java-spanner/commit/d1712f7c51752c2359045e5eabac8fc0530a2421))

## [6.7.0](https://www.github.com/googleapis/java-spanner/compare/v6.6.1...v6.7.0) (2021-06-21)


### Features

* add support for instance processing units ([#665](https://www.github.com/googleapis/java-spanner/issues/665)) ([9c1c8e9](https://www.github.com/googleapis/java-spanner/commit/9c1c8e90b0e02e26ea3c16def49bb7e07c2b04b1))
* **spanner:** add processing_units to Instance resource ([#1248](https://www.github.com/googleapis/java-spanner/issues/1248)) ([e3c7e8f](https://www.github.com/googleapis/java-spanner/commit/e3c7e8fbdfb5d41a1c418f176679bf5b19f22f83))


### Bug Fixes

* Update dependencies.sh to not break on mac ([#1249](https://www.github.com/googleapis/java-spanner/issues/1249)) ([1e1df84](https://www.github.com/googleapis/java-spanner/commit/1e1df84e74011fb2b665e94b428cfa78102de7fe))

### [6.6.1](https://www.github.com/googleapis/java-spanner/compare/v6.6.0...v6.6.1) (2021-06-10)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.3.3 ([#1241](https://www.github.com/googleapis/java-spanner/issues/1241)) ([9816b3f](https://www.github.com/googleapis/java-spanner/commit/9816b3fe90419486e94a4927f368c8cecfaac424))

## [6.6.0](https://www.github.com/googleapis/java-spanner/compare/v6.5.0...v6.6.0) (2021-06-07)


### Features

* adds query optimizer statistics support ([#385](https://www.github.com/googleapis/java-spanner/issues/385)) ([e294532](https://www.github.com/googleapis/java-spanner/commit/e2945324783bc6d5a7a323578e8dbf00969f3163))
* support encoded credentials in connection URL ([#1223](https://www.github.com/googleapis/java-spanner/issues/1223)) ([43d5d7e](https://www.github.com/googleapis/java-spanner/commit/43d5d7e8d7fc1b0304a6fcf940846fe269fd661a))


### Documentation

* document retry settings in sample ([#1214](https://www.github.com/googleapis/java-spanner/issues/1214)) ([ab4592d](https://www.github.com/googleapis/java-spanner/commit/ab4592d6f5040d0125b2848369c516d01fd38106))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.3.0 ([#1225](https://www.github.com/googleapis/java-spanner/issues/1225)) ([2023839](https://www.github.com/googleapis/java-spanner/commit/2023839cce80de0ff6451a4b6274f5da9b18416f))
* update dependency com.google.cloud:google-cloud-monitoring to v2.3.2 ([#1229](https://www.github.com/googleapis/java-spanner/issues/1229)) ([8a23ad0](https://www.github.com/googleapis/java-spanner/commit/8a23ad047ec7fc4a8a5c8d6292678e579c323eb2))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v1.3.0 ([#1230](https://www.github.com/googleapis/java-spanner/issues/1230)) ([db64451](https://www.github.com/googleapis/java-spanner/commit/db6445133de143391dbd9da6d3393b0d2736971a))
* update dependency com.google.cloud:google-cloud-trace to v1.4.0 ([#1226](https://www.github.com/googleapis/java-spanner/issues/1226)) ([da4407a](https://www.github.com/googleapis/java-spanner/commit/da4407a60fb2917d1ea8043b57bdff41263af241))
* update dependency com.google.cloud:google-cloud-trace to v1.4.1 ([#1231](https://www.github.com/googleapis/java-spanner/issues/1231)) ([76af3ac](https://www.github.com/googleapis/java-spanner/commit/76af3ace6d6745673006cc1a529d66a74513c615))
* update dependency org.openjdk.jmh:jmh-core to v1.32 ([#1221](https://www.github.com/googleapis/java-spanner/issues/1221)) ([b009c9b](https://www.github.com/googleapis/java-spanner/commit/b009c9b09a9200a674b629cc74a479f8b746e727))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.32 ([#1222](https://www.github.com/googleapis/java-spanner/issues/1222)) ([7ef76a9](https://www.github.com/googleapis/java-spanner/commit/7ef76a910defd6f9cd24191de4eb0c523a294fea))

## [6.5.0](https://www.github.com/googleapis/java-spanner/compare/v6.4.4...v6.5.0) (2021-05-25)


### Features

* add `gcf-owl-bot[bot]` to `ignoreAuthors` ([#1196](https://www.github.com/googleapis/java-spanner/issues/1196)) ([4f6e18d](https://www.github.com/googleapis/java-spanner/commit/4f6e18d9c8afab0acf1b66e2b32a0907008d4ff5))
* add bufferAsync methods ([#1145](https://www.github.com/googleapis/java-spanner/issues/1145)) ([7d6816f](https://www.github.com/googleapis/java-spanner/commit/7d6816f1fd14bcd2c7f91d814855b5d921ba970d))


### Bug Fixes

* stop invoking callback after pausing and cancelling result set ([#1192](https://www.github.com/googleapis/java-spanner/issues/1192)) ([78e6784](https://www.github.com/googleapis/java-spanner/commit/78e678448782d5d16ba43ec7c10ab85b89059d88)), closes [#1191](https://www.github.com/googleapis/java-spanner/issues/1191)


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v1.2.0 ([#1194](https://www.github.com/googleapis/java-spanner/issues/1194)) ([9935066](https://www.github.com/googleapis/java-spanner/commit/99350663fb638d913e803b139d89be597be9ce1d))

### [6.4.4](https://www.github.com/googleapis/java-spanner/compare/v6.4.3...v6.4.4) (2021-05-17)


### Bug Fixes

* re-adds test verifyStatementsInFile ([#1181](https://www.github.com/googleapis/java-spanner/issues/1181)) ([7a715b4](https://www.github.com/googleapis/java-spanner/commit/7a715b429ba2a9561d24ba66404142bdc9de5a4f))

### [6.4.3](https://www.github.com/googleapis/java-spanner/compare/v6.4.2...v6.4.3) (2021-05-16)


### Bug Fixes

* re-adds test utility method for connection ([#1178](https://www.github.com/googleapis/java-spanner/issues/1178)) ([0e0dcb7](https://www.github.com/googleapis/java-spanner/commit/0e0dcb7cdc412e54c26d5e8f0176ac1917fa4c59))

### [6.4.2](https://www.github.com/googleapis/java-spanner/compare/v6.4.1...v6.4.2) (2021-05-14)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.2.3 ([#1170](https://www.github.com/googleapis/java-spanner/issues/1170)) ([3bb6885](https://www.github.com/googleapis/java-spanner/commit/3bb688519774d2865701c6ffea5687513a8c7776))
* update dependency com.google.cloud:google-cloud-trace to v1.3.4 ([#1171](https://www.github.com/googleapis/java-spanner/issues/1171)) ([6faa310](https://www.github.com/googleapis/java-spanner/commit/6faa310a5c7f035c39eeaa65eb73584f535a4aeb))

### [6.4.1](https://www.github.com/googleapis/java-spanner/compare/v6.4.0...v6.4.1) (2021-05-13)


### Documentation

* close Spanner instance when it is no longer needed ([#1116](https://www.github.com/googleapis/java-spanner/issues/1116)) ([85bd0cf](https://www.github.com/googleapis/java-spanner/commit/85bd0cf11eab7b2ec47a082a4c2c0c4d9cea01d4))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.2.2 ([#1158](https://www.github.com/googleapis/java-spanner/issues/1158)) ([63eed2e](https://www.github.com/googleapis/java-spanner/commit/63eed2e66fb063358e8b123ba5f919663b70bbe4))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v1.1.0 ([#1152](https://www.github.com/googleapis/java-spanner/issues/1152)) ([2e7f18a](https://www.github.com/googleapis/java-spanner/commit/2e7f18a52ef2ed5de6a87169eeefd570844a4c55))
* update dependency org.openjdk.jmh:jmh-core to v1.30 ([#1137](https://www.github.com/googleapis/java-spanner/issues/1137)) ([699a426](https://www.github.com/googleapis/java-spanner/commit/699a4260e3b1a4cf53fc690910aeeadac293e469))
* update dependency org.openjdk.jmh:jmh-core to v1.31 ([#1160](https://www.github.com/googleapis/java-spanner/issues/1160)) ([43a0fb9](https://www.github.com/googleapis/java-spanner/commit/43a0fb97352d928e16ec5138ed2ea494ebaae343))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.30 ([#1138](https://www.github.com/googleapis/java-spanner/issues/1138)) ([ad6649d](https://www.github.com/googleapis/java-spanner/commit/ad6649df03a1a193dd524a84fe9dc1a72ed14e09))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.31 ([#1161](https://www.github.com/googleapis/java-spanner/issues/1161)) ([4d17da2](https://www.github.com/googleapis/java-spanner/commit/4d17da25977dde0cc1032192045d9ee26d3fae09))

## [6.4.0](https://www.github.com/googleapis/java-spanner/compare/v6.3.3...v6.4.0) (2021-04-29)


### Features

* adds getValue to ResultSet ([#1073](https://www.github.com/googleapis/java-spanner/issues/1073)) ([7792c90](https://www.github.com/googleapis/java-spanner/commit/7792c9085a6e4ce1fb9fe2f8df4279f30539d87e))


### Bug Fixes

* allow using case-insensitive user-agent key ([#1110](https://www.github.com/googleapis/java-spanner/issues/1110)) ([f4f9e43](https://www.github.com/googleapis/java-spanner/commit/f4f9e43ce102788b81c032df8da223108e484252))
* check for timeout in connection after last statement finished ([#1086](https://www.github.com/googleapis/java-spanner/issues/1086)) ([aec0b54](https://www.github.com/googleapis/java-spanner/commit/aec0b541672d66fe0c34816b1c1b5a6bdeffccd1)), closes [#1077](https://www.github.com/googleapis/java-spanner/issues/1077)
* check for timeout in connection after last statement finished ([#1086](https://www.github.com/googleapis/java-spanner/issues/1086)) ([51d753c](https://www.github.com/googleapis/java-spanner/commit/51d753c507e7248132eb5d6ea2c4b735542eda49)), closes [#1077](https://www.github.com/googleapis/java-spanner/issues/1077)
* do not keep references to invalidated clients ([#1093](https://www.github.com/googleapis/java-spanner/issues/1093)) ([b4595a6](https://www.github.com/googleapis/java-spanner/commit/b4595a6b52417c716f8e70563bb5a7ef05067707)), closes [#1089](https://www.github.com/googleapis/java-spanner/issues/1089)
* prevent potential NullPointerException in Struct with Array field that contains null elements ([#1107](https://www.github.com/googleapis/java-spanner/issues/1107)) ([c414abb](https://www.github.com/googleapis/java-spanner/commit/c414abb9ec59f8200ba20e08846e442321de76bd)), closes [#1106](https://www.github.com/googleapis/java-spanner/issues/1106)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.2.1 ([#1104](https://www.github.com/googleapis/java-spanner/issues/1104)) ([37ca990](https://www.github.com/googleapis/java-spanner/commit/37ca9905bb150d1791e70103e002261e40261b05))
* update dependency com.google.cloud:google-cloud-trace to v1.3.3 ([#1103](https://www.github.com/googleapis/java-spanner/issues/1103)) ([b4327c0](https://www.github.com/googleapis/java-spanner/commit/b4327c0666bb97d1d591b5ce65a6ecdc51f5a49d))


### Documentation

* fix javadoc for Date type ([#1102](https://www.github.com/googleapis/java-spanner/issues/1102)) ([ce095f7](https://www.github.com/googleapis/java-spanner/commit/ce095f7b0c196e03ea248eeb9c5060f4f430d8c4))
* use default timeout for restore operation ([#1109](https://www.github.com/googleapis/java-spanner/issues/1109)) ([3f3c13e](https://www.github.com/googleapis/java-spanner/commit/3f3c13e7fcbf08b8ab6f0d11d7451b3ae86c9500)), closes [#1019](https://www.github.com/googleapis/java-spanner/issues/1019)

### [6.3.3](https://www.github.com/googleapis/java-spanner/compare/v6.3.2...v6.3.3) (2021-04-24)


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v1 ([#1095](https://www.github.com/googleapis/java-spanner/issues/1095)) ([a21e0bb](https://www.github.com/googleapis/java-spanner/commit/a21e0bbafad086f29d3c719b9e4a7690c1cac129))

### [6.3.2](https://www.github.com/googleapis/java-spanner/compare/v6.3.1...v6.3.2) (2021-04-20)


### Dependencies

* update dependency com.google.cloud:google-cloud-trace to v1.3.2 ([#1081](https://www.github.com/googleapis/java-spanner/issues/1081)) ([e145c95](https://www.github.com/googleapis/java-spanner/commit/e145c9531d70af6c11be9f682fb52708d0dcb569))

### [6.3.1](https://www.github.com/googleapis/java-spanner/compare/v6.3.0...v6.3.1) (2021-04-20)


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.21.1 ([#1074](https://www.github.com/googleapis/java-spanner/issues/1074)) ([ccd8cd1](https://www.github.com/googleapis/java-spanner/commit/ccd8cd1fb96c9d2046cc9c3ec4f35d8e45ebb5f5))

## [6.3.0](https://www.github.com/googleapis/java-spanner/compare/v6.2.1...v6.3.0) (2021-04-19)


### Features

* async work as functional interface ([#1068](https://www.github.com/googleapis/java-spanner/issues/1068)) ([734fb60](https://www.github.com/googleapis/java-spanner/commit/734fb6095819bde94ea482b02a8e77983f2a5449))
* **spanner:** add `progress` field to `UpdateDatabaseDdlMetadata` ([#1063](https://www.github.com/googleapis/java-spanner/issues/1063)) ([7992342](https://www.github.com/googleapis/java-spanner/commit/7992342bffc273ad8249e7564ae9ef51764bf83c))
* transaction callable as functional interface ([#1066](https://www.github.com/googleapis/java-spanner/issues/1066)) ([b036a77](https://www.github.com/googleapis/java-spanner/commit/b036a77196886f16d2738e70f676ccc99a52874c))


### Bug Fixes

* release scripts from issuing overlapping phases ([#1064](https://www.github.com/googleapis/java-spanner/issues/1064)) ([2f6fe5e](https://www.github.com/googleapis/java-spanner/commit/2f6fe5e87cc4c9ae26a6f2867411004a8c2b39fe))


### Dependencies

* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.29 ([#1014](https://www.github.com/googleapis/java-spanner/issues/1014)) ([81ee9b0](https://www.github.com/googleapis/java-spanner/commit/81ee9b02d5846f6569f588d3b17da4faf2f2dae9))

### [6.2.1](https://www.github.com/googleapis/java-spanner/compare/v6.2.0...v6.2.1) (2021-04-13)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.2.0 ([#1054](https://www.github.com/googleapis/java-spanner/issues/1054)) ([0b59b94](https://www.github.com/googleapis/java-spanner/commit/0b59b946b31c4b5ca95a2c279bdc835f23f1a923))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.21.0 ([#1045](https://www.github.com/googleapis/java-spanner/issues/1045)) ([94dcb46](https://www.github.com/googleapis/java-spanner/commit/94dcb468e807516f07777fc62faff345441ccdf6))
* update dependency com.google.cloud:google-cloud-trace to v1.3.1 ([#1050](https://www.github.com/googleapis/java-spanner/issues/1050)) ([cbb1038](https://www.github.com/googleapis/java-spanner/commit/cbb103846e33210c914f51f64e1e47f32ff775da))

## [6.2.0](https://www.github.com/googleapis/java-spanner/compare/v6.1.0...v6.2.0) (2021-04-07)


### Features

* add support for tagging ([#576](https://www.github.com/googleapis/java-spanner/issues/576)) ([2a9086f](https://www.github.com/googleapis/java-spanner/commit/2a9086fcc7e8caae55f71bf5616b2d0db18681d3))
* Support query hints for DML statements ([#1030](https://www.github.com/googleapis/java-spanner/issues/1030)) ([6a58433](https://www.github.com/googleapis/java-spanner/commit/6a58433919d9f69e91639a1b52cbbc1151ca6804))


### Bug Fixes

* local connection checker ignores exceptions ([#1036](https://www.github.com/googleapis/java-spanner/issues/1036)) ([2d61bc4](https://www.github.com/googleapis/java-spanner/commit/2d61bc410b7c680169129725bcc11069c2390505))

## [6.1.0](https://www.github.com/googleapis/java-spanner/compare/v6.0.0...v6.1.0) (2021-03-31)


### Features

* support RPC priority ([#676](https://www.github.com/googleapis/java-spanner/issues/676)) ([0bc9972](https://www.github.com/googleapis/java-spanner/commit/0bc9972b140d6a3de9c5481a4b73ecba3e139656))


### Bug Fixes

* plain text when testing emulator connection ([#1020](https://www.github.com/googleapis/java-spanner/issues/1020)) ([1e6e23f](https://www.github.com/googleapis/java-spanner/commit/1e6e23f8d64cd16d5e5034c89c65283b3b0cae89))
* retry cancelled error on first statement in transaction ([#999](https://www.github.com/googleapis/java-spanner/issues/999)) ([a95f6f8](https://www.github.com/googleapis/java-spanner/commit/a95f6f8dc21d27133a0150ea8df963e2bc543e40)), closes [#938](https://www.github.com/googleapis/java-spanner/issues/938)
* transaction retries should not timeout ([#1009](https://www.github.com/googleapis/java-spanner/issues/1009)) ([6d9c3b8](https://www.github.com/googleapis/java-spanner/commit/6d9c3b884357ddc4d314ebdfac5fc6dda2de3b49)), closes [#1008](https://www.github.com/googleapis/java-spanner/issues/1008)
* update link and directory ([#1012](https://www.github.com/googleapis/java-spanner/issues/1012)) ([865bf01](https://www.github.com/googleapis/java-spanner/commit/865bf011093341382a2c70f5530e9f7ef58b2d5a))


### Dependencies

* update dependency org.openjdk.jmh:jmh-core to v1.29 ([#1013](https://www.github.com/googleapis/java-spanner/issues/1013)) ([a71079f](https://www.github.com/googleapis/java-spanner/commit/a71079f5bb7f209f6afe6f5bc21a58d39e131086))


### Documentation

* improve error messages ([#1011](https://www.github.com/googleapis/java-spanner/issues/1011)) ([7dacfdc](https://www.github.com/googleapis/java-spanner/commit/7dacfdc7ca1219a0ddf5929d7b46860b46e3c300))
* new libraries-bom ([#1025](https://www.github.com/googleapis/java-spanner/issues/1025)) ([3485252](https://www.github.com/googleapis/java-spanner/commit/3485252ce3d98a01fca1b6a9e1ca031283440b5e))

## [6.0.0](https://www.github.com/googleapis/java-spanner/compare/v5.2.0...v6.0.0) (2021-03-21)


### ⚠ BREAKING CHANGES

* add closeAsync() method to Connection (#984)
* drops support of Java 7 (#946)
* customer-managed encryption keys for Spanner (#666)

### Features

* add closeAsync() method to Connection ([#984](https://www.github.com/googleapis/java-spanner/issues/984)) ([e7ec96e](https://www.github.com/googleapis/java-spanner/commit/e7ec96ec09a9d273d4f576356d3e4c6cbbb6de9e))
* customer-managed encryption keys for Spanner ([#666](https://www.github.com/googleapis/java-spanner/issues/666)) ([8338116](https://www.github.com/googleapis/java-spanner/commit/8338116dffe847931cae1212333af04338ea1d45))
* drops support of Java 7 ([#946](https://www.github.com/googleapis/java-spanner/issues/946)) ([7af1951](https://www.github.com/googleapis/java-spanner/commit/7af19514dfae5f87ba50572d8867568d2c09daab))

## [5.2.0](https://www.github.com/googleapis/java-spanner/compare/v5.1.0...v5.2.0) (2021-03-18)


### Features

* add autoConfigEmulator connection option ([#931](https://www.github.com/googleapis/java-spanner/issues/931)) ([32fdd60](https://www.github.com/googleapis/java-spanner/commit/32fdd606f392bc97dab7f37b1c566b3954839f7e))


### Bug Fixes

* all throwables should be ignored in shutdown hook ([#950](https://www.github.com/googleapis/java-spanner/issues/950)) ([213dddc](https://www.github.com/googleapis/java-spanner/commit/213dddcb4f84e19be2f98115493208e3af819485)), closes [#949](https://www.github.com/googleapis/java-spanner/issues/949)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.1.0 ([#953](https://www.github.com/googleapis/java-spanner/issues/953)) ([f991c87](https://www.github.com/googleapis/java-spanner/commit/f991c875d7ec62d19d048576263c5714d4d48a3f))
* update dependency com.google.cloud:google-cloud-trace to v1.3.0 ([#947](https://www.github.com/googleapis/java-spanner/issues/947)) ([c1d560b](https://www.github.com/googleapis/java-spanner/commit/c1d560ba4e799953aff6ba146f6f1b679a4b75b7))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.28 ([#924](https://www.github.com/googleapis/java-spanner/issues/924)) ([693fe5d](https://www.github.com/googleapis/java-spanner/commit/693fe5d4df3d279edb8f6f7f9879366980fd81d8))

## [5.1.0](https://www.github.com/googleapis/java-spanner/compare/v5.0.0...v5.1.0) (2021-03-10)


### Features

* add client lib token for Liquibase ([#925](https://www.github.com/googleapis/java-spanner/issues/925)) ([0d93d92](https://www.github.com/googleapis/java-spanner/commit/0d93d92fcd7c8bb2ffd3198560c4be3e4afc4990))
* adds samples for PITR ([#837](https://www.github.com/googleapis/java-spanner/issues/837)) ([55fa0cc](https://www.github.com/googleapis/java-spanner/commit/55fa0ccca4faf44da8f9a3553ab4b35574c14830))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.14 ([#919](https://www.github.com/googleapis/java-spanner/issues/919)) ([178500c](https://www.github.com/googleapis/java-spanner/commit/178500c7e48cbdeb45f657d9c413e9afdacefbab))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.20.1 ([#944](https://www.github.com/googleapis/java-spanner/issues/944)) ([b74b764](https://www.github.com/googleapis/java-spanner/commit/b74b7648343dc789b60fb2636615f288b6e6c854))
* update dependency org.json:json to v20210307 ([#943](https://www.github.com/googleapis/java-spanner/issues/943)) ([4088981](https://www.github.com/googleapis/java-spanner/commit/4088981314097647e3ed79f2c748545cac6fc34e))
* update dependency org.openjdk.jmh:jmh-core to v1.28 ([#923](https://www.github.com/googleapis/java-spanner/issues/923)) ([b4d6e5a](https://www.github.com/googleapis/java-spanner/commit/b4d6e5ac762393b70b684159d11a55edf8f2fba7))

## [5.0.0](https://www.github.com/googleapis/java-spanner/compare/v4.0.2...v5.0.0) (2021-02-26)


### ⚠ BREAKING CHANGES

* add CommitStats to Connection API (#608)

### Features

* add CommitStats to Connection API ([#608](https://www.github.com/googleapis/java-spanner/issues/608)) ([b2b1191](https://www.github.com/googleapis/java-spanner/commit/b2b1191763cd47ca39849bdf93292ed5ef3e0c8a))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.20.0 ([#917](https://www.github.com/googleapis/java-spanner/issues/917)) ([aca9d45](https://www.github.com/googleapis/java-spanner/commit/aca9d45c4e86c45a75e6b5e0d3794e7ac97bdf1a))
* update dependency com.google.cloud:google-cloud-trace to v1.2.13 ([#918](https://www.github.com/googleapis/java-spanner/issues/918)) ([8843998](https://www.github.com/googleapis/java-spanner/commit/8843998a1c5ddb9228fa16162e0ea13f859f7f35))

### [4.0.2](https://www.github.com/googleapis/java-spanner/compare/v4.0.1...v4.0.2) (2021-02-23)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.13 ([#901](https://www.github.com/googleapis/java-spanner/issues/901)) ([10749c7](https://www.github.com/googleapis/java-spanner/commit/10749c7a074d33c853b0f11a0e6c6ee5f09e75c9))
* update dependency com.google.cloud:google-cloud-trace to v1.2.12 ([#896](https://www.github.com/googleapis/java-spanner/issues/896)) ([84ee6e0](https://www.github.com/googleapis/java-spanner/commit/84ee6e0d442a29893e1ac77fa7882ed0407c9a7d))

### [4.0.1](https://www.github.com/googleapis/java-spanner/compare/v4.0.0...v4.0.1) (2021-02-22)


### Bug Fixes

* wrong use of getRetryDelayInMillis() / 1000 in documentation and retry loops ([#885](https://www.github.com/googleapis/java-spanner/issues/885)) ([a55d7ce](https://www.github.com/googleapis/java-spanner/commit/a55d7ce64fff434151c1c3af0796d290e9db7470)), closes [#874](https://www.github.com/googleapis/java-spanner/issues/874)


### Documentation

* Add OpenCensus to OpenTelemetry shim to README ([#879](https://www.github.com/googleapis/java-spanner/issues/879)) ([b58d73d](https://www.github.com/googleapis/java-spanner/commit/b58d73ddb768c0d33d149ed8bc84f5af618514e1))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.19.0 ([#895](https://www.github.com/googleapis/java-spanner/issues/895)) ([e3e2c95](https://www.github.com/googleapis/java-spanner/commit/e3e2c95936f40a7954639a95c84cc9495e318e55))

## [4.0.0](https://www.github.com/googleapis/java-spanner/compare/v3.3.2...v4.0.0) (2021-02-17)


### ⚠ BREAKING CHANGES

* Point In Time Recovery (PITR) (#452)
* add support for CommitStats (#544)

### Features

* add option for returning Spanner commit stats ([#817](https://www.github.com/googleapis/java-spanner/issues/817)) ([80d3585](https://www.github.com/googleapis/java-spanner/commit/80d3585870b81949ec641291e5a88fe391f78e27))
* add support for CommitStats ([#544](https://www.github.com/googleapis/java-spanner/issues/544)) ([44aa384](https://www.github.com/googleapis/java-spanner/commit/44aa384429056dd6c6563351c43fe7dcac451008))
* allow session pool settings in connection url ([#821](https://www.github.com/googleapis/java-spanner/issues/821)) ([e1e9152](https://www.github.com/googleapis/java-spanner/commit/e1e915289755e5f46ba07569d85afda5df5e3f0d))
* generate sample code in the Java microgenerator ([#859](https://www.github.com/googleapis/java-spanner/issues/859)) ([7cdfb82](https://www.github.com/googleapis/java-spanner/commit/7cdfb82b40487600547d0bad92119508161ca689))
* Point In Time Recovery (PITR) ([#452](https://www.github.com/googleapis/java-spanner/issues/452)) ([ab14a5e](https://www.github.com/googleapis/java-spanner/commit/ab14a5ec2dc2b7e2141305b5326f436eb6eee76f))


### Bug Fixes

* allows user-agent header with header provider ([#871](https://www.github.com/googleapis/java-spanner/issues/871)) ([3de7e2a](https://www.github.com/googleapis/java-spanner/commit/3de7e2a91349cac5d79a32d2cda7ca727140f0bf))
* make compiled statements immutable ([#843](https://www.github.com/googleapis/java-spanner/issues/843)) ([118d1b3](https://www.github.com/googleapis/java-spanner/commit/118d1b31f5f7771023766fd72a8229db80f1f5a2))


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.12 ([#854](https://www.github.com/googleapis/java-spanner/issues/854)) ([58cebd8](https://www.github.com/googleapis/java-spanner/commit/58cebd85a9d82bd1526b9eae98892181f1a022f1))
* update dependency com.google.cloud:google-cloud-trace to v1.2.11 ([#825](https://www.github.com/googleapis/java-spanner/issues/825)) ([49c8c5d](https://www.github.com/googleapis/java-spanner/commit/49c8c5d241803565fa9ff96ba55f3eb00ed5b85e))


### Documentation

* libraries-bom 16.4.0 ([#867](https://www.github.com/googleapis/java-spanner/issues/867)) ([5af3673](https://www.github.com/googleapis/java-spanner/commit/5af36739532037360dfd504a4a0988562550526c))

### [3.3.2](https://www.github.com/googleapis/java-spanner/compare/v3.3.1...v3.3.2) (2021-01-18)


### Bug Fixes

* closes pool maintainer on invalidation ([#784](https://www.github.com/googleapis/java-spanner/issues/784)) ([d122ed9](https://www.github.com/googleapis/java-spanner/commit/d122ed9662c9f01efd7d2a9797b1252f0427089c))
* UNAVAILABLE error on first query could cause transaction to get stuck ([#807](https://www.github.com/googleapis/java-spanner/issues/807)) ([c7dc6e6](https://www.github.com/googleapis/java-spanner/commit/c7dc6e6b11af76cb5db1f160c4466a5d75b524b2)), closes [#799](https://www.github.com/googleapis/java-spanner/issues/799)


### Dependencies

* update opencensus.version to v0.28.3 ([#806](https://www.github.com/googleapis/java-spanner/issues/806)) ([77910a0](https://www.github.com/googleapis/java-spanner/commit/77910a04e0fa42c90064fd533b6c13fe0372fb1e))

### [3.3.1](https://www.github.com/googleapis/java-spanner/compare/v3.3.0...v3.3.1) (2021-01-14)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Bug Fixes

* blanks span for session keepAlive traces ([#797](https://www.github.com/googleapis/java-spanner/issues/797)) ([1a86e4f](https://www.github.com/googleapis/java-spanner/commit/1a86e4fd5b6198c300c13eba4d3d9d91c12c43f7))
* mark transaction as invalid if no tx is returned before RS is closed ([#791](https://www.github.com/googleapis/java-spanner/issues/791)) ([e02e5a7](https://www.github.com/googleapis/java-spanner/commit/e02e5a7d95c0e92d9f13640dd2afe5b899f4e56d))
* remove time series before adding it ([#766](https://www.github.com/googleapis/java-spanner/issues/766)) ([90255ea](https://www.github.com/googleapis/java-spanner/commit/90255ea7a1cc70ba4f4ab48551c509f503981540)), closes [#202](https://www.github.com/googleapis/java-spanner/issues/202)
* safeguard against statements errors when requesting tx ([#800](https://www.github.com/googleapis/java-spanner/issues/800)) ([c4776e4](https://www.github.com/googleapis/java-spanner/commit/c4776e42ad4a2795b0bfc6e1a9fb10c40d64a809))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.17.1 ([#794](https://www.github.com/googleapis/java-spanner/issues/794)) ([f0beabb](https://www.github.com/googleapis/java-spanner/commit/f0beabb228a4f555e1bcb1817a14e8074a54ef8c))
* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.18.0 ([#796](https://www.github.com/googleapis/java-spanner/issues/796)) ([1a71e50](https://www.github.com/googleapis/java-spanner/commit/1a71e503c68eb10ca140fe93f281a0474ddf21d3))

## [3.3.0](https://www.github.com/googleapis/java-spanner/compare/v3.2.1...v3.3.0) (2021-01-07)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Features

* attempt DirectPath by default ([#770](https://www.github.com/googleapis/java-spanner/issues/770)) ([dc02244](https://www.github.com/googleapis/java-spanner/commit/dc02244d5ad29715f0c5d4c0ba8070659744c512))


### Bug Fixes

* Set up DirectPath e2e tests correctly ([#780](https://www.github.com/googleapis/java-spanner/issues/780)) ([9b94c6e](https://www.github.com/googleapis/java-spanner/commit/9b94c6ef54776fdb8868acf04e371599b7500d57))

### [3.2.1](https://www.github.com/googleapis/java-spanner/compare/v3.2.0...v3.2.1) (2021-01-06)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Bug Fixes

* grpc-alts is used not only in tests ([#761](https://www.github.com/googleapis/java-spanner/issues/761)) ([72d93d5](https://www.github.com/googleapis/java-spanner/commit/72d93d5aa9a301c64c9d572d10211882a359e414))


### Dependencies

* grpc-alts is only used for tests ([#757](https://www.github.com/googleapis/java-spanner/issues/757)) ([c8ef46f](https://www.github.com/googleapis/java-spanner/commit/c8ef46f2637b58cc71d023764cdc11a7414d855f))
* update dependency com.google.cloud:google-cloud-monitoring to v2.0.11 ([#754](https://www.github.com/googleapis/java-spanner/issues/754)) ([ee2de33](https://www.github.com/googleapis/java-spanner/commit/ee2de3356038cef429eb4d3fa67656e68994bc46))


### Documentation

* add sample for timeout for one RPC ([#707](https://www.github.com/googleapis/java-spanner/issues/707)) ([056f54f](https://www.github.com/googleapis/java-spanner/commit/056f54f3cc10d103151fccba569d46796a103591))
* cleanup inner region tags ([#764](https://www.github.com/googleapis/java-spanner/issues/764)) ([90ad9d6](https://www.github.com/googleapis/java-spanner/commit/90ad9d614bc1950f46d148930e06bde93aeb2098))
* documents resume on update database ddl ([#767](https://www.github.com/googleapis/java-spanner/issues/767)) ([aeb255d](https://www.github.com/googleapis/java-spanner/commit/aeb255d2e5998ebb6f3eb7f655f63c957d5d92bd))

## [3.2.0](https://www.github.com/googleapis/java-spanner/compare/v3.1.3...v3.2.0) (2020-12-17)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Features

* include client version in user agent header ([#747](https://www.github.com/googleapis/java-spanner/issues/747)) ([fc63bc3](https://www.github.com/googleapis/java-spanner/commit/fc63bc3f1bd9cdd83156cc63548b544188de6592))
* introduce TransactionOptions and UpdateOptions ([#716](https://www.github.com/googleapis/java-spanner/issues/716)) ([5c96fab](https://www.github.com/googleapis/java-spanner/commit/5c96fab6d1c19518d52d0a7f0d634f0526066f03))


### Bug Fixes

* reduce the probability of RESOURCE_EXHAUSTED errors during tests ([#734](https://www.github.com/googleapis/java-spanner/issues/734)) ([cd946d7](https://www.github.com/googleapis/java-spanner/commit/cd946d71501a2af7a2b3bb986ef75272c3ed92e1)), closes [#733](https://www.github.com/googleapis/java-spanner/issues/733)


### Documentation

* homogenize region tags ([#752](https://www.github.com/googleapis/java-spanner/issues/752)) ([2b3775a](https://www.github.com/googleapis/java-spanner/commit/2b3775a02466176695d7b88312b17c1aeedfbc16))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.17.0 ([#751](https://www.github.com/googleapis/java-spanner/issues/751)) ([f52776f](https://www.github.com/googleapis/java-spanner/commit/f52776f3af1c9653bfdd38aa1dac1a0d1e727b7f))
* update dependency com.google.cloud:google-cloud-trace to v1.2.10 ([#759](https://www.github.com/googleapis/java-spanner/issues/759)) ([405c4cc](https://www.github.com/googleapis/java-spanner/commit/405c4cc1af42d4440157438986c8911695ee32d6))

### [3.1.3](https://www.github.com/googleapis/java-spanner/compare/v3.1.2...v3.1.3) (2020-12-14)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.16.1 ([09968d5](https://www.github.com/googleapis/java-spanner/commit/09968d5092268b6ac2083b6914185f5e73d23648))

### [3.1.2](https://www.github.com/googleapis/java-spanner/compare/v3.1.1...v3.1.2) (2020-12-14)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.9 ([#710](https://www.github.com/googleapis/java-spanner/issues/710)) ([37a636d](https://www.github.com/googleapis/java-spanner/commit/37a636d989d2783875065b89141e532064f2647b))

### [3.1.1](https://www.github.com/googleapis/java-spanner/compare/v3.1.0...v3.1.1) (2020-12-10)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Dependencies

* update dependency com.google.cloud:google-cloud-trace to v1.2.8 ([#699](https://www.github.com/googleapis/java-spanner/issues/699)) ([e3289bd](https://www.github.com/googleapis/java-spanner/commit/e3289bdf1f5c723c88f4e719c4a7a15f5d131556))

## [3.1.0](https://www.github.com/googleapis/java-spanner/compare/v3.0.5...v3.1.0) (2020-12-10)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Features

* allow lenient mode for connection properties ([#671](https://www.github.com/googleapis/java-spanner/issues/671)) ([f6a8ba6](https://www.github.com/googleapis/java-spanner/commit/f6a8ba6baff53ededf890e3f22a8e49402c98775))
* retry admin request limit exceeded error ([#669](https://www.github.com/googleapis/java-spanner/issues/669)) ([3f9f74a](https://www.github.com/googleapis/java-spanner/commit/3f9f74aed52bce681b4bfd10d1006e5fa05b7cc9)), closes [#655](https://www.github.com/googleapis/java-spanner/issues/655)


### Bug Fixes

* fixes changelog of upgrade 2.0.0 ([#672](https://www.github.com/googleapis/java-spanner/issues/672)) ([c035546](https://www.github.com/googleapis/java-spanner/commit/c0355462d839a1e38a4efec9e4019272a76d822f))
* transaction retry could fail if tx contained failed statements ([#688](https://www.github.com/googleapis/java-spanner/issues/688)) ([f78c64e](https://www.github.com/googleapis/java-spanner/commit/f78c64e3e2bee6d6ed1f44a0b2e57249cba0e6d0)), closes [#685](https://www.github.com/googleapis/java-spanner/issues/685)


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.16.0 ([#680](https://www.github.com/googleapis/java-spanner/issues/680)) ([81cba9a](https://www.github.com/googleapis/java-spanner/commit/81cba9ade891aa65176d4be137f902651499b05c))
* update dependency com.google.cloud:google-cloud-trace to v1.2.7 ([#646](https://www.github.com/googleapis/java-spanner/issues/646)) ([0e17be0](https://www.github.com/googleapis/java-spanner/commit/0e17be0f81483eba4570faf884388cb43a42d84d))
* update dependency org.openjdk.jmh:jmh-core to v1.27 ([#691](https://www.github.com/googleapis/java-spanner/issues/691)) ([a2e82e4](https://www.github.com/googleapis/java-spanner/commit/a2e82e424802f1544443ee29588bd1fabe3f38c3))
* update dependency org.openjdk.jmh:jmh-generator-annprocess to v1.27 ([#692](https://www.github.com/googleapis/java-spanner/issues/692)) ([bca15c2](https://www.github.com/googleapis/java-spanner/commit/bca15c226a914c8728a6a52083dd1ff074cc97e8))

### [3.0.5](https://www.github.com/googleapis/java-spanner/compare/v3.0.4...v3.0.5) (2020-11-19)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Bug Fixes

* delete stale sample databases ([#622](https://www.github.com/googleapis/java-spanner/issues/622)) ([7584baa](https://www.github.com/googleapis/java-spanner/commit/7584baa8b7051764f1055ddb1616069e7d591b64))
* does not generate codeowners ([#631](https://www.github.com/googleapis/java-spanner/issues/631)) ([9e133a9](https://www.github.com/googleapis/java-spanner/commit/9e133a972f648ee804f324bbf55163849cb478b8))
* query could hang transaction if ResultSet#next() is not called ([#643](https://www.github.com/googleapis/java-spanner/issues/643)) ([48f92e3](https://www.github.com/googleapis/java-spanner/commit/48f92e3d1b26644bde62a8d864cec96c3c71687d)), closes [#641](https://www.github.com/googleapis/java-spanner/issues/641)


### Dependencies

* update dependency com.google.cloud:google-cloud-monitoring to v2.0.8 ([#644](https://www.github.com/googleapis/java-spanner/issues/644)) ([447a99b](https://www.github.com/googleapis/java-spanner/commit/447a99b9a6ccdfd3855505fca13e849fb9513943))

### [3.0.4](https://www.github.com/googleapis/java-spanner/compare/v3.0.3...v3.0.4) (2020-11-17)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Reverts

* Revert "fix: skip failing backup tests for now" (#634) ([b22cd7d](https://www.github.com/googleapis/java-spanner/commit/b22cd7dfc377a0445534946af29500cee316e6b1)), closes [#634](https://www.github.com/googleapis/java-spanner/issues/634)

### [3.0.3](https://www.github.com/googleapis/java-spanner/compare/v3.0.2...v3.0.3) (2020-11-16)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Dependencies

* update dependency org.json:json to v20201115 ([#624](https://www.github.com/googleapis/java-spanner/issues/624)) ([60e31d1](https://www.github.com/googleapis/java-spanner/commit/60e31d1947b6930ec030e1f3170dfbde62833b96))

### [3.0.2](https://www.github.com/googleapis/java-spanner/compare/v3.0.1...v3.0.2) (2020-11-13)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

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

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

### Bug Fixes

* adds assembly descriptor to snippets samples ([#559](https://www.github.com/googleapis/java-spanner/issues/559)) ([d4ae85c](https://www.github.com/googleapis/java-spanner/commit/d4ae85c91c2bda3f46cab8c9f7a4033ddd639c94))
* always delete all backups from an owned test instance ([#557](https://www.github.com/googleapis/java-spanner/issues/557)) ([ff571b0](https://www.github.com/googleapis/java-spanner/commit/ff571b01b9dffdda44a9bd322e04ff04b5b5c57a)), closes [#542](https://www.github.com/googleapis/java-spanner/issues/542)
* fixes the code of conduct document ([#541](https://www.github.com/googleapis/java-spanner/issues/541)) ([7b9d1db](https://www.github.com/googleapis/java-spanner/commit/7b9d1db28b7037d6b18df88f00b9213f2f6dab80))
* SessionNotFound was not retried for AsyncTransactionManager ([#552](https://www.github.com/googleapis/java-spanner/issues/552)) ([5969f83](https://www.github.com/googleapis/java-spanner/commit/5969f8313a4df6ece63ee8f14df98cbc8511f026))


### Dependencies

* update dependency com.google.cloud:google-cloud-shared-dependencies to v0.13.0 ([#521](https://www.github.com/googleapis/java-spanner/issues/521)) ([0f4c017](https://www.github.com/googleapis/java-spanner/commit/0f4c017f112478ffc7dd15b0b234a9c48cd55a6e))

## [3.0.0](https://www.github.com/googleapis/java-spanner/compare/v2.0.2...v3.0.0) (2020-10-23)

### ⚠ IMPORTANT: Known issue with this version of the client

Since [v3.0.0](https://github.com/googleapis/java-spanner/releases/tag/v3.0.0), transactions can get stuck if the Spanner backend returns a retryable error when consuming the first record of a read / query in a transaction.

A [fix](https://github.com/googleapis/java-spanner/pull/807) is submitted and available in version [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2)

**Please use [v3.3.2](https://github.com/googleapis/java-spanner/releases/tag/v3.3.2) instead of this version.**

Apologies for the inconvenience.

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

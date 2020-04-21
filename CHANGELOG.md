# Changelog

### [1.52.1](https://www.github.com/googleapis/java-spanner/compare/v1.52.0...v1.52.1) (2020-04-21)


### Bug Fixes

* assign unique id's per test case ([#129](https://www.github.com/googleapis/java-spanner/issues/129)) ([a553b6d](https://www.github.com/googleapis/java-spanner/commit/a553b6d48c4f5ee2d0583e5b825d73a85f06216e))
* check for not null input for Id classes ([#159](https://www.github.com/googleapis/java-spanner/issues/159)) ([ecf5826](https://www.github.com/googleapis/java-spanner/commit/ecf582670818f32e85f534ec400d0b8d31cf9ca6)), closes [#145](https://www.github.com/googleapis/java-spanner/issues/145)
* fix flaky test and remove warnings ([#153](https://www.github.com/googleapis/java-spanner/issues/153)) ([d534e35](https://www.github.com/googleapis/java-spanner/commit/d534e350346b0c9ab8057ede36bc3aac473c0b06)), closes [#146](https://www.github.com/googleapis/java-spanner/issues/146)
* increase test timeout and remove warnings ([#160](https://www.github.com/googleapis/java-spanner/issues/160)) ([63a6bd8](https://www.github.com/googleapis/java-spanner/commit/63a6bd8be08a56d002f58bc2cdb2856ad0dc5fa3)), closes [#158](https://www.github.com/googleapis/java-spanner/issues/158)
* retry non-idempotent long-running RPCs ([#141](https://www.github.com/googleapis/java-spanner/issues/141)) ([4669c02](https://www.github.com/googleapis/java-spanner/commit/4669c02a24e0f7b1d53c9edf5ab7b146b4116960))
* retry restore if blocked by pending restore ([#119](https://www.github.com/googleapis/java-spanner/issues/119)) ([220653d](https://www.github.com/googleapis/java-spanner/commit/220653d8e25c518d0df447bf777a7fcbf04a01ca)), closes [#118](https://www.github.com/googleapis/java-spanner/issues/118)
* wait for initialization to finish before test ([#161](https://www.github.com/googleapis/java-spanner/issues/161)) ([fe434ff](https://www.github.com/googleapis/java-spanner/commit/fe434ff7068b4b618e70379c224e1c5ab88f6ba1)), closes [#146](https://www.github.com/googleapis/java-spanner/issues/146)


### Performance Improvements

* increase sessions in the pool in batches ([#134](https://www.github.com/googleapis/java-spanner/issues/134)) ([9e5a1cd](https://www.github.com/googleapis/java-spanner/commit/9e5a1cdaacf71147b67681861f063c3276705f44))


### Dependencies

* update core dependencies ([#109](https://www.github.com/googleapis/java-spanner/issues/109)) ([5753f1f](https://www.github.com/googleapis/java-spanner/commit/5753f1f4fed83df87262404f7a7ba7eedcd366cb))
* update core dependencies ([#132](https://www.github.com/googleapis/java-spanner/issues/132)) ([77c1558](https://www.github.com/googleapis/java-spanner/commit/77c1558652ee00e529674ac3a2dcf3210ef049fa))
* update dependency com.google.api:api-common to v1.9.0 ([#127](https://www.github.com/googleapis/java-spanner/issues/127)) ([b2c744f](https://www.github.com/googleapis/java-spanner/commit/b2c744f01a4d5a8981df5ff900f3536c83265a61))
* update dependency com.google.guava:guava-bom to v29 ([#147](https://www.github.com/googleapis/java-spanner/issues/147)) ([3fe3ae0](https://www.github.com/googleapis/java-spanner/commit/3fe3ae02376af552564c93c766f562d6454b7ac1))
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

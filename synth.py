# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""This script is used to synthesize generated parts of this library."""

import synthtool as s
import synthtool.gcp as gcp
import synthtool.languages.java as java

AUTOSYNTH_MULTIPLE_COMMITS = True

gapic = gcp.GAPICBazel()

library = gapic.java_library(
    service='spanner',
    version='v1',
    proto_path=f'google/spanner/v1',
    bazel_target=f'//google/spanner/v1:google-cloud-spanner-v1-java',
)

library = library / f"google-cloud-spanner-v1-java"

java.fix_proto_headers(library / 'proto-google-cloud-spanner-v1-java')
java.fix_grpc_headers(library / 'grpc-google-cloud-spanner-v1-java', 'com.google.spanner.v1')
s.copy(library / 'gapic-google-cloud-spanner-v1-java/src', 'google-cloud-spanner/src')
s.copy(library / 'grpc-google-cloud-spanner-v1-java/src', 'grpc-google-cloud-spanner-v1/src')
s.copy(library / 'proto-google-cloud-spanner-v1-java/src', 'proto-google-cloud-spanner-v1/src')

library = gapic.java_library(
    service='admin-database',
    version='v1',
    proto_path=f'google/spanner/admin/database/v1',
    bazel_target=f'//google/spanner/admin/database/v1:google-cloud-admin-database-v1-java',
)

library = library / f"google-cloud-admin-database-v1-java"

java.fix_proto_headers(library / 'proto-google-cloud-admin-database-v1-java')
java.fix_grpc_headers(library / 'grpc-google-cloud-admin-database-v1-java', 'com.google.spanner.admin.database.v1')
s.copy(library / 'gapic-google-cloud-admin-database-v1-java/src', 'google-cloud-spanner/src')
s.copy(library / 'grpc-google-cloud-admin-database-v1-java/src', 'grpc-google-cloud-spanner-admin-database-v1/src')
s.copy(library / 'proto-google-cloud-admin-database-v1-java/src', 'proto-google-cloud-spanner-admin-database-v1/src')

library = gapic.java_library(
    service='admin-instance',
    version='v1',
    proto_path=f'google/spanner/admin/instance/v1',
    bazel_target=f'//google/spanner/admin/instance/v1:google-cloud-admin-instance-v1-java',
)

library = library / f"google-cloud-admin-instance-v1-java"

java.fix_proto_headers(library / 'proto-google-cloud-admin-instance-v1-java')
java.fix_grpc_headers(library / 'grpc-google-cloud-admin-instance-v1-java', 'com.google.spanner.admin.instance.v1')
s.copy(library / 'gapic-google-cloud-admin-instance-v1-java/src', 'google-cloud-spanner/src')
s.copy(library / 'grpc-google-cloud-admin-instance-v1-java/src', 'grpc-google-cloud-spanner-admin-instance-v1/src')
s.copy(library / 'proto-google-cloud-admin-instance-v1-java/src', 'proto-google-cloud-spanner-admin-instance-v1/src')

java.format_code('google-cloud-spanner/src')
java.format_code('grpc-google-cloud-spanner-v1/src')
java.format_code('proto-google-cloud-spanner-v1/src')
java.format_code('grpc-google-cloud-spanner-admin-database-v1/src')
java.format_code('proto-google-cloud-spanner-admin-database-v1/src')
java.format_code('grpc-google-cloud-spanner-admin-instance-v1/src')
java.format_code('proto-google-cloud-spanner-admin-instance-v1/src')

java.common_templates(excludes=[
    '.kokoro/continuous/common.cfg',
    '.kokoro/nightly/common.cfg',
    '.kokoro/nightly/java8-samples.cfg',
    '.kokoro/nightly/java11-samples.cfg',
    '.kokoro/nightly/samples.cfg',
    '.kokoro/presubmit/common.cfg',
    '.kokoro/presubmit/java8-samples.cfg',
    '.kokoro/presubmit/java11-samples.cfg',
    '.kokoro/presubmit/samples.cfg',
    'samples/install-without-bom/pom.xml',
    'samples/snapshot/pom.xml',
    'samples/snippets/pom.xml',
    '.github/CODEOWNERS',
    '.github/sync-repo-settings.yaml',
    '.github/release-please.yml',
])

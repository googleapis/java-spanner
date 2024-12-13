# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import synthtool as s
from synthtool.languages import java


for library in s.get_staging_dirs():
    # put any special-case replacements here
    s.move(library)

s.remove_staging_dirs()
java.common_templates(
    excludes=[
        ".kokoro/continuous/common.cfg",
        ".kokoro/nightly/common.cfg",
        ".kokoro/nightly/integration.cfg",
        ".kokoro/nightly/java8-samples.cfg",
        ".kokoro/nightly/java11-samples.cfg",
        ".kokoro/nightly/samples.cfg",
        ".kokoro/build.bat",
        ".kokoro/presubmit/common.cfg",
        ".kokoro/presubmit/graalvm-native.cfg",
        ".kokoro/presubmit/graalvm-native-17.cfg",
        "samples/install-without-bom/pom.xml",
        "samples/snapshot/pom.xml",
        "samples/snippets/pom.xml",
        ".github/CODEOWNERS",
        ".github/sync-repo-settings.yaml",
        ".github/release-please.yml",
        ".github/blunderbuss.yml",
        ".github/workflows/samples.yaml",
        ".github/workflows/ci.yaml",
        ".kokoro/common.sh",
        ".kokoro/build.sh",
        ".kokoro/dependencies.sh",
        ".kokoro/requirements.in",
        ".kokoro/requirements.txt"
    ]
)

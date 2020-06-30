#!/bin/bash

# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License..

# Fail on any error
set -e

# Display commands being run
set -x

export SPANNER_EMULATOR_HOST=localhost:9010
export GOOGLE_CLOUD_PROJECT=emulator-test-project
echo "Running the Cloud Spanner emulator: $SPANNER_EMULATOR_HOST";

# Download the emulator
EMULATOR_VERSION=0.8.0
wget https://storage.googleapis.com/cloud-spanner-emulator/releases/${EMULATOR_VERSION}/cloud-spanner-emulator_linux_amd64-${EMULATOR_VERSION}.tar.gz
tar zxvf cloud-spanner-emulator_linux_amd64-${EMULATOR_VERSION}.tar.gz
chmod u+x emulator_main

# Start the emulator
./emulator_main --host_port $SPANNER_EMULATOR_HOST &

EMULATOR_PID=$!

# Stop the emulator & clean the environment variable
trap "kill -15 $EMULATOR_PID; unset SPANNER_EMULATOR_HOST; unset GOOGLE_CLOUD_PROJECT; echo \"Cleanup the emulator\";" EXIT

mvn -B -Dspanner.testenv.instance="" \
  -Penable-integration-tests \
  -DtrimStackTrace=false \
  -Dclirr.skip=true \
  -Denforcer.skip=true \
  -fae \
  verify

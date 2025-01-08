#!/bin/bash
# Copyright 2019 Google LLC
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
# limitations under the License.

set -eo pipefail
shopt -s nullglob

set -x

## Get the directory of the build script
scriptDir=$(realpath $(dirname "${BASH_SOURCE[0]}"))
## cd to the parent directory, i.e. the root of the git repo
cd ${scriptDir}/..

# include common functions
source ${scriptDir}/common.sh

# Print out Java
java -version
echo $JOB_TYPE

function determineMavenOpts() {
  local javaVersion=$(
    # filter down to the version line, then pull out the version between quotes,
    # then trim the version number down to its minimal number (removing any
    # update or suffix number).
    java -version 2>&1 | grep "version" \
      | sed -E 's/^.*"(.*?)".*$/\1/g' \
      | sed -E 's/^(1\.[0-9]\.0).*$/\1/g'
  )

  if [[ $javaVersion == 17* ]]
    then
      # MaxPermSize is no longer supported as of jdk 17
      echo -n "-Xmx1024m"
  else
      echo -n "-Xmx1024m -XX:MaxPermSize=128m"
  fi
}

export MAVEN_OPTS=$(determineMavenOpts)

if [ ! -z "${JAVA11_HOME}" ]; then
  setJava "${JAVA11_HOME}"
fi

INSTALL_OPTS=""
if [[ $ENABLE_AIRLOCK = 'true' ]]; then
  INSTALL_OPTS="-Pairlock-trusted"
  wget -q https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip -O /tmp/maven.zip && \
    unzip /tmp/maven.zip -d /tmp/maven && \
    mv /tmp/maven/apache-maven-3.9.9 /usr/local/lib/maven && \
    rm /tmp/maven.zip && \
    ln -s $JAVA_HOME/lib $JAVA_HOME/conf
    ls -la /usr/local/lib/maven
fi

# this should run maven enforcer
retry_with_backoff 3 10 \
  mvn install -B -V \
    ${INSTALL_OPTS} \
    -DskipTests=true \
    -Dmaven.javadoc.skip=true \
    -Dclirr.skip=true

if [ ! -z "${JAVA8_HOME}" ]; then
  setJava "${JAVA8_HOME}"
fi

mvn -B ${INSTALL_OPTS} dependency:analyze -DfailOnWarning=true

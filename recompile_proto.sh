# Generate grpc stubs
protoc -I=/usr/local/include -I=/googleapis-master -I=/java-spanner/proto-google-cloud-spanner-executor-v1/src/main/proto --plugin=protoc-gen-grpc=/usr/bin/protoc-gen-grpc-java-1.49.0-linux-x86_64.exe --grpc_out=/java-spanner/grpc-google-cloud-spanner-executor-v1/src/main/java/ /java-spanner/proto-google-cloud-spanner-executor-v1/src/main/proto/google/spanner/executor/v1/*.proto

# Generate
protoc -I=/usr/local/include -I=/googleapis-master -I=/java-spanner/proto-google-cloud-spanner-executor-v1/src/main/proto --java_out=/java-spanner/proto-google-cloud-spanner-executor-v1/src/main/java/ /java-spanner/proto-google-cloud-spanner-executor-v1/src/main/proto/google/spanner/executor/v1/*.proto

# Add license headers
~/go/bin/addlicense /java-spanner/google-cloud-spanner-executor/src/main/java/
~/go/bin/addlicense /java-spanner/proto-google-cloud-spanner-executor-v1/src/main/java/
~/go/bin/addlicense /java-spanner/grpc-google-cloud-spanner-executor-v1/src/main/java/

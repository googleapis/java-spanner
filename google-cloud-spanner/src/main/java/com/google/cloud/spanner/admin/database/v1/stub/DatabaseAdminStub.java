/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.admin.database.v1.stub;

import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabaseOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabaseRolesPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabasesPagedResponse;

import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.longrunning.stub.OperationsStub;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.CopyBackupMetadata;
import com.google.spanner.admin.database.v1.CopyBackupRequest;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateBackupRequest;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DeleteBackupRequest;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetBackupRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsResponse;
import com.google.spanner.admin.database.v1.ListBackupsRequest;
import com.google.spanner.admin.database.v1.ListBackupsResponse;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsRequest;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsResponse;
import com.google.spanner.admin.database.v1.ListDatabaseRolesRequest;
import com.google.spanner.admin.database.v1.ListDatabaseRolesResponse;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseRequest;
import com.google.spanner.admin.database.v1.UpdateBackupRequest;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Base stub class for the DatabaseAdmin service API.
 *
 * <p>This class is for advanced usage and reflects the underlying API directly.
 */
@Generated("by gapic-generator-java")
public abstract class DatabaseAdminStub implements BackgroundResource {

  public OperationsStub getOperationsStub() {
    throw new UnsupportedOperationException("Not implemented: getOperationsStub()");
  }

  public UnaryCallable<ListDatabasesRequest, ListDatabasesPagedResponse>
      listDatabasesPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listDatabasesPagedCallable()");
  }

  public UnaryCallable<ListDatabasesRequest, ListDatabasesResponse> listDatabasesCallable() {
    throw new UnsupportedOperationException("Not implemented: listDatabasesCallable()");
  }

  public OperationCallable<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createDatabaseOperationCallable()");
  }

  public UnaryCallable<CreateDatabaseRequest, Operation> createDatabaseCallable() {
    throw new UnsupportedOperationException("Not implemented: createDatabaseCallable()");
  }

  public UnaryCallable<GetDatabaseRequest, Database> getDatabaseCallable() {
    throw new UnsupportedOperationException("Not implemented: getDatabaseCallable()");
  }

  public OperationCallable<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationCallable() {
    throw new UnsupportedOperationException(
        "Not implemented: updateDatabaseDdlOperationCallable()");
  }

  public UnaryCallable<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlCallable() {
    throw new UnsupportedOperationException("Not implemented: updateDatabaseDdlCallable()");
  }

  public UnaryCallable<DropDatabaseRequest, Empty> dropDatabaseCallable() {
    throw new UnsupportedOperationException("Not implemented: dropDatabaseCallable()");
  }

  public UnaryCallable<GetDatabaseDdlRequest, GetDatabaseDdlResponse> getDatabaseDdlCallable() {
    throw new UnsupportedOperationException("Not implemented: getDatabaseDdlCallable()");
  }

  public UnaryCallable<SetIamPolicyRequest, Policy> setIamPolicyCallable() {
    throw new UnsupportedOperationException("Not implemented: setIamPolicyCallable()");
  }

  public UnaryCallable<GetIamPolicyRequest, Policy> getIamPolicyCallable() {
    throw new UnsupportedOperationException("Not implemented: getIamPolicyCallable()");
  }

  public UnaryCallable<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsCallable() {
    throw new UnsupportedOperationException("Not implemented: testIamPermissionsCallable()");
  }

  public OperationCallable<CreateBackupRequest, Backup, CreateBackupMetadata>
      createBackupOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createBackupOperationCallable()");
  }

  public UnaryCallable<CreateBackupRequest, Operation> createBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: createBackupCallable()");
  }

  public OperationCallable<CopyBackupRequest, Backup, CopyBackupMetadata>
      copyBackupOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: copyBackupOperationCallable()");
  }

  public UnaryCallable<CopyBackupRequest, Operation> copyBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: copyBackupCallable()");
  }

  public UnaryCallable<GetBackupRequest, Backup> getBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: getBackupCallable()");
  }

  public UnaryCallable<UpdateBackupRequest, Backup> updateBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: updateBackupCallable()");
  }

  public UnaryCallable<DeleteBackupRequest, Empty> deleteBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteBackupCallable()");
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse> listBackupsPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupsPagedCallable()");
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupsCallable()");
  }

  public OperationCallable<RestoreDatabaseRequest, Database, RestoreDatabaseMetadata>
      restoreDatabaseOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: restoreDatabaseOperationCallable()");
  }

  public UnaryCallable<RestoreDatabaseRequest, Operation> restoreDatabaseCallable() {
    throw new UnsupportedOperationException("Not implemented: restoreDatabaseCallable()");
  }

  public UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsPagedResponse>
      listDatabaseOperationsPagedCallable() {
    throw new UnsupportedOperationException(
        "Not implemented: listDatabaseOperationsPagedCallable()");
  }

  public UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
      listDatabaseOperationsCallable() {
    throw new UnsupportedOperationException("Not implemented: listDatabaseOperationsCallable()");
  }

  public UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsPagedResponse>
      listBackupOperationsPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupOperationsPagedCallable()");
  }

  public UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsResponse>
      listBackupOperationsCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupOperationsCallable()");
  }

  public UnaryCallable<ListDatabaseRolesRequest, ListDatabaseRolesPagedResponse>
      listDatabaseRolesPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listDatabaseRolesPagedCallable()");
  }

  public UnaryCallable<ListDatabaseRolesRequest, ListDatabaseRolesResponse>
      listDatabaseRolesCallable() {
    throw new UnsupportedOperationException("Not implemented: listDatabaseRolesCallable()");
  }

  @Override
  public abstract void close();
}

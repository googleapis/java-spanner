/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

// [START spanner_create_backup_schedule_sample]

import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.protobuf.Duration;
import com.google.spanner.admin.database.v1.BackupSchedule;
import com.google.spanner.admin.database.v1.BackupScheduleSpec;
import com.google.spanner.admin.database.v1.CreateBackupScheduleRequest;
import com.google.spanner.admin.database.v1.CrontabSpec;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

class CreateBackupScheduleSample {

    static void createBackupSchedule() throws IOException {
      // TODO(developer): Replace these variables before running the sample.
      String projectId = "my-project";
      String instanceId = "my-instance";
      String databaseId = "my-database";
      String backupScheduleId = "my-backup-schedule";
	  	createBackupScheduleSample(projectId, instanceId, databaseId, backupScheduleId);
    }

    static void createBackupSchedule(String projectId,
									 									 String instanceId,
									 									 String databaseId,
																		 String backupScheduleId) {
		  final BackupSchedule backupSchedule = BackupSchedule.newBuilder()
				.setFullBackupSpec(new FullBackupSpec())
				.setRetentionDuration(Duration.newBuilder().setSeconds(60*24*7).build());
				.setSpec(BackupScheduleSpec
									 .newBuilder()
									 .setCronSpec(CrontabSpec
																  .newBuilder()
																	.setText("0 0 * * *")
																	.build()
															 )	
									 .build()
								)
				.build();
			
			try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
				DatabaseName databaseName = DatabaseName.of(projectId,
																										instanceId,
																										databaseId);
				final BackupSchedule createdBackupSchedule = databaseAdminClient
					.createBackupSchedule(CreateBackupScheduleRequest
																  .newBuilder()
																	.setParent(databaseName)
																	.setBackupScheduleId(backupScheduleId)
																	.setBackupSchedule(backupSchedule)
																	.build()
															 )
					.get();
				System.out.println(String.format("Created backup schedule: %s",
																				 createBackupSchedule.getName()));
			} catch (ExecutionException e) {
      	throw (SpannerException) e.getCause();
    	} catch (InterruptedException e) {
      	throw SpannerExceptionFactory.propagateInterrupt(e);
    	}
    }
}
// [START spanner_create_backup_schedule_sample]

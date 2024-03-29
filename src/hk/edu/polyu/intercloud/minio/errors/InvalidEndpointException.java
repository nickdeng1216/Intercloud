/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hk.edu.polyu.intercloud.minio.errors;


@SuppressWarnings({"WeakerAccess", "unused"})
public class InvalidEndpointException extends MinioException {
  private final String endpoint;


  public InvalidEndpointException(String endpoint, String message) {
    super(message);
    this.endpoint = endpoint;
  }


  @Override
  public String toString() {
    return this.endpoint + ": " + super.toString();
  }
}

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

package hk.edu.polyu.intercloud.minio.messages;

import com.google.api.client.util.Key;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"WeakerAccess", "unused"})
public class ListMultipartUploadsResult extends XmlEntity {
  @Key("Upload")
  List<Upload> uploads;
  @Key("Bucket")
  private String bucketName;
  @Key("KeyMarker")
  private String keyMarker;
  @Key("UploadIdMarker")
  private String uploadIdMarker;
  @Key("NextKeyMarker")
  private String nextKeyMarker;
  @Key("NextUploadIdMarker")
  private String nextUploadIdMarker;
  @Key("MaxUploads")
  private int maxUploads;
  @Key("IsTruncated")
  private boolean isTruncated;


  public ListMultipartUploadsResult() throws XmlPullParserException {
    super();
    super.name = "ListMultipartUploadsResult";
  }


  public boolean isTruncated() {
    return isTruncated;
  }


  public String bucketName() {
    return bucketName;
  }


  public String keyMarker() {
    return keyMarker;
  }


  public String uploadIdMarker() {
    return uploadIdMarker;
  }


  public String nextKeyMarker() {
    return nextKeyMarker;
  }


  public String nextUploadIdMarker() {
    return nextUploadIdMarker;
  }


  public int maxUploads() {
    return maxUploads;
  }


  /**
   * get uploads.
   */
  public List<Upload> uploads() {
    if (uploads == null) {
      return new ArrayList<>();
    }
    return uploads;
  }
}

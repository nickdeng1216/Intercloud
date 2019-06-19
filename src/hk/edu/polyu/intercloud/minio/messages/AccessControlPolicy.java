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

import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

@SuppressWarnings({ "SameParameterValue", "unused" })
public class AccessControlPolicy extends XmlEntity {
	@Key("Owner")
	private Owner owner;
	@Key("AccessControlList")
	private AccessControlList accessControlList;

	public AccessControlPolicy() throws XmlPullParserException {
		super();
		this.name = "AccessControlPolicy";
	}

	public Owner owner() {
		return owner;
	}

	/**
	 * get access control list.
	 */
	public List<Grant> grants() {
		if (accessControlList == null) {
			return new LinkedList<>();
		}

		return accessControlList.grantList();
	}
}

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

package hk.edu.polyu.intercloud.minio.main;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.xmlpull.v1.XmlPullParserException;

import hk.edu.polyu.intercloud.minio.errors.ErrorResponseException;
import hk.edu.polyu.intercloud.minio.errors.InsufficientDataException;
import hk.edu.polyu.intercloud.minio.errors.InternalException;
import hk.edu.polyu.intercloud.minio.errors.InvalidBucketNameException;
import hk.edu.polyu.intercloud.minio.errors.NoResponseException;

public class Result<T> {
	private final T type;
	private final Exception ex;

	public Result(T type, Exception ex) {
		this.type = type;
		this.ex = ex;
	}

	/**
	 * get result.
	 */
	public T get() throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
			InternalException {
		if (ex == null) {
			return type;
		}

		if (ex instanceof InvalidBucketNameException) {
			throw (InvalidBucketNameException) ex;
		} else if (ex instanceof NoSuchAlgorithmException) {
			throw (NoSuchAlgorithmException) ex;
		} else if (ex instanceof InsufficientDataException) {
			throw (InsufficientDataException) ex;
		} else if (ex instanceof IOException) {
			throw (IOException) ex;
		} else if (ex instanceof InvalidKeyException) {
			throw (InvalidKeyException) ex;
		} else if (ex instanceof NoResponseException) {
			throw (NoResponseException) ex;
		} else if (ex instanceof XmlPullParserException) {
			throw (XmlPullParserException) ex;
		} else if (ex instanceof ErrorResponseException) {
			throw (ErrorResponseException) ex;
		} else {
			throw (InternalException) ex;
		}
	}
}

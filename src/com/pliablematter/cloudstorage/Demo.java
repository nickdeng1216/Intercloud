package com.pliablematter.cloudstorage;

import java.util.List;

public class Demo {

	public static void main(String[] args) throws Exception {

		CloudStorage.deleteBucket("my-bucket-2");

		CloudStorage.uploadFile("my-bucket", "/var/uploads/some-file.txt");

		CloudStorage.downloadFile("my-bucket", "some-file.txt",
				"/var/downloads");

		List<String> buckets = CloudStorage.listBuckets();

		List<String> files = CloudStorage.listBucket("my-bucket");

	}
}

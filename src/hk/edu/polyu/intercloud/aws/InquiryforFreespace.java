package hk.edu.polyu.intercloud.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * The class enquires about free space of the Amazon cloud.
 * 
 * @author Kate.Xie
 *
 */
public class InquiryforFreespace {
	/**
	 * @return free space size
	 * @throws Exception
	 */
	public String amazondo(String path) throws Exception {
		String freesize = null;

		/*
		 * Log in AWS.
		 */
		GetAccountInfo account = new GetAccountInfo();
		BasicAWSCredentials credentials = new BasicAWSCredentials(
				account.getValue("Username"), account.getValue("Password"));
		AmazonS3 s3 = new AmazonS3Client(credentials);
		final String serviceurl = account.getValue("Url");
		s3.setEndpoint(serviceurl);
		S3ClientOptions s3ClientOptions = new S3ClientOptions();
		s3ClientOptions.setPathStyleAccess(true);
		s3.setS3ClientOptions(s3ClientOptions);
		ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
				.withBucketName(account.getValue("Bucketname")));
		int size = 0;
		for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
			size = (int) (size + objectSummary.getSize());
		}
		long a = 5 * 1024 * 1024;
		long b = a * 1024;
		// for free part
		freesize = Long.toString(b - size);
		return freesize;
	}
}

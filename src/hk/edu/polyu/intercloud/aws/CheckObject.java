package hk.edu.polyu.intercloud.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class CheckObject {
	/**
	 * @key the file you will check
	 * @return true if the object exist, false otherwise
	 */
	public boolean amazondo(String key) throws Exception {

		GetAccountInfo account = new GetAccountInfo();
		BasicAWSCredentials credentials = new BasicAWSCredentials(
				account.getValue("Username"), account.getValue("Password"));
		AmazonS3 s3 = new AmazonS3Client(credentials);
		final String serviceurl = account.getValue("Url");
		s3.setEndpoint(serviceurl);
		S3ClientOptions s3ClientOptions = new S3ClientOptions();
		s3ClientOptions.setPathStyleAccess(true);
		s3.setS3ClientOptions(s3ClientOptions);
		long start = System.currentTimeMillis();
		System.out.println("Getting metadata of " + key + " from S3, START, "
				+ start + ", 0");
		ObjectMetadata meta = s3
				.getObjectMetadata(new GetObjectMetadataRequest(account
						.getValue("Bucketname"), key));
		long end = System.currentTimeMillis();
		System.out.println("Getting metadata of " + key + " from S3, END, "
				+ end + ", " + (end - start));
		if (meta != null) {
			return true;
		}
		return false;
	}
}

package hk.edu.polyu.intercloud.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

public class Delete {

	public String amazondo(String path) throws Exception {

		GetAccountInfo account = new GetAccountInfo();
		BasicAWSCredentials credentials = new BasicAWSCredentials(
				account.getValue("Username"), account.getValue("Password"));
		AmazonS3 s3 = new AmazonS3Client(credentials);
		String serviceurl = account.getValue("Url");
		s3.setEndpoint(serviceurl);
		S3ClientOptions s3ClientOptions = new S3ClientOptions();
		s3ClientOptions.setPathStyleAccess(true);
		s3.setS3ClientOptions(s3ClientOptions);
		String bucketName = account.getValue("Bucketname");
		s3.deleteObject(new DeleteObjectRequest(bucketName, path));
		System.out.println("Deleted " + path + "in S3.");
		return null;
	}

}

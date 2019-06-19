package hk.edu.polyu.intercloud.aws;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * The class uploads file to Amazon cloud.
 * 
 * @author Kate.xie
 *
 */
public class Upload {

	public String amazondo(String path) throws Exception {
		String bucketName = null;
		String key = FilenameUtils.getName(path);
		String serviceurl = null;

		GetAccountInfo account = new GetAccountInfo();
		BasicAWSCredentials credentials = new BasicAWSCredentials(
				account.getValue("Username"), account.getValue("Password"));
		AmazonS3 s3 = new AmazonS3Client(credentials);
		serviceurl = account.getValue("Url");
		s3.setEndpoint(serviceurl);
		S3ClientOptions s3ClientOptions = new S3ClientOptions();
		s3ClientOptions.setPathStyleAccess(true);
		s3.setS3ClientOptions(s3ClientOptions);

		bucketName = account.getValue("Bucketname");
		long start = System.currentTimeMillis();
		System.out.println("Uploading " + key + " to S3, START, " + start
				+ ", 0");
		s3.putObject(new PutObjectRequest(bucketName, key, new File(path)));
		long end = System.currentTimeMillis();
		System.out.println(": Uploading " + key + " to S3, END, " + end + ", "
				+ (end - start));

		return serviceurl + bucketName + "/" + key;

	}

}
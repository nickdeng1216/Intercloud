package hk.edu.polyu.intercloud.aws;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * The class is used to download file from Amazon cloud.
 * 
 * @author Kate.xie
 *
 */
public class Download {
	/**
	 * @key the file you will download
	 * @toPath download destination path
	 * @return download destination path
	 */
	public String amazondo(String key, String path) throws Exception {

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
		System.out.println("Downloading " + key + " from S3, START, " + start
				+ ", 0");
		S3Object object = s3.getObject(new GetObjectRequest(account
				.getValue("Bucketname"), key));
		long end1 = System.currentTimeMillis();
		System.out.println("Downloading " + key + " from S3, REQUESTED, "
				+ end1 + ", " + (end1 - start));
		InputStream reader = new BufferedInputStream(object.getObjectContent());
		File file = new File(path);
		OutputStream writer = new BufferedOutputStream(new FileOutputStream(
				file));
		int read = -1;
		while ((read = reader.read()) != -1) {
			writer.write(read);
		}
		writer.flush();
		writer.close();
		reader.close();
		long end2 = System.currentTimeMillis();
		System.out.println("Downloading " + key + " from S3, END, " + end2
				+ ", " + (end2 - start));
		return path;
	}

}

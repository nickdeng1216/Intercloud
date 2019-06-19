package hk.edu.polyu.intercloud.ctlstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class CtlStorageForIntercloud {

	private static final String CTL_PROP_FILE = System.getProperty("user.dir")
			+ "/ctl.properties";
	private static AWSCredentials awsCred;
	private static AmazonS3 s3Client;

	public CtlStorageForIntercloud() throws CtlStorageForIntercloudException {
		Properties properties = new Properties();

		InputStream ins;
		try {
			ins = new FileInputStream(CTL_PROP_FILE);
			properties.load(ins);
		} catch (IOException e1) {
			throw new CtlStorageForIntercloudException(e1.getMessage(), e1);
		}

		String endPoint = properties.getProperty("EndPoint");
		String accessKey = properties.getProperty("AccessKey");
		String secretKey = properties.getProperty("SecretKey");

		awsCred = new BasicAWSCredentials(accessKey, secretKey);
		s3Client = new AmazonS3Client(awsCred);
		s3Client.setEndpoint(endPoint);
	}

	public List<Bucket> listBuckets() throws CtlStorageForIntercloudException {
		List<Bucket> bucketList = new ArrayList<Bucket>();
		System.out.println("===================================");
		try {
			bucketList = s3Client.listBuckets();
			for (Bucket bucket : bucketList) {
				System.out.println("Name: " + bucket.getName()
						+ ", Creation Date: " + bucket.getCreationDate());
				return bucketList;
			}
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
		return bucketList;
	}

	public void makeBucket(String bucketName)
			throws CtlStorageForIntercloudException {
		try {
			Bucket bucket = s3Client.createBucket(bucketName);
			System.out.println("Name: " + bucket.getName()
					+ ", Creation Date: " + bucket.getCreationDate());
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public void removeBucket(String bucketName)
			throws CtlStorageForIntercloudException {
		try {
			s3Client.deleteBucket(bucketName);
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public ObjectListing listObjects(String bucketname)
			throws CtlStorageForIntercloudException {
		try {
			ObjectListing list = s3Client.listObjects(bucketname);
			do {
				List<S3ObjectSummary> summaries = list.getObjectSummaries();
				for (S3ObjectSummary summary : summaries) {
					System.out.println("[" + summary.getKey() + "] Size: "
							+ summary.getSize() + ", Last modified: "
							+ summary.getLastModified());
				}
				list = s3Client.listNextBatchOfObjects(list);
			} while (list.isTruncated());
			return list;
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public void removeObject(String bucketName, String key)
			throws CtlStorageForIntercloudException {
		try {
			DeleteObjectRequest dor = new DeleteObjectRequest(bucketName, key);
			s3Client.deleteObject(dor);
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public void getObject(String bucketName, String key, File toFile)
			throws CtlStorageForIntercloudException {
		try {
			GetObjectRequest getObj = new GetObjectRequest(bucketName, key);
			S3Object s3FileObj = s3Client.getObject(getObj);
			FileOutputStream out = new FileOutputStream(toFile);
			IOUtils.copy(s3FileObj.getObjectContent(), out);
			out.close();
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		} catch (IOException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public void putObject(String bucketName, String key, File fromFile)
			throws CtlStorageForIntercloudException {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(fromFile.length());
		try {
			PutObjectResult por = s3Client.putObject(new PutObjectRequest(
					bucketName, key, new FileInputStream(fromFile), meta));
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

	public boolean checkObject(String bucketName, String key)
			throws CtlStorageForIntercloudException {
		try {
			if (s3Client.getObjectMetadata(bucketName, key) != null) {
				return true;
			}
			return false;
		} catch (AmazonClientException e) {
			throw new CtlStorageForIntercloudException(e.getMessage(), e);
		}
	}

}

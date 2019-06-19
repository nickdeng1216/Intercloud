package hk.edu.polyu.intercloud.minio;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidObjectPrefixException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import io.minio.policy.PolicyType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

/**
 * <b>Feel Free to Modify</b>
 * 
 * @author harry
 * @version 2.0
 * @minioversion 3.0.4
 * @since 0.1
 */

public class MinioForIntercloud {

	private MinioClient minio;
	private static final String MINIO_PROP_FILE = System
			.getProperty("user.dir") + "/minio.properties";

	/**
	 * Constructor </p> <b>Examples:</b>
	 * 
	 * <pre>
	 * 		&#64;code
	 * 		MinioClient("http://10.11.11.11:9000", "ILKYO5O1BKWCC0EFQLC8",
	 * 	    "2jE21vn9dDRAzuPULm7BTjzC/hwainmXinfVGK3X");
	 * </pre>
	 * 
	 * @param Endpoint
	 *            Endpoint is an URL, domain name, IPv4 or IPv6 address. AWS S3
	 *            is compatible.
	 * @param AccessKey
	 *            Access key to access service in endpoint.
	 * @param SecretKey
	 *            Secret key to access service in endpoint.
	 * @param SecureConnection
	 *            (Not in use currently.) If true, access endpoint using HTTPS
	 *            else access it using HTTP.
	 * @throws MinioStorageException
	 */
	public MinioForIntercloud() throws MinioStorageException {
		Properties properties = new Properties();
		InputStream ins;
		try {
			ins = new FileInputStream(MINIO_PROP_FILE);
			properties.load(ins);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String endPoint = properties.getProperty("EndPoint");
		String accessKey = properties.getProperty("AccessKey");
		String secretKey = properties.getProperty("SecretKey");
		// boolean secure =
		// Boolean.valueOf(properties.getProperty("SecureConnection"));

		try {
			minio = new MinioClient(endPoint, accessKey, secretKey, false);
		} catch (InvalidEndpointException | InvalidPortException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Return free space of Minio server </b>
	 * 
	 * @return In unit of GB
	 */
	public long getFreeSpace() {
		File file = new File("c:");
		return file.getFreeSpace() / 1024 / 1024 / 1024;
	}

	/**
	 * <b>List buckets in this Minio</b>
	 * 
	 * @throws MinioStorageException
	 */
	public void listBuckets() throws MinioStorageException {
		List<Bucket> bucketList;
		try {
			bucketList = minio.listBuckets();
			ArrayList<String> buckets = new ArrayList<>();
			for (Bucket bucket : bucketList) {
				buckets.add(bucket.creationDate() + ", " + bucket.name());
			}
			fmtPrint(buckets);
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Check if bucketName exists</b>
	 * 
	 * @param bucketName
	 * @throws MinioStorageException
	 */
	public void checkBucket(String bucketName) throws MinioStorageException {
		try {
			boolean found = minio.bucketExists(bucketName);
			if (found) {
				fmtPrint(bucketName + " exists");
			} else {
				fmtPrint(bucketName + " does not exist");
			}
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Make a bucket in Minio</b>
	 * 
	 * @param bucketName
	 *            bucketName will change to lower case automatically
	 * @throws MinioStorageException
	 */
	public void makeBucket(String bucketName) throws MinioStorageException {
		try {
			if (minio.bucketExists(bucketName)) {
				fmtPrint(bucketName + " already exists");
			} else {
				minio.makeBucket(bucketName.toLowerCase());
				fmtPrint(bucketName + " is created successfully");
			}
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException
				| RegionConflictException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * Set policy on bucket and object prefix.
	 * 
	 * @param bucketName
	 *            Name of the bucket.
	 * @param objectPrefix
	 *            Policy applies to objects with prefix.
	 * @param policy
	 *            Policy to apply, available types are [PolicyType.NONE,
	 *            PolicyType.READ_ONLY, PolicyType.READ_WRITE,
	 *            PolicyType.WRITE_ONLY].
	 */
	public void setBucketPolicy(String bucketName, String objectPrefix,
			PolicyType policy) {
		try {
			minio.setBucketPolicy(bucketName, objectPrefix, policy);
			fmtPrint("Objects with prefix: " + objectPrefix
					+ " , have been set to " + policy + " in " + bucketName);
		} catch (InvalidKeyException | InvalidBucketNameException
				| InvalidObjectPrefixException | NoSuchAlgorithmException
				| InsufficientDataException | NoResponseException
				| ErrorResponseException | InternalException | IOException
				| XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get bucket policy at given objectPrefix.
	 * 
	 * @param bucketName
	 *            Name of the bucket.
	 * @param objectPrefix
	 *            Policy applies to objects with prefix.
	 */
	public void getBucketPolicy(String bucketName, String objectPrefix) {
		try {
			fmtPrint("Current policy: "
					+ minio.getBucketPolicy(bucketName, objectPrefix));
		} catch (InvalidKeyException | InvalidBucketNameException
				| InvalidObjectPrefixException | NoSuchAlgorithmException
				| InsufficientDataException | NoResponseException
				| ErrorResponseException | InternalException | IOException
				| XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <b>Remove a bucket</b>
	 * 
	 * @param bucketName
	 * @throws MinioStorageException
	 */
	public void removeBucket(String bucketName) throws MinioStorageException {
		try {
			if (minio.bucketExists(bucketName)) {
				minio.removeBucket(bucketName);
				fmtPrint(bucketName + " is removed successfully");
			} else {
				fmtPrint(bucketName + " does not exist");
			}
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>List objects in bucketName</b>
	 * 
	 * @param bucketName
	 * @throws MinioStorageException
	 */
	public void listObjects(String bucketName) throws MinioStorageException {
		Iterable<Result<Item>> theObjects;
		try {
			if (minio.bucketExists(bucketName)) {
				ArrayList<String> objs = new ArrayList<>();
				theObjects = minio.listObjects(bucketName);
				for (Result<Item> result : theObjects) {
					Item item = result.get();
					objs.add(item.lastModified() + ", " + item.objectSize()
							+ ", " + item.objectName());
				}
				fmtPrint(objs);
			} else {
				fmtPrint(bucketName + " does not exist");
			}
		} catch (XmlPullParserException | InvalidKeyException
				| InvalidBucketNameException | NoSuchAlgorithmException
				| InsufficientDataException | NoResponseException
				| ErrorResponseException | InternalException | IOException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Download objectName in bucketName to localPath; The Console display
	 * "downloading" when getting file, then will show "Already get XXX".</b>
	 * </p> <b>Example:</b>
	 * 
	 * <pre>
	 * 	&#64;code
	 * 	minio.getObject("cloud", "1.txt", "C:/test/2.txt");
	 * </pre>
	 * 
	 * @param bucketName
	 * @param objectName
	 *            objectName must contain extension
	 * @param localPath
	 * @throws MinioStorageException
	 */
	public void getObject(String bucketName, String objectName, String localPath)
			throws MinioStorageException {
		try {
			InputStream stream = minio.getObject(bucketName, objectName);
			ObjectInputStream oStream = new ObjectInputStream(stream);
			File targetFile = new File(localPath);
			while (!oStream.closed) {
				// System.out.println("downloading...");
				FileUtils.copyInputStreamToFile(oStream, targetFile);
			}
			fmtPrint("Already get " + objectName);
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | InvalidArgumentException | IOException
				| XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	&#64;code
	 * 	  minio.putObject("C:/test/2.txt", "cloud", "1.txt");
	 * </pre>
	 * 
	 * @param localPath
	 *            The path of file to be uploaded.
	 * @param bucketName
	 *            Name of the destination bucket.
	 * @param objectName
	 *            ObjectName is what will be shown in bucket.
	 * @throws MinioStorageException
	 */
	public void putObject(String localPath, String bucketName, String objectName)
			throws MinioStorageException {
		try {
			FileInputStream fStream = new FileInputStream(localPath);
			minio.putObject(bucketName, objectName, localPath);
			fStream.close();
			fmtPrint(objectName + " is uploaded to " + bucketName
					+ " successfully");
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | InvalidArgumentException | IOException
				| XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	&#64;code
	 * 	  minio.putObject("123qwe", "cloud", "1.txt");
	 * </pre>
	 * 
	 * @param data
	 *            Data String.
	 * @param bucketName
	 *            Name of the destination bucket.
	 * @param objectName
	 *            ObjectName is what will be shown in bucket.
	 * @throws MinioStorageException
	 */
	public void putData(String data, String bucketName, String objectName)
			throws MinioStorageException {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(
					data.getBytes("UTF-8"));
			minio.putObject(bucketName, objectName, bais, bais.available(),
					"application/octet-stream");
			fmtPrint("Data has been uploaded to " + objectName
					+ " successfully");
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | InvalidArgumentException | IOException
				| XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * Gets metadata of an object.
	 * 
	 * @param bucketName
	 * @param objectName
	 */
	public void statObject(String bucketName, String objectName) {
		try {
			ObjectStat objectStat = minio.statObject(bucketName, objectName);
			fmtPrint(objectStat.toString());
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check whether an object exists.
	 * 
	 * @param bucketName
	 *            Name of the destination bucket.
	 * @param objectName
	 *            ObjectName is what will be shown in bucket.
	 * @return
	 * @throws MinioStorageException
	 */
	public boolean checkObject(String bucketName, String objectName)
			throws MinioStorageException {
		try {
			ObjectStat objectStat = minio.statObject(bucketName, objectName);
			if (objectStat != null) {
				return true;
			}
			return false;
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	/**
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	&#64;code
	 * 	  minio.removeObject("cloud", "1.txt");
	 * </pre>
	 * 
	 * @param bucketName
	 * @param objectName
	 *            objectName must contain extension
	 * @throws MinioStorageException
	 */
	public void removeObject(String bucketName, String objectName)
			throws MinioStorageException {
		try {
			minio.removeObject(bucketName, objectName);
			fmtPrint("Successfully removed " + objectName + " in " + bucketName);
		} catch (InvalidKeyException | InvalidBucketNameException
				| NoSuchAlgorithmException | InsufficientDataException
				| NoResponseException | ErrorResponseException
				| InternalException | IOException | XmlPullParserException e) {
			throw new MinioStorageException(e.getMessage());
		}
	}

	private void fmtPrint(String s) {
		int l = s.length();
		String second = "=  " + s + "  =";
		for (int m = l + 6; m > 0; m--) {
			System.out.print("=");
		}
		System.out.println("\n" + second);
		for (int m = l + 6; m > 0; m--) {
			System.out.print("=");
		}
		System.out.println("\n");
	}

	private void fmtPrint(ArrayList<String> info) {
		String max = Collections.max(info,
				Comparator.comparing(s -> s.length()));
		int l = max.length();
		for (int m = l + 6; m > 0; m--) {
			System.out.print("=");
		}
		for (String str : info) {
			int m = l - str.length() + 3;
			String format = "%n%s%s" + "%" + m + "s";
			System.out.printf(format, "=  ", str, "=");
		}
		System.out.println("");
		for (int m = l + 6; m > 0; m--) {
			System.out.print("=");
		}
		System.out.println("\n");
	}
}

class ObjectInputStream extends InputStream {

	private InputStream stream;
	public boolean closed = false;

	public ObjectInputStream(InputStream stream) {
		super();
		this.stream = stream;
	}

	@Override
	public synchronized int read() throws IOException {
		return stream.read();
	}

	@Override
	public void close() throws IOException {
		stream.close();
		closed = true;
	}
}

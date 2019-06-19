package hk.edu.polyu.intercloud.gstorage;

import hk.edu.polyu.intercloud.common.Common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import com.pliablematter.cloudstorage.CloudStorage;

public class GoogleStorageIntercloud {

	public long getQuotaStorage() {
		return Long.MAX_VALUE;
	}

	public long getAvailableStorage() {
		return Long.MAX_VALUE;
	}

	public List<String> listBuckets() throws Exception {
		return CloudStorage.listBuckets();
	}

	public void createBucket(String bucketName, String storageClass)
			throws Exception {
		try {
			CloudStorage.createBucket(bucketName, storageClass);
		} catch (Exception e) {
			String[] err = getException(e);
			if (err[0].equals("409")) {
				System.err.println("Unable to create *" + bucketName
						+ "* due to name conflict.");
				return; // still treat as successful
			}
			throw e;
		}
	}

	public void deleteBucket(String bucketName, boolean force) throws Exception {
		try {
			CloudStorage.deleteBucket(bucketName);
		} catch (Exception e) {
			String[] err = getException(e);
			if (err[0].equals("404")) {
				System.err.println("Unable to delete *" + bucketName
						+ "* because it does not exist.");
				return; // still treat as successful
			}
			if (err[0].equals("409")) {
				if (force) {
					System.out.println("*" + bucketName
							+ "* is not empty. Force deleting it.");
					List<String> allFiles = listBucket(bucketName);
					for (String file : allFiles) {
						deleteFile(bucketName, file);
					}
					deleteBucket(bucketName, false);
					return;
				}
				System.err.println("Unable to delete *" + bucketName
						+ "* because it is not empty.");
			}
			throw e;
		}
	}

	public List<String> listBucket(String bucketName) throws Exception {
		return CloudStorage.listBucket(bucketName);
	}

	public String uploadFile(String bucketName, String filePath)
			throws Exception {
		CloudStorage.uploadFile(bucketName, filePath);
		String baseName = FilenameUtils.getBaseName(filePath);
		String extension = FilenameUtils.getExtension(filePath);
		return "https://console.cloud.google.com/m/cloudstorage/b/"
				+ bucketName + "/o/" + baseName + "." + extension
				+ "?authuser=1";
	}

	public String downloadFile(String bucketName, String fileName,
			String destinationDirectory) throws Exception {
		CloudStorage.downloadFile(bucketName, fileName, destinationDirectory);
		return Common.DOWNLOAD_PATH + fileName;
	}

	public void deleteFile(String bucketName, String fileName) throws Exception {
		CloudStorage.deleteFile(bucketName, fileName);
	}

	public JSONObject getMetaData(String bucketName, String fileName)
			throws Exception {
		return new JSONObject(
				CloudStorage.getFileMetadata(bucketName, fileName));
	}

	public Date getRemoteFileDate(String bucketName, String fileName)
			throws Exception {
		String dateString = getMetaData(bucketName, fileName).getString(
				"updated");
		dateString = dateString.replace("T", " ");
		dateString = dateString.replace("Z", "");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return sdf.parse(dateString);
	}

	public String[] getException(Exception e) {
		String newline = System.getProperty("line.separator");
		String fullMessage = e.getMessage();
		String firstLine = fullMessage.substring(0,
				fullMessage.indexOf(newline));
		JSONObject j = new JSONObject(fullMessage.substring(fullMessage
				.indexOf(newline) + 1));
		String[] err = { String.valueOf(j.getInt("code")), firstLine,
				j.getString("message") };
		System.err.println(err[1] + ": " + err[2]);
		return err;
	}
}

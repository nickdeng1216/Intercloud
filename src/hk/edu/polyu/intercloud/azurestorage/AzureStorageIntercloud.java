package hk.edu.polyu.intercloud.azurestorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Properties;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class AzureStorageIntercloud {

	private static final String AZURE_STORAGE_PROP_FILE = System
			.getProperty("user.dir") + "/" + "azurestorage.properties";
	private String connectionstring;
	private CloudStorageAccount account;
	private CloudBlobClient blobClient;

	public AzureStorageIntercloud() throws IOException, InvalidKeyException,
			URISyntaxException, StorageException {
		Properties properties = new Properties();
		InputStream inputStream;
		inputStream = new FileInputStream(AZURE_STORAGE_PROP_FILE);
		properties.load(inputStream);
		connectionstring = properties.getProperty("connectionstring");
		// Setup the cloud storage account.
		account = CloudStorageAccount.parse(connectionstring);
		// Create a blob service client
		blobClient = account.createCloudBlobClient();
	}

	public long getQuotaSize() {
		return Long.MAX_VALUE;
	}

	public long getAvailableSize() {
		return Long.MAX_VALUE;
	}

	public CloudBlobContainer getContainer(String containername)
			throws StorageException, URISyntaxException {
		// Get a reference to a container
		CloudBlobContainer container = blobClient
				.getContainerReference(containername);
		// Create the container if it does not exist
		container.createIfNotExists();

		// Make the container public
		// Create a permissions object
		BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
		// Include public access in the permissions object
		containerPermissions
				.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
		// Set the permissions on the container
		container.uploadPermissions(containerPermissions);
		return container;
	}

	public String uploadTextData(String containername, String text, String ref,
			boolean overwrite) throws URISyntaxException, StorageException,
			IOException, AzureStorageIntercloudException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		if (!overwrite && blob.exists()) {
			throw new AzureStorageIntercloudException(
					"Blob reference already exists.");
		}
		// Upload text to the blob
		blob.uploadText(text);
		return "https://azure.com";
	}

	public String downloadTextData(String containername, String ref)
			throws URISyntaxException, StorageException, IOException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		return blob.downloadText();
	}

	public void uploadFile(String containername, String uploadFrom, String ref,
			boolean overwrite) throws StorageException, URISyntaxException,
			AzureStorageIntercloudException, FileNotFoundException, IOException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		if (!overwrite && blob.exists()) {
			throw new AzureStorageIntercloudException(
					"Blob reference already exists.");
		}
		// Upload file to blob
		File file = new File(uploadFrom);
		blob.upload(new FileInputStream(file.getAbsolutePath()), file.length());
	}

	public String downloadFile(String containername, String ref,
			String downloadTo) throws URISyntaxException, StorageException,
			IOException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		// Download file
		File destinationFile = new File(downloadTo);
		blob.downloadToFile(destinationFile.getAbsolutePath());
		return downloadTo;
	}

	public void deleteFile(String containername, String ref)
			throws URISyntaxException, StorageException, IOException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		// Delete File
		blob.delete();
	}

	public boolean checkFile(String containername, String ref)
			throws StorageException, URISyntaxException {
		// Get the container
		CloudBlobContainer container = getContainer(containername);
		// Get a reference to a blob in the container
		CloudBlockBlob blob = container.getBlockBlobReference(ref);
		// Check file
		return blob.exists();
	}
}

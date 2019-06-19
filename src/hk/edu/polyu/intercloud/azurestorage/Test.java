package hk.edu.polyu.intercloud.azurestorage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.microsoft.azure.storage.StorageException;

public class Test {

	public static void main(String[] args) throws InvalidKeyException,
			IOException, URISyntaxException, StorageException,
			AzureStorageIntercloudException {
		AzureStorageIntercloud a = new AzureStorageIntercloud();
		a.uploadTextData("intercloudperftest01", "Hello cloud", "Salute1", true);
		System.out.println("Downloaded: "
				+ a.downloadTextData("intercloudperftest01", "Salute1"));
		a.uploadFile("intercloudperftest01", "sofia.jpg", "Image1", true);
		a.downloadFile("intercloudperftest01", "Image1", "sofia2.jpg");
	}
}

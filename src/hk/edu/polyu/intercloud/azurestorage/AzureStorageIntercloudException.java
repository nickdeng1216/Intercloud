package hk.edu.polyu.intercloud.azurestorage;

public class AzureStorageIntercloudException extends Exception {

	private static final long serialVersionUID = -7290102775895059044L;

	public AzureStorageIntercloudException(String message) {
		super(message);
	}

	public AzureStorageIntercloudException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

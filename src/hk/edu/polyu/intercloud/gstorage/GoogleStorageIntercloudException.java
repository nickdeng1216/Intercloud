package hk.edu.polyu.intercloud.gstorage;

public class GoogleStorageIntercloudException extends Exception {

	private static final long serialVersionUID = 3547469164599464503L;

	public GoogleStorageIntercloudException(String message) {
		super(message);
	}

	public GoogleStorageIntercloudException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

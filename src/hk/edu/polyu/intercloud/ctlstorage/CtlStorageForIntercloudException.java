package hk.edu.polyu.intercloud.ctlstorage;

public class CtlStorageForIntercloudException extends Exception {

	private static final long serialVersionUID = -4196162405069012381L;

	public CtlStorageForIntercloudException(String message) {
		super(message);
	}

	public CtlStorageForIntercloudException(String message, Throwable throwable) {
		super(message, throwable);
	}

}

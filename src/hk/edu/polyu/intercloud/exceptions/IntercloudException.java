package hk.edu.polyu.intercloud.exceptions;

public class IntercloudException extends Exception {

	private static final long serialVersionUID = -3810899313904354550L;

	public IntercloudException(String message) {
		super(message);
	}

	public IntercloudException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

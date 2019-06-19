package hk.edu.polyu.intercloud.exceptions;

public class ObjectStorageAPIException extends Exception {

	private static final long serialVersionUID = 6640813125598642662L;

	public ObjectStorageAPIException(String message) {
		super(message);
	}

	public ObjectStorageAPIException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

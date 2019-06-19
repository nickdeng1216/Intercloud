package hk.edu.polyu.intercloud.exceptions;

public class SecurityException extends Exception {

	private static final long serialVersionUID = 2817260583807041838L;

	public SecurityException(String message) {
		super(message);
	}

	public SecurityException(String message, Throwable throwable) {
		super(message, throwable);
	}

}

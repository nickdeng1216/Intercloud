package hk.edu.polyu.intercloud.fileserver.exceptions;

public class HttpdException extends Exception {

	private static final long serialVersionUID = -2958482794394319131L;

	public HttpdException(String message) {
		super(message);
	}

	public HttpdException(String message, Throwable throwable) {
		super(message, throwable);
	}

}

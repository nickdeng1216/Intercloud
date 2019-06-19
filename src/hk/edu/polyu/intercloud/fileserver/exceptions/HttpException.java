package hk.edu.polyu.intercloud.fileserver.exceptions;

public class HttpException extends Exception {

	private static final long serialVersionUID = 3903855254221130354L;

	public HttpException(String message) {
		super(message);
	}

	public HttpException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

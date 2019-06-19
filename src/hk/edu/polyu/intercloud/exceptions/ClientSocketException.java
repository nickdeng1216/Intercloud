package hk.edu.polyu.intercloud.exceptions;

public class ClientSocketException extends Exception {

	private static final long serialVersionUID = 2579006903416099153L;

	public ClientSocketException(String message) {
		super(message);
	}

	public ClientSocketException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

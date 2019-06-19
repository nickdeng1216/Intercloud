package hk.edu.polyu.intercloud.exceptions;

public class AuthenticationUnsuccessException extends Exception {
	private static final long serialVersionUID = 2212136348188825603L;

	public AuthenticationUnsuccessException(String message) {
		super(message);
	}

	public AuthenticationUnsuccessException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

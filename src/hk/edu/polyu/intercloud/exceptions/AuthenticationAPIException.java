package hk.edu.polyu.intercloud.exceptions;

public class AuthenticationAPIException extends Exception {

	private static final long serialVersionUID = -1263477042757861096L;

	public AuthenticationAPIException(String msg) {
		super(msg);
	}

	public AuthenticationAPIException(String msg, Throwable t) {
		super(msg, t);
	}
}

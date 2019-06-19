package hk.edu.polyu.intercloud.exceptions;

public class ClientInvokerException extends Exception {

	private static final long serialVersionUID = 6164817309337729373L;

	public ClientInvokerException(String message) {
		super(message);
	}

	public ClientInvokerException(String message, Throwable throwable) {
		super(message, throwable);
	}

}

package hk.edu.polyu.intercloud.fileserver.exceptions;

public class UdtException extends Exception {

	private static final long serialVersionUID = -6991157419493609576L;

	public UdtException(String message) {
		super(message);
	}

	public UdtException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

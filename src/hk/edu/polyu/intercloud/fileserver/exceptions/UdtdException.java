package hk.edu.polyu.intercloud.fileserver.exceptions;

public class UdtdException extends Exception {

	private static final long serialVersionUID = -6991157419493609576L;

	public UdtdException(String message) {
		super(message);
	}

	public UdtdException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

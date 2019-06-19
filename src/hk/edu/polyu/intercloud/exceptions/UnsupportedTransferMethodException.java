package hk.edu.polyu.intercloud.exceptions;

public class UnsupportedTransferMethodException extends Exception {

	private static final long serialVersionUID = 1568992348900L;

	public UnsupportedTransferMethodException(String msg) {
		super(msg);
	}

	public UnsupportedTransferMethodException(String msg, Throwable throwable) {
		super(msg, throwable);
	}
}

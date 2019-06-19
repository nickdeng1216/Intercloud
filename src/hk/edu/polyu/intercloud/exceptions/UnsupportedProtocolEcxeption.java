package hk.edu.polyu.intercloud.exceptions;

public class UnsupportedProtocolEcxeption extends Exception {
	private static final long serialVersionUID = 241153648188825603L;

	public UnsupportedProtocolEcxeption(String message) {
		super(message);
	}

	public UnsupportedProtocolEcxeption(String message, Throwable throwable) {
		super(message, throwable);
	}
}

package hk.edu.polyu.intercloud.exceptions;

public class DataVerificationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8855675647863898454L;

	public DataVerificationException(String msg) {
		super(msg);
	}

	public DataVerificationException(String msg, Throwable t) {
		super(msg, t);
	}
}

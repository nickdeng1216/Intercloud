package hk.edu.polyu.intercloud.exceptions;

public class NoSuchDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8909288272937508227L;

	public NoSuchDataException(String msg) {
		super(msg);
	}

	public NoSuchDataException(String msg, Throwable t) {
		super(msg, t);
	}
}

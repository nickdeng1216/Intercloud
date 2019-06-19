package hk.edu.polyu.intercloud.exceptions;

public class NoProviderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7093396013484473947L;

	public NoProviderException(String msg) {
		super(msg);
	}

	public NoProviderException(String msg, Throwable t) {
		super(msg, t);
	}

}

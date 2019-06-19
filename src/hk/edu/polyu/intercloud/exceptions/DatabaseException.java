package hk.edu.polyu.intercloud.exceptions;

public class DatabaseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8965289969803033306L;

	public DatabaseException(String msg) {
		super(msg);
	}

	public DatabaseException(String msg, Throwable t) {
		super(msg, t);
	}

}

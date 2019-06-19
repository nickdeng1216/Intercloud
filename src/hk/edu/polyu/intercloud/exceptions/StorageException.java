package hk.edu.polyu.intercloud.exceptions;

public class StorageException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9127828566104322645L;

	public StorageException(String msg) {
		super(msg);
	}

	public StorageException(String msg, Throwable t) {
		super(msg, t);
	}

}

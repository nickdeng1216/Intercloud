package hk.edu.polyu.intercloud.exceptions;

public class FileServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1262831670467659706L;

	public FileServerException(String msg) {
		super(msg);
	}

	public FileServerException(String msg, Throwable t) {
		super(msg, t);
	}
}

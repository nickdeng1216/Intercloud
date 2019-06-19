package hk.edu.polyu.intercloud.exceptions;

public class DataSecurityLevelUnmatchedException extends Exception {

	private static final long serialVersionUID = 1312531670467659709L;

	public DataSecurityLevelUnmatchedException(String message) {
		super(message);
	}

	public DataSecurityLevelUnmatchedException(String message, Throwable t) {
		super(message, t);

	}

}

package hk.edu.polyu.intercloud.exceptions;

public class UnmatchedDataSecuritylevel extends Exception {

	private static final long serialVersionUID = 1312531670467659709L;

	public UnmatchedDataSecuritylevel(String message) {
		super(message);
	}

	public UnmatchedDataSecuritylevel(String message, Throwable t) {
		super(message, t);

	}

}

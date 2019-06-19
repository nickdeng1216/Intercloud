package hk.edu.polyu.intercloud.exceptions;

public class InquireResourceAPIException extends Exception {

	private static final long serialVersionUID = -1262177046757661096L;

	public InquireResourceAPIException(String msg) {
		super(msg);
	}

	public InquireResourceAPIException(String msg, Throwable t) {
		super(msg, t);
	}
}

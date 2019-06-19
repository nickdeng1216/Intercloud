package hk.edu.polyu.intercloud.exceptions;

public class DNSException extends Exception {

	private static final long serialVersionUID = -1262836570467659706L;

	public DNSException(String msg) {
		super(msg);
	}

	public DNSException(String msg, Throwable t) {
		super(msg, t);
	}
}

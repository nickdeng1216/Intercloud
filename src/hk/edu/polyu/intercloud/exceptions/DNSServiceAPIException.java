package hk.edu.polyu.intercloud.exceptions;

public class DNSServiceAPIException extends Exception {

	private static final long serialVersionUID = -1262837046757659706L;

	public DNSServiceAPIException(String msg) {
		super(msg);
	}

	public DNSServiceAPIException(String msg, Throwable t) {
		super(msg, t);
	}

}

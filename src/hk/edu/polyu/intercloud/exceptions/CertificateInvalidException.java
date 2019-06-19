package hk.edu.polyu.intercloud.exceptions;

public class CertificateInvalidException extends Exception {
	private static final long serialVersionUID = 212213648188825603L;

	public CertificateInvalidException(String message) {
		super(message);
	}

	public CertificateInvalidException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

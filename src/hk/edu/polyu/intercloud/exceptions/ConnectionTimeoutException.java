package hk.edu.polyu.intercloud.exceptions;

public class ConnectionTimeoutException extends IntercloudException {

	private static final long serialVersionUID = 5641153648188825603L;

	public ConnectionTimeoutException(String message) {
		super(message);
	}

	public ConnectionTimeoutException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

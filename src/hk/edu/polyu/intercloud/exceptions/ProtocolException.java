package hk.edu.polyu.intercloud.exceptions;

public class ProtocolException extends IntercloudException {

	private static final long serialVersionUID = -116749016101721721L;

	public ProtocolException(String message) {
		super(message);
	}

	public ProtocolException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
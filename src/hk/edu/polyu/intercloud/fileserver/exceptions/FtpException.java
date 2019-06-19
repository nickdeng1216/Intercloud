package hk.edu.polyu.intercloud.fileserver.exceptions;

public class FtpException extends Exception {

	private static final long serialVersionUID = 4689801693057891943L;

	public FtpException(String message) {
		super(message);
	}

	public FtpException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

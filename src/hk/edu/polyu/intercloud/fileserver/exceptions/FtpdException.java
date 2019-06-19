package hk.edu.polyu.intercloud.fileserver.exceptions;

public class FtpdException extends Exception {

	private static final long serialVersionUID = 4689801693057891943L;

	public FtpdException(String message) {
		super(message);
	}

	public FtpdException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

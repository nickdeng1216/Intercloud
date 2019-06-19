package hk.edu.polyu.intercloud.exceptions;

public class UnmatchedChallengeException extends Exception {

	private static final long serialVersionUID = 82531670467659709L;

	public UnmatchedChallengeException(String message) {
		super(message);
	}

	public UnmatchedChallengeException(String message, Throwable t) {
		super(message, t);

	}
}

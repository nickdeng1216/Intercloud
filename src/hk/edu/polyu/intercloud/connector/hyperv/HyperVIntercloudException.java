package hk.edu.polyu.intercloud.connector.hyperv;

public class HyperVIntercloudException extends Exception {

	private static final long serialVersionUID = -2188322602011197716L;

	public static final String NAME_ALREADY_EXISTS = "A VM with the same name already exists.";
	public static final String NAME_NOT_UNIQUE = "The VM name provided is not unique.";

	public HyperVIntercloudException(String message) {
		super(message);
	}

	public HyperVIntercloudException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

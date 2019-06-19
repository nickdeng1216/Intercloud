package hk.edu.polyu.intercloud.exceptions;

public class VirtualMachineAPIException extends Exception {
	private static final long serialVersionUID = 6621813125598642662L;

	public VirtualMachineAPIException(String message) {
		super(message);
	}

	public VirtualMachineAPIException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

package hk.edu.polyu.intercloud.exceptions;

public class VirtualMachineException extends Exception {

	private static final long serialVersionUID = -6242372551769183797L;

	public VirtualMachineException(String message) {
		super(message);
	}

	public VirtualMachineException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

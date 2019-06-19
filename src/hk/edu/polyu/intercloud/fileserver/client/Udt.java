package hk.edu.polyu.intercloud.fileserver.client;

import hk.edu.polyu.intercloud.fileserver.exceptions.UdtException;
import hk.edu.polyu.intercloud.fileserver.util.CmdLauncher;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Udt implements Callable<Void> {
	private String ip, remote, local;
	private int port;

	private Udt(String ip, int port, String remote, String local) {
		this.ip = ip;
		this.port = port;
		this.remote = remote;
		this.local = local;
	}

	/**
	 * Receive a file from UDT server
	 * 
	 * @param ip
	 * @param port
	 * @param remote
	 *            absolute or relative path to the file on server
	 * @param local
	 *            destination of the file to be saved
	 * @throws UdtException
	 */
	public static void download(String ip, int port, String remote, String local)
			throws UdtException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Udt udt = new Udt(ip, port, remote, local);
		Future<Void> future = executor.submit(udt);
		long start = System.currentTimeMillis();
		System.out.println(">>> UDT downloading (" + remote + ") from " + ip
				+ " starts. [" + start + "]");
		try {
			future.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new UdtException(e.getMessage(), e);
		}
		long end = System.currentTimeMillis();
		long timeUsed = end - start;
		long fileSize = new File(local).length() / 1024;
		double speed = (double) fileSize / (timeUsed / 1000);
		System.out.println(">>> UDT downloading (" + remote + ") from " + ip
				+ " ends in " + timeUsed + "ms, at "
				+ new DecimalFormat("#.##").format(speed) + "KB/s. [" + end
				+ "]");
		executor.shutdown();
	}

	@Override
	public Void call() throws Exception {
		startClient(ip, port, remote, local);
		return null;
	}

	private void startClient(String ip, int port, String remote, String local)
			throws UdtException {
		String command = System.getProperty("user.dir") + "/UDT/recvfile.exe "
				+ ip + " " + port + " " + remote + " " + local;
		String output;
		try {
			output = CmdLauncher.runCommand(command);
		} catch (IOException e) {
			throw new UdtException(e.getMessage(), e);
		}
		System.out.println(output);
		if (!output.equals("")) {
			throw new UdtException("Unable to start UDT client. Output: "
					+ output);
		}
		System.out.println("OK");
	}
}

package hk.edu.polyu.intercloud.fileserver.server;

import hk.edu.polyu.intercloud.fileserver.exceptions.UdtdException;
import hk.edu.polyu.intercloud.fileserver.util.CmdLauncher;

import java.io.IOException;

public class Udtd {

	/**
	 * Start the UDT server
	 * 
	 * @param port
	 */
	public static void startServer(int port) {
		System.out.println("Starting UDT server on port " + port);
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					String command = System.getProperty("user.dir")
							+ "/UDT/sendfile.exe " + port;
					String output = CmdLauncher.runCommand(command);
					if (!output.startsWith("server is ready at port")) {
						throw new UdtdException(
								"Unable to start UDT server. Output: " + output);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UdtdException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	/**
	 * Kill the UDT server
	 * 
	 * @throws UdtdException
	 */
	public static void killServer() throws UdtdException {
		String command = "taskkill /f /im sendfile.exe";
		try {
			CmdLauncher.runCommand(command);
		} catch (IOException e) {
			throw new UdtdException("Unable to kill UDT server.", e);
		}
	}

}

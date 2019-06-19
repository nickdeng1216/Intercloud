package hk.edu.polyu.intercloud.fileserver.server;

import hk.edu.polyu.intercloud.fileserver.exceptions.HttpdException;
import hk.edu.polyu.intercloud.fileserver.util.CmdLauncher;

import java.io.IOException;

public class Httpd {

	/**
	 * Start Caddy HTTP server
	 * 
	 * @param domain
	 *            The domain name to be used
	 * @param httpDocsPath
	 *            The root path of HTTP docs
	 * @param sslEmail
	 *            The email address for SSL cert
	 * @param ssl
	 *            Use HTTPS or not
	 * @throws HttpdException
	 */
	public static void startHttpServer(String domain, String httpDocsPath,
			String sslEmail, boolean ssl) throws HttpdException {
		if (isRunning()) {
			return;
		}
		if (ssl) {
			System.out
					.println("Starting HTTP server on 80 and HTTPS server on 443");
			final String command = "caddy -host " + domain + " -root "
					+ httpDocsPath + " -agree -email " + sslEmail;
			System.out.println(command);
			try {
				Thread t = new Thread() {
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(command);
				};
				t.start();
			} catch (IOException e) {
				throw new HttpdException(e.getMessage(), e);
			}
		} else {
			System.out.println("Starting HTTP server on 80 (NO HTTPS server)");
			final String command = "caddy -port 80 -root " + httpDocsPath;
			System.out.println(command);
			try {
				Thread t = new Thread() {
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(command);
				};
				t.start();
			} catch (IOException e) {
				throw new HttpdException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Kill all running Caddy HTTP server instance
	 * 
	 * @throws HttpdException
	 */
	public static void killHttpServer() throws HttpdException {
		String command = "taskkill /f /im caddy.exe";
		try {
			CmdLauncher.runCommand(command);
		} catch (IOException e) {
			throw new HttpdException("Unable to kill Caddy HTTP server.", e);
		}
	}

	/**
	 * Check if Caddy HTTP server is running
	 * 
	 * @return true if running, false otherwise
	 */
	public static boolean isRunning() {
		String command = "check_caddy.cmd";
		try {
			String result = CmdLauncher.runCommand(command);
			if (result.contains("Caddy server is running")) {
				System.out.println(result);
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
}

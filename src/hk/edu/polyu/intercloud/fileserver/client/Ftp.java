package hk.edu.polyu.intercloud.fileserver.client;

import hk.edu.polyu.intercloud.fileserver.exceptions.FtpException;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Ftp implements Callable<Void> {

	public static final String DOWNLOAD = "DOWNLOAD";
	public static final String UPLOAD = "UPLOAD";
	public static final String LIST = "LIST";

	private String action, ip, username, password, remoteFile, localFile;
	private int port;
	private boolean activeMode, anonymous, ssl;

	private Ftp(String action, String ip, int port, String username,
			String password, String remoteFile, String localFile,
			boolean activeMode, boolean anonymous, boolean ssl) {
		this.action = action;
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.remoteFile = remoteFile;
		this.localFile = localFile;
		this.activeMode = activeMode;
		this.anonymous = anonymous;
		this.ssl = ssl;
	}

	/**
	 * Download a file from the FTP or FTPS server
	 * 
	 * @param ip
	 * @param port
	 * @param username
	 * @param password
	 * @param remoteFile
	 * @param localFile
	 * @param activeMode
	 * @param anonymous
	 * @param ssl
	 * @throws FtpException
	 */
	public static void download(String ip, int port, String username,
			String password, String remoteFile, String localFile,
			boolean activeMode, boolean anonymous, boolean ssl)
			throws FtpException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Ftp ftp = new Ftp(Ftp.DOWNLOAD, ip, port, username, password,
				remoteFile, localFile, activeMode, anonymous, ssl);
		Future<Void> future = executor.submit(ftp);
		long start = System.currentTimeMillis();
		System.out.println(">>> FTP downloading (" + remoteFile + ") from "
				+ ip + " starts. [" + start + "]");
		try {
			future.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new FtpException(e.getMessage(), e);
		}
		long end = System.currentTimeMillis();
		long timeUsed = end - start;
		long fileSize = new File(localFile).length() / 1024;
		double speed = (double) fileSize / (timeUsed / 1000);
		System.out.println(">>> FTP downloading (" + remoteFile + ") from "
				+ ip + " ends in " + timeUsed + "ms, at "
				+ new DecimalFormat("#.##").format(speed) + "KB/s. [" + end
				+ "]");
		executor.shutdown();
	}

	/**
	 * Upload a file to the FTP or FTPS server
	 * 
	 * @param ip
	 * @param port
	 * @param username
	 * @param password
	 * @param remoteFile
	 * @param localFile
	 * @param activeMode
	 * @param anonymous
	 * @param ssl
	 * @throws FtpException
	 */
	public static void upload(String ip, int port, String username,
			String password, String remoteFile, String localFile,
			boolean activeMode, boolean anonymous, boolean ssl)
			throws FtpException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Ftp ftp = new Ftp(Ftp.UPLOAD, ip, port, username, password, remoteFile,
				localFile, activeMode, anonymous, ssl);
		Future<Void> future = executor.submit(ftp);
		try {
			future.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new FtpException(e.getMessage(), e);
		}
		executor.shutdown();
	}

	/**
	 * List the files located on the FTP or FTPS server
	 * 
	 * @param ip
	 * @param port
	 * @param username
	 * @param password
	 * @param remoteFile
	 * @param activeMode
	 * @param anonymous
	 * @param ssl
	 * @throws FtpException
	 */
	public static void list(String ip, int port, String username,
			String password, String remoteFile, boolean activeMode,
			boolean anonymous, boolean ssl) throws FtpException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Ftp ftp = new Ftp(Ftp.LIST, ip, port, username, password, remoteFile,
				"", activeMode, anonymous, ssl);
		Future<Void> future = executor.submit(ftp);
		try {
			future.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new FtpException(e.getMessage(), e);
		}
		executor.shutdown();
	}

	@Override
	public Void call() throws Exception {
		if (action.equals(Ftp.DOWNLOAD)) {
			downloadFile(ip, port, username, password, remoteFile, localFile,
					activeMode, anonymous, ssl);
		} else if (action.equals(Ftp.UPLOAD)) {
			uploadFile(ip, port, username, password, remoteFile, localFile,
					activeMode, anonymous, ssl);
		} else if (action.equals(Ftp.LIST)) {
			listFiles(ip, port, username, password, remoteFile, activeMode,
					anonymous, ssl);
		}
		return null;
	}

	private void downloadFile(String ip, int port, String username,
			String password, String remoteFile, String localFile,
			boolean activeMode, boolean anonymous, boolean ssl)
			throws FtpException {

		// Check params
		if (ip == null || username == null || password == null
				|| remoteFile == null || localFile == null) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n" + "Parameters cannot be null.");
		}
		if ((ssl && activeMode) || (ssl && anonymous)) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n"
					+ "FTPS does not allow active mode or anonymous login.");
		}

		// Build args
		List<String> argList = new ArrayList<String>();
		if (activeMode) {
			argList.add("-a");
		}
		if (anonymous) {
			argList.add("-A");
		}
		if (ssl) {
			argList.add("-p");
			argList.add("SSL,true");
			System.out.println("FTP over SSL");
		} else {
			System.out.println("Plain FTP");
		}
		argList.add("-b");
		argList.add(ip + ":" + port);
		argList.add(username);
		argList.add(password);
		argList.add(remoteFile);
		argList.add(localFile);
		String args[] = argList.toArray(new String[0]);
		// System.out.println(Arrays.toString(args));

		// Run Apache Common's FTP client
		try {
			FtpClientExample.main(args);
		} catch (IOException e) {
			throw new FtpException(e.getMessage(), e);
		}
	}

	private void listFiles(String ip, int port, String username,
			String password, String remoteFile, boolean activeMode,
			boolean anonymous, boolean ssl) throws FtpException {

		// Check params
		if (ip == null || username == null || password == null
				|| remoteFile == null) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n" + "Parameters cannot be null.");
		}
		if ((ssl && activeMode) || (ssl && anonymous)) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n"
					+ "FTPS does not allow active mode or anonymous login.");
		}

		// Build args
		List<String> argList = new ArrayList<String>();
		if (activeMode) {
			argList.add("-a");
		}
		if (anonymous) {
			argList.add("-A");
		}
		if (ssl) {
			argList.add("-p");
			argList.add("SSL,true");
			System.out.println("FTP over SSL");
		} else {
			System.out.println("Plain FTP");
		}
		argList.add("-n");
		argList.add(ip + ":" + port);
		argList.add(username);
		argList.add(password);
		argList.add(remoteFile);
		String args[] = argList.toArray(new String[0]);
		// System.out.println(Arrays.toString(args));

		// Run Apache Common's FTP client
		try {
			FtpClientExample.main(args);
		} catch (IOException e) {
			throw new FtpException(e.getMessage(), e);
		}
	}

	private void uploadFile(String ip, int port, String username,
			String password, String remoteFile, String localFile,
			boolean activeMode, boolean anonymous, boolean ssl)
			throws FtpException {

		// Check params
		if (ip == null || username == null || password == null
				|| remoteFile == null || localFile == null) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n" + "Parameters cannot be null.");
		}
		if ((ssl && activeMode) || (ssl && anonymous)) {
			throw new FtpException(Thread.currentThread().getStackTrace()
					.toString()
					+ "\n"
					+ "FTPS does not allow active mode or anonymous login.");
		}

		// Build args
		List<String> argList = new ArrayList<String>();
		if (activeMode) {
			argList.add("-a");
		}
		if (anonymous) {
			argList.add("-A");
		}
		if (ssl) {
			argList.add("-p");
			argList.add("SSL,true");
			System.out.println("FTP over SSL");
		} else {
			System.out.println("Plain FTP");
		}
		argList.add("-b");
		argList.add("-s");
		argList.add(ip + ":" + port);
		argList.add(username);
		argList.add(password);
		argList.add(remoteFile);
		argList.add(localFile);
		String args[] = argList.toArray(new String[0]);
		// System.out.println(Arrays.toString(args));

		// Run Apache Common's FTP client
		try {
			FtpClientExample.main(args);
		} catch (IOException e) {
			throw new FtpException(e.getMessage(), e);
		}
	}
}

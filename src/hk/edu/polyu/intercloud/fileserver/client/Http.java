package hk.edu.polyu.intercloud.fileserver.client;

import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.fileserver.util.CmdLauncher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Http implements Callable<Void> {
	private String fullUrl, localFile;

	private Http(String fullUrl, String localFile) {
		this.fullUrl = fullUrl;
		this.localFile = localFile.replace("/", File.separator);
	}

	/**
	 * Receive a file from HTTP or HTTPS server
	 * 
	 * @param fullUrl
	 *            e.g. https://c1.e1.r1.iccp.us/something.zip
	 * @param localFile
	 *            e.g. C:\\Intercloud\\data1.zip
	 * @throws HttpException
	 */
	public static void download(String fullUrl, String localFile)
			throws HttpException {
		if (fullUrl.endsWith("/")) {
			return;
		}
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Http http = new Http(fullUrl, localFile);
		Future<Void> future = executor.submit(http);
		long start = System.currentTimeMillis();
		System.out.println(">>> HTTP downloading (" + fullUrl + ") starts. ["
				+ start + "]");
		try {
			future.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new HttpException(e.getMessage(), e);
		}
		long end = System.currentTimeMillis();
		long timeUsed = end - start;
		long fileSize = new File(localFile).length() / 1024;
		double speed = (double) fileSize / (timeUsed / 1000);
		System.out.println(">>> HTTP downloading (" + fullUrl + ") ends in "
				+ timeUsed + "ms, at "
				+ new DecimalFormat("#.##").format(speed) + "KB/s. [" + end
				+ "]");
		executor.shutdown();
	}

	@Override
	public Void call() throws Exception {
		startClient(fullUrl, localFile);
		return null;
	}

	private void startClient(String fullUrl, String localFile)
			throws HttpException {
		final int connections = 4;
		final int minSplitSizeMB = 5;
		Path p = Paths.get(localFile);
		String dir = p.getParent().toString();
		String lastChar = dir.substring(dir.length() - 1);
		if (lastChar.equals(File.separator)) {
			dir = dir.substring(0, dir.length() - 1);
		}
		String filename = p.getFileName().toString();
		String cmd = "aria2c -s" + connections + " -x" + connections + " -j"
				+ connections + " --min-split-size=" + minSplitSizeMB
				+ "M --file-allocation=none --dir=\"" + dir + "\" --out=\""
				+ filename + "\" \"" + fullUrl + "\"";
		System.out.println(cmd);
		try {
			File f = new File(localFile);
			if (f.isFile()) {
				// TODO Check digest
			} else {
				System.out.println("HTTP downloading " + fullUrl + " to "
						+ localFile);
				String result = CmdLauncher.runCommand(cmd);
				if (!result.contains("(OK):download completed.")) {
					throw new HttpException(result);
				}
			}
		} catch (IOException e) {
			throw new HttpException(e.getMessage(), e);
		}
	}
}

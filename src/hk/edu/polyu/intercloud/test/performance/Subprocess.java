package hk.edu.polyu.intercloud.test.performance;

import hk.edu.polyu.intercloud.aws.Download;
import hk.edu.polyu.intercloud.aws.Upload;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.security.Decryption;
import hk.edu.polyu.intercloud.security.Encryption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import javax.crypto.Cipher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Subprocess {

	static final String operation = "Download";
	static final String basefile = "1GB";
	static final int duplicates = 100;
	static final int maxWorkers = 1;
	static final String cloudType = "minio";
	static String ip = "";

	static final String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss")
			.format(new Date());
	static final String baseFilename = "log\\SubprocessTest\\" + operation
			+ "_" + maxWorkers + "_" + datetime;
	static final String logFile = baseFilename + ".csv";
	static final String picFile = baseFilename + ".jpg";

	static final String fileRoot = System.getProperty("user.dir")
			+ "/download/";
	static final String httpsDir = "https://iccp3.iccp.cf";

	public static void main(String[] args) throws IOException,
			InterruptedException {
		addHook();
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				whatismyip.openStream()));
		ip = in.readLine();
		System.out.println("IP: " + ip);
		new File("log/SubprocessTest/").mkdir();
		File outFile = new File(logFile);
		outFile.createNewFile();
		long heapSize = Runtime.getRuntime().totalMemory();
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		System.out.println("Current heap (MB): " + (heapSize / 1024 / 1024));
		System.out.println("Maximum heap (MB): " + (heapMaxSize / 1024 / 1024));
		System.out.println(operation + " " + basefile + "x" + duplicates
				+ " in " + cloudType + " // " + maxWorkers);
		Queue<Thread> workers = new LinkedList<>();
		for (int i = 0; i < duplicates; i++) {
			String filename = basefile + "." + i + ".zip";
			workers.offer(new Thread(getRunnable(filename), operation + " "
					+ filename));
		}
		System.out.println("Press ENTER to continue...");
		System.in.read();
		log("TEST START", basefile + "x" + duplicates + " // " + maxWorkers,
				"", 0L);
		for (Thread worker : workers) {
			while (currentWorkers() >= maxWorkers) {
				Thread.sleep(500);
			}
			worker.start();
			Thread.sleep(500);
		}
	}

	static void addHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					String screenshot = "nircmd savescreenshot \"" + picFile
							+ "\"";
					String message = operation + "(" + maxWorkers + ") on "
							+ ip + " FINISHED";
					String sendmail = "mailsend -smtp hv19.iccp.cf -port 22 -t ckplaw@polyu.edu.hk -f cscklaw@comp.polyu.edu.hk -sub \""
							+ message + "\" -M \"" + message + "\"";
					System.out.println(screenshot);
					System.out.println(sendmail);
					Runtime.getRuntime().exec(screenshot);
					Runtime.getRuntime().exec(sendmail);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	static Runnable getRunnable(String filename) {
		return new Runnable() {
			@Override
			public void run() {
				String filePath = fileRoot + filename;
				long start = System.currentTimeMillis();
				System.out.println(new Date() + ": " + operation + " "
						+ filename + " START");
				try {
					if (operation.equalsIgnoreCase("encrypt")) {
						encrypt(filePath);
					} else if (operation.equalsIgnoreCase("decrypt")) {
						decrypt(filePath);
					} else if (operation.equalsIgnoreCase("digest")) {
						digest(filePath);
					} else if (operation.equalsIgnoreCase("download")) {
						downloadFromStorage(filePath);
					} else if (operation.equalsIgnoreCase("upload")) {
						uploadToStorage(filePath);
					} else if (operation.equalsIgnoreCase("https")) {
						httpsDownload(httpsDir + "/" + filename, filePath);
					}
					long end = System.currentTimeMillis();
					System.out.println(new Date() + ": " + operation + " "
							+ filename + " END");
					log(operation, filename, "SUCCESS", end - start);
				} catch (Exception e) {
					long end = System.currentTimeMillis();
					System.out.println(new Date() + ": " + operation + " "
							+ filename + " EXCEPTION");
					try {
						log(operation, filename, e.getClass()
								.getCanonicalName(), end - start);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		};
	}

	static int currentWorkers() {
		int i = 0;
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getName().contains(operation)) {
				i++;
			}
		}
		return i;
	}

	static void encrypt(String filePath) throws Exception {
		String encryptPath = filePath + "_enc";
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		Encryption.copy(Cipher.ENCRYPT_MODE, filePath, encryptPath, keyPath);
		new File(filePath).delete();
		FileUtils.moveFile(new File(encryptPath), new File(filePath));
	}

	static void decrypt(String filePath) throws Exception {
		String keyPath = System.getProperty("user.dir") + "/Key/private.pem";
		String decPath = filePath + "_dec";
		Decryption.copy(Cipher.DECRYPT_MODE, filePath, decPath, keyPath);
		new File(filePath).delete();
		FileUtils.moveFile(new File(decPath), new File(filePath));
	}

	static void digest(String filePath) throws NoSuchAlgorithmException,
			IOException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		FileInputStream fis = new FileInputStream(filePath);
		byte[] dataBytes = new byte[1024];
		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		byte[] mdbytes = md.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		fis.close();
		System.out.println("Digest of " + filePath + ": " + sb.toString());
	}

	static void uploadToStorage(String filePath) throws Exception {
		if (cloudType.equalsIgnoreCase("amazon")) {
			Upload upload = new Upload();
			upload.amazondo(filePath);
		} else if (cloudType.equalsIgnoreCase("googlecloud")) {
			GoogleStorageIntercloud g = new GoogleStorageIntercloud();
			g.uploadFile("intercloud-perf-test01.appspot.com", filePath);
		} else if (cloudType.equalsIgnoreCase("minio")) {
			MinioForIntercloud minio = new MinioForIntercloud();
			minio.putObject(filePath, "intercloud",
					FilenameUtils.getName(filePath));
		}
	}

	static void downloadFromStorage(String filePath) throws Exception {
		if (cloudType.equalsIgnoreCase("amazon")) {
			Download download = new Download();
			download.amazondo(FilenameUtils.getName(filePath), fileRoot);
		} else if (cloudType.equalsIgnoreCase("googlecloud")) {
			GoogleStorageIntercloud g = new GoogleStorageIntercloud();
			g.downloadFile("intercloud-perf-test01.appspot.com",
					FilenameUtils.getName(filePath), fileRoot);
		} else if (cloudType.equalsIgnoreCase("minio")) {
			MinioForIntercloud minio = new MinioForIntercloud();
			minio.getObject("intercloud", FilenameUtils.getName(filePath),
					filePath);
		}
	}

	static void httpsDownload(String link, String filePath)
			throws HttpException {
		Http.download(link, filePath);
	}

	static void log(String operation, String object, String result,
			long duration) throws IOException {
		String text = operation + ", " + object + ", " + result + ", "
				+ System.currentTimeMillis() + ", " + (duration / 1000)
				+ System.lineSeparator();
		System.out.print(">>>>> " + text);
		Files.write(Paths.get(logFile), text.getBytes(),
				StandardOpenOption.APPEND);
	}
}

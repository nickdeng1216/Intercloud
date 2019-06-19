package hk.edu.polyu.intercloud.test.performance;

import hk.edu.polyu.intercloud.aws.Download;
import hk.edu.polyu.intercloud.aws.Upload;
import hk.edu.polyu.intercloud.fileserver.client.Http;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpException;
import hk.edu.polyu.intercloud.fileserver.server.Httpd;
import hk.edu.polyu.intercloud.gstorage.GoogleStorageIntercloud;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.util.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FilenameUtils;

public class CompressedTransfer {

	static final String operation = "Transfer";
	static final String testType = "os"; // VM or OS?
	static final String cloudType = "minio";
	static final String myDomain = "iccp3.iccp.cf";
	static final String baseUrl = "https://iccp1.iccp.cf/";

	static final int compression = ZipUtil.HIGH_COMPRESSION;
	static final String[] vmImages = { "TestWin8x64", "TestWin2012x64",
			"TestCentOS6x64" };
	static final String[] vmExts = { ".vhdx", "-flat.vmdk" };
	static final String[] osBigfiles = { "Test4GB_A.txt", "Test4GB_Random.txt",
			"TestWin2012.iso", "TestAlice.mkv" };

	static final String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss")
			.format(new Date());
	static final String baseFilename = "log\\CompressedTransfer\\" + operation
			+ "_" + testType + "_" + datetime;
	static final String logFile = baseFilename + ".csv";
	static final String picFile = baseFilename + ".jpg";

	public static void main(String args[]) {
		try {
			addHook();
			printHeap();
			System.out.println(operation + " on " + myDomain);
			Httpd.startHttpServer(myDomain, System.getProperty("user.dir"),
					"intercloud@comp.polyu.edu.hk", true);
			new File("log/CompressedTransfer/").mkdir();
			File outFile = new File(logFile);
			outFile.createNewFile();
			System.out.println("Press ENTER to continue...");
			System.in.read();
			if (operation.equalsIgnoreCase("Upload")) {
				upload();
			} else if (operation.equalsIgnoreCase("Download")) {
				download();
			} else if (operation.equalsIgnoreCase("Compress")) {
				compress();
			} else if (operation.equalsIgnoreCase("Extract")) {
				extract();
			} else if (operation.equalsIgnoreCase("Transfer")) {
				transfer();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void upload() throws Exception {
		if (testType.equals("os")) {
			for (String file : osBigfiles) {
				uploadToStorage(file);
				Thread.sleep(100);
				uploadToStorage(file + ".zip");
				Thread.sleep(100);
			}
		}
	}

	private static void uploadToStorage(String filePath) throws Exception {
		System.out.println("[" + new Date() + "] Uploading " + filePath);
		long start = System.currentTimeMillis();
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
		long end = System.currentTimeMillis();
		log("Upload", filePath, cloudType, end - start);
	}

	static void download() throws Exception {
		if (testType.equals("os")) {
			for (String file : osBigfiles) {
				downloadFromStorage(file);
				Thread.sleep(100);
				downloadFromStorage(file + ".zip");
				Thread.sleep(100);
			}
		}
	}

	private static void downloadFromStorage(String filePath) throws Exception {
		System.out.println("[" + new Date() + "] Downloading " + filePath);
		long start = System.currentTimeMillis();
		if (cloudType.equalsIgnoreCase("amazon")) {
			Download download = new Download();
			download.amazondo(FilenameUtils.getName(filePath),
					System.getProperty("user.dir"));
		} else if (cloudType.equalsIgnoreCase("googlecloud")) {
			GoogleStorageIntercloud g = new GoogleStorageIntercloud();
			g.downloadFile("intercloud-perf-test01.appspot.com",
					FilenameUtils.getName(filePath),
					System.getProperty("user.dir"));
		} else if (cloudType.equalsIgnoreCase("minio")) {
			MinioForIntercloud minio = new MinioForIntercloud();
			minio.getObject("intercloud", FilenameUtils.getName(filePath),
					filePath);
		}
		long end = System.currentTimeMillis();
		log("Download", filePath, cloudType, end - start);
	}

	static void compress() throws ZipException, InterruptedException,
			IOException {
		if (testType.equalsIgnoreCase("vm")) {
			for (String image : vmImages) {
				for (String ext : vmExts) {
					compressFile(image + ext);
					Thread.sleep(100);
				}
			}
		} else if (testType.equals("os")) {
			for (String file : osBigfiles) {
				compressFile(file);
				Thread.sleep(100);
			}
		}
	}

	private static void compressFile(String file) throws ZipException,
			IOException {
		System.out.println("[" + new Date() + "] Compressing " + file);
		long start = System.currentTimeMillis();
		ZipUtil.create(file + ".zip", file, compression, null);
		long end = System.currentTimeMillis();
		log("Compress", file, "OK", end - start);
	}

	static void extract() throws ZipException, InterruptedException,
			IOException {
		if (testType.equalsIgnoreCase("vm")) {
			for (String image : vmImages) {
				for (String ext : vmExts) {
					extractFile(image + ext);
					Thread.sleep(100);
				}
			}
		} else if (testType.equalsIgnoreCase("os")) {
			for (String file : osBigfiles) {
				extractFile(file);
			}
		}
	}

	private static void extractFile(String file) throws ZipException,
			InterruptedException, IOException {
		System.out.println("[" + new Date() + "] Extracting " + file + ".zip");
		long start = System.currentTimeMillis();
		ZipUtil.extractAll(file + ".zip", System.getProperty("user.dir"), null);
		long end = System.currentTimeMillis();
		log("Extract", file, "OK", end - start);
		Thread.sleep(100);
	}

	static void transfer() throws HttpException, InterruptedException,
			IOException {
		if (testType.equalsIgnoreCase("vm")) {
			for (String image : vmImages) {
				for (String ext : vmExts) {
					transferFile(image + ext);
					Thread.sleep(100);
					transferFile(image + ext + ".zip");
					Thread.sleep(100);
				}
			}
		} else if (testType.equalsIgnoreCase("os")) {
			for (String file : osBigfiles) {
				transferFile(file);
				Thread.sleep(100);
				transferFile(file + ".zip");
				Thread.sleep(100);
			}
		}
	}

	private static void transferFile(String file) throws HttpException,
			IOException {
		System.out.println("[" + new Date() + "] Downloading " + file);
		long start = System.currentTimeMillis();
		Http.download(baseUrl + file, System.getProperty("user.dir") + "/"
				+ file);
		long end = System.currentTimeMillis();
		log("Transfer", file, baseUrl, end - start);
	}

	static void addHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Httpd.killHttpServer();
					String screenshot = "nircmd savescreenshot \"" + picFile
							+ "\"";
					String message = operation + " on " + myDomain
							+ " FINISHED";
					String sendmail = "mailsend -smtp hv19.iccp.cf -port 22 -t ckplaw@polyu.edu.hk -f cscklaw@comp.polyu.edu.hk -sub \""
							+ message + "\" -M \"" + message + "\"";
					System.out.println(screenshot);
					System.out.println(sendmail);
					Runtime.getRuntime().exec(screenshot);
					Runtime.getRuntime().exec(sendmail);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	static void printHeap() {
		long heapSize = Runtime.getRuntime().totalMemory();
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		System.out.println("Current heap (MB): " + (heapSize / 1024 / 1024));
		System.out.println("Maximum heap (MB): " + (heapMaxSize / 1024 / 1024));
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

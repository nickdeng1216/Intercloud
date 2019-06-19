package hk.edu.polyu.intercloud.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class ZipUtil {

	public static final int HIGH_COMPRESSION = Zip4jConstants.DEFLATE_LEVEL_ULTRA;
	public static final int NORMAL_COMPRESSION = Zip4jConstants.DEFLATE_LEVEL_NORMAL;
	public static final int LOW_COMPRESSION = Zip4jConstants.DEFLATE_LEVEL_FASTEST;
	public static final int NO_COMPRESSION = Zip4jConstants.COMP_STORE;

	/**
	 * List all files in a ZIP file
	 * 
	 * @param zipFile
	 *            Path of the ZIP file
	 * @param password
	 *            Password
	 * @return A list of files
	 * @throws ZipException
	 */
	public static List<String> listFiles(String zipFile, String password)
			throws ZipException {
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		List<String> files = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<FileHeader> fileHeaderList = z.getFileHeaders();
		for (FileHeader h : fileHeaderList) {
			files.add(h.getFileName());
		}
		return files;
	}

	/**
	 * Create a new ZIP file with one file
	 * 
	 * @param zipFile
	 *            Path of the ZIP file
	 * @param fileName
	 *            The file to be compressed
	 * @param compression
	 *            Compression level, specified by a final int of this class
	 *            (e.g. ZipUtil.HIGH_COMPRESSION)
	 * @param password
	 *            Password
	 * @throws ZipException
	 */
	public static void create(String zipFile, String fileName, int compression,
			String password) throws ZipException {
		Path path = new File(zipFile).toPath();
		if (Files.exists(path)) {
			throw new ZipException(zipFile + " already exists.");
		}
		ZipFile z = new ZipFile(zipFile);
		pAddFile(z, fileName, null, compression, password);
	}

	public static void create(String zipFile, ArrayList<String> files,
			int compression, String password) throws ZipException {
		Path path = new File(zipFile).toPath();
		if (Files.exists(path)) {
			throw new ZipException(zipFile + " already exists.");
		}
		ZipFile z = new ZipFile(zipFile);
		pAddFiles(z, files, null, compression, password);
	}

	public static void createFromFolder(String zipFile, String folder,
			int compression, String password) throws ZipException {
		Path path = new File(zipFile).toPath();
		if (Files.exists(path)) {
			throw new ZipException(zipFile + " is invalid.");
		}
		ZipFile z = new ZipFile(zipFile);
		ZipParameters parameters = new ZipParameters();
		if (compression == NO_COMPRESSION) {
			parameters.setCompressionMethod(Zip4jConstants.COMP_STORE);
		} else {
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(compression);
		}
		if (password != null && !password.equals("")) {
			parameters.setEncryptFiles(true);
			parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
			parameters.setPassword(password);
		}
		z.addFolder(folder, parameters);
	}

	public static void extractFile(String zipFile, String fileName,
			String destPath, String password) throws ZipException {
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		z.extractFile(fileName, destPath);
	}

	public static void extractAll(String zipFile, String destPath,
			String password) throws ZipException {
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		z.extractAll(destPath);
	}

	public static void addFile(String zipFile, String fileName,
			String toFolder, int compression, String password)
			throws ZipException {
		Path path = new File(zipFile).toPath();
		if (!Files.exists(path) || !Files.isRegularFile(path)) {
			throw new ZipException(zipFile + " does not exist or is invalid.");
		}
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		pAddFile(z, fileName, toFolder, compression, password);
	}

	public static void addFiles(String zipFile, ArrayList<String> files,
			String toFolder, int compression, String password)
			throws ZipException {
		Path path = new File(zipFile).toPath();
		if (!Files.exists(path) || !Files.isRegularFile(path)) {
			throw new ZipException(zipFile + " does not exist or is invalid.");
		}
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		pAddFiles(z, files, toFolder, compression, password);
	}

	public static void removeFile(String zipFile, String fileName,
			String password) throws ZipException {
		ZipFile z = new ZipFile(zipFile);
		if (z.isEncrypted()) {
			z.setPassword(password);
		}
		z.removeFile(fileName);
	}

	static void pAddFile(ZipFile z, String fileName, String toFolder,
			int compression, String password) throws ZipException {
		ZipParameters parameters = new ZipParameters();
		if (toFolder != null && !toFolder.equals("")) {
			parameters.setRootFolderInZip(toFolder);
		}
		if (compression == NO_COMPRESSION) {
			parameters.setCompressionMethod(Zip4jConstants.COMP_STORE);
		} else {
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(compression);
		}
		if (password != null && !password.equals("")) {
			parameters.setEncryptFiles(true);
			parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
			parameters.setPassword(password);
		}
		z.addFile(new File(fileName), parameters);
	}

	static void pAddFiles(ZipFile z, ArrayList<String> files, String toFolder,
			int compression, String password) throws ZipException {
		ZipParameters parameters = new ZipParameters();
		if (toFolder != null && !toFolder.equals("")) {
			parameters.setRootFolderInZip(toFolder);
		}
		if (compression == NO_COMPRESSION) {
			parameters.setCompressionMethod(Zip4jConstants.COMP_STORE);
		} else {
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(compression);
		}
		if (password != null && !password.equals("")) {
			parameters.setEncryptFiles(true);
			parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
			parameters.setPassword(password);
		}
		ArrayList<File> fileList = new ArrayList<>();
		for (String file : files) {
			fileList.add(new File(file));
		}
		z.addFiles(fileList, parameters);
	}
}

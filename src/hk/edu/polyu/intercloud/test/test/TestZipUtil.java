package hk.edu.polyu.intercloud.test.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import hk.edu.polyu.intercloud.util.ZipUtil;
import net.lingala.zip4j.exception.ZipException;

public class TestZipUtil {

	public static void main(String args[]) throws ZipException {
		listFiles("test.zip");
		// create("created.zip", "Sunset.jpg");
		ArrayList<String> files = new ArrayList<>();
		files.add("Sunset.jpg");
		files.add("Winter.jpg");
		create("created.zip", files);
		// createFromFolder("created.zip", "pictures/");
		JOptionPane.showMessageDialog(null, "Continue");
		extractFile("created.zip", "Winter.jpg", "extracted/");
		extractAll("created.zip", "extracted/");
	}

	static void listFiles(String zipFile) throws ZipException {
		List<String> list = ZipUtil.listFiles(zipFile, null);
		System.out.println(Arrays.toString(list.toArray()));
	}

	static void create(String zipFile, String fileName) throws ZipException {
		ZipUtil.create(zipFile, fileName, ZipUtil.HIGH_COMPRESSION, null);
	}

	static void create(String zipFile, ArrayList<String> files) throws ZipException {
		ZipUtil.create(zipFile, files, ZipUtil.HIGH_COMPRESSION, null);
	}

	static void createFromFolder(String zipFile, String folder) throws ZipException {
		ZipUtil.createFromFolder(zipFile, folder, ZipUtil.HIGH_COMPRESSION, null);
	}

	static void extractFile(String zipFile, String fileName, String destPath) throws ZipException {
		ZipUtil.extractFile(zipFile, fileName, destPath, null);
	}

	static void extractAll(String zipFile, String destPath) throws ZipException {
		ZipUtil.extractAll(zipFile, destPath, null);
	}

}

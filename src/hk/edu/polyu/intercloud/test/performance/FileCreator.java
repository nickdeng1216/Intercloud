package hk.edu.polyu.intercloud.test.performance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class FileCreator {

	public static void main(String[] args) throws IOException,
			InterruptedException {
		makeRandom(null);
	}

	static void makeRandom(String str) throws IOException, InterruptedException {
		String filename = "Test.txt";
		double filesize = 2.0 * 1024 * 1024 * 1024;
		long start = System.currentTimeMillis();
		System.out.println("Generating " + filename + " (" + filesize + ")");
		File file = new File(filename);
		file.createNewFile();
		double currentfilesize = Long.valueOf(file.length()).doubleValue();

		// TODO
		if (str == null || str.equals("")) {
			while (currentfilesize < filesize) {
				str = RandomStringUtils.randomAscii(10247680);
				Files.write(Paths.get(filename), str.getBytes(),
						StandardOpenOption.APPEND);
				currentfilesize = Long.valueOf(file.length()).doubleValue();
				System.out.printf(
						"Current size: %.0f" + System.lineSeparator(),
						currentfilesize);
				Thread.sleep(5);
			}
		} else {
			System.out.println("Writing " + str);
			while (currentfilesize < filesize) {
				Files.write(Paths.get(filename), str.getBytes(),
						StandardOpenOption.APPEND);
				currentfilesize = Long.valueOf(file.length()).doubleValue();
				System.out.printf(
						"Current size: %.0f" + System.lineSeparator(),
						currentfilesize);
				Thread.sleep(5);
			}
		}

		long end = System.currentTimeMillis();
		System.out.println("\nTime used (ms): " + (end - start));
	}

	static void makeCopy(String name) throws IOException {
		long start = System.currentTimeMillis();
		System.out.print("Copying ");
		for (int i = 0; i < 100; i++) {
			System.out.print(i + ", ");
			File f = new File("download/1GB.zip");
			FileUtils.copyFile(f, new File("download/1GB." + i + ".zip"));
		}
		long end = System.currentTimeMillis();
		System.out.println("\nTime used (ms): " + (end - start));
	}
}

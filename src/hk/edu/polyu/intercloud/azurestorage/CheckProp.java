package hk.edu.polyu.intercloud.azurestorage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CheckProp {

	private static final String AZURE_STORAGE_PROP_FILE = System
			.getProperty("user.dir") + "/" + "azurestorage.properties";

	public static void main(String args[]) throws Exception {
		System.out.println("This program will check the properties file.");
		Properties properties = new Properties();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(AZURE_STORAGE_PROP_FILE);
			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Sorry, the file " + AZURE_STORAGE_PROP_FILE
					+ " is not found or unable to read.");
		}

		try {
			String connectionstring = properties
					.getProperty("connectionstring");
			if (connectionstring.equals("") || connectionstring == null) {
				System.err
						.println("Please check if *connectionstring* is properly set.");
				System.exit(1);
			}
			System.out.println("connectionstring = " + connectionstring);
		} catch (Exception e) {
			System.err
					.println("Please check if *connectionstring* are properly set.");
			System.exit(1);
		}
		System.exit(0);
	}
}

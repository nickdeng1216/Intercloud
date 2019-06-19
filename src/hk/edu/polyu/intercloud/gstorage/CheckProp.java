package hk.edu.polyu.intercloud.gstorage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CheckProp {

	private static final String GSTORAGE_PROP_FILE = System
			.getProperty("user.dir") + "/" + "googlestorage.properties";

	public static void main(String[] args) {
		System.out.println("This program will check the properties file.");
		Properties properties = new Properties();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(GSTORAGE_PROP_FILE);
			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Sorry, the file " + GSTORAGE_PROP_FILE
					+ " is not found or unable to read.");
		}

		try {
			long projectid = Long.parseLong(properties
					.getProperty("project.id"));
			System.out.println("project.id = " + projectid);
		} catch (Exception e) {
			System.err
					.println("Please check if *project.id* is properly set as a numeric.");
			System.exit(1);
		}

		try {
			String applicationname = properties.getProperty("application.name");
			String accountid = properties.getProperty("account.id");
			if (applicationname.equals("") || applicationname == null
					|| accountid.equals("") || accountid == null) {
				System.err
						.println("Please check if *application.name* and *account.id* (service account id, e.g. service-account@intercloudpolyu.iam.gserviceaccount.com) are properly set.");
				System.exit(1);
			}
			System.out.println("application.name = " + applicationname);
			System.out.println("account.id = " + accountid);
		} catch (Exception e) {
			System.err
					.println("Please check if *application.name* and *account.id* (service account id, e.g. service-account@intercloudpolyu.iam.gserviceaccount.com) are properly set.");
			System.exit(1);
		}

		try {
			String privatekeypath = properties.getProperty("private.key.path");
			if (privatekeypath.equals("") || privatekeypath == null) {
				System.err
						.println("Please check if *private.key.path* (e.g. /IntercloudPolyU-225dbcd9b3da.p12) is properly set.");
				System.exit(1);
			}
			System.out.println("private.key.path = " + privatekeypath);
		} catch (Exception e) {
			System.err
					.println("Please check if *private.key.path* (e.g. /IntercloudPolyU-225dbcd9b3da.p12) is properly set.");
			System.exit(1);
		}
	}
}

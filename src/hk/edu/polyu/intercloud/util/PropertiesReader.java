package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.exceptions.StorageException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This program is used for read properties from gateway.properties
 * 
 * @author harry
 *
 */
public class PropertiesReader {

	private String propFile;

	public PropertiesReader(String vendor) throws StorageException {
		vendor = vendor.toLowerCase();
		switch (vendor) {

		case "amazon": {
			propFile = System.getProperty("user.dir") + "/aws.properties";
			break;
		}

		case "minio": {
			propFile = System.getProperty("user.dir") + "/" + vendor
					+ ".properties";
			break;
		}

		case "googlecloud": {
			propFile = System.getProperty("user.dir")
					+ "/googlestorage.properties";
			break;
		}

		case "azure": {
			propFile = System.getProperty("user.dir")
					+ "/azurestorage.properties";
			break;
		}
		case "openstack": {
			propFile = System.getProperty("user.dir") + "/ops.properties";
			break;
		}

		case "centurylink": {
			propFile = System.getProperty("user.dir") + "/ctl.properties";
			break;
		}

		default: {
			throw new StorageException("Gateway does not support this vendor.");
		}

		}
	}

	public String getProp() {
		return propFile;
	}

	public String getBucket() throws IOException {
		Properties properties = new Properties();
		InputStream ins = new FileInputStream(propFile);
		properties.load(ins);
		return properties.getProperty("Bucketname");
	}
}

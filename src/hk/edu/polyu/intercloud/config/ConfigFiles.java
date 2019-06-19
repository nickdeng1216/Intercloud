package hk.edu.polyu.intercloud.config;

import hk.edu.polyu.intercloud.util.CmdExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

public class ConfigFiles {

	public static final String LINE = System.lineSeparator();
	public static final String GW_PROP_FILE = "gateway.properties",
			AMAZON_PROP_FILE = "aws.properties",
			CTL_PROP_FILE = "ctl.properties",
			GCS_PROP_FILE = "googlestorage.properties",
			AZURE_PROP_FILE = "azurestorage.properties",
			MINIO_PROP_FILE = "minio.properties",
			SWIFT_PROP_FILE = "ops.properties",
			VMWARE_PROP_FILE = "vsphere.properties",
			HYPERV_PROP_FILE = "hyperv.properties";
	private static String[] httpAndHttps = { "http", "https" };
	private static UrlValidator httpAndHttpsValidator = new UrlValidator(
			httpAndHttps, UrlValidator.ALLOW_LOCAL_URLS);
	private static String[] https = { "https" };
	private static UrlValidator httpsValidator = new UrlValidator(https);
	private static EmailValidator emailValidator = EmailValidator.getInstance();
	private static String title = "Properties";

	public static void main(String[] args) throws IOException {
		propForGW();
	}

	private static void propForGW() throws IOException {
		JOptionPane
				.showMessageDialog(
						null,
						"This program only generates a basic properties file, capable for general use."
								+ LINE
								+ "For advanced options, please refer to the related documentations available online.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String name = getInput("Domain name of this Gateway (e.g. iccp3.iccp.cf):");
		String[] roles = new String[] { "Root", "Exchange", "Cloud" };
		int r = JOptionPane.showOptionDialog(null,
				"Select the role of this Gateway.", title,
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				roles, roles[2]);
		String role = roles[r].toUpperCase();
		String transfermethods = "HTTP,HTTPS"; // Basic
		String vendor = "private"; // Basic
		int workload = 1; // Basic
		int clientworkload = 1; // Basic
		String perflog = ""; // Basic
		String errlog = getInput("Error log file (e.g. log/error.log):");
		sb.append("name=").append(name).append(LINE).append("ip=").append(name)
				.append(LINE).append("role=").append(role).append(LINE)
				.append("transfermethods=").append(transfermethods)
				.append(LINE).append("vendor=").append(vendor).append(LINE)
				.append("workload=").append(workload).append(LINE)
				.append("clientworkload=").append(clientworkload).append(LINE)
				.append("perflog=").append(perflog).append(LINE)
				.append("errlog=").append(errlog).append(LINE);
		String services = "";
		String objectstorage = propForOS();
		if (!objectstorage.equals("")) {
			services += "ObjectStorage:" + objectstorage + ";";
		}
		String vm = propForVM();
		if (!vm.equals("")) {
			services += "VM:" + vm;
		}
		sb.append("services=").append(services);
		title = "Properties";
		if (!writeFile(GW_PROP_FILE, sb.toString())) {
			propForGW();
		}
	}

	private static String propForOS() throws IOException {
		String[] vendors = new String[] { "Amazon S3", "CenturyLink",
				"Google Cloud", "Microsoft Azure", "Minio", "Openstack Swift",
				"Not provided" };
		String v = (String) JOptionPane
				.showInputDialog(
						null,
						"Select your object storage provider. If object storage is not provided, select [Not Provided]",
						"Options", JOptionPane.QUESTION_MESSAGE, null, vendors,
						vendors[0]);
		if (v.equals(vendors[0])) {
			propForAmazonS3();
			return "amazon";
		} else if (v.equals(vendors[1])) {
			propForCenturyLink();
			return "centurylink";
		} else if (v.equals(vendors[2])) {
			propForGCS();
			return "googlecloud";
		} else if (v.equals(vendors[3])) {
			propForAzure();
			return "azure";
		} else if (v.equals(vendors[4])) {
			propForMinio();
			return "minio";
		} else if (v.equals(vendors[5])) {
			propForSwift();
			return "openstack";
		}
		return "";
	}

	private static String propForVM() throws IOException {
		String[] vendors = new String[] { "VMware", "Hyper-V", "Not Provided" };
		int v = JOptionPane
				.showOptionDialog(
						null,
						"Select your Virtual Machine hypervisor. If VM support is not provided, select [Not Provided]",
						"Options", JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, vendors, vendors[0]);
		if (v == 0) {
			propForVMware();
			return "vmware";
		} else if (v == 1) {
			propForHyperV();
			return "hyper-v";
		}
		return "";
	}

	private static void propForVMware() throws IOException {
		if (checkFile(VMWARE_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of VMware";
		StringBuilder sb = new StringBuilder();
		String ip = getInput("vSphere host name or IP address (e.g. 192.168.11.101):");
		String username = getInput("vSphere username (e.g. root, administrator, etc.):");
		String password = getInput("vSphere password:");
		String datastore = getInput("The name of the storage used to host VMs (e.g. ESXi02-DS01):");
		String diskmode = "persistent";
		sb.append("Url=https://").append(ip).append("/sdk").append(LINE)
				.append("Hostip=").append(ip).append(LINE).append("Username=")
				.append(username).append(LINE).append("Password=")
				.append(password).append(LINE).append("Datastore=")
				.append(datastore).append(LINE).append("Diskmode=")
				.append(diskmode);
		if (!writeFile(VMWARE_PROP_FILE, sb.toString())) {
			propForVMware();
		}
	}

	private static void propForHyperV() throws IOException {
		if (checkFile(HYPERV_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of Hyper-V";
		StringBuilder sb = new StringBuilder();
		String[] nics = CmdExecutor.runSinglePsCmd("(Get-VMSwitch).Name")
				.split(LINE);
		if (nics.length < 1) {
			JOptionPane
					.showMessageDialog(
							null,
							"You don't have a network card (i.e. VM switch) for Hyper-V.",
							"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (String nic : nics) {
			if (nic.contains("CommandNotFoundException")) {
				JOptionPane.showMessageDialog(null,
						"You are not running Hyper-V.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		if (nics.length == 1) {
			sb.append("default_network_card=").append(nics[0]);
		} else {
			sb.append("default_network_card=")
					.append((String) JOptionPane
							.showInputDialog(
									null,
									"Select your object storage provider. If object storage is not provided, select [Not Provided]",
									"Options", JOptionPane.QUESTION_MESSAGE,
									null, nics, nics[0]));
		}
		if (!writeFile(HYPERV_PROP_FILE, sb.toString())) {
			propForHyperV();
		}
	}

	private static void propForAmazonS3() throws IOException {
		if (checkFile(AMAZON_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of Amazon S3";
		JOptionPane
				.showMessageDialog(
						null,
						"Please visit the AWS Web Console at https://console.aws.amazon.com."
								+ LINE
								+ "We now edit the properties file with the data you get from the Console.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String username = getInput("AWS S3 access key:");
		String password = getInput("AWS S3 secret key:");
		String bucket = getInput("The name of the bucket to be used:");
		String url = "";
		do {
			url = getInput("The URL for accessing objects (e.g. https://s3-us-west-2.amazonaws.com):");
			if (!httpsValidator.isValid(url)) {
				popupIncorrectFormat();
			}
		} while (!httpsValidator.isValid(url));
		sb.append("Username=").append(username).append(LINE)
				.append("Password=").append(password).append(LINE)
				.append("Bucketname=").append(bucket).append(LINE)
				.append("Url=").append(url);
		if (!writeFile(AMAZON_PROP_FILE, sb.toString())) {
			propForAmazonS3();
		}
	}

	private static void propForCenturyLink() throws IOException {
		if (checkFile(CTL_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of CenturyLink Object Storage";
		JOptionPane
				.showMessageDialog(
						null,
						"Please visit the Control Portal of CenturyLink at https://control.ctl.io."
								+ LINE
								+ "We now edit the properties file with the data you get from the Portal.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String endpoint = "";
		do {
			endpoint = getInput("Endpoint URL (e.g. https://useast.os.ctl.io):");
			if (!httpsValidator.isValid(endpoint)) {
				popupIncorrectFormat();
			}
		} while (!httpsValidator.isValid(endpoint));
		String accesskey = getInput("Access key:");
		String secretkey = getInput("Secret key:");
		String bucket = getInput("The name of the bucket to be used:");
		sb.append("EndPoint=").append(endpoint).append(LINE)
				.append("AccessKey=").append(accesskey).append(LINE)
				.append("SecretKey=").append(secretkey).append(LINE)
				.append("Bucketname=").append(bucket);
		if (!writeFile(CTL_PROP_FILE, sb.toString())) {
			propForCenturyLink();
		}
	}

	private static void propForGCS() throws IOException {
		if (checkFile(GCS_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of Google Cloud Storage";
		JOptionPane
				.showMessageDialog(
						null,
						"Please visit visit the Google Cloud Services Console at https://console.cloud.google.com."
								+ LINE
								+ "Use the console to generate and download a P12 private key (*.p12) which links to the bucket used by the Gateway. (IMPORTANT!)"
								+ LINE
								+ "Click [OK] to continue editing the properties file afterwards.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String projectid = "";
		do {
			projectid = getInput("Google project ID (numeric):");
			if (!projectid.matches("\\d+")) {
				popupIncorrectFormat();
			}
		} while (!projectid.matches("\\d+"));
		String applicationname = getInput("Application name:");
		String accountid = "";
		do {
			accountid = getInput("Account ID (e.g. cellular-effect-134823@appspot.gserviceaccount.com):");
			if (!emailValidator.isValid(accountid)) {
				popupIncorrectFormat();
			}
		} while (!emailValidator.isValid(accountid));
		String key = "/"
				+ getInput("The relative path to the private key file (e.g. Intercloud-53e2e5ac5b18.p12):");
		String email = "";
		do {
			email = getInput("Google account username/e-mail (e.g. intercloud@gmail.com):");
			if (!emailValidator.isValid(email)) {
				popupIncorrectFormat();
			}
		} while (!emailValidator.isValid(email));
		String password = getInput("Google account password:");
		sb.append("project.id=").append(projectid).append(LINE)
				.append("application.name=").append(applicationname)
				.append(LINE).append("account.id=").append(accountid)
				.append(LINE).append("private.key.path=").append(key)
				.append(LINE).append("email=").append(email).append(LINE)
				.append("password=").append(password);
		if (!writeFile(GCS_PROP_FILE, sb.toString())) {
			propForGCS();
		}
	}

	private static void propForAzure() throws IOException {
		if (checkFile(AZURE_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of Azure Blob Storage";
		StringBuilder sb = new StringBuilder();
		String connstring = getInput("Please visit the Azure Portal at https://portal.azure.com."
				+ LINE
				+ "Copy and paste the \"connection string\" of the container to be used here:");
		sb.append("connectionstring=").append(connstring);
		if (!writeFile(AZURE_PROP_FILE, sb.toString())) {
			propForAzure();
		}
	}

	private static void propForMinio() throws IOException {
		if (checkFile(MINIO_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of Minio";
		JOptionPane
				.showMessageDialog(
						null,
						"If Minio is not running, run it now."
								+ LINE
								+ "We now edit the properties file with the data shown in the console.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String endpoint = "";
		do {
			endpoint = getInput("Endpoint URL (e.g. http://127.0.0.1:9000):");
			if (!httpAndHttpsValidator.isValid(endpoint)) {
				popupIncorrectFormat();
			}
		} while (!httpAndHttpsValidator.isValid(endpoint));
		String accesskey = getInput("Access key:");
		String secretkey = getInput("Secret key:");
		String bucket = getInput("The name of the bucket to be used:");
		sb.append("EndPoint=").append(endpoint).append(LINE)
				.append("AccessKey=").append(accesskey).append(LINE)
				.append("SecretKey=").append(secretkey).append(LINE)
				.append("Bucketname=").append(bucket);
		if (!writeFile(MINIO_PROP_FILE, sb.toString())) {
			propForMinio();
		}
	}

	private static void propForSwift() throws IOException {
		if (checkFile(SWIFT_PROP_FILE)) {
			return; // Already exist and don't want to overwrite.
		}
		title = "Properties of OpenStack Swift";
		JOptionPane
				.showMessageDialog(
						null,
						"We now edit the properties file with the data shown in the OpenStack console.",
						title, JOptionPane.INFORMATION_MESSAGE);
		StringBuilder sb = new StringBuilder();
		String username = getInput("Username (e.g. admin):");
		String password = getInput("Password:");
		String container = getInput("Container to be used:");
		String bucket = getInput("The name of the bucket to be used:");
		String url = "";
		do {
			url = getInput("The URL for access (e.g. http://192.168.11.76:5000/v3):");
			if (!httpAndHttpsValidator.isValid(url)) {
				popupIncorrectFormat();
			}
		} while (!httpAndHttpsValidator.isValid(url));
		sb.append("Username=").append(username).append(LINE)
				.append("Password=").append(password).append(LINE)
				.append("Container=").append(container).append(LINE)
				.append("Bucketname=").append(bucket).append(LINE)
				.append("Url=").append(url);
		if (!writeFile(SWIFT_PROP_FILE, sb.toString())) {
			propForSwift();
		}
	}

	private static String getInput(String message) {
		return JOptionPane.showInputDialog(null, message, title,
				JOptionPane.QUESTION_MESSAGE);
	}

	private static void popupIncorrectFormat() {
		JOptionPane.showMessageDialog(null,
				"Invalid format, please input again.", "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	private static boolean checkFile(String file) {
		Path path = Paths.get(file);
		if (Files.exists(path) && Files.isRegularFile(path)) {
			int i = JOptionPane.showConfirmDialog(null, "The properties file '"
					+ file + "' already exist. Overwrite?");
			if (i != JOptionPane.YES_OPTION) {
				return true; // Exist, don't overwrite!
			} else {
				return false; // Ignore, regard as not exist to continue.
			}
		}
		return false; // Not exist.
	}

	private static boolean writeFile(String file, String text)
			throws IOException {
		if (JOptionPane.showConfirmDialog(null,
				"The following will be written to the properties file." + LINE
						+ LINE + text + LINE + LINE + "Confirm?", title,
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return false;
		}
		Files.write(Paths.get(file), text.getBytes());
		return true;
	}
}

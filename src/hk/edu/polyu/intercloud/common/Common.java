package hk.edu.polyu.intercloud.common;

import hk.edu.polyu.intercloud.model.cloud.Cloud;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONObject;

public class Common {

	// Versions
	public static final String ICCP_VER = "1.1";
	public static final String GW_VER = "M201703";
	// Gateway prop. file path
	public static final String GW_PROP_FILE = System.getProperty("user.dir")
			+ "/" + "gateway.properties";
	public static final String DB_USER = "INTERCLOUD";
	public static final String DB_PASS = "p@ssw0rd";
	// Standard port numbers
	public static final int CLIENT_SOCKET_PORT = 2001;
	public static final int GW_PORT = 2002;
	public static final int UDT_PORT = 9000;
	public static final int FTP_PORT = 21;
	public static final int FTPS_PORT = 990;
	public static final int HTTP_PORT = 80;
	public static final int HTTPS_PORT = 443;
	public static final String JKS_PW = "password";

	// Path storing the downloaded files from own cloud
	public static final String DOWNLOAD_PATH = System.getProperty("user.dir")
			+ File.separator + "download" + File.separator;
	// Path storing the retrieved files from another gateway
	public static final String RETRIEVE_PATH = System.getProperty("user.dir")
			+ File.separator + "retrieve" + File.separator;
	// Path storing the retrieved keys
	public static final String KEY_PATH = System.getProperty("user.dir")
			+ File.separator + "key" + File.separator;
	// For HTTPS
	public static final String CADDY_PATH = System.getProperty("user.dir")
			+ File.separator + "caddy" + File.separator;
	public static final String SSL_EMAIL = "intercloud@comp.polyu.edu.hk";
	// Cloud specs.
	public static String my_name, my_ip, my_public_ip, my_role, my_exchange,
			my_root;
	public static Cloud my_cloud;
	public static Map<String, String> my_service_providers = new HashMap<>();
	public static Map<String, Cloud> my_friends = new HashMap<>();
	// A set of *SUPPORTED* transfer methods
	public static Set<String> file_transfer_methods = new HashSet<>();
	// A map of API class names and service names
	public static Map<String, String> api_services = new HashMap<>();
	// Location
	public static String my_country_code, my_country, my_region, my_city;
	// Protocol which is waiting for the response
	// <ProtocolId, String[0]:Status, String[1]:ReferencedId>
	public static HashMap<String, String[]> flag = new HashMap<>();
	// Client requests
	public static int clientWorkload = 1;
	public static Queue<JSONObject> clientQ = new ConcurrentLinkedQueue<>();
	public static ThreadPoolExecutor clientExecutor;
	// Intercloud requests
	public static int gatewayWorkload = 1;
	public static Queue<String> gatewayQ = new ConcurrentLinkedQueue<>();
	public static ThreadPoolExecutor executor;
	// TODO Add one more queue
	public static Set<String> msgSet = new HashSet<>();
	public static int gatewayWorkload_light = 1;
	public static Queue<String> gatewayQ_light = new ConcurrentLinkedQueue<>();
	public static ThreadPoolExecutor executor_light;
	// Performance log
	public static boolean perfLog = false;
	public static String perfLogFile = "";
	// Exception log
	public static String errLogFile = "";
	// Watchdog log
	public static String watchdogLogFile = "";
	// CA info
	public static final String ca_ip = "hv2.iccp.cf";
	public static final String ca_name = "iccp.us";
	public static Connection con = null;
	// Housekeeper
	public static int housekeeperInterval = 3600;
	// Watchdog
	public static int watchdogInterval = 30;

}

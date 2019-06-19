package hk.edu.polyu.intercloud.main;

import hk.edu.polyu.intercloud.api.DNSServiceAPI;
import hk.edu.polyu.intercloud.client.ClientProcessor;
import hk.edu.polyu.intercloud.client.ClientSocket;
import hk.edu.polyu.intercloud.client.ClientTesterLite;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.communication.Sockets;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.exceptions.ClientSocketException;
import hk.edu.polyu.intercloud.exceptions.DNSException;
import hk.edu.polyu.intercloud.exceptions.DNSServiceAPIException;
import hk.edu.polyu.intercloud.fileserver.exceptions.FtpdException;
import hk.edu.polyu.intercloud.fileserver.exceptions.HttpdException;
import hk.edu.polyu.intercloud.fileserver.exceptions.UdtdException;
import hk.edu.polyu.intercloud.fileserver.server.Ftpd;
import hk.edu.polyu.intercloud.fileserver.server.Httpd;
import hk.edu.polyu.intercloud.fileserver.server.Udtd;
import hk.edu.polyu.intercloud.health.Housekeeper;
import hk.edu.polyu.intercloud.health.Watchdog;
import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.util.DNSUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.apache.commons.io.output.TeeOutputStream;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

/**
 * Main class
 * 
 * @author Priere
 *
 */
public class Main {

	static long start = 0L;
	static long end = 0L;

	public static void main(String[] args) throws InterruptedException {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					main();
				} catch (IOException | SQLException | DNSException
						| DNSServiceAPIException | AuthenticationAPIException
						| UdtdException | FtpdException | HttpdException e) {
					LogUtil.logException(e);
				} catch (Exception e) {
					LogUtil.logException(e);
				}
			}
		};
		new Thread(r, "Gateway").start();
		TimeUnit.SECONDS.sleep(10);
		start = System.currentTimeMillis();
		LogUtil.logPerformance("Gateway START", "", start, 0);
	}

	static void main() throws IOException, SQLException, DNSException,
			DNSServiceAPIException, AuthenticationAPIException, UdtdException,
			FtpdException, HttpdException, ClassNotFoundException {
		// Set output
		setOutput();
		// Check ports
		checkPorts();
		// Initialize
		init();
		// Add a shutdown hook.
		addHook();
		// Get the friends.
		getFriends();
		// Get services.
		getServices();
		// Gateway Socket.
		runGatewaySocket();
		// Update DNS records.
		// updateDnsRecords(); // XXX
		// Initiate file servers.
		initFileServers();
		// Client Socket Receiver.
		runClientSocket();
		// Housekeeper and Watchdog.
		housekeeperWatchdog();
		// Client Tester Lite.
		runClientTesterLite();
		// At last, click when you need to kill me.
		String[] options = new String[] { "Stop" };
		int o = JOptionPane.showOptionDialog(null, "Gateway is running. Stop?",
				"Options", JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		System.exit(0);
	}

	static void setOutput() throws IOException {
		// Output and Error log
		new File("log/").mkdirs();
		String output = "log/"
				+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
				+ ".log";
		File outFile = new File(output);
		outFile.createNewFile();
		TeeOutputStream teeOut = new TeeOutputStream(System.out,
				new FileOutputStream(output));
		TeeOutputStream teeErr = new TeeOutputStream(System.err,
				new FileOutputStream(output));
		System.setOut(new PrintStream(teeOut));
		System.setErr(new PrintStream(teeErr));
		// Perf log
		Common.perfLogFile = getProp("perflog");
		if (Common.perfLogFile != null && !Common.perfLogFile.equals("")) {
			Common.perfLog = true;
			File perfFile = new File(Common.perfLogFile);
			perfFile.createNewFile();
		}
		// Exception log
		Common.errLogFile = getProp("errlog");
		if (Common.errLogFile == null || Common.errLogFile.equals("")) {
			Common.errLogFile = "log/error.log";
		}
		File errFile = new File(Common.errLogFile);
		errFile.createNewFile();
		// Watchdog log
		Common.watchdogLogFile = getProp("watchdoglog");
		if (Common.watchdogLogFile == null || Common.watchdogLogFile.equals("")) {
			Common.watchdogLogFile = "log/watchdog.log";
		}
		File watchdogFile = new File(Common.watchdogLogFile);
		watchdogFile.createNewFile();
	}

	static void checkPorts() {
		if (!portAvailable(Common.CLIENT_SOCKET_PORT)
				|| !portAvailable(Common.GW_PORT)) {
			int confirm = JOptionPane
					.showConfirmDialog(
							null,
							"Either the Gateway or the Client port cannot be opened. \nCheck if another Gateway instance is running. \nClick [Yes] to retry or [No] to exit.");
			if (confirm != JOptionPane.YES_OPTION) {
				System.exit(0);
			}
			checkPorts();
		}
	}

	static void init() throws IOException, SQLException {
		try {
			Common.clientWorkload = Integer.valueOf(getProp("clientworkload"));
		} catch (Exception e) {
		}
		try {
			Common.gatewayWorkload = Integer.valueOf(getProp("workload"));
		} catch (Exception e) {
		}
		System.out.println("Client workload set to " + Common.clientWorkload);
		System.out.println("Gateway workload set to " + Common.gatewayWorkload);
		Common.clientExecutor = new ThreadPoolExecutor(Common.clientWorkload,
				Common.clientWorkload, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		Common.executor = new ThreadPoolExecutor(Common.gatewayWorkload,
				Common.gatewayWorkload, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		Common.executor_light = new ThreadPoolExecutor(Common.gatewayWorkload,
				Common.gatewayWorkload, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		Common.api_services.put("hk.edu.polyu.intercloud.api.ObjectStorageAPI",
				"ObjectStorage");
		Common.api_services.put("hk.edu.polyu.intercloud.api.DNSServiceAPI",
				"dns"); // TODO need?
		Common.api_services.put(
				"hk.edu.polyu.intercloud.api.InquireResourceAPI",
				"InquireResource"); // TODO need?
		Common.my_name = getProp("name");
		Common.my_ip = getProp("ip");
		Common.my_role = getProp("role");
		Common.my_cloud = new Cloud(Common.my_ip, Common.my_name,
				Common.my_role, true);

		System.out.println("You are " + Common.my_name + ", a "
				+ Common.my_role + " on " + Common.my_ip);
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				whatismyip.openStream()));
		Common.my_public_ip = in.readLine();
		LookupService lookup = new LookupService("GeoLiteCity.dat",
				LookupService.GEOIP_MEMORY_CACHE
						| LookupService.GEOIP_CHECK_CACHE);
		Location location = lookup.getLocation(Common.my_public_ip);
		Common.my_country = location.countryName;
		Common.my_country_code = location.countryCode;
		Common.my_region = location.region;
		Common.my_city = location.city;
		System.out.println("IP address: " + Common.my_public_ip
				+ ", Location: " + Common.my_city + ", " + Common.my_region
				+ ", " + Common.my_country + ", " + Common.my_country_code);
		{
			Common.msgSet
					.add("<RequestInformation Service=\"ObjectStorage\" Command=\"GetObject\">");
			Common.msgSet
					.add("<RequestInformation Service=\"ObjectStorage\" Command=\"DeleteObject\">");
			Common.msgSet
					.add("<ResponseInformation Service=\"ObjectStorage\" Command=\"ConfirmationForPut\">");
			Common.msgSet
					.add("<ResponseInformation Service=\"ObjectStorage\" Command=\"ConfirmationForDelete\">");
		}
	}

	static void addHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Exiting...");
				end = System.currentTimeMillis();
				LogUtil.logPerformance("Gateway END", "", end, end - start);
				try {
					kill();
				} catch (UdtdException | HttpdException | SQLException e) {
					LogUtil.logException(e);
				}
			}
		});
	}

	static void getFriends() throws SQLException, ClassNotFoundException {
		Common.my_friends = DatabaseUtil.getFriends();
		if (Common.my_role.equalsIgnoreCase("CLOUD")) {
			for (Cloud c : Common.my_friends.values()) {
				if (c.getRole().equalsIgnoreCase("EXCHANGE")) {
					Common.my_exchange = c.getName();
				}
			}
			if (Common.my_exchange == null) {
				System.out.println("You do not have any Exchange.");
			}
		} else if (Common.my_role.equalsIgnoreCase("EXCHANGE")) {
			for (Cloud c : Common.my_friends.values()) {
				if (c.getRole().equalsIgnoreCase("ROOT")) {
					Common.my_root = c.getName();
				}
			}
			if (Common.my_root == null) {
				LogUtil.logError("You do not have any Root.");
			}
		} else if (Common.my_role.equalsIgnoreCase("ROOT")) {
		}
	}

	static void getServices() throws IOException {
		if (getProp("services") == null || getProp("services").equals("")) {
			return;
		}
		String[] services = getProp("services").split(";");
		for (String s : services) {
			String service = s.split(":")[0];
			String provider = s.split(":")[1];
			Common.my_service_providers.put(service, provider);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void updateDnsRecords() throws DNSException, IOException,
			DNSServiceAPIException, AuthenticationAPIException {
		Map m = new HashMap();
		m.put("Version", Common.ICCP_VER);
		m.put("Geolocation", "City:" + Common.my_city + ";" + "Region:"
				+ Common.my_region + ";" + "Country:" + Common.my_country + ";"
				+ "CountryCode:" + Common.my_country_code);
		m.put("Vendor", getProp("vendor"));
		if (!getProp("services").equals("")) {
			m.put("Service", getProp("services"));
		}
		if (Common.my_role.equalsIgnoreCase("CLOUD")) {
			DNSServiceAPI d = new DNSServiceAPI(Common.my_exchange);
			d.updateRecord((HashMap) m, true);
		} else if (Common.my_role.equalsIgnoreCase("EXCHANGE")) {
			DNSUtil.updateRecord(Common.my_name, Common.my_name + ".", m);
		} else if (Common.my_role.equalsIgnoreCase("ROOT")) {
			DNSUtil.updateRecord(Common.my_name, Common.my_name + ".", m);
		}
	}

	static void initFileServers() throws UdtdException, FtpdException,
			HttpdException, IOException {
		// Set transfer methods.
		if (getProp("transfermethods") == null
				|| getProp("transfermethods").equals("")) {
			return;
		}
		String[] values = getProp("transfermethods").split(",");
		Common.file_transfer_methods = new HashSet<>(Arrays.asList(values));
		// Start servers
		if (Common.file_transfer_methods.contains("UDT")) {
			Udtd.startServer(Common.UDT_PORT);
		}
		if (Common.file_transfer_methods.contains("FTPS")) {
			Ftpd.startFtpsServer(Common.FTPS_PORT, Common.JKS_PW);
		}
		if (Common.file_transfer_methods.contains("FTP")) {
			Ftpd.startFtpServer(Common.FTP_PORT);
		}
		if (Common.file_transfer_methods.contains("HTTPS")) {
			Httpd.startHttpServer(Common.my_name, Common.DOWNLOAD_PATH,
					Common.SSL_EMAIL, true);
		}
		if (Common.file_transfer_methods.contains("HTTP")
				&& !Common.file_transfer_methods.contains("HTTPS")) {
			Httpd.startHttpServer(Common.my_name, Common.DOWNLOAD_PATH,
					Common.SSL_EMAIL, false);
		}
	}

	static void runGatewaySocket() {
		Runnable gs = new Runnable() {
			@Override
			public void run() {
				try {
					new Sockets(Common.GW_PORT, Common.my_name);
				} catch (IOException e) {
					LogUtil.logException(e);
				}
			}
		};
		new Thread(gs, "GatewaySocketListener").start();
	}

	static void runClientSocket() {
		// TODO: Will we use ActiveMQ or Apollo? http://queues.io/
		// Listener
		Runnable cs = new Runnable() {
			@Override
			public void run() {
				try {
					ClientSocket cs = new ClientSocket("",
							Common.CLIENT_SOCKET_PORT, ClientSocket.RECEIVER);
				} catch (ClientSocketException e) {
					LogUtil.logException(e);
				}
			}
		};
		new Thread(cs, "ClientSocketListener").start();
		// Client request queue
		new Thread(new ClientProcessor(), "ClientRequestQueue").start();
	}

	static void housekeeperWatchdog() throws IOException {
		// Housekeeper
		try {
			Common.housekeeperInterval = Integer
					.valueOf(getProp("housekeeper_interval"));
		} catch (Exception e) {
		}
		Housekeeper.start();
		// Watchdog
		try {
			Common.watchdogInterval = Integer
					.valueOf(getProp("watchdog_interval"));
			Watchdog.start();
		} catch (Exception e) {
			System.out
					.println("WATCHDOG: Watchdog disabled, or either [watchdog_interval] or [watchdoglog] setting is incorrect.");
		}
	}

	static void runClientTesterLite() {
		Runnable ct = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				ClientTesterLite.main(null);
			}
		};
		new Thread(ct, "ClientTesterLite").start();
	}

	static void kill() throws UdtdException, HttpdException, SQLException {
		Udtd.killServer();
		Ftpd.killFtpServer();
		Ftpd.killFtpsServer();
		Httpd.killHttpServer();
		Common.con.close();
	}

	static boolean portAvailable(int port) {
		try {
			(new Socket("127.0.0.1", port)).close();
			return false; // Connectable, so the port is taken.
		} catch (IOException e) {
			return true;
		}
	}

	static String getProp(String item) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream;
		String prop = "";
		inputStream = new FileInputStream(Common.GW_PROP_FILE);
		properties.load(inputStream);
		prop = properties.getProperty(item);
		System.out.println(item + " = " + prop);
		return prop;
	}

}

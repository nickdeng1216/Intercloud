package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.exceptions.DNSException;
import hk.edu.polyu.intercloud.model.dns.DNSTXTRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DNSUtil {

	/*-----Get IP and TXT record from DNS server-----*/
	public static String getIP(String domainName) throws DNSException {
		Path currentRelativePath = Paths.get("");
		String command = currentRelativePath.toAbsolutePath().toString()
				+ File.separator + "bind" + File.separator + "dig "
				+ domainName + " A";
		StringBuffer output = new StringBuffer();

		String[] parts = domainName.split("\\.");
		if (parts.length > 1) {
			command += " @"
					+ domainName.substring(parts[0].length() + 1,
							domainName.length());
		}

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			boolean flag = false;
			Pattern pattern_Answer = Pattern
					.compile(";; flags:.*QUERY.*ANSWER: (\\d+), AUTHORITY.*ADDITIONAL.*");
			Pattern pattern_IP = Pattern.compile(domainName + ".*A\\t"
					+ "(\\d+\\.\\d+\\.\\d+\\.\\d+)");

			while ((line = reader.readLine()) != null) {
				if (line.contains(";; flags:")) {
					Matcher matcher = pattern_Answer.matcher(line);
					if (!(matcher.find() && Integer.parseInt(matcher.group(1)) > 0)) {
						return "";
					}
				}

				if (line.contains(";; ANSWER SECTION:")) {
					flag = true;
					continue;
				} else if (line.equals("")) {
					flag = false;
				}
				if (flag) {
					Matcher matcher = pattern_IP.matcher(line);
					if (matcher.find()) {
						output.append(matcher.group(1) + ";");
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new DNSException(e.getMessage(), e);
		}

		return output.toString().substring(0, output.toString().length() - 1);
	}

	public static DNSTXTRecord getTXTRecord(String domainName)
			throws DNSException {
		Path currentRelativePath = Paths.get("");
		String command = currentRelativePath.toAbsolutePath().toString()
				+ File.separator + "bind" + File.separator + "dig "
				+ domainName + " TXT";
		DNSTXTRecord dnsRecord = new DNSTXTRecord();

		// String[] parts = domainName.split("\\.");
		// if (parts.length > 1) {
		// command += " @"
		// + domainName.substring(parts[0].length() + 1,
		// domainName.length());
		// }

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			boolean flag = false;
			Pattern pattern_Answer = Pattern
					.compile(";; flags:.*QUERY.*ANSWER: (\\d+), AUTHORITY.*ADDITIONAL.*");
			Pattern pattern_TXT = Pattern.compile(domainName + ".*TXT\\t"
					+ "\"([a-zA-Z_0-9:;.]+)=(.+)\"");

			while ((line = reader.readLine()) != null) {
				if (line.contains(";; flags:")) {
					Matcher matcher = pattern_Answer.matcher(line);
					if (!(matcher.find() && Integer.parseInt(matcher.group(1)) > 0)) {
						return dnsRecord;
					}
				}

				if (line.contains(";; ANSWER SECTION:")) {
					flag = true;
					continue;
				} else if (line.equals("")) {
					flag = false;
				}
				if (flag) {
					Matcher matcher = pattern_TXT.matcher(line);
					if (matcher.find()) {
						if (matcher.group(1).equalsIgnoreCase(
								"Intercloud_Service")) {
							dnsRecord.setService(matcher.group(2));
						} else if (matcher.group(1).equalsIgnoreCase(
								"Intercloud_Vendor")) {
							dnsRecord.setVendor(matcher.group(2));
						} else if (matcher.group(1).equalsIgnoreCase(
								"Intercloud_Geolocation")) {
							dnsRecord.setGeolocation(matcher.group(2));
						} else if (matcher.group(1).equalsIgnoreCase(
								"Intercloud_ICCPVer")) {
							dnsRecord.setIccpVersion(matcher.group(2));
						}
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new DNSException(e.getMessage(), e);
		}

		return dnsRecord;
	}

	public static String getService(String domainName) throws DNSException {
		return DNSUtil.getTXTRecord(domainName).getService();
	}

	/*-----List out records of a host from local DNS server (Used by Exchange and Root)-----*/
	/* This method is not being used!!! */
	public static ArrayList<String[]> listRecords(String domain, String hostName)
			throws DNSException {
		String command = "dnscmd /enumrecords " + domain + " " + hostName;
		ArrayList<String[]> records = new ArrayList<String[]>();
		Pattern pattern_IP = Pattern
				.compile(".*A\\t(\\d+\\.\\d+\\.\\d+\\.\\d+)");
		Pattern pattern_TXT = Pattern
				.compile(".*TXT\\t\\t([a-zA-Z_0-9:;.]+)=([a-zA-Z_0-9:;.]+)");

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern_TXT.matcher(line);
				if (matcher.find()) {
					String[] temp = { matcher.group(1), matcher.group(2) };
					records.add(temp);
					continue;
				}
				Matcher matcher2 = pattern_IP.matcher(line);
				if (matcher2.find()) {
					String[] temp = { "IP", matcher2.group(1) };
					records.add(temp);
				}
			}

		} catch (IOException | InterruptedException e) {
			throw new DNSException(e.getMessage(), e);
		}
		return records;
	}

	public static ArrayList<String[]> listIntercloudTXTRecords(String domain,
			String hostName) throws DNSException {
		String command = "dnscmd /enumrecords " + domain + " " + hostName
				+ " /type TXT";
		ArrayList<String[]> records = new ArrayList<String[]>();
		// Pattern pattern_TXT = Pattern
		// .compile(".*TXT\\t\\t([a-zA-Z_0-9:;.]+)=([a-zA-Z_0-9:;.]+)");
		Pattern pattern_TXT = Pattern.compile(".*TXT\\t\\t(.+)=(.+)");
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern_TXT.matcher(line);
				if (matcher.find()
						&& matcher.group(1).startsWith("Intercloud_")) {
					String[] temp = {
							matcher.group(1).substring("Intercloud_".length(),
									matcher.group(1).length()),
							matcher.group(2) };
					records.add(temp);
				}
			}

		} catch (IOException | InterruptedException e) {
			throw new DNSException(e.getMessage(), e);
		}
		return records;
	}

	/*-----Add/Update/DeleteAll records of a host on local DNS server (Used by Exchange and Root)-----*/
	public static void addRecord(String domain, String hostName,
			Map<String, String> records) throws DNSException {
		String command_add = "dnscmd /recordadd " + domain + " " + hostName;
		String command_delete_A = "dnscmd /recorddelete " + domain + " "
				+ hostName + " A /f";

		for (Entry<String, String> entry : records.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().replaceAll("\\s+", "");
			String command = command_add + " TXT " + "Intercloud_" + key + "="
					+ value;

			Process p;
			try {
				p = Runtime.getRuntime().exec(command);
				p.waitFor();
			} catch (IOException | InterruptedException e) {
				throw new DNSException(e.getMessage(), e);
			}
		}
		/*
		 * BufferedReader reader = new BufferedReader(new InputStreamReader(
		 * p.getInputStream()));
		 * 
		 * String line = ""; while ((line = reader.readLine()) != null) {
		 * System.out.println(line); }
		 */
	}

	public static void updateRecord(String domain, String hostName,
			Map<String, String> records) throws DNSException {
		DNSUtil.deleteAllRecord(domain, hostName);
		DNSUtil.addRecord(domain, hostName, records);
	}

	public static void deleteAllRecord(String domain, String hostName)
			throws DNSException {
		// String command_deleteAll = "dnscmd /nodedelete " + domain + " "
		// + hostName + " /tree /f";
		String command_deleteSingleTXTRecord = "dnscmd /recorddelete " + domain
				+ " " + hostName + " TXT ";

		// System.err.println(domain + "=" + hostName);
		ArrayList<String[]> records = DNSUtil.listIntercloudTXTRecords(domain,
				hostName);
		for (int i = 0; i < records.size(); i++) {
			Process p;
			try {
				// System.err.println(i + " " + command_deleteSingleTXTRecord
				// + "Intercloud_" + records.get(i)[0] + "="
				// + records.get(i)[1] + " /f");
				p = Runtime.getRuntime().exec(
						command_deleteSingleTXTRecord + "Intercloud_"
								+ records.get(i)[0] + "=" + records.get(i)[1]
								+ " /f");
				p.waitFor();
			} catch (IOException | InterruptedException e) {
				throw new DNSException(e.getMessage(), e);
			}
		}
	}
}

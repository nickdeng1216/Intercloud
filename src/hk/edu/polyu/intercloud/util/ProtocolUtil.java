package hk.edu.polyu.intercloud.util;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.command.ExceptionCommand;
import hk.edu.polyu.intercloud.command.Unauthenticated;
import hk.edu.polyu.intercloud.command.UnsupportedCommand;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.security.RSA;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The class is used for generating Protocol String
 * 
 * @author Kate.Xie
 * @author harry
 *
 */
public class ProtocolUtil {

	/**
	 * Generate request string
	 * 
	 * @param protocol
	 *            It is an object which contains all protocol's components.
	 * @param security
	 *            If true, the request string needed to be signed.
	 * @return A protocol request string in XML format.
	 * @throws ProtocolException
	 * @throws RequestGenerationFailException
	 */
	public static String generateRequest(Protocol protocol, boolean security)
			throws ProtocolException {

		GeneralInformation generalInformation = protocol
				.getGeneralInformation();
		RequestInformation requestInformation = protocol
				.getRequestInformation();
		AdditionalInformation additionalInfomation = protocol
				.getAdditionalInformation();

		String request = "<Request Version=\"" + protocol.getProtocolVersion()
				+ "\"" + " ID=\"" + protocol.getId() + "\">\n";

		// GeneralInfo
		request += "<GeneralInformation From=\"" + generalInformation.getFrom()
				+ "\"" + " To=\"" + generalInformation.getTo() + "\""
				+ " Date=\"" + generalInformation.getDate() + "\"" + " Time=\""
				+ generalInformation.getTime() + "\">\n";
		request += "</GeneralInformation>\n";

		// RequestInfo
		request += "<RequestInformation Service=\""
				+ requestInformation.getService() + "\"" + " Command=\""
				+ requestInformation.getCommand() + "\">\n";
		for (Map.Entry<String, String> entry : requestInformation.getTags()
				.entrySet()) {
			request += "<" + entry.getKey() + ">" + entry.getValue() + "</"
					+ entry.getKey() + ">\n";
		}
		request += "</RequestInformation>\n";

		// AdditionalInfo
		request += "<AdditionalInformation>\n";
		for (Map.Entry<String, String> add : additionalInfomation.getTags()
				.entrySet()) {
			request += "<" + add.getKey() + ">" + add.getValue() + "</"
					+ add.getKey() + ">\n";
		}

		String nonSecReq = request;

		request += "</AdditionalInformation>\n";
		request += "</Request>\n";

		if (security) {
			String sign = null;
			try {
				sign = RSA.sign(request.getBytes(), Common.KEY_PATH
						+ "private.pem");
			} catch (Exception e) {
				throw new ProtocolException("Failed to generate request.", e);
			}
			nonSecReq += "<Signature>" + sign + "</Signature>\n";
			nonSecReq += "<SignatureAlgorithm>" + "SHA1/RSA"
					+ "</SignatureAlgorithm>\n";
			nonSecReq += "</AdditionalInformation>\n";
			nonSecReq += "</Request>\n";
			return nonSecReq;
		} else {
			return request;
		}
	}

	/**
	 * Generate response string
	 * 
	 * @param protocol
	 *            It is an object which contains all protocol's components.
	 * @param security
	 *            If true, the response string needed to be signed.
	 * @return A protocol response string in XML format.
	 * @throws ProtocolException
	 */
	public static String generateResponse(Protocol protocol, boolean security)
			throws ProtocolException {
		GeneralInformation generalInformation = protocol
				.getGeneralInformation();
		ResponseInformation responseInformation = protocol
				.getResponseInformation();
		AdditionalInformation additionalInformation = protocol
				.getAdditionalInformation();

		String response = "<Response Version=\""
				+ protocol.getProtocolVersion() + "\"" + " ID=\""
				+ protocol.getId() + "\">\n";

		// GeneralInfo
		response += "\t<GeneralInformation From=\""
				+ generalInformation.getFrom() + "\"" + " To=\""
				+ generalInformation.getTo() + "\"" + " Date=\""
				+ generalInformation.getDate() + "\"" + " Time=\""
				+ generalInformation.getTime() + "\">\n";
		response += "\t</GeneralInformation>\n";

		// ResponseInfo
		response += "\t<ResponseInformation Service=\""
				+ responseInformation.getService() + "\"" + " Command=\""
				+ responseInformation.getCommand() + "\">\n";
		for (Map.Entry<String, String> entry : responseInformation.getTags()
				.entrySet()) {
			response += "\t\t<" + entry.getKey() + ">" + entry.getValue()
					+ "</" + entry.getKey() + ">\n";
		}
		response += "\t</ResponseInformation>\n";

		// AdditionalInfo
		response += "\t<AdditionalInformation>\n";
		for (Map.Entry<String, String> add : additionalInformation.getTags()
				.entrySet()) {
			response += "\t\t<" + add.getKey() + ">" + add.getValue() + "</"
					+ add.getKey() + ">\n";
		}

		String nonSecRes = response;

		response += "\t</AdditionalInformation>\n";
		response += "</Response>\n";

		if (security) {
			String privateKey = Common.KEY_PATH + "private.pem";
			String sign;
			try {
				sign = RSA.sign(response.getBytes(), privateKey);
			} catch (Exception e) {
				throw new ProtocolException("Failed to generate response.", e);
			}
			nonSecRes += "<Signature>" + sign + "</Signature>\n";
			nonSecRes += "<SignatureAlgorithm>" + "SHA1/RSA"
					+ "</SignatureAlgorithm>\n";
			nonSecRes += "</AdditionalInformation>\n";
			nonSecRes += "</Response>\n";
			return nonSecRes;
		} else {
			return response;
		}
	}

	/**
	 * Generate exception string
	 * 
	 * @param protocol
	 *            It is an object which contains all protocol's components.
	 * @param security
	 *            If true, the exception string needed to be signed.
	 * @return A protocol exception string in XML format.
	 */
	public static String generateException(ExceptionProtocol exceptionProtocol) {
		GeneralInformation generalInformation = exceptionProtocol
				.getGeneralInformation();
		ExceptionInformation exceptionInformation = exceptionProtocol
				.getExceptionInformation();
		AdditionalInformation additionalInformation = exceptionProtocol
				.getAdditionalInformation();

		String exception = "<Exception Version=\""
				+ exceptionProtocol.getProtocolVersion() + "\" ID=\""
				+ exceptionProtocol.getId() + "\">\n";

		// GeneralInformation
		exception += "\t<GeneralInformation From=\""
				+ generalInformation.getFrom() + "\"" + " To=\""
				+ generalInformation.getTo() + "\"" + " Date=\""
				+ generalInformation.getDate() + "\"" + " Time=\""
				+ generalInformation.getTime() + "\">\n"
				+ "\t</GeneralInformation>\n";

		// ExceptionInformation
		exception += "\t<ExceptionInformation>\n";
		for (Map.Entry<String, String> tags : exceptionInformation.getTags()
				.entrySet()) {
			exception += "\t\t<" + tags.getKey() + ">" + tags.getValue() + "</"
					+ tags.getKey() + ">\n";
		}
		exception += "\t</ExceptionInformation>\n";

		// AdditionalInformation
		exception += "\t<AdditionalInformation>\n";
		for (Map.Entry<String, String> tags : additionalInformation.getTags()
				.entrySet()) {
			exception += "\t\t<" + tags.getKey() + ">" + tags.getValue() + "</"
					+ tags.getKey() + ">\n";
		}

		exception += "\t</AdditionalInformation>\n";
		exception += "</Exception>\n";

		return exception;
	}

	/**
	 * In this method, protocol string is decomposed to object and will create a
	 * new command instance according to the protocol command. The object also
	 * is sent to the new instance.
	 * 
	 * @param protocolString
	 *            The protocol in String.
	 * @return A command instance
	 * @throws ProtocolException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InterruptedException
	 */
	public static Command parseProtocol(String protocolString)
			throws ProtocolException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(
					protocolString)));

			String protocolVersion, id, from, to, date, time;
			String body = null;
			String command = null;
			String service = null;
			Map<String, String> enquiry_para = new HashMap<>();
			Map<String, String> additional_para = new HashMap<>();
			GeneralInformation generalInformation = null;
			RequestInformation requestInformation = null;
			ResponseInformation responseInformation = null;
			ExceptionInformation exceptionInformation = null;
			AdditionalInformation additionalInformation = null;
			Protocol protocol = null;
			ExceptionProtocol exceptionProtocol = null;

			// root part
			Node roottag = doc.getFirstChild();
			NamedNodeMap roottag_attrs = roottag.getAttributes();
			protocolVersion = roottag_attrs.getNamedItem("Version")
					.getNodeValue();
			id = roottag_attrs.getNamedItem("ID").getNodeValue();
			// System.out.println("12---------" + version + id);

			// general part
			NodeList generalinfo = doc
					.getElementsByTagName("GeneralInformation");
			NamedNodeMap general_attrs = generalinfo.item(0).getAttributes();
			from = general_attrs.getNamedItem("From").getNodeValue();
			// System.out.println("---------" + from);
			to = general_attrs.getNamedItem("To").getNodeValue();
			// System.out.println("---------" + to);
			date = general_attrs.getNamedItem("Date").getNodeValue();
			// System.out.println("---------" + date);
			time = general_attrs.getNamedItem("Time").getNodeValue();
			// System.out.println("---------" + time);
			generalInformation = new GeneralInformation(from, to, date, time);

			// enquire part
			if (!doc.getFirstChild().getNodeName().equals("Exception")) {

				if (doc.getFirstChild().getNodeName().equals("Response")) {
					NodeList enquiryinfo = doc
							.getElementsByTagName("ResponseInformation");
					NamedNodeMap enquiryinfo_attrs = enquiryinfo.item(0)
							.getAttributes();
					command = enquiryinfo_attrs.getNamedItem("Command")
							.getNodeValue();
					service = enquiryinfo_attrs.getNamedItem("Service")
							.getNodeValue();
					// System.out.println("---------" + command);

					NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
					// System.out.println(enquirynode.item(1).getNodeName());
					if (enquirynode != null && enquirynode.getLength() > 0) {
						for (int i = 0; i < enquirynode.getLength(); i++) {
							Node node = enquirynode.item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								enquiry_para.put(node.getNodeName(),
										node.getTextContent());
							}
						}
						responseInformation = new ResponseInformation();
						responseInformation.setCommand(command);
						responseInformation.setService(service);
						responseInformation.setTags(enquiry_para);
					}
				} else if (doc.getFirstChild().getNodeName().equals("Request")) {
					NodeList enquiryinfo = doc
							.getElementsByTagName("RequestInformation");
					NamedNodeMap enquiryinfo_attrs = enquiryinfo.item(0)
							.getAttributes();
					command = enquiryinfo_attrs.getNamedItem("Command")
							.getNodeValue();
					service = enquiryinfo_attrs.getNamedItem("Service")
							.getNodeValue();
					// System.out.println("---------" + command);

					NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
					// System.out.println(enquirynode.item(1).getNodeName());
					if (enquirynode != null && enquirynode.getLength() > 0) {
						for (int i = 0; i < enquirynode.getLength(); i++) {
							Node node = enquirynode.item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								enquiry_para.put(node.getNodeName(),
										node.getTextContent());
							}
						}
						requestInformation = new RequestInformation();
						requestInformation.setCommand(command);
						requestInformation.setService(service);
						requestInformation.setTags(enquiry_para);
					}
				}

				// additional part
				NodeList additionalinfo = doc
						.getElementsByTagName("AdditionalInformation");
				NodeList additionalnode = additionalinfo.item(0)
						.getChildNodes();
				if (additionalnode != null && additionalnode.getLength() > 0) {
					for (int i = 0; i < additionalnode.getLength(); i++) {
						Node node = additionalnode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							additional_para.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					additionalInformation = new AdditionalInformation();
					additionalInformation.setTags(additional_para);
				}

				protocol = new Protocol(protocolVersion, id,
						generalInformation, requestInformation,
						responseInformation, additionalInformation, body);

				// filter unknown command
				if (!filterCommand(service, command)) {
					protocol.getAdditionalInformation().removeTags("Signature");
					UnsupportedCommand unknown = new UnsupportedCommand();
					unknown.setProtocol(protocol);
					return unknown;
				}
				if (!checkCommandType(protocol)) {
					if (!(Common.my_friends.containsKey(from) && Common.my_friends
							.get(from).getAuth())) {
						protocol.getAdditionalInformation().removeTags(
								"Signature");
						Unauthenticated unauthenticated = new Unauthenticated();
						unauthenticated.setProtocol(protocol);
						return unauthenticated;
					}
				}

				Class<?> action = Class
						.forName("hk.edu.polyu.intercloud.command."
								+ service.toLowerCase() + "." + command);
				Command c = (Command) action.newInstance();
				c.setProtocol(protocol);

				return c;

			} else {
				LinkedHashMap<String, String> enquiry_tag = new LinkedHashMap<>();

				NodeList enquiryinfo = doc
						.getElementsByTagName("ExceptionInformation");

				command = "ExceptionCommand";

				NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
				if (enquirynode != null && enquirynode.getLength() > 0) {
					for (int i = 0; i < enquirynode.getLength(); i++) {
						Node node = enquirynode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							enquiry_tag.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					exceptionInformation = new ExceptionInformation();
					exceptionInformation.setCommand(command);
					exceptionInformation.setTags(enquiry_tag);
				}

				// additional part
				NodeList additionalinfo = doc
						.getElementsByTagName("AdditionalInformation");
				NodeList additionalnode = additionalinfo.item(0)
						.getChildNodes();
				if (additionalnode != null && additionalnode.getLength() > 0) {
					for (int i = 0; i < additionalnode.getLength(); i++) {
						Node node = additionalnode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							additional_para.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					additionalInformation = new AdditionalInformation();
					additionalInformation.setTags(additional_para);
				}

				exceptionProtocol = new ExceptionProtocol(protocolVersion, id,
						generalInformation, exceptionInformation,
						additionalInformation);

				Class<?> action = Class
						.forName("hk.edu.polyu.intercloud.command." + command);
				ExceptionCommand c = (ExceptionCommand) action.newInstance();
				c.setProtocol(exceptionProtocol);
				return c;
			}
		} catch (ClassNotFoundException e) {
			LogUtil.logException(e);
		} catch (ParserConfigurationException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (SAXException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			LogUtil.logException(e);
		}
		return null;
	}

	/**
	 * Verify the protocol (i.e. check signature)
	 * 
	 * @param protocol
	 *            The protocol object to be verified.
	 * @return True if correct, false otherwise.
	 * @throws ProtocolException
	 * @throws Exception
	 */
	public static boolean verifyProtocol(Protocol protocol)
			throws ProtocolException {

		if (protocol.getAdditionalInformation().getTags()
				.containsKey("Signature")) {
			if (protocol.getRequestInformation() != null
					&& protocol.getRequestInformation().getCommand()
							.equalsIgnoreCase("Authentication"))
				return true;
			else if (protocol.getResponseInformation() != null
					&& protocol.getResponseInformation().getCommand()
							.equalsIgnoreCase("AuthChallenge"))
				return true;
			String sing_Protocol = null;

			String publicKey = Common.KEY_PATH + "Others" + File.separator
					+ protocol.getGeneralInformation().getFrom() + ".pem";

			String recieve_Sign = protocol.getAdditionalInformation().getValue(
					"Signature");
			String recieve_SignAlg = protocol.getAdditionalInformation()
					.getValue("SignatureAlgorithm");

			AdditionalInformation additionalInformation = protocol
					.getAdditionalInformation();
			additionalInformation.removeTags("Signature");
			additionalInformation.removeTags("SignatureAlgorithm");

			// protocol.setAdditionalInfomation(additionalInformation);
			if (protocol.getRequestInformation() != null
					&& protocol.getResponseInformation() == null) {
				sing_Protocol = generateRequest(protocol, false);
			} else {
				sing_Protocol = generateResponse(protocol, false);
			}

			boolean result;
			try {
				result = RSA.verify(sing_Protocol.getBytes(), publicKey,
						recieve_Sign);
			} catch (Exception e) {
				throw new ProtocolException("Failed to verify protocol.", e);
			}
			additionalInformation.addTags("Signature", recieve_Sign);
			additionalInformation
					.addTags("SignatureAlgorithm", recieve_SignAlg);
			return result;
		} else {
			return true;
		}
	}

	/**
	 * In this method, protocol string is decomposed to object and will create a
	 * new command instance according to the protocol command. The object also
	 * is sent to the new instance.
	 * 
	 * @param protocolString
	 *            The protocol in String.
	 * @return command instance
	 * @throws ProtocolException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static Protocol parseProtocolType(String protocolString)
			throws ProtocolException {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(
					protocolString)));

			String protocolVersion, id, from, to, date, time;
			String body = null;
			String command = null;
			Map<String, String> enquiry_para = new HashMap<>();
			Map<String, String> additional_para = new HashMap<>();
			GeneralInformation generalInformation = null;
			RequestInformation requestInformation = null;
			ResponseInformation responseInformation = null;
			ExceptionInformation exceptionInformation = null;
			AdditionalInformation additionalInformation = null;
			Protocol protocol = null;
			ExceptionProtocol exceptionProtocol = null;
			String service = null;

			// root part
			Node roottag = doc.getFirstChild();
			NamedNodeMap roottag_attrs = roottag.getAttributes();
			protocolVersion = roottag_attrs.getNamedItem("Version")
					.getNodeValue();
			id = roottag_attrs.getNamedItem("ID").getNodeValue();
			// System.out.println("12---------" + version + id);

			// general part
			NodeList generalinfo = doc
					.getElementsByTagName("GeneralInformation");
			NamedNodeMap general_attrs = generalinfo.item(0).getAttributes();
			from = general_attrs.getNamedItem("From").getNodeValue();
			// System.out.println("---------" + from);
			to = general_attrs.getNamedItem("To").getNodeValue();
			// System.out.println("---------" + to);
			date = general_attrs.getNamedItem("Date").getNodeValue();
			// System.out.println("---------" + date);
			time = general_attrs.getNamedItem("Time").getNodeValue();
			// System.out.println("---------" + time);
			generalInformation = new GeneralInformation(from, to, date, time);

			// enquire part
			if (!doc.getFirstChild().getNodeName().equals("Exception")) {

				if (doc.getFirstChild().getNodeName().equals("Response")) {
					NodeList enquiryinfo = doc
							.getElementsByTagName("ResponseInformation");
					NamedNodeMap enquiryinfo_attrs = enquiryinfo.item(0)
							.getAttributes();
					command = enquiryinfo_attrs.getNamedItem("Command")
							.getNodeValue();
					service = enquiryinfo_attrs.getNamedItem("Service")
							.getNodeValue();
					// System.out.println("---------" + command);

					NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
					// System.out.println(enquirynode.item(1).getNodeName());
					if (enquirynode != null && enquirynode.getLength() > 0) {
						for (int i = 0; i < enquirynode.getLength(); i++) {
							Node node = enquirynode.item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								enquiry_para.put(node.getNodeName(),
										node.getTextContent());
							}
						}
						responseInformation = new ResponseInformation();
						responseInformation.setCommand(command);

						responseInformation.setService(service);
						responseInformation.setTags(enquiry_para);
					}
				} else if (doc.getFirstChild().getNodeName().equals("Request")) {
					NodeList enquiryinfo = doc
							.getElementsByTagName("RequestInformation");
					NamedNodeMap enquiryinfo_attrs = enquiryinfo.item(0)
							.getAttributes();
					command = enquiryinfo_attrs.getNamedItem("Command")
							.getNodeValue();
					service = enquiryinfo_attrs.getNamedItem("Service")
							.getNodeValue();
					// System.out.println("---------" + command);

					NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
					// System.out.println(enquirynode.item(1).getNodeName());
					if (enquirynode != null && enquirynode.getLength() > 0) {
						for (int i = 0; i < enquirynode.getLength(); i++) {
							Node node = enquirynode.item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								enquiry_para.put(node.getNodeName(),
										node.getTextContent());
							}
						}
						requestInformation = new RequestInformation();
						requestInformation.setCommand(command);
						requestInformation.setService(service);
						requestInformation.setTags(enquiry_para);
					}
				}

				// additional part
				NodeList additionalinfo = doc
						.getElementsByTagName("AdditionalInformation");
				NodeList additionalnode = additionalinfo.item(0)
						.getChildNodes();
				if (additionalnode != null && additionalnode.getLength() > 0) {
					for (int i = 0; i < additionalnode.getLength(); i++) {
						Node node = additionalnode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							additional_para.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					additionalInformation = new AdditionalInformation();
					additionalInformation.setTags(additional_para);
				}

				protocol = new Protocol(protocolVersion, id,
						generalInformation, requestInformation,
						responseInformation, additionalInformation, body);

				return protocol;

			} else {
				LinkedHashMap<String, String> enquiry_tag = new LinkedHashMap<>();

				NodeList enquiryinfo = doc
						.getElementsByTagName("ExceptionInformation");

				command = "ExceptionCommand";

				NodeList enquirynode = enquiryinfo.item(0).getChildNodes();
				if (enquirynode != null && enquirynode.getLength() > 0) {
					for (int i = 0; i < enquirynode.getLength(); i++) {
						Node node = enquirynode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							enquiry_tag.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					exceptionInformation = new ExceptionInformation();
					exceptionInformation.setCommand(command);
					exceptionInformation.setTags(enquiry_tag);
				}

				// additional part
				NodeList additionalinfo = doc
						.getElementsByTagName("AdditionalInformation");
				NodeList additionalnode = additionalinfo.item(0)
						.getChildNodes();
				if (additionalnode != null && additionalnode.getLength() > 0) {
					for (int i = 0; i < additionalnode.getLength(); i++) {
						Node node = additionalnode.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							additional_para.put(node.getNodeName(),
									node.getTextContent());
						}
					}
					additionalInformation = new AdditionalInformation();
					additionalInformation.setTags(additional_para);
				}

				exceptionProtocol = new ExceptionProtocol(protocolVersion, id,
						generalInformation, exceptionInformation,
						additionalInformation);

				return exceptionProtocol;
			}
		} catch (SAXException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			throw new ProtocolException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ProtocolException(e.getMessage(), e);
		}
	}

	public static boolean filterCommand(String service, String command) {
		if (!checkService(service)) {
			return false;
		}
		try {
			Class.forName("hk.edu.polyu.intercloud.command."
					+ service.toLowerCase() + "." + command);
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}

	public static boolean checkService(String service) {
		boolean flag = false;
		if (Common.my_service_providers.containsKey(service)
				|| service.equalsIgnoreCase("Security")
				|| service.equalsIgnoreCase("DNS")
				|| service.equalsIgnoreCase("InquireResource"))
			flag = true;
		return flag;

	}

	public static boolean checkCommandType(Protocol protocol) {
		boolean flag = false;
		if (protocol.getRequestInformation() != null
				&& protocol.getRequestInformation().getCommand()
						.equalsIgnoreCase("Authentication"))
			return true;
		else if (protocol.getResponseInformation() != null
				&& protocol.getResponseInformation().getCommand()
						.equalsIgnoreCase("AuthChallenge"))
			return true;
		else if (protocol.getResponseInformation() != null
				&& protocol.getResponseInformation().getCommand()
						.equalsIgnoreCase("ConfirmationOfAuthentication"))
			return true;
		else if (protocol.getRequestInformation() != null
				&& protocol.getRequestInformation().getCommand()
						.equalsIgnoreCase("AuthResponseChallenge"))
			return true;

		return flag;
	}

	public static String generateID() {
		String part1 = String.valueOf(System.currentTimeMillis());
		String part2 = String.format("%06d", new Random().nextInt(1000000));
		return part1 + part2;
	}

}

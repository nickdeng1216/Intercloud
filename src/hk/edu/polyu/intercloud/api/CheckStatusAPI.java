package hk.edu.polyu.intercloud.api;

import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * The API class for checking the status of requests
 * 
 * @author Priere
 */
public class CheckStatusAPI {

	/**
	 * <b>For requests invoked by JSON messages.</b><br>
	 * Get the status of an operation using a request ID. Example:
	 * 
	 * <pre>
	 * getStatus(13579);
	 * </pre>
	 * 
	 * gets the status of the operation referenced by RID 13579.
	 * 
	 * @param rid
	 *            The request ID.
	 * @return The status in String. If the operation was successfully finished,
	 *         the status will be in JSON format. Otherwise, message such as
	 *         "Waiting", "ID NOT found" or "Exception" will be returned.
	 * @throws SQLException
	 * @throws ParseException
	 * @throws ProtocolException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public String getStatus(long rid) throws SQLException, ParseException,
			ProtocolException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, SAXException, IOException,
			ParserConfigurationException {
		String status = "";
		long pid = DatabaseUtil.getPid(rid);
		if (pid == -1) {
			status = "ID NOT found";
		} else {
			status = getStatusNativeJava(String.valueOf(pid));
		}
		return status;
	}

	/**
	 * <b>For requests invoked by native Java calls.</b><br>
	 * Get the status of an operation using a request ID. Example:
	 * 
	 * <pre>
	 * getStatusNativeJava(&quot;213543135213&quot;);
	 * </pre>
	 * 
	 * gets the status of the operation referenced by protocol ID 213543135213.
	 * 
	 * @param pid
	 *            The protocol ID
	 * @return The status in String. If the operation was successfully finished,
	 *         the status will be in JSON format. Otherwise, message such as
	 *         "Waiting", "ID NOT found" or "Exception" will be returned.
	 * @throws SQLException
	 * @throws ParseException
	 * @throws ProtocolException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public String getStatusNativeJava(String pid) throws SQLException,
			ParseException, ProtocolException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, SAXException,
			IOException, ParserConfigurationException {
		String status = "";
		String result = DatabaseUtil.getResultTrack(Long.valueOf(pid));
		Protocol protocol;
		if (result != null) {
			protocol = ProtocolUtil.parseProtocolType(result);
			if (protocol instanceof ExceptionProtocol) {
				String message = ((ExceptionProtocol) protocol)
						.getExceptionInformation().getValue("Message");
				String type = ((ExceptionProtocol) protocol)
						.getExceptionInformation().getValue("Type");
				status = "Exception: " + type + ", " + message;
			} else if (protocol instanceof Protocol) {
				status = protocol.toJSON().toString();
			}
		} else {
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Long.valueOf(pid));
			status = "Waiting, " + formatter.format(calendar.getTime());
		}
		System.out.println("STATUS: " + status);
		return status;
	}
}

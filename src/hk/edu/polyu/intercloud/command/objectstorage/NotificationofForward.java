package hk.edu.polyu.intercloud.command.objectstorage;

import hk.edu.polyu.intercloud.api.ObjectStorageAPI;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.AuthenticationAPIException;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.ObjectStorageAPIException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * 
 * @author Kate.xie
 *
 */
public class NotificationofForward implements Command {
	private Protocol protocol;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private AdditionalInformation additionalInformation;

	@Override
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	@Override
	public Protocol getProtocol() {
		return this.protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		initialization();
		String forwardcloud = this.additionalInformation
				.getValue("ForwardCloud");
		String owner = this.generalInformation.getFrom();
		String permission_id = this.additionalInformation
				.getValue("PermissionID");
		String objName = this.requestInformation.getValue("ObjectName");

		if (forwardcloud.equalsIgnoreCase(Common.my_name)) {
			return this.generateProtocol();
		} else {
			try {
				boolean protocolseurity;
				if (protocol.getAdditionalInformation().getTags()
						.containsKey("Signature"))
					protocolseurity = true;
				else
					protocolseurity = false;
				ObjectStorageAPI api = new ObjectStorageAPI(forwardcloud);
				String id = api.forward(
						this.protocol.getId(),
						objName,
						owner,
						permission_id,
						protocolseurity,
						this.protocol.getAdditionalInformation().getValue(
								"Overwrite"));
				String msg = DatabaseUtil.getResultTrack(Long.valueOf(id));
				Protocol protocol = ProtocolUtil.parseProtocolType(msg);
				if (protocol instanceof ExceptionProtocol) {
					String message = ((ExceptionProtocol) protocol)
							.getExceptionInformation().getValue("Message");
					return generateException(
							this.protocol.getProtocolVersion(),
							"Forward Exception", message);
				}
				return generateProtocol();
			} catch (AuthenticationAPIException | ObjectStorageAPIException e) {
				LogUtil.logException(e);
				return this.generateException("0",
						IntercloudException.class.getSimpleName(),
						e.getMessage());
			} catch (NumberFormatException e) {
				LogUtil.logException(e);
				return this.generateException("0",
						IntercloudException.class.getSimpleName(),
						e.getMessage());
			} catch (ClassNotFoundException | SQLException | ParseException e) {
				LogUtil.logException(e);
				return this
						.generateException("2",
								DatabaseException.class.getSimpleName(),
								e.getMessage());
			} catch (ProtocolException e) {
				LogUtil.logException(e);
				return this.generateException("0",
						IntercloudException.class.getSimpleName(),
						e.getMessage());
			}
		}

	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		AdditionalInformation additional = new AdditionalInformation();
		additional.addTags("Overwrite", info.get("Overwrite"));
		additional.addTags("PermissionID", info.get("PermissionID"));
		additional.addTags("ForwardCloud", info.get("ForwardCloud"));

		return additional;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
	}

	/**
	 * Generate response protocol
	 * 
	 * @param digest
	 * @param security_tags
	 * @return
	 */
	private Protocol generateProtocol() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ConfirmationForNotification");
		responseInformation.setService("ObjectStorage");
		responseInformation.addTags("ObjectName",
				this.requestInformation.getValue("ObjectName"));

		AdditionalInformation additionalInformation = new AdditionalInformation();

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;
	}

	private ExceptionProtocol generateException(String code, String type,
			String message) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ExceptionInformation exceptionInformation = new ExceptionInformation();
		exceptionInformation.addTags("Command", "Notificationofforward");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}

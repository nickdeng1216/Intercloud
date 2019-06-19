package hk.edu.polyu.intercloud.command.vm;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloud;
import hk.edu.polyu.intercloud.connector.hyperv.HyperVIntercloudException;
import hk.edu.polyu.intercloud.connector.hyperv.VHD;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.VirtualMachineException;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.vmware.vim25.mo.samples.CreateVM_api;

/**
 * @author Kate
 *
 */
@Deprecated
public class CreateVM implements Command {
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
		return protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		try {
			initialization();
			String owner_vmname = this.protocol.getRequestInformation()
					.getValue("VMName");
			String vmname = Long.toString(System.currentTimeMillis());
			// in GB
			String memorysize = this.protocol.getRequestInformation().getValue(
					"MemorySize");
			// in GB
			String disksize = this.protocol.getRequestInformation().getValue(
					"DiskSize");
			String cpucore = this.protocol.getRequestInformation().getValue(
					"CpuCoreNum");
			String cloudType = Common.my_service_providers.get("VM");
			String value = null;
			if (cloudType.equalsIgnoreCase("vmware")) {
				CreateVM_api vmware = new CreateVM_api();
				value = vmware.CreateVM_execute(vmname, memorysize, disksize,
						Integer.parseInt(cpucore));
			} else if (cloudType.equalsIgnoreCase("hyper-v")) {
				final String HYPERV_PROP_FILE = System.getProperty("user.dir")
						+ "/" + "hyperv.properties";
				Properties properties = new Properties();
				InputStream inputStream;
				inputStream = new FileInputStream(HYPERV_PROP_FILE);
				properties.load(inputStream);
				int numberOfNetworkAdapters = 1;
				List<VHD> newVHDs = new ArrayList<VHD>();
				String vhdpath = vmname + ".vhdx";
				String vhdsize = disksize;
				newVHDs.add(new VHD(vhdpath, Integer.valueOf(vhdsize)));
				HyperVIntercloud hyper = new HyperVIntercloud();
				value = "200";
				hyper.newVM(vmname, Integer.parseInt(cpucore),
						Integer.parseInt(memorysize), null, newVHDs, null,
						numberOfNetworkAdapters, false);
			}
			return this.generateProtocol(vmname, value);
		} catch (HyperVIntercloudException e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		} catch (IOException | NumberFormatException e) {
			LogUtil.logException(e);
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		} catch (Exception e) {
			LogUtil.logException(e);
			return this.generateException("199",
					VirtualMachineException.class.getSimpleName(),
					e.getMessage());
		}

	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {

		return null;
	}

	@Override
	public void initialization() {
		this.generalInformation = protocol.getGeneralInformation();
		this.requestInformation = protocol.getRequestInformation();
		this.additionalInformation = protocol.getAdditionalInformation();
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
		exceptionInformation.addTags("Command", "CreateVM");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

	private Protocol generateProtocol(String vmname, String value) {
		if (value.equals("200")) {
			value = "OK";
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");
		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("StatusConfirmation");
		responseInformation.setService("VM");
		responseInformation.addTags("VMName", vmname);
		responseInformation.addTags("Status", value);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;
	}

}

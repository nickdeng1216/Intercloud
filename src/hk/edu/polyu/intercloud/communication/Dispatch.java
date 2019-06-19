package hk.edu.polyu.intercloud.communication;

import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class Dispatch implements Runnable {
	private Command command;
	private Protocol protocol;
	private boolean pSecurity = false;

	public Dispatch(String msg) throws InterruptedException {
		try {
			this.command = ProtocolUtil.parseProtocol(msg);
			pSecurity = command.getProtocol().getAdditionalInformation()
					.getTags().containsKey("Signature");
			// Insert req_track
			if (this.command.getProtocol().getRequestInformation() != null
					&& this.command.getProtocol().getResponseInformation() == null) {
				DatabaseUtil.insertReqTrack(this.command.getProtocol()
						.getId(), this.command.getProtocol()
						.getGeneralInformation().getFrom(), msg);
			} else {
				DatabaseUtil.insertResultTrack(this.command.getProtocol()
						.getId(), this.command.getProtocol()
						.getGeneralInformation().getFrom(), msg);
				// Update flag
				if (Common.flag.containsKey(command.getProtocol().getId())) {
					String[] values = Common.flag.get(command.getProtocol()
							.getId());
					values[0] = "1";
					Common.flag.replace(command.getProtocol().getId(), values);
				}
			}

		} catch (Exception e) {
			LogUtil.logException(e);
		}
	}

	public void execute() throws Exception {
		// Verify Protocol
		boolean pSecur = ProtocolUtil.verifyProtocol(command.getProtocol());
		if (pSecur == false) {
			LogUtil.logError("Dispatch.java: Received Protocol Error");
			return;
		}

		ArrayList<Object> objects = new ArrayList<>();
		HashMap<String, String> storageInfo = new HashMap<>();
		objects.add(storageInfo);
		protocol = command.execute(objects);

		if (protocol instanceof ExceptionProtocol) {
			String exceptionProtocol = ProtocolUtil
					.generateException((ExceptionProtocol) protocol);
			String fromName = command.getProtocol().getGeneralInformation()
					.getTo();
			String toName = command.getProtocol().getGeneralInformation()
					.getFrom();
			int port = Common.GW_PORT;
			Sockets socket = new Sockets(toName, port, fromName);
			socket.sendMessage(exceptionProtocol);
			// Update flag
			if (Common.flag.containsKey(command.getProtocol().getId())) {
				String[] values = Common.flag
						.get(command.getProtocol().getId());
				values[0] = "2";
				Common.flag.replace(command.getProtocol().getId(), values);
			}
		} else if (protocol.getRequestInformation() == null
				&& protocol.getResponseInformation() == null) {
			// Update flag
			if (Common.flag.containsKey(command.getProtocol().getId())) {
				String[] values = Common.flag
						.get(command.getProtocol().getId());
				values[0] = "2";
				Common.flag.replace(command.getProtocol().getId(), values);
			}
		} else if (protocol.getRequestInformation() == null
				&& protocol.getResponseInformation() != null) {
			String resProtocol = ProtocolUtil.generateResponse(protocol,
					pSecurity);
			String fromName = command.getProtocol().getGeneralInformation()
					.getTo();
			String toName = command.getProtocol().getGeneralInformation()
					.getFrom();
			int port = Common.GW_PORT;
			Sockets socket = new Sockets(toName, port, fromName);
			socket.sendMessage(resProtocol);
		}

	}

	@Override
	public void run() {
		try {
			execute();
		} catch (Exception e) {
			LogUtil.logException(e);
		}
	}
}

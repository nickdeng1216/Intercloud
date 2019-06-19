package hk.edu.polyu.intercloud.model.protocol;

import org.json.JSONObject;

public class Protocol {

	private String protocolVersion;
	private String id;
	private GeneralInformation generalInformation;
	private RequestInformation requestInformation;
	private ResponseInformation responseInformation;
	private AdditionalInformation additionalInformation;
	private String body;

	public Protocol() {
		// Do nothing.
	}

	public Protocol(String protocolVersion, String id,
			GeneralInformation generalInformation,
			RequestInformation requestInformation,
			ResponseInformation responseInformation,
			AdditionalInformation additionalInformation, String body) {
		this.protocolVersion = protocolVersion;
		this.id = id;
		this.generalInformation = generalInformation;
		this.requestInformation = requestInformation;
		this.responseInformation = responseInformation;
		this.additionalInformation = additionalInformation;
		this.body = body;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public GeneralInformation getGeneralInformation() {
		return generalInformation;
	}

	public void setGeneralInformation(GeneralInformation generalInformation) {
		this.generalInformation = generalInformation;
	}

	public RequestInformation getRequestInformation() {
		return requestInformation;
	}

	public void setRequestInformation(RequestInformation requestInformation) {
		this.requestInformation = requestInformation;
	}

	public ResponseInformation getResponseInformation() {
		return responseInformation;
	}

	public void setResponseInformation(ResponseInformation responseInformation) {
		this.responseInformation = responseInformation;
	}

	public AdditionalInformation getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(
			AdditionalInformation additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public JSONObject toJSON() {

		JSONObject j = new JSONObject();
		j.append("ProtocolID", id);
		j.append("ProtocolVersion", protocolVersion);

		// General Information
		GeneralInformation generalInfo = getGeneralInformation();
		JSONObject g = new JSONObject();
		g.append("From", generalInfo.getFrom());
		g.append("To", generalInfo.getTo());
		g.append("Date", generalInfo.getDate());
		g.append("Time", generalInfo.getTime());
		j.append("GeneralInformation", g);

		// Request and Response Information
		RequestInformation requestInfo = getRequestInformation();
		ResponseInformation responseInfo = getResponseInformation();
		if (requestInfo != null && responseInfo == null) {
			JSONObject r = new JSONObject();
			r.append("Service", requestInfo.getService());
			r.append("Command", requestInfo.getCommand());
			JSONObject i = new JSONObject();
			for (String key : requestInfo.getKeys()) {
				i.append(key, requestInfo.getValue(key));
			}
			r.append("Parameters", i);
			j.append("RequestInformation", r);
		} else if (requestInfo == null && responseInfo != null) {
			JSONObject r = new JSONObject();
			r.append("Service", responseInfo.getService());
			r.append("Command", responseInfo.getCommand());
			JSONObject i = new JSONObject();
			for (String key : responseInfo.getKeys()) {
				i.append(key, responseInfo.getValue(key));
			}
			r.append("Parameters", i);
			j.append("ResponseInformation", r);
		}

		// Additional Information
		AdditionalInformation additionalInfo = getAdditionalInformation();
		if (additionalInformation != null) {
			JSONObject a = new JSONObject();
			JSONObject i = new JSONObject();
			for (String key : additionalInfo.getKeys()) {
				if (key.equalsIgnoreCase("Signature")
						|| key.equals("SignatureAlgorithm")) {
					continue;
				}
				i.append(key, additionalInfo.getValue(key));
			}
			a.append("Parameters", i);
			j.append("AdditionalInformation", a);
		}

		return j;
	}
}

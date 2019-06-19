package hk.edu.polyu.intercloud.model.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestInformation {

	public RequestInformation() {
		this.tags = new HashMap<String, String>();
	}

	// Compulsory attributes
	private String command;
	private String service;

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	// Parameter tags
	private Map<String, String> tags;

	public Map<String, String> getTags() {
		return tags;
	}

	public Set<String> getKeys() {
		return this.tags.keySet();
	}

	public String getValue(String key) {
		return this.tags.get(key);
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public void addTags(String key, String value) {
		this.tags.put(key, value);
	}
}

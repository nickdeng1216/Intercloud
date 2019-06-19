package hk.edu.polyu.intercloud.model.protocol;

import java.util.LinkedHashMap;

public class ExceptionInformation {

	private LinkedHashMap<String, String> tags;
	private String command;

	public ExceptionInformation() {
		this.tags = new LinkedHashMap<String, String>();
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setTags(LinkedHashMap<String, String> tags) {
		this.tags = tags;
	}

	public LinkedHashMap<String, String> getTags() {
		return tags;
	}

	public void addTags(String key, String value) {
		this.tags.put(key, value);
	}

	public String getValue(String key) {
		return this.tags.get(key);
	}

	public static void main(String[] args) {
		// TODO For Test
	}

}

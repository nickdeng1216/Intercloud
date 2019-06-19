package hk.edu.polyu.intercloud.model.protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneralInformation {

	// Compulsory attributes
	private String from;
	private String to;
	private String date;
	private String time;

	// Parameter tags
	private Map<String, String> tags;

	public GeneralInformation() {
		this.tags = new HashMap<String, String>();
	}

	public GeneralInformation(String from, String to, String date, String time) {
		super();
		this.tags = new HashMap<String, String>();
		this.from = from;
		this.to = to;
		this.date = date;
		this.time = time;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public List<String> getKeys() {
		String s[] = (String[]) this.tags.keySet().toArray();
		return Arrays.asList(s);
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

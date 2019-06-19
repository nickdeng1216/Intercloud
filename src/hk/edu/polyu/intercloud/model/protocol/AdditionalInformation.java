package hk.edu.polyu.intercloud.model.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AdditionalInformation {
	// Parameter tags
	private Map<String, String> tags;

	public AdditionalInformation() {
		this.tags = new HashMap<String, String>();
	}

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

	public void removeTags(String key) {
		this.tags.remove(key);
	}
}

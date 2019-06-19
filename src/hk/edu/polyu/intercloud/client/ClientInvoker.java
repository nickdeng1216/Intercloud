package hk.edu.polyu.intercloud.client;

import hk.edu.polyu.intercloud.exceptions.ClientInvokerException;
import hk.edu.polyu.intercloud.model.client.ClientMethod;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class uses JSON messages to call API methods by Java reflection
 * 
 * @author Priere
 *
 */
public class ClientInvoker implements Runnable {
	private JSONObject json;

	public ClientInvoker(JSONObject json) {
		this.json = json;
	}

	@Override
	public void run() {
		parseRequest();
	}

	private void parseRequest() {
		try {
			ClientMethod cm = parseJSON(json);
			String apiClass = cm.getCls();
			long rid = cm.getId(); // Request ID
			String target = cm.getTarget();

			Class<?> api = Class.forName(apiClass);
			Object instance = null;
			if (target.equals("")) {
				instance = api.newInstance();
			} else {
				Constructor<?> c = api.getConstructor(String.class);
				instance = c.newInstance(target);
			}
			Method method = api.getMethod(cm.getMethod(), cm.getParamClasses());
			LogUtil.logPerformance("API Invoke", target,
					System.currentTimeMillis(), 0L);
			Object o = method.invoke(instance, cm.getParams());
			if (o != null) {
				try {
					long pid = Long.valueOf(o.toString()); // Protocol ID
					DatabaseUtil.insertId(pid, rid);
				} catch (Exception e) {
					LogUtil.logException(e);
				}
			}
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| InstantiationException e) {
			LogUtil.logException(e);
		} catch (ClientInvokerException e) {
			LogUtil.logException(e);
		} catch (Exception e) {
			LogUtil.logException(e);
		}
	}

	private ClientMethod parseJSON(JSONObject j) throws ClientInvokerException {
		try {
			if (!j.has("API") || !j.has("Method") || !j.has("Parameters")) {
				System.out.println("Invalid message: Component missing.");
				return null;
			}
			ClientMethod cm = new ClientMethod();
			// Get the class and method
			String cls = j.getString("API");
			if (!cls.startsWith("hk.edu.polyu.intercloud.api.")) {
				cls = "hk.edu.polyu.intercloud.api." + cls;
			}
			cm.setCls(cls);
			long id = j.getLong("ID");
			cm.setId(id);
			String target = "";
			try {
				target = j.getString("Target");
			} catch (JSONException e) {
			}
			cm.setTarget(target);
			cm.setMethod(j.getString("Method"));
			// Get the method params and param classes
			JSONArray a = j.getJSONArray("Parameters");
			@SuppressWarnings("rawtypes")
			List<Class> paramClasses = new ArrayList<Class>();
			List<Object> params = new ArrayList<Object>();
			for (int i = 0; i < a.length(); i++) {
				Object o = a.get(i);
				if (o instanceof JSONObject) {
					// Turn it into a Map
					paramClasses.add(HashMap.class);
					// Get all keys
					Set<String> keys = ((JSONObject) o).keySet();
					// Put into a Map
					Map<String, Object> m = new HashMap<String, Object>();
					for (String key : keys) {
						Object oo = ((JSONObject) o).get(key);
						// If such JSONObject contains a JSONArray,
						// turn it to ArrayList then put it to the Map;
						// else, put the Object directly into the Map
						if (oo instanceof JSONArray) {
							List<Object> l = new ArrayList<Object>();
							for (int k = 0; k < ((JSONArray) oo).length(); k++) {
								l.add(((JSONArray) oo).get(k));
							}
							m.put(key, l);
						} else {
							m.put(key, oo);
						}
					}
					params.add(m);
				} else if (o instanceof JSONArray) {
					// Turn it into a List
					paramClasses.add(ArrayList.class);
					List<Object> l = new ArrayList<Object>();
					for (int k = 0; k < ((JSONArray) o).length(); k++) {
						l.add(((JSONArray) o).get(k));
					}
					params.add(l);
				} else if (o instanceof Float || o instanceof Double) {
					paramClasses.add(double.class);
					params.add(Double.parseDouble(String.valueOf(o)));
				} else if (o instanceof Integer || o instanceof Long) {
					// Convert to double
					paramClasses.add(double.class);
					params.add(Double.parseDouble(String.valueOf(o)));
				} else if (o instanceof Boolean) {
					paramClasses.add(boolean.class);
					params.add((boolean) o);
				} else if (o instanceof String) {
					paramClasses.add(String.class);
					params.add(o);
				} else if (o.equals(JSONObject.NULL)) {
					paramClasses.add(null);
					params.add(null);
				} else {
					// What!?
					paramClasses.add(Object.class);
					params.add(o);
				}
			}
			cm.setParamClasses(paramClasses.toArray(new Class[paramClasses
					.size()]));
			cm.setParams(params.toArray());
			return cm;
		} catch (Exception e) {
			throw new ClientInvokerException(e.getMessage(), e);
		}
	}
}

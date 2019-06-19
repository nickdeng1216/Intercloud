package hk.edu.polyu.intercloud.model.client;

public class ClientMethod {

	private String cls;
	private long id;
	private String target;
	private String method;
	private Object[] params;

	@SuppressWarnings("rawtypes")
	private Class[] paramClasses;

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	@SuppressWarnings("rawtypes")
	public Class[] getParamClasses() {
		return paramClasses;
	}

	@SuppressWarnings("rawtypes")
	public void setParamClasses(Class[] paramClasses) {
		this.paramClasses = paramClasses;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
}

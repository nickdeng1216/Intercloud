package hk.edu.polyu.intercloud.model.protocol;

/**
 * 
 * @author harry
 *
 */
public class ExceptionProtocol extends Protocol {

	private String version;
	private String id;
	private GeneralInformation generalInformation;
	private ExceptionInformation exceptionInformation;
	private AdditionalInformation additionalInformation;

	public ExceptionProtocol() {

	}

	public ExceptionProtocol(String version, String id, GeneralInformation generalInformation,
			ExceptionInformation exceptionInformation, AdditionalInformation additionalInformation) {
		this.version = version;
		this.id = id;
		this.generalInformation = generalInformation;
		this.exceptionInformation = exceptionInformation;
		this.additionalInformation = additionalInformation;
	}

	@Override
	public String getProtocolVersion() {
		return version;
	}

	@Override
	public void setProtocolVersion(String version) {
		this.version = version;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public GeneralInformation getGeneralInformation() {
		return generalInformation;
	}

	@Override
	public void setGeneralInformation(GeneralInformation generalInformation) {
		this.generalInformation = generalInformation;
	}

	@Override
	public AdditionalInformation getAdditionalInformation() {
		return additionalInformation;
	}

	@Override
	public void setAdditionalInformation(AdditionalInformation additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	public ExceptionInformation getExceptionInformation() {
		return exceptionInformation;
	}

	public void setExceptionInformation(ExceptionInformation exceptionInformation) {
		this.exceptionInformation = exceptionInformation;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

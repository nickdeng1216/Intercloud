package hk.edu.polyu.intercloud.controller;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

/**
 * The class is used for controlling task to run at certain time.
 * 
 * @author Kate
 *
 */
public class Scheduletask extends TimerTask {

	private String jobName = "";
	private Date enddate;
	private Map<String, Object> files;

	private String tocloud;
	private boolean securitystatus;
	private String transferProtocol;
	private String data_security_level;
	private boolean overwrite;

	public Scheduletask(String jobName, Date enddate,
			Map<String, Object> files, String transferProtocol,
			Boolean securitystatus, String tocloud, String data_security_level,
			boolean overwrite) {
		super();

		this.jobName = jobName;
		this.enddate = enddate;
		this.files = files;
		this.transferProtocol = transferProtocol;
		this.tocloud = tocloud;
		this.securitystatus = securitystatus;
		this.data_security_level = data_security_level;
		this.overwrite = overwrite;

	}

	@Override
	public void run() {
		System.out.print(enddate);
		Date todayDate = new Date();
		if (todayDate.before(enddate))
			try {
				Taskdetails a = new Taskdetails(files, transferProtocol,
						securitystatus, tocloud, data_security_level, overwrite);

				a.start();
				a.join();

			} catch (Exception e) {
				System.out.print("Exception in schdeled backup");
			}

		else {
			System.out.println("stop");
			Thread.currentThread().destroy();

		}
	}
}

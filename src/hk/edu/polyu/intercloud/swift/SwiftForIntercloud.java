package hk.edu.polyu.intercloud.swift;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.payloads.FilePayload;
import org.openstack4j.openstack.OSFactory;

/**
 * 
 * @author Kate
 *
 */
public class SwiftForIntercloud {

	public void download(String bucket, String object, String download)
			throws Exception {

		GetAccountInfo info = new GetAccountInfo();
		String url = info.getValue("Url");
		String username = info.getValue("Username");
		String pwd = info.getValue("Password");
		String con = info.getValue("Container");

		System.out.print(url);

		Identifier domainIdentifier = Identifier.byId("default");
		OSClientV3 os = OSFactory.builderV3().endpoint(url)
				.credentials(username, pwd, domainIdentifier)
				.scopeToProject(Identifier.byName("admin"), domainIdentifier)
				.authenticate();

		os.objectStorage().objects().download(bucket, object)
				.writeToFile(new File(download));
	}

	public void upload(String bucket, String upload) throws Exception {

		GetAccountInfo info = new GetAccountInfo();
		String url = info.getValue("Url");
		String username = info.getValue("Username");
		String pwd = info.getValue("Password");
		String con = info.getValue("Container");

		System.out.print(url);

		Identifier domainIdentifier = Identifier.byId("default");
		OSClientV3 os = OSFactory.builderV3().endpoint(url)
				.credentials(username, pwd, domainIdentifier)
				.scopeToProject(Identifier.byName("admin"), domainIdentifier)
				.authenticate();

		String name = FilenameUtils.getBaseName(upload);
		String ext = FilenameUtils.getExtension(upload);
		String objectname = name + "." + ext;

		os.objectStorage().objects()
				.put(bucket, objectname, new FilePayload(new File(upload)));
	}

	public void delete(String bucket, String object) throws Exception {

		GetAccountInfo info = new GetAccountInfo();
		String url = info.getValue("Url");
		String username = info.getValue("Username");
		String pwd = info.getValue("Password");
		String con = info.getValue("Container");

		System.out.print(url);

		Identifier domainIdentifier = Identifier.byId("default");
		OSClientV3 os = OSFactory.builderV3().endpoint(url)
				.credentials(username, pwd, domainIdentifier)
				.scopeToProject(Identifier.byName("admin"), domainIdentifier)
				.authenticate();

		os.objectStorage().objects().delete(bucket, object);
	}

	public boolean checkObject(String bucket, String object) throws Exception {

		GetAccountInfo info = new GetAccountInfo();
		String url = info.getValue("Url");
		String username = info.getValue("Username");
		String pwd = info.getValue("Password");
		String con = info.getValue("Container");

		System.out.print(url);

		Identifier domainIdentifier = Identifier.byId("default");
		OSClientV3 os = OSFactory.builderV3().endpoint(url)
				.credentials(username, pwd, domainIdentifier)
				.scopeToProject(Identifier.byName("admin"), domainIdentifier)
				.authenticate();

		Map<String, String> map = os.objectStorage().objects()
				.getMetadata(bucket, object);
		if (map != null && !map.isEmpty()) {
			return true;
		}
		return false;
	}
}

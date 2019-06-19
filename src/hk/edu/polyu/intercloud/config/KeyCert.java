package hk.edu.polyu.intercloud.config;

import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.SecurityException;
import hk.edu.polyu.intercloud.security.CSR;
import hk.edu.polyu.intercloud.util.KeyUtil;
import hk.edu.polyu.intercloud.util.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

public class KeyCert {

	public static void main(String args[]) throws IOException,
			SecurityException, ZipException {
		genKeyPair();
		csr();
	}

	static void genKeyPair() throws IOException, SecurityException {
		Path privatePem = Paths.get(Common.KEY_PATH + "private.pem");
		Path publicPem = Paths.get(Common.KEY_PATH + "public.pem");
		if ((Files.exists(privatePem) && Files.isRegularFile(privatePem))
				|| (Files.exists(publicPem) && Files.isRegularFile(publicPem))) {
			int i = JOptionPane.showConfirmDialog(null,
					"Pem files already exist. Overwrite?");
			if (i != JOptionPane.YES_OPTION) {
				return;
			}
		}
		KeyUtil.generateKeyPair();
	}

	static void csr() throws SecurityException, IOException, ZipException {
		String cloudName = JOptionPane.showInputDialog("Name:");
		String csr = CSR.generatePKCS10("CN=" + cloudName
				+ ", L=PolyU, C=HKSAR", Common.KEY_PATH + "private.pem",
				Common.KEY_PATH + "public.pem");
		KeyUtil.writefile(Common.KEY_PATH + cloudName + ".csr", csr);
		File file = new File(cloudName + "_csr.zip");
		if (file.exists() && file.isFile()) {
			FileUtils.forceDelete(file);
		}
		ZipUtil.create(cloudName + "_csr.zip", Common.KEY_PATH + cloudName
				+ ".csr", ZipUtil.HIGH_COMPRESSION, cloudName);
		JOptionPane.showMessageDialog(null,
				"CSR generated. Please send the file '" + cloudName
						+ "_csr.zip" + "' to the administrator's e-mail.");
	}

}

package hk.edu.polyu.intercloud.connector.hyperv;

import java.io.File;
import java.io.FilenameFilter;

class CommonUtil {

	public static File[] filterFilesByExt(String directory,
			final String extension) {
		return new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(extension);
			}
		});
	}
}

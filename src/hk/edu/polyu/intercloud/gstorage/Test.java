package hk.edu.polyu.intercloud.gstorage;

import javax.swing.JOptionPane;

public class Test {

	private static final String FROM_LOCAL = System.getProperty("user.dir")
			+ "/" + "test_ul.txt";
	private static final String TO_LOCAL_DIR = System.getProperty("user.dir")
			+ "/dl";

	// private static final String FROM_LOCAL =
	// "C:/share/workspace/Intercloud/download/Image1.jpg";
	// private static final String TO_LOCAL_DIR =
	// "C:/share/workspace/Intercloud/retrieve";

	public static void main(String[] args) throws Exception {
		// (new File(TO_LOCAL_DIR)).mkdirs();

		GoogleStorageIntercloud g = new GoogleStorageIntercloud();
		System.out.println("Available storage: " + g.getAvailableStorage());
		System.out.println("List of buckets: " + g.listBuckets());

		JOptionPane.showMessageDialog(null, "Create buckets 1, 2");
		g.createBucket("intercloud-bucket-20160718", "NEARLINE");
		// g.createBucket("intercloud-bucket-2", "NEARLINE");
		System.out.println("List of buckets: " + g.listBuckets());

		JOptionPane.showMessageDialog(null, "Upload " + FROM_LOCAL
				+ " to buckets.");
		g.uploadFile("intercloud-bucket-20160718", FROM_LOCAL);
		// g.uploadFile("intercloud-bucket-2", FROM_LOCAL);
		// System.out.println("Items in bucket 1: " +
		g.listBucket("intercloud-bucket-20160718");
		// System.out.println("Items in bucket 2: " +
		// g.listBucket("intercloud-bucket-2"));

		JOptionPane.showMessageDialog(null, "Download to " + TO_LOCAL_DIR
				+ " from bucket 1");
		g.downloadFile("intercloud-bucket-20160718", "test_ul.txt",
				TO_LOCAL_DIR);

		// JOptionPane.showMessageDialog(null, "Delete from bucket 1");
		// g.deleteFile("intercloud-bucket-1", "test_ul.txt");
		// System.out.println("Items in bucket 1: " +
		// g.listBucket("intercloud-bucket-1"));
		//
		// JOptionPane.showMessageDialog(null, "Delete buckets 2");
		// g.deleteBucket("intercloud-bucket-2", true);
		// g.deleteBucket("intercloud-bucket-3", true);
		System.out.println("List of buckets: " + g.listBuckets());
	}
}

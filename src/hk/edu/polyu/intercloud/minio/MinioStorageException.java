package hk.edu.polyu.intercloud.minio;

public class MinioStorageException extends Exception {

	private static final long serialVersionUID = 5847708382271151927L;

	public MinioStorageException(String msg) {
		super(msg);
	}

	public MinioStorageException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}

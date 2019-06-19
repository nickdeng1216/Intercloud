package hk.edu.polyu.intercloud.minio.main;

import hk.edu.polyu.intercloud.minio.errors.ErrorResponseException;
import hk.edu.polyu.intercloud.minio.errors.InputSizeMismatchException;
import hk.edu.polyu.intercloud.minio.errors.InsufficientDataException;
import hk.edu.polyu.intercloud.minio.errors.InternalException;
import hk.edu.polyu.intercloud.minio.errors.InvalidAclNameException;
import hk.edu.polyu.intercloud.minio.errors.InvalidArgumentException;
import hk.edu.polyu.intercloud.minio.errors.InvalidBucketNameException;
import hk.edu.polyu.intercloud.minio.errors.InvalidEndpointException;
import hk.edu.polyu.intercloud.minio.errors.InvalidExpiresRangeException;
import hk.edu.polyu.intercloud.minio.errors.InvalidPortException;
import hk.edu.polyu.intercloud.minio.errors.NoResponseException;
import hk.edu.polyu.intercloud.minio.http.HeaderParser;
import hk.edu.polyu.intercloud.minio.http.Method;
import hk.edu.polyu.intercloud.minio.http.Scheme;
import hk.edu.polyu.intercloud.minio.messages.AccessControlPolicy;
import hk.edu.polyu.intercloud.minio.messages.Bucket;
import hk.edu.polyu.intercloud.minio.messages.CompleteMultipartUpload;
import hk.edu.polyu.intercloud.minio.messages.CreateBucketConfiguration;
import hk.edu.polyu.intercloud.minio.messages.ErrorResponse;
import hk.edu.polyu.intercloud.minio.messages.Grant;
import hk.edu.polyu.intercloud.minio.messages.InitiateMultipartUploadResult;
import hk.edu.polyu.intercloud.minio.messages.Item;
import hk.edu.polyu.intercloud.minio.messages.ListAllMyBucketsResult;
import hk.edu.polyu.intercloud.minio.messages.ListBucketResult;
import hk.edu.polyu.intercloud.minio.messages.ListMultipartUploadsResult;
import hk.edu.polyu.intercloud.minio.messages.ListPartsResult;
import hk.edu.polyu.intercloud.minio.messages.Part;
import hk.edu.polyu.intercloud.minio.messages.Prefix;
import hk.edu.polyu.intercloud.minio.messages.Upload;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import okio.BufferedSink;
import okio.Okio;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.joda.time.DateTime;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.common.io.ByteStreams;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

@SuppressWarnings({ "SameParameterValue", "WeakerAccess" })
public class MinioClient {
	// maximum allowed object size is 5TiB
	private static final long MAX_OBJECT_SIZE = 5L * 1024 * 1024 * 1024 * 1024;
	private static final int MAX_MULTIPART_COUNT = 10000;
	// minimum allowed multipart size is 5MiB
	private static final int MIN_MULTIPART_SIZE = 5 * 1024 * 1024;
	// default expiration for a presigned URL is 7 days in seconds
	private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;
	private static final String DEFAULT_USER_AGENT = "Minio ("
			+ System.getProperty("os.arch") + "; "
			+ System.getProperty("os.arch") + ") minio-java/"
			+ MinioProperties.INSTANCE.getVersion();

	private static XmlPullParserFactory xmlPullParserFactory = null;

	static {
		try {
			xmlPullParserFactory = XmlPullParserFactory.newInstance();
			xmlPullParserFactory.setNamespaceAware(true);
		} catch (XmlPullParserException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private PrintWriter traceStream;

	// the current client instance's base URL.
	private HttpUrl baseUrl;
	// access key to sign all requests with
	private String accessKey;
	// Secret key to sign all requests with
	private String secretKey;

	private String userAgent = DEFAULT_USER_AGENT;

	private OkHttpClient httpClient = new OkHttpClient();

	public MinioClient(String endpoint) throws InvalidEndpointException,
			InvalidPortException {
		this(endpoint, 0, null, null, false);
	}

	public MinioClient(URL url) throws NullPointerException,
			InvalidEndpointException, InvalidPortException {
		this(url.toString(), 0, null, null, false);
	}

	public MinioClient(HttpUrl url) throws NullPointerException,
			InvalidEndpointException, InvalidPortException {
		this(url.toString(), 0, null, null, false);
	}

	public MinioClient(String endpoint, String accessKey, String secretKey)
			throws InvalidEndpointException, InvalidPortException {
		this(endpoint, 0, accessKey, secretKey, false);
	}

	public MinioClient(URL url, String accessKey, String secretKey)
			throws NullPointerException, InvalidEndpointException,
			InvalidPortException {
		this(url.toString(), 0, accessKey, secretKey, false);
	}

	public MinioClient(HttpUrl url, String accessKey, String secretKey)
			throws NullPointerException, InvalidEndpointException,
			InvalidPortException {
		this(url.toString(), 0, accessKey, secretKey, false);
	}

	public MinioClient(String endpoint, int port, String accessKey,
			String secretKey) throws InvalidEndpointException,
			InvalidPortException {
		this(endpoint, port, accessKey, secretKey, false);
	}

	public MinioClient(String endpoint, String accessKey, String secretKey,
			boolean insecure) throws InvalidEndpointException,
			InvalidPortException {
		this(endpoint, 0, accessKey, secretKey, insecure);
	}

	/**
	 * Creates Minio client object.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	MinioClient s3Client = new MinioClient(&quot;https://s3.amazonaws.com&quot;,
	 * 			&quot;YOUR-ACCESSKEYID&quot;, &quot;YOUR-SECRETACCESSKEY&quot;);
	 * }
	 * </pre>
	 *
	 * @param endpoint
	 *            request endpoint. Endpoint is an URL, domain name, IPv4 or
	 *            IPv6 address.
	 * 
	 *            <pre>
	 *              Valid endpoints:
	 *              * https://s3.amazonaws.com
	 *              * https://s3.amazonaws.com/
	 *              * https://play.minio.io:9000
	 *              * http://play.minio.io:9010/
	 *              * localhost
	 *              * localhost.localdomain
	 *              * play.minio.io
	 *              * 127.0.0.1
	 *              * 192.168.1.60
	 *              * ::1
	 * </pre>
	 *
	 * @param port
	 *            valid port. It should be in between 1 and 65535. Unused if
	 *            endpoint is an URL.
	 * @param accessKey
	 *            access key to access service in endpoint.
	 * @param secretKey
	 *            secret key to access service in endpoint.
	 * @param insecure
	 *            if true, access endpoint using HTTP else access it using
	 *            HTTPS.
	 *
	 * @see #MinioClient(String endpoint)
	 * @see #MinioClient(URL url)
	 * @see #MinioClient(String endpoint, String accessKey, String secretKey)
	 * @see #MinioClient(URL url, String accessKey, String secretKey)
	 * @see #MinioClient(String endpoint, int port, String accessKey, String
	 *      secretKey)
	 * @see #MinioClient(String endpoint, String accessKey, String secretKey,
	 *      boolean insecure)
	 */
	public MinioClient(String endpoint, int port, String accessKey,
			String secretKey, boolean insecure)
			throws InvalidEndpointException, InvalidPortException {
		if (endpoint == null) {
			throw new InvalidEndpointException("(null)", "null endpoint");
		}

		// for valid URL endpoint, port and insecure are ignored
		HttpUrl url = HttpUrl.parse(endpoint);
		if (url != null) {
			if (!"/".equals(url.encodedPath())) {
				throw new InvalidEndpointException(endpoint,
						"no path allowed in endpoint");
			}

			// treat Amazon S3 host as special case
			String amzHost = url.host();
			if (amzHost.endsWith(".amazonaws.com")
					&& !amzHost.equals("s3.amazonaws.com")) {
				throw new InvalidEndpointException(endpoint,
						"for Amazon S3, host should be 's3.amazonaws.com' in endpoint");
			}

			this.baseUrl = url;
			this.accessKey = accessKey;
			this.secretKey = secretKey;

			return;
		}

		// endpoint may be a valid hostname, IPv4 or IPv6 address
		if (!this.isValidEndpoint(endpoint)) {
			throw new InvalidEndpointException(endpoint, "invalid host");
		}

		// treat Amazon S3 host as special case
		if (endpoint.endsWith(".amazonaws.com")
				&& !endpoint.equals("s3.amazonaws.com")) {
			throw new InvalidEndpointException(endpoint,
					"for amazon S3, host should be 's3.amazonaws.com'");
		}

		if (port < 0 || port > 65535) {
			throw new InvalidPortException(port,
					"port must be in range of 1 to 65535");
		}

		Scheme scheme = Scheme.HTTPS;
		if (insecure) {
			scheme = Scheme.HTTP;
		}

		if (port == 0) {
			this.baseUrl = new HttpUrl.Builder().scheme(scheme.toString())
					.host(endpoint).build();
		} else {
			this.baseUrl = new HttpUrl.Builder().scheme(scheme.toString())
					.host(endpoint).port(port).build();
		}
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	private boolean isValidEndpoint(String endpoint) {
		if (InetAddressValidator.getInstance().isValid(endpoint)) {
			return true;
		}

		// endpoint may be a hostname
		// refer
		// https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names
		// why checks are done like below
		if (endpoint.length() < 1 || endpoint.length() > 253) {
			return false;
		}

		for (String label : endpoint.split("\\.")) {
			if (label.length() < 1 || label.length() > 63) {
				return false;
			}

			if (!(label.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*") && endpoint
					.matches(".*[a-zA-Z0-9]$"))) {
				return false;
			}
		}

		return true;
	}

	// Validates if input bucket name is DNS compatible.
	private void checkBucketName(String name) throws InvalidBucketNameException {
		if (name == null) {
			throw new InvalidBucketNameException("(null)", "null bucket name");
		}

		// Bucket names cannot be no less than 3 and no more than 63 characters
		// long.
		if (name.length() < 3 || name.length() > 63) {
			String msg = "bucket name must be at least 3 and no more than 63 characters long";
			throw new InvalidBucketNameException(name, msg);
		}
		// Successive periods in bucket names are not allowed.
		if (name.matches("\\.\\.")) {
			String msg = "bucket name cannot contain successive periods. For more information refer "
					+ "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
			throw new InvalidBucketNameException(name, msg);
		}
		// Bucket names should be dns compatible.
		if (!name.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
			String msg = "bucket name does not follow Amazon S3 standards. For more information refer "
					+ "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
			throw new InvalidBucketNameException(name, msg);
		}
	}

	private Request createRequest(Method method, String bucketName,
			String objectName, String region, Map<String, String> headerMap,
			Map<String, String> queryParamMap, final String contentType,
			final Object body, final int length)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException {
		if (bucketName == null && objectName != null) {
			throw new InvalidBucketNameException("(null)",
					"null bucket name for object '" + objectName + "'");
		}

		HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();

		if (bucketName != null) {
			checkBucketName(bucketName);

			String host = this.baseUrl.host();
			if (host.equals("s3.amazonaws.com")) {
				// special case: handle s3.amazonaws.com separately
				if (region != null) {
					host = AwsS3Endpoints.INSTANCE.endpoint(region);
				}

				boolean usePathStyle = false;
				if (method == Method.PUT && objectName == null
						&& queryParamMap == null) {
					// use path style for make bucket to workaround
					// "AuthorizationHeaderMalformed" error from
					// s3.amazonaws.com
					usePathStyle = true;
				} else if (queryParamMap != null
						&& queryParamMap.containsKey("location")) {
					// use path style for location query
					usePathStyle = true;
				} else if (bucketName.contains(".") && this.baseUrl.isHttps()) {
					// use path style where '.' in bucketName causes SSL
					// certificate validation error
					usePathStyle = true;
				}

				if (usePathStyle) {
					urlBuilder.host(host);
					urlBuilder.addPathSegment(bucketName);
				} else {
					urlBuilder.host(bucketName + "." + host);
				}
			} else {
				urlBuilder.addPathSegment(bucketName);
			}
		}

		if (objectName != null) {
			for (String pathSegment : objectName.split("/")) {
				urlBuilder.addPathSegment(pathSegment);
			}
		}

		if (queryParamMap != null) {
			for (Map.Entry<String, String> entry : queryParamMap.entrySet()) {
				urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
			}
		}

		RequestBody requestBody = null;
		if (body != null) {
			requestBody = new RequestBody() {
				@Override
				public MediaType contentType() {
					if (contentType != null) {
						return MediaType.parse(contentType);
					} else {
						return MediaType.parse("application/octet-stream");
					}
				}

				@Override
				public long contentLength() {
					if (length == 0) {
						return -1;
					} else {
						return length;
					}
				}

				@Override
				public void writeTo(BufferedSink sink) throws IOException {
					byte[] data = null;

					if (body instanceof InputStream) {
						InputStream stream = (InputStream) body;
						sink.write(Okio.source(stream), length);
					} else if (body instanceof RandomAccessFile) {
						RandomAccessFile file = (RandomAccessFile) body;
						sink.write(Okio.source(Channels.newInputStream(file
								.getChannel())), length);
					} else if (body instanceof byte[]) {
						sink.write(data, 0, length);
					} else {
						sink.writeUtf8(body.toString());
					}
				}
			};
		}

		String sha256Hash = null;
		String md5Hash = null;
		if (this.accessKey != null && this.secretKey != null) {
			if (body == null) {
				sha256Hash = Digest.sha256Hash(new byte[0]);
			} else {
				if (body instanceof BufferedInputStream) {
					String[] hashes = Digest.sha256md5Hashes(body, length);
					sha256Hash = hashes[0];
					md5Hash = hashes[1];
				} else if (body instanceof RandomAccessFile) {
					String[] hashes = Digest.sha256md5Hashes(body, length);
					sha256Hash = hashes[0];
					md5Hash = hashes[1];
				} else if (body instanceof byte[]) {
					byte[] data = (byte[]) body;
					sha256Hash = Digest.sha256Hash(data, length);
					md5Hash = Digest.md5Hash(data, length);
				} else {
					sha256Hash = Digest.sha256Hash(body.toString());
				}
			}
		}

		HttpUrl url = urlBuilder.build();
		Request.Builder requestBuilder = new Request.Builder();
		requestBuilder.url(url);
		requestBuilder.method(method.toString(), requestBody);
		if (headerMap != null) {
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				requestBuilder.header(entry.getKey(), entry.getValue());
			}
		}

		if (md5Hash != null) {
			requestBuilder.header("Content-MD5", md5Hash);
		}
		if (url.port() == 80 || url.port() == 443) {
			requestBuilder.header("Host", url.host());
		} else {
			requestBuilder.header("Host", url.host() + ":" + url.port());
		}
		requestBuilder.header("User-Agent", this.userAgent);
		if (sha256Hash != null) {
			requestBuilder.header("x-amz-content-sha256", sha256Hash);
		}
		DateTime date = new DateTime();
		requestBuilder.header("x-amz-date",
				date.toString(DateFormat.AMZ_DATE_FORMAT));

		return requestBuilder.build();
	}

	private HttpResponse execute(Method method, String region,
			String bucketName, String objectName,
			Map<String, String> headerMap, Map<String, String> queryParamMap,
			String contentType, Object body, int length)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Request request = createRequest(method, bucketName, objectName, region,
				headerMap, queryParamMap, contentType, body, length);

		if (this.accessKey != null && this.secretKey != null) {
			request = Signer.signV4(request, region, accessKey, secretKey);
		}

		if (this.traceStream != null) {
			this.traceStream.println("---------START-HTTP---------");
			String encodedPath = request.httpUrl().encodedPath();
			String encodedQuery = request.httpUrl().encodedQuery();
			if (encodedQuery != null) {
				encodedPath += "?" + encodedQuery;
			}
			this.traceStream.println(request.method() + " " + encodedPath
					+ " HTTP/1.1");
			String headers = request
					.headers()
					.toString()
					.replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*");
			this.traceStream.println(headers);
		}

		Response response = this.httpClient.newCall(request).execute();
		if (response == null) {
			if (this.traceStream != null) {
				this.traceStream.println("<NO RESPONSE>");
				this.traceStream.println("----------END-HTTP----------");
			}
			throw new NoResponseException();
		}

		if (this.traceStream != null) {
			this.traceStream.println(response.protocol().toString()
					.toUpperCase()
					+ " " + response.code());
			this.traceStream.println(response.headers());
		}

		ResponseHeader header = new ResponseHeader();
		HeaderParser.set(response.headers(), header);

		if (response.isSuccessful()) {
			if (this.traceStream != null) {
				this.traceStream.println("----------END-HTTP----------");
			}
			return new HttpResponse(header, response.body());
		}

		String errorXml = "";
		ErrorResponse errorResponse = null;
		boolean emptyBody = false;

		try {
			// read enitre body stream to string.
			Scanner scanner = new java.util.Scanner(response.body()
					.charStream()).useDelimiter("\\A");
			if (scanner.hasNext()) {
				errorXml = scanner.next();
			}

			errorResponse = new ErrorResponse(new StringReader(errorXml));
		} catch (EOFException e) {
			emptyBody = true;
		} finally {
			response.body().close();
		}

		if (this.traceStream != null) {
			this.traceStream.println(errorXml);
			this.traceStream.println("----------END-HTTP----------");
		}

		if (emptyBody || errorResponse == null) {
			ErrorCode ec;
			switch (response.code()) {
			case 404:
				if (objectName != null) {
					ec = ErrorCode.NO_SUCH_KEY;
				} else if (bucketName != null) {
					ec = ErrorCode.NO_SUCH_BUCKET;
				} else {
					ec = ErrorCode.RESOURCE_NOT_FOUND;
				}
				break;
			case 501:
			case 405:
				ec = ErrorCode.METHOD_NOT_ALLOWED;
				break;
			case 409:
				if (bucketName != null) {
					ec = ErrorCode.NO_SUCH_BUCKET;
				} else {
					ec = ErrorCode.RESOURCE_CONFLICT;
				}
				break;
			case 403:
				ec = ErrorCode.ACCESS_DENIED;
				break;
			default:
				throw new InternalException("unhandled HTTP code "
						+ response.code() + ".  Please report this issue at "
						+ "https://github.com/minio/minio-java/issues");
			}

			errorResponse = new ErrorResponse(ec, bucketName, objectName,
					request.httpUrl().encodedPath(), header.xamzRequestId(),
					header.xamzId2());
		}

		// invalidate region cache if needed
		if (errorResponse.errorCode() == ErrorCode.NO_SUCH_BUCKET) {
			BucketRegionCache.INSTANCE.remove(bucketName);
			// TODO: handle for other cases as well
			// observation: on HEAD of a bucket with wrong region gives 400
			// without body
		}

		throw new ErrorResponseException(errorResponse, response);
	}

	private void updateRegionCache(String bucketName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		if (bucketName != null
				&& "s3.amazonaws.com".equals(this.baseUrl.host())
				&& this.accessKey != null && this.secretKey != null
				&& BucketRegionCache.INSTANCE.exists(bucketName) == false) {
			Map<String, String> queryParamMap = new HashMap<>();
			queryParamMap.put("location", null);

			HttpResponse response = execute(Method.GET, "us-east-1",
					bucketName, null, null, queryParamMap, null, null, 0);

			// existing XmlEntity does not work, so fallback to regular parsing.
			XmlPullParser xpp = xmlPullParserFactory.newPullParser();
			String location = null;

			xpp.setInput(response.body().charStream());
			while (xpp.getEventType() != xpp.END_DOCUMENT) {
				if (xpp.getEventType() == xpp.START_TAG
						&& xpp.getName() == "LocationConstraint") {
					xpp.next();
					location = getText(xpp, location);
					break;
				}
				xpp.next();
			}

			// close response body.
			response.body().close();

			String region;
			if (location == null) {
				region = "us-east-1";
			} else {
				// eu-west-1 can be sometimes 'EU'.
				if ("EU".equals(location)) {
					region = "eu-west-1";
				} else {
					region = location;
				}
			}

			// Add the new location.
			BucketRegionCache.INSTANCE.add(bucketName, region);
		}
	}

	private String getText(XmlPullParser xpp, String location)
			throws XmlPullParserException {
		if (xpp.getEventType() == xpp.TEXT) {
			location = xpp.getText();
		}
		return location;
	}

	private HttpResponse executeGet(String bucketName, String objectName,
			Map<String, String> headerMap, Map<String, String> queryParamMap)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(bucketName);
		return execute(Method.GET,
				BucketRegionCache.INSTANCE.region(bucketName), bucketName,
				objectName, headerMap, queryParamMap, null, null, 0);
	}

	private HttpResponse executeHead(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(bucketName);
		HttpResponse response = execute(Method.HEAD,
				BucketRegionCache.INSTANCE.region(bucketName), bucketName,
				objectName, null, null, null, null, 0);
		response.body().close();
		return response;
	}

	private HttpResponse executeDelete(String bucketName, String objectName,
			Map<String, String> queryParamMap)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(bucketName);
		HttpResponse response = execute(Method.DELETE,
				BucketRegionCache.INSTANCE.region(bucketName), bucketName,
				objectName, null, queryParamMap, null, null, 0);
		response.body().close();
		return response;
	}

	private HttpResponse executePost(String bucketName, String objectName,
			Map<String, String> headerMap, Map<String, String> queryParamMap,
			Object data) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(bucketName);
		return execute(Method.POST,
				BucketRegionCache.INSTANCE.region(bucketName), bucketName,
				objectName, headerMap, queryParamMap, null, data, 0);
	}

	private HttpResponse executePut(String bucketName, String objectName,
			Map<String, String> headerMap, Map<String, String> queryParamMap,
			String region, Object data, int length)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		HttpResponse response = execute(Method.PUT, region, bucketName,
				objectName, headerMap, queryParamMap, null, data, length);
		response.body().close();
		return response;
	}

	private HttpResponse executePut(String bucketName, String objectName,
			Map<String, String> headerMap, Map<String, String> queryParamMap,
			Object data, int length) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(bucketName);
		return executePut(bucketName, objectName, headerMap, queryParamMap,
				BucketRegionCache.INSTANCE.region(bucketName), data, length);
	}

	/**
	 * Sets application's name/version to user agent. For more information about
	 * user agent refer <a
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616
	 * </a>.
	 *
	 * @param name
	 *            your application name
	 * @param version
	 *            your application version
	 */
	@SuppressWarnings("unused")
	public void setAppInfo(String name, String version) {
		if (name == null || version == null) {
			// nothing to do
			return;
		}

		this.userAgent = DEFAULT_USER_AGENT + " " + name.trim() + "/"
				+ version.trim();
	}

	/**
	 * Returns metadata of given object.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	ObjectStat objectStat = s3Client.statObject(&quot;my-bucketname&quot;,
	 * 			&quot;my-objectname&quot;);
	 * 	System.out.println(objectStat);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name.
	 * @param objectName
	 *            Object name in the bucket.
	 *
	 * @return Populated object metadata.
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 * @see ObjectStat
	 */
	public ObjectStat statObject(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		HttpResponse response = executeHead(bucketName, objectName);
		ResponseHeader header = response.header();
		return new ObjectStat(bucketName, objectName, header.lastModified(),
				header.contentLength(), header.etag(), header.contentType());
	}

	/**
	 * Returns an {@link InputStream} containing the object. The InputStream
	 * must be closed after use else the connection will remain open.
	 *
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	InputStream stream = s3Client.getObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;);
	 * 	byte[] buf = new byte[16384];
	 * 	int bytesRead;
	 * 	while ((bytesRead = stream.read(buf, 0, buf.length)) &gt;= 0) {
	 * 		System.out.println(new String(buf, 0, bytesRead));
	 * 	}
	 * 	stream.close();
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 *
	 * @return {@link InputStream} containing the object
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public InputStream getObject(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException, InvalidArgumentException {
		return getObject(bucketName, objectName, 0, null);
	}

	/**
	 * Returns an {@link InputStream} containing a subset of the object. The
	 * InputStream must be closed after use else the connection will remain
	 * open.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	InputStream stream = s3Client.getObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;,
	 * 			1024L);
	 * 	byte[] buf = new byte[16384];
	 * 	int bytesRead;
	 * 	while ((bytesRead = stream.read(buf, 0, buf.length)) &gt;= 0) {
	 * 		System.out.println(new String(buf, 0, bytesRead));
	 * 	}
	 * 	stream.close();
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name.
	 * @param objectName
	 *            Object name in the bucket.
	 * @param offset
	 *            Offset to read at.
	 *
	 * @return {@link InputStream} containing the object.
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public InputStream getObject(String bucketName, String objectName,
			long offset) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException, InvalidArgumentException {
		return getObject(bucketName, objectName, offset, null);
	}

	/**
	 * Returns an {@link InputStream} containing a subset of the object. The
	 * InputStream must be closed after use else the connection will remain
	 * open.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	InputStream stream = s3Client.getObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;,
	 * 			1024L, 4096L);
	 * 	byte[] buf = new byte[16384];
	 * 	int bytesRead;
	 * 	while ((bytesRead = stream.read(buf, 0, buf.length)) &gt;= 0) {
	 * 		System.out.println(new String(buf, 0, bytesRead));
	 * 	}
	 * 	stream.close();
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name.
	 * @param objectName
	 *            Object name in the bucket.
	 * @param offset
	 *            Offset to read at.
	 * @param length
	 *            Length to read.
	 *
	 * @return {@link InputStream} containing the object.
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public InputStream getObject(String bucketName, String objectName,
			long offset, Long length) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException, InvalidArgumentException {
		if (offset < 0) {
			throw new InvalidArgumentException(
					"offset should be zero or greater");
		}

		if (length != null && length <= 0) {
			throw new InvalidArgumentException(
					"length should be greater than zero");
		}

		Map<String, String> headerMap = new HashMap<>();
		if (length != null) {
			headerMap.put("Range", "bytes=" + offset + "-"
					+ (offset + length - 1));
		} else {
			headerMap.put("Range", "bytes=" + offset + "-");
		}

		HttpResponse response = executeGet(bucketName, objectName, headerMap,
				null);
		return response.body().byteStream();
	}

	/**
	 * Downloads object and store it to given file name.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.getObject("my-bucketname", "my-objectname", "photo.jpg"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name.
	 * @param objectName
	 *            Object name in the bucket.
	 * @param fileName
	 *            file name.
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void getObject(String bucketName, String objectName, String fileName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException, InvalidArgumentException {
		Path filePath = Paths.get(fileName);
		boolean fileExists = Files.exists(filePath);

		if (fileExists && !Files.isRegularFile(filePath)) {
			throw new InvalidArgumentException(fileName
					+ ": not a regular file");
		}

		ObjectStat objectStat = statObject(bucketName, objectName);
		long length = objectStat.length();
		String etag = objectStat.etag();

		String tempFileName = fileName + "." + etag + ".part.minio";
		Path tempFilePath = Paths.get(tempFileName);
		boolean tempFileExists = Files.exists(filePath);

		if (tempFileExists && !Files.isRegularFile(tempFilePath)) {
			throw new IOException(tempFileName + ": not a regular file");
		}

		long tempFileSize = 0;
		if (tempFileExists) {
			tempFileSize = Files.size(tempFilePath);
			if (tempFileSize > length) {
				Files.delete(tempFilePath);
				tempFileExists = false;
				tempFileSize = 0;
			}
		}

		if (fileExists) {
			long fileSize = Files.size(filePath);
			if (fileSize == length) {
				// already downloaded. nothing to do
				return;
			} else if (fileSize > length) {
				throw new InvalidArgumentException("'" + fileName
						+ "': object size " + length
						+ " is smaller than file size " + fileSize);
			} else if (!tempFileExists) {
				// before resuming the download, copy filename to tempfilename
				Files.copy(filePath, tempFilePath);
				tempFileSize = fileSize;
				tempFileExists = true;
			}
		}

		InputStream is = null;
		OutputStream os = null;
		try {
			is = getObject(bucketName, objectName, tempFileSize);
			os = Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
			long bytesWritten = ByteStreams.copy(is, os);
			is.close();
			os.close();

			if (bytesWritten != length - tempFileSize) {
				throw new IOException(tempFileName
						+ ": unexpected data written.  expected = "
						+ (length - tempFileSize) + ", written = "
						+ bytesWritten);
			}

			Files.move(tempFilePath, filePath,
					StandardCopyOption.REPLACE_EXISTING);
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

	/**
	 * Returns an presigned URL containing the object.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	String url = s3Client.presignedGetObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;,
	 * 			60 * 60 * 24);
	 * 	System.out.println(url);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 * @param expires
	 *            object expiration
	 *
	 * @return string contains URL to download the object
	 *
	 * @throws InvalidBucketNameException
	 *             upon an invalid bucket name
	 * @throws InvalidKeyException
	 *             upon an invalid access key or secret key
	 * @throws IOException
	 *             upon signature calculation failure
	 * @throws NoSuchAlgorithmException
	 *             upon requested algorithm was not found during signature
	 *             calculation
	 * @throws InvalidExpiresRangeException
	 *             upon input expires is out of range
	 */
	public String presignedGetObject(String bucketName, String objectName,
			Integer expires) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidExpiresRangeException {
		// Validate input.
		if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
			throw new InvalidExpiresRangeException(expires,
					"expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
		}

		updateRegionCache(bucketName);
		String region = BucketRegionCache.INSTANCE.region(bucketName);

		Request request = createRequest(Method.GET, bucketName, objectName,
				region, null, null, null, null, 0);
		HttpUrl url = Signer.presignV4(request, region, accessKey, secretKey,
				expires);
		return url.toString();
	}

	/**
	 * Returns an presigned URL containing the object.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	String url = s3Client.presignedGetObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;);
	 * 	System.out.println(url);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 *
	 * @return string contains URL to download the object
	 *
	 * @throws IOException
	 *             upon connection error
	 * @throws NoSuchAlgorithmException
	 *             upon requested algorithm was not found during signature
	 *             calculation
	 * @throws InvalidExpiresRangeException
	 *             upon input expires is out of range
	 */
	public String presignedGetObject(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidExpiresRangeException {
		return presignedGetObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
	}

	/**
	 * Returns an presigned URL for PUT.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	String url = s3Client.presignedPutObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;,
	 * 			60 * 60 * 24);
	 * 	System.out.println(url);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 * @param expires
	 *            object expiration
	 *
	 * @return string contains URL to upload the object
	 *
	 * @throws InvalidBucketNameException
	 *             upon an invalid bucket name
	 * @throws InvalidKeyException
	 *             upon an invalid access key or secret key
	 * @throws IOException
	 *             upon signature calculation failure
	 * @throws NoSuchAlgorithmException
	 *             upon requested algorithm was not found during signature
	 *             calculation
	 * @throws InvalidExpiresRangeException
	 *             upon input expires is out of range
	 */
	public String presignedPutObject(String bucketName, String objectName,
			Integer expires) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidExpiresRangeException {
		if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
			throw new InvalidExpiresRangeException(expires,
					"expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
		}

		updateRegionCache(bucketName);
		String region = BucketRegionCache.INSTANCE.region(bucketName);

		Request request = createRequest(Method.PUT, bucketName, objectName,
				region, null, null, null, "", 0);
		HttpUrl url = Signer.presignV4(request, region, accessKey, secretKey,
				expires);
		return url.toString();
	}

	/**
	 * Returns an presigned URL for PUT.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	String url = s3Client.presignedPutObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;);
	 * 	System.out.println(url);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 *
	 * @return string contains URL to upload the object
	 *
	 * @throws IOException
	 *             upon connection error
	 * @throws NoSuchAlgorithmException
	 *             upon requested algorithm was not found during signature
	 *             calculation
	 * @throws InvalidExpiresRangeException
	 *             upon input expires is out of range
	 */
	public String presignedPutObject(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidExpiresRangeException {
		return presignedPutObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
	}

	/**
	 * Returns string map for given {@link PostPolicy}.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	PostPolicy policy = new PostPolicy(&quot;my-bucketname&quot;, &quot;my-objectname&quot;,
	 * 			DateTime.now().plusDays(7));
	 * 	policy.setContentType(&quot;image/png&quot;);
	 * 	Map&lt;String, String&gt; formData = s3Client.presignedPostPolicy(policy);
	 * 	System.out.print(&quot;curl -X POST &quot;);
	 * 	for (Map.Entry&lt;String, String&gt; entry : formData.entrySet()) {
	 * 		System.out.print(&quot; -F &quot; + entry.getKey() + &quot;=&quot; + entry.getValue());
	 * 	}
	 * 	System.out
	 * 			.println(&quot; -F file=@/tmp/userpic.png https://my-bucketname.s3.amazonaws.com/&quot;);
	 * }
	 * </pre>
	 *
	 * @param policy
	 *            Post policy
	 * @return Map of strings to construct form-data
	 *
	 * @see PostPolicy
	 */
	public Map<String, String> presignedPostPolicy(PostPolicy policy)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		updateRegionCache(policy.bucketName());
		return policy.formData(this.accessKey, this.secretKey);
	}

	/**
	 * Removes an object from a bucket.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.removeObject("my-bucketname", "my-objectname"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void removeObject(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		executeDelete(bucketName, objectName, null);
	}

	/**
	 * Returns all object information of given bucket name.
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @return an iterator of Items.
	 */
	public Iterable<Result<Item>> listObjects(final String bucketName)
			throws XmlPullParserException {
		return listObjects(bucketName, null);
	}

	/**
	 * Returns all object information those has given prefix and bucket name.
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param prefix
	 *            Prefix string. List objects whose name starts with `prefix`
	 *
	 * @return an iterator of Items.
	 */
	public Iterable<Result<Item>> listObjects(final String bucketName,
			final String prefix) throws XmlPullParserException {
		// list all objects recursively
		return listObjects(bucketName, prefix, true);
	}

	/**
	 * Returns {@code Iterable<Result><Item>>} of object information.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Iterable&lt;Result&lt;Item&gt;&gt; myObjects = s3Client.listObjects(&quot;my-bucketname&quot;);
	 * 	for (Result&lt;Item&gt; result : myObjects) {
	 * 		Item item = result.get();
	 * 		System.out.println(item.lastModified() + &quot;, &quot; + item.size() + &quot;, &quot;
	 * 				+ item.objectName());
	 * 	}
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param prefix
	 *            Prefix string. List objects whose name starts with `prefix`
	 * @param recursive
	 *            when false, emulates a directory structure where each listing
	 *            returned is either a full object or part of the object's key
	 *            up to the first '/'. All objects wit the same prefix up to the
	 *            first '/' will be merged into one entry.
	 *
	 * @return an iterator of Items.
	 *
	 * @see #listObjects(String bucketName)
	 * @see #listObjects(String bucketName, String prefix)
	 */
	public Iterable<Result<Item>> listObjects(final String bucketName,
			final String prefix, final boolean recursive) {
		return new Iterable<Result<Item>>() {
			@Override
			public Iterator<Result<Item>> iterator() {
				return new Iterator<Result<Item>>() {
					private String lastObjectName;
					private ListBucketResult listBucketResult;
					private Result<Item> error;
					private Iterator<Item> itemIterator;
					private Iterator<Prefix> prefixIterator;
					private boolean completed = false;

					private synchronized void populate() {
						String delimiter = "/";
						if (recursive) {
							delimiter = null;
						}

						String marker = null;
						if (this.listBucketResult != null) {
							if (delimiter != null) {
								marker = listBucketResult.nextMarker();
							} else {
								marker = this.lastObjectName;
							}
						}

						this.listBucketResult = null;
						this.itemIterator = null;
						this.prefixIterator = null;

						try {
							this.listBucketResult = listObjects(bucketName,
									marker, prefix, delimiter, null);
						} catch (InvalidBucketNameException
								| NoSuchAlgorithmException
								| InsufficientDataException | IOException
								| InvalidKeyException | NoResponseException
								| XmlPullParserException
								| ErrorResponseException | InternalException
								| InvocationTargetException e) {
							this.error = new Result<>(null, e);
						} finally {
							if (this.listBucketResult != null) {
								this.itemIterator = this.listBucketResult
										.contents().iterator();
								this.prefixIterator = this.listBucketResult
										.commonPrefixes().iterator();
							} else {
								this.itemIterator = new LinkedList<Item>()
										.iterator();
								this.prefixIterator = new LinkedList<Prefix>()
										.iterator();
							}
						}
					}

					@Override
					public boolean hasNext() {
						if (this.completed) {
							return false;
						}

						if (this.error == null && this.itemIterator == null
								&& this.prefixIterator == null) {
							populate();
						}

						if (this.error == null && !this.itemIterator.hasNext()
								&& !this.prefixIterator.hasNext()
								&& this.listBucketResult.isTruncated()) {
							populate();
						}

						if (this.error != null) {
							return true;
						}

						if (this.itemIterator.hasNext()) {
							return true;
						}

						if (this.prefixIterator.hasNext()) {
							return true;
						}

						this.completed = true;
						return false;
					}

					@Override
					public Result<Item> next() {
						if (this.completed) {
							throw new NoSuchElementException();
						}

						if (this.error == null && this.itemIterator == null
								&& this.prefixIterator == null) {
							populate();
						}

						if (this.error == null && !this.itemIterator.hasNext()
								&& !this.prefixIterator.hasNext()
								&& this.listBucketResult.isTruncated()) {
							populate();
						}

						if (this.error != null) {
							this.completed = true;
							return this.error;
						}

						if (this.itemIterator.hasNext()) {
							Item item = this.itemIterator.next();
							this.lastObjectName = item.objectName();
							return new Result<>(item, null);
						}

						if (this.prefixIterator.hasNext()) {
							Prefix prefix = this.prefixIterator.next();
							Item item;
							try {
								item = new Item(prefix.prefix(), true);
							} catch (XmlPullParserException e) {
								// special case: ignore the error as we can't
								// propagate the exception in next()
								item = null;
							}

							return new Result<>(item, null);
						}

						this.completed = true;
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private ListBucketResult listObjects(String bucketName, String marker,
			String prefix, String delimiter, Integer maxKeys)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvocationTargetException {
		Map<String, String> queryParamMap = new HashMap<>();

		if (marker != null) {
			queryParamMap.put("marker", marker);
		}

		if (prefix != null) {
			queryParamMap.put("prefix", prefix);
		}

		if (delimiter != null) {
			queryParamMap.put("delimiter", delimiter);
		}

		if (maxKeys != null) {
			queryParamMap.put("max-keys", maxKeys.toString());
		}

		HttpResponse response = executeGet(bucketName, null, null,
				queryParamMap);

		ListBucketResult result = new ListBucketResult();
		result.parseXml(response.body().charStream());
		response.body().close();
		return result;
	}

	/**
	 * Returns all bucket information owned by the current user.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Bucket&gt; bucketList = s3Client.listBuckets();
	 * 	for (Bucket bucket : bucketList) {
	 * 		System.out.println(bucket.creationDate() + &quot;, &quot; + bucket.name());
	 * 	}
	 * }
	 * </pre>
	 *
	 * @return List of bucket type.
	 *
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public List<Bucket> listBuckets() throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvocationTargetException {
		HttpResponse response = executeGet(null, null, null, null);
		ListAllMyBucketsResult result = new ListAllMyBucketsResult();
		result.parseXml(response.body().charStream());
		response.body().close();
		return result.buckets();
	}

	/**
	 * Checks if given bucket exist and is having read access.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	boolean found = s3Client.bucketExists(&quot;my-bucketname&quot;);
	 * 	if (found) {
	 * 		System.out.println(&quot;my-bucketname exists&quot;);
	 * 	} else {
	 * 		System.out.println(&quot;my-bucketname does not exist&quot;);
	 * 	}
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @return true if the bucket exists and the user has at least read access
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public boolean bucketExists(String bucketName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		try {
			executeHead(bucketName, null);
			return true;
		} catch (ErrorResponseException e) {
			if (e.errorCode() != ErrorCode.NO_SUCH_BUCKET) {
				// throw e;
			}
		} catch (IllegalStateException e) {
			// TODO: Add IllegalStateException to origin source
		}

		return false;
	}

	/**
	 * Creates a bucket with default region and ACL.
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void makeBucket(String bucketName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		this.makeBucket(bucketName, null, null);
	}

	/**
	 * Creates a bucket with given region and default ACL.
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param region
	 *            region in which the bucket will be created
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void makeBucket(String bucketName, String region)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		this.makeBucket(bucketName, region, null);
	}

	/**
	 * Creates a bucket with given ACL and default region.
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param acl
	 *            Canned ACL
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void makeBucket(String bucketName, Acl acl)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		this.makeBucket(bucketName, null, acl);
	}

	/**
	 * Creates a bucket with given region and ACL.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.makeBucket("my-bucketname");
	 * System.out.println("my-bucketname is created successfully"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param region
	 *            region in which the bucket will be created
	 * @param acl
	 *            Canned ACL
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void makeBucket(String bucketName, String region, Acl acl)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> headerMap = new HashMap<>();
		if (acl == null) {
			headerMap.put("x-amz-acl", Acl.PRIVATE.toString());
		} else {
			headerMap.put("x-amz-acl", acl.toString());
		}

		String configString;
		if (region == null || "us-east-1".equals(region)) {
			// for 'us-east-1', location constraint is not required. for more
			// info
			// http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region
			configString = "";
		} else {
			CreateBucketConfiguration config = new CreateBucketConfiguration(
					region);
			configString = config.toString();
		}

		executePut(bucketName, null, headerMap, null, "us-east-1",
				configString, 0);
	}

	/**
	 * Removes a bucket.
	 * <p>
	 * NOTE: - All objects (including all object versions and delete markers) in
	 * the bucket must be deleted prior, this API will not recursively delete
	 * objects
	 * </p>
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.removeBucket("my-bucketname");
	 * System.out.println("my-bucketname is removed successfully"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void removeBucket(String bucketName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		executeDelete(bucketName, null, null);
	}

	/**
	 * Returns {@link Acl} of given bucket.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Acl acl = s3Client.getBucketAcl(&quot;my-bucketname&quot;);
	 * 	System.out.println(acl);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @return Acl type
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public Acl getBucketAcl(String bucketName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("acl", "");

		HttpResponse response = executeGet(bucketName, null, null,
				queryParamMap);

		AccessControlPolicy result = new AccessControlPolicy();
		result.parseXml(response.body().charStream());
		response.body().close();

		Acl acl = Acl.PRIVATE;
		List<Grant> grants = result.grants();
		switch (grants.size()) {
		case 1:
			for (Grant grant : grants) {
				if (grant.grantee().uri() == null
						&& "FULL_CONTROL".equals(grant.permission())) {
					acl = Acl.PRIVATE;
					break;
				}
			}
			break;
		case 2:
			for (Grant grant : grants) {
				if ("http://acs.amazonaws.com/groups/global/AuthenticatedUsers"
						.equals(grant.grantee().uri())
						&& "READ".equals(grant.permission())) {
					acl = Acl.AUTHENTICATED_READ;
					break;
				} else if ("http://acs.amazonaws.com/groups/global/AllUsers"
						.equals(grant.grantee().uri())
						&& "READ".equals(grant.permission())) {
					acl = Acl.PUBLIC_READ;
					break;
				}
			}
			break;
		case 3:
			for (Grant grant : grants) {
				if ("http://acs.amazonaws.com/groups/global/AllUsers"
						.equals(grant.grantee().uri())
						&& "WRITE".equals(grant.permission())) {
					acl = Acl.PUBLIC_READ_WRITE;
					break;
				}
			}
			break;
		default:
			throw new InternalException(
					"Invalid control flow.  Please report this issue at "
							+ "https://github.com/minio/minio-java/issues");
		}
		return acl;
	}

	/**
	 * Sets {@link Acl} to given bucket.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.setBucketAcl("my-bucketname", Acl.PUBLIC_READ_WRITE);
	 * System.out.println("Canned ACL " + Acl.PUBLIC_READ_WRITE + " is set successfully to my-bucketname"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param acl
	 *            Canned ACL
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws InvalidAclNameException
	 *             upon invalid ACL is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void setBucketAcl(String bucketName, Acl acl)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException, InvalidAclNameException {
		if (acl == null) {
			throw new InvalidAclNameException("(null) ACL");
		}

		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("acl", "");

		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("x-amz-acl", acl.toString());

		executePut(bucketName, null, headerMap, queryParamMap, "", 0);
	}

	/**
	 * Uploads given file as object.
	 * <p>
	 * If the object is larger than 5MB, the client will automatically use a
	 * multipart session.
	 * </p>
	 * <p>
	 * If the session fails, the user may attempt to re-upload the object by
	 * attempting to create the exact same object again. The client will examine
	 * all parts of any current upload session and attempt to reuse the session
	 * automatically. If a mismatch is discovered, the upload will fail before
	 * uploading any more data. Otherwise, it will resume uploading where the
	 * session left off.
	 * </p>
	 * <p>
	 * If the multipart session fails, the user is responsible for resuming or
	 * removing the session.
	 * </p>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name to create in the bucket
	 * @param fileName
	 *            File name to upload
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void putObject(String bucketName, String objectName, String fileName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidArgumentException, InsufficientDataException,
			InputSizeMismatchException {
		if (fileName == null || "".equals(fileName)) {
			throw new InvalidArgumentException("empty file name is not allowed");
		}

		Path filePath = Paths.get(fileName);
		if (!Files.isRegularFile(filePath)) {
			throw new InvalidArgumentException("'" + fileName
					+ "': not a regular file");
		}

		String contentType = Files.probeContentType(filePath);
		long size = Files.size(filePath);

		RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r");
		try {
			putObject(bucketName, objectName, contentType, size, file);
		} finally {
			file.close();
		}
	}

	/**
	 * Uploads data from given stream as object.
	 * <p>
	 * If the object is larger than 5MB, the client will automatically use a
	 * multipart session.
	 * </p>
	 * <p>
	 * If the session fails, the user may attempt to re-upload the object by
	 * attempting to create the exact same object again. The client will examine
	 * all parts of any current upload session and attempt to reuse the session
	 * automatically. If a mismatch is discovered, the upload will fail before
	 * uploading any more data. Otherwise, it will resume uploading where the
	 * session left off.
	 * </p>
	 * <p>
	 * If the multipart session fails, the user is responsible for resuming or
	 * removing the session.
	 * </p>
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	StringBuilder builder = new StringBuilder();
	 * 	for (int i = 0; i &lt; 1000; i++) {
	 * 		builder.append(&quot;Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. &quot;);
	 * 		builder.append(&quot;(29 letters)\n&quot;);
	 * 		builder.append(&quot;Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. &quot;);
	 * 		builder.append(&quot;(31 letters)\n&quot;);
	 * 		builder.append(&quot;Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on &quot;);
	 * 		builder.append(&quot;NASAs Space Shuttle. (32 letters)\n&quot;);
	 * 		builder.append(&quot;The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. &quot;);
	 * 		builder.append(&quot;(39 letters)\n&quot;);
	 * 		builder.append(&quot;How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity &quot;);
	 * 		builder.append(&quot;awards at 49 letters long, but old-time Mac users may recognize it.\n&quot;);
	 * 		builder.append(&quot;Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac &quot;);
	 * 		builder.append(&quot;computers after System 7.\n&quot;);
	 * 		builder.append(&quot;A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my &quot;);
	 * 		builder.append(&quot;brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n&quot;);
	 * 		builder.append(&quot;---\n&quot;);
	 * 	}
	 * 	ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString()
	 * 			.getBytes(&quot;UTF-8&quot;));
	 * 	// create object
	 * 	s3Client.putObject(&quot;my-bucketname&quot;, &quot;my-objectname&quot;, bais,
	 * 			bais.available(), &quot;application/octet-stream&quot;);
	 * 	bais.close();
	 * 	System.out.println(&quot;my-bucketname is uploaded successfully&quot;);
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name to create in the bucket
	 * @param stream
	 *            stream to upload
	 * @param size
	 *            Size of all the data that will be uploaded
	 * @param contentType
	 *            Content type of the stream
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 *
	 * @see #putObject(String bucketName, String objectName, String fileName)
	 */
	public void putObject(String bucketName, String objectName,
			InputStream stream, long size, String contentType)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidArgumentException, InsufficientDataException,
			InputSizeMismatchException {
		putObject(bucketName, objectName, contentType, size,
				new BufferedInputStream(stream));
	}

	private String putObject(String bucketName, String objectName,
			String contentType, int length, Object data, String uploadId,
			int partNumber) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> queryParamMap = null;
		if (partNumber > 0 && uploadId != null && !"".equals(uploadId)) {
			queryParamMap = new HashMap<>();
			queryParamMap.put("partNumber", Integer.toString(partNumber));
			queryParamMap.put("uploadId", uploadId);
		}

		HttpResponse response = executePut(bucketName, objectName, null,
				queryParamMap, data, length);
		return response.header().etag();
	}

	private void putObject(String bucketName, String objectName,
			String contentType, long size, Object data)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException,
			InvalidArgumentException, InsufficientDataException,
			InputSizeMismatchException {
		if (size <= MIN_MULTIPART_SIZE) {
			// single put object
			putObject(bucketName, objectName, contentType, (int) size, data,
					null, 0);
			return;
		}

		/* Multipart upload */

		int[] rv = calculateMultipartSize(size);
		int partSize = rv[0];
		int partCount = rv[1];
		int lastPartSize = rv[2];
		Part[] totalParts = new Part[partCount];

		// check whether there is incomplete multipart upload or not
		String uploadId = getLatestIncompleteUploadId(bucketName, objectName);
		Iterator<Result<Part>> existingParts = null;
		Part part = null;
		if (uploadId != null) {
			// resume previous multipart upload
			existingParts = listObjectParts(bucketName, objectName, uploadId)
					.iterator();
			if (existingParts.hasNext()) {
				part = existingParts.next().get();
			}
		} else {
			// initiate new multipart upload ie no previous multipart found or
			// no previous valid parts for
			// multipart found
			uploadId = initMultipartUpload(bucketName, objectName, contentType);
		}

		int expectedReadSize = partSize;
		for (int partNumber = 1; partNumber <= partCount; partNumber++) {
			if (partNumber == partCount) {
				expectedReadSize = lastPartSize;
			}

			if (part != null && partNumber == part.partNumber()
					&& expectedReadSize == part.partSize()) {
				String md5Hash = Digest.md5Hash(data, expectedReadSize);
				if (md5Hash.equals(part.etag())) {
					// this part is already uploaded
					totalParts[partNumber - 1] = new Part(part.partNumber(),
							part.etag());
					skipStream(data, expectedReadSize);

					part = getPart(existingParts);

					continue;
				}
			}

			String etag = putObject(bucketName, objectName, contentType,
					expectedReadSize, data, uploadId, partNumber);
			totalParts[partNumber - 1] = new Part(partNumber, etag);
		}

		completeMultipart(bucketName, objectName, uploadId, totalParts);
	}

	private Part getPart(Iterator<Result<Part>> existingParts)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Part part;
		part = null;
		if (existingParts.hasNext()) {
			part = existingParts.next().get();
		}
		return part;
	}

	private String getLatestIncompleteUploadId(String bucketName,
			String objectName) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Upload latestUpload = null;
		for (Result<Upload> result : listIncompleteUploads(bucketName,
				objectName, true, false)) {
			Upload upload = result.get();
			if (upload.objectName().equals(objectName)
					&& (latestUpload == null || latestUpload.initiated()
							.compareTo(upload.initiated()) < 0)) {
				latestUpload = upload;
			}
		}

		if (latestUpload != null) {
			return latestUpload.uploadId();
		} else {
			return null;
		}
	}

	/**
	 * Returns {@code Iterable<Result><Upload>>} of incomplete uploads of given
	 * bucket name.
	 *
	 * @param bucketName
	 *            Bucket name
	 *
	 * @return an iterator of Upload.
	 * @see #listIncompleteUploads(String, String, boolean)
	 */
	public Iterable<Result<Upload>> listIncompleteUploads(String bucketName)
			throws XmlPullParserException {
		return listIncompleteUploads(bucketName, null, true, true);
	}

	/**
	 * Returns {@code Iterable<Result><Upload>>} of incomplete uploads of given
	 * bucket name and prefix.
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param prefix
	 *            filters the list of uploads to include only those that start
	 *            with prefix
	 *
	 * @return an iterator of Upload.
	 * @see #listIncompleteUploads(String, String, boolean)
	 */
	public Iterable<Result<Upload>> listIncompleteUploads(String bucketName,
			String prefix) throws XmlPullParserException {
		return listIncompleteUploads(bucketName, prefix, true, true);
	}

	/**
	 * Returns {@code Iterable<Result><Upload>>} of incomplete uploads.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Iterable&lt;Result&lt;Upload&gt;&gt; myObjects = s3Client
	 * 			.listIncompleteUploads(&quot;my-bucketname&quot;);
	 * 	for (Result&lt;Upload&gt; result : myObjects) {
	 * 		Upload upload = result.get();
	 * 		System.out.println(upload.uploadId() + &quot;, &quot; + upload.objectName());
	 * 	}
	 * }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param prefix
	 *            Prefix string. List objects whose name starts with `prefix`
	 * @param recursive
	 *            when false, emulates a directory structure where each listing
	 *            returned is either a full object or part of the object's key
	 *            up to the first '/'. All uploads with the same prefix up to
	 *            the first '/' will be merged into one entry.
	 *
	 * @return an iterator of Upload.
	 *
	 * @see #listIncompleteUploads(String bucketName)
	 * @see #listIncompleteUploads(String bucketName, String prefix)
	 */
	public Iterable<Result<Upload>> listIncompleteUploads(String bucketName,
			String prefix, boolean recursive) {
		return listIncompleteUploads(bucketName, prefix, recursive, true);
	}

	private Iterable<Result<Upload>> listIncompleteUploads(
			final String bucketName, final String prefix,
			final boolean recursive, final boolean aggregatePartSize) {
		return new Iterable<Result<Upload>>() {
			@Override
			public Iterator<Result<Upload>> iterator() {
				return new Iterator<Result<Upload>>() {
					private String nextKeyMarker;
					private String nextUploadIdMarker;
					private ListMultipartUploadsResult listMultipartUploadsResult;
					private Result<Upload> error;
					private Iterator<Upload> uploadIterator;
					private boolean completed = false;

					private synchronized void populate() {
						String delimiter = "/";
						if (recursive) {
							delimiter = null;
						}

						this.listMultipartUploadsResult = null;
						this.uploadIterator = null;

						try {
							this.listMultipartUploadsResult = listIncompleteUploads(
									bucketName, nextKeyMarker,
									nextUploadIdMarker, prefix, delimiter, 1000);
						} catch (InvalidBucketNameException
								| NoSuchAlgorithmException
								| InsufficientDataException | IOException
								| InvalidKeyException | NoResponseException
								| XmlPullParserException
								| ErrorResponseException | InternalException e) {
							this.error = new Result<>(null, e);
						} finally {
							if (this.listMultipartUploadsResult != null) {
								this.uploadIterator = this.listMultipartUploadsResult
										.uploads().iterator();
							} else {
								this.uploadIterator = new LinkedList<Upload>()
										.iterator();
							}
						}
					}

					private synchronized long getAggregatedPartSize(
							String objectName, String uploadId)
							throws InvalidBucketNameException,
							NoSuchAlgorithmException,
							InsufficientDataException, IOException,
							InvalidKeyException, NoResponseException,
							XmlPullParserException, ErrorResponseException,
							InternalException {
						long aggregatedPartSize = 0;

						for (Result<Part> result : listObjectParts(bucketName,
								objectName, uploadId)) {
							aggregatedPartSize += result.get().partSize();
						}

						return aggregatedPartSize;
					}

					@Override
					public boolean hasNext() {
						if (this.completed) {
							return false;
						}

						if (this.error == null && this.uploadIterator == null) {
							populate();
						}

						if (this.error == null
								&& !this.uploadIterator.hasNext()
								&& this.listMultipartUploadsResult
										.isTruncated()) {
							this.nextKeyMarker = this.listMultipartUploadsResult
									.nextKeyMarker();
							this.nextUploadIdMarker = this.listMultipartUploadsResult
									.nextUploadIdMarker();
							populate();
						}

						if (this.error != null) {
							return true;
						}

						if (this.uploadIterator.hasNext()) {
							return true;
						}

						this.completed = true;
						return false;
					}

					@Override
					public Result<Upload> next() {
						if (this.completed) {
							throw new NoSuchElementException();
						}

						if (this.error == null && this.uploadIterator == null) {
							populate();
						}

						if (this.error == null
								&& !this.uploadIterator.hasNext()
								&& this.listMultipartUploadsResult
										.isTruncated()) {
							this.nextKeyMarker = this.listMultipartUploadsResult
									.nextKeyMarker();
							this.nextUploadIdMarker = this.listMultipartUploadsResult
									.nextUploadIdMarker();
							populate();
						}

						if (this.error != null) {
							this.completed = true;
							return this.error;
						}

						if (this.uploadIterator.hasNext()) {
							Upload upload = this.uploadIterator.next();

							if (aggregatePartSize) {
								long aggregatedPartSize;

								try {
									aggregatedPartSize = getAggregatedPartSize(
											upload.objectName(),
											upload.uploadId());
								} catch (InvalidBucketNameException
										| NoSuchAlgorithmException
										| InsufficientDataException
										| IOException | InvalidKeyException
										| NoResponseException
										| XmlPullParserException
										| ErrorResponseException
										| InternalException e) {
									// special case: ignore the error as we
									// can't propagate the exception in next()
									aggregatedPartSize = -1;
								}

								upload.setAggregatedPartSize(aggregatedPartSize);
							}

							return new Result<>(upload, null);
						}

						this.completed = true;
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private ListMultipartUploadsResult listIncompleteUploads(String bucketName,
			String keyMarker, String uploadIdMarker, String prefix,
			String delimiter, int maxUploads)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		if (maxUploads < 0 || maxUploads > 1000) {
			maxUploads = 1000;
		}

		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("uploads", "");
		queryParamMap.put("max-uploads", Integer.toString(maxUploads));
		queryParamMap.put("prefix", prefix);
		queryParamMap.put("key-marker", keyMarker);
		queryParamMap.put("upload-id-marker", uploadIdMarker);
		queryParamMap.put("delimiter", delimiter);

		HttpResponse response = executeGet(bucketName, null, null,
				queryParamMap);

		ListMultipartUploadsResult result = new ListMultipartUploadsResult();
		result.parseXml(response.body().charStream());
		response.body().close();
		return result;
	}

	private String initMultipartUpload(String bucketName, String objectName,
			String contentType) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> headerMap = new HashMap<>();
		if (contentType != null) {
			headerMap.put("Content-Type", contentType);
		} else {
			headerMap.put("Content-Type", "application/octet-stream");
		}

		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("uploads", "");

		HttpResponse response = executePost(bucketName, objectName, headerMap,
				queryParamMap, "");

		InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
		result.parseXml(response.body().charStream());
		response.body().close();
		return result.uploadId();
	}

	private void completeMultipart(String bucketName, String objectName,
			String uploadId, Part[] parts) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("uploadId", uploadId);

		CompleteMultipartUpload completeManifest = new CompleteMultipartUpload(
				parts);

		HttpResponse response = executePost(bucketName, objectName, null,
				queryParamMap, completeManifest);
		response.body().close();
	}

	private Iterable<Result<Part>> listObjectParts(final String bucketName,
			final String objectName, final String uploadId) {
		return new Iterable<Result<Part>>() {
			@Override
			public Iterator<Result<Part>> iterator() {
				return new Iterator<Result<Part>>() {
					private int nextPartNumberMarker;
					private ListPartsResult listPartsResult;
					private Result<Part> error;
					private Iterator<Part> partIterator;
					private boolean completed = false;

					private synchronized void populate() {
						this.listPartsResult = null;
						this.partIterator = null;

						try {
							this.listPartsResult = listObjectParts(bucketName,
									objectName, uploadId, nextPartNumberMarker);
						} catch (InvalidBucketNameException
								| NoSuchAlgorithmException
								| InsufficientDataException | IOException
								| InvalidKeyException | NoResponseException
								| XmlPullParserException
								| ErrorResponseException | InternalException e) {
							this.error = new Result<>(null, e);
						} finally {
							if (this.listPartsResult != null) {
								this.partIterator = this.listPartsResult
										.partList().iterator();
							} else {
								this.partIterator = new LinkedList<Part>()
										.iterator();
							}
						}
					}

					@Override
					public boolean hasNext() {
						if (this.completed) {
							return false;
						}

						if (this.error == null && this.partIterator == null) {
							populate();
						}

						if (this.error == null && !this.partIterator.hasNext()
								&& this.listPartsResult.isTruncated()) {
							this.nextPartNumberMarker = this.listPartsResult
									.nextPartNumberMarker();
							populate();
						}

						if (this.error != null) {
							return true;
						}

						if (this.partIterator.hasNext()) {
							return true;
						}

						this.completed = true;
						return false;
					}

					@Override
					public Result<Part> next() {
						if (this.completed) {
							throw new NoSuchElementException();
						}

						if (this.error == null && this.partIterator == null) {
							populate();
						}

						if (this.error == null && !this.partIterator.hasNext()
								&& this.listPartsResult.isTruncated()) {
							this.nextPartNumberMarker = this.listPartsResult
									.nextPartNumberMarker();
							populate();
						}

						if (this.error != null) {
							this.completed = true;
							return this.error;
						}

						if (this.partIterator.hasNext()) {
							return new Result<>(this.partIterator.next(), null);
						}

						this.completed = true;
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private ListPartsResult listObjectParts(String bucketName,
			String objectName, String uploadId, int partNumberMarker)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("uploadId", uploadId);
		if (partNumberMarker > 0) {
			queryParamMap.put("part-number-marker",
					Integer.toString(partNumberMarker));
		}

		HttpResponse response = executeGet(bucketName, objectName, null,
				queryParamMap);

		ListPartsResult result = new ListPartsResult();
		result.parseXml(response.body().charStream());
		response.body().close();
		return result;
	}

	private void abortMultipartUpload(String bucketName, String objectName,
			String uploadId) throws InvalidBucketNameException,
			NoSuchAlgorithmException, InsufficientDataException, IOException,
			InvalidKeyException, NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("uploadId", uploadId);
		executeDelete(bucketName, objectName, queryParamMap);
	}

	/**
	 * Removes incomplete multipart upload of given object.
	 *
	 * </p> <b>Example:</b><br>
	 * 
	 * <pre>
	 * {@code s3Client.removeIncompleteUpload("my-bucketname", "my-objectname");
	 * System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname"); }
	 * </pre>
	 *
	 * @param bucketName
	 *            Bucket name
	 * @param objectName
	 *            Object name in the bucket
	 *
	 * @throws InvalidBucketNameException
	 *             upon invalid bucket name is given
	 * @throws NoResponseException
	 *             upon no response from server
	 * @throws IOException
	 *             upon connection error
	 * @throws XmlPullParserException
	 *             upon parsing response xml
	 * @throws ErrorResponseException
	 *             upon unsuccessful execution
	 * @throws InternalException
	 *             upon internal library error
	 */
	public void removeIncompleteUpload(String bucketName, String objectName)
			throws InvalidBucketNameException, NoSuchAlgorithmException,
			InsufficientDataException, IOException, InvalidKeyException,
			NoResponseException, XmlPullParserException,
			ErrorResponseException, InternalException {
		for (Result<Upload> r : listIncompleteUploads(bucketName, objectName,
				true, false)) {
			Upload upload = r.get();
			if (objectName.equals(upload.objectName())) {
				abortMultipartUpload(bucketName, objectName, upload.uploadId());
				return;
			}
		}
	}

	private void skipStream(Object inputStream, long n)
			throws IllegalArgumentException, IOException,
			InsufficientDataException {
		RandomAccessFile file = null;
		BufferedInputStream stream = null;
		if (inputStream instanceof RandomAccessFile) {
			file = (RandomAccessFile) inputStream;
		} else if (inputStream instanceof BufferedInputStream) {
			stream = (BufferedInputStream) inputStream;
		} else {
			throw new IllegalArgumentException(
					"unsupported input stream object");
		}

		if (file != null) {
			file.seek(file.getFilePointer() + n);
			return;
		}

		long bytesSkipped;
		long totalBytesSkipped = 0;

		while ((bytesSkipped = stream.skip(n - totalBytesSkipped)) >= 0) {
			totalBytesSkipped += bytesSkipped;
			if (totalBytesSkipped == n) {
				return;
			}
		}

		throw new InsufficientDataException(
				"Insufficient data.  bytes skipped " + totalBytesSkipped
						+ " expected " + n);
	}

	private static int[] calculateMultipartSize(long size)
			throws InvalidArgumentException {
		if (size > MAX_OBJECT_SIZE) {
			throw new InvalidArgumentException("size " + size
					+ " is greater than allowed size 5TiB");
		}

		double partSize = Math.ceil((double) size / MAX_MULTIPART_COUNT);
		partSize = Math.ceil(partSize / MIN_MULTIPART_SIZE)
				* MIN_MULTIPART_SIZE;

		double partCount = Math.ceil(size / partSize);

		double lastPartSize = partSize - (partSize * partCount - size);
		if (lastPartSize == 0.0) {
			lastPartSize = partSize;
		}

		int[] rv = { (int) partSize, (int) partCount, (int) lastPartSize };

		return rv;
	}

	/**
	 * Enables HTTP call tracing and written to traceStream.
	 *
	 * @param traceStream
	 *            {@link OutputStream} for writing HTTP call tracing.
	 *
	 * @see #traceOff
	 */
	public void traceOn(OutputStream traceStream) throws NullPointerException {
		if (traceStream == null) {
			throw new NullPointerException();
		} else {
			this.traceStream = new PrintWriter(new OutputStreamWriter(
					traceStream, StandardCharsets.UTF_8), true);
		}
	}

	/**
	 * Disables HTTP call tracing previously enabled.
	 *
	 * @see #traceOn
	 */
	public void traceOff() throws IOException {
		this.traceStream = null;
	}
}

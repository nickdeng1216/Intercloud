package hk.edu.polyu.intercloud.command.inquireresource;

import hk.edu.polyu.intercloud.api.InquireResourceAPI;
import hk.edu.polyu.intercloud.command.Command;
import hk.edu.polyu.intercloud.common.Common;
import hk.edu.polyu.intercloud.exceptions.DNSException;
import hk.edu.polyu.intercloud.exceptions.DatabaseException;
import hk.edu.polyu.intercloud.exceptions.InquireResourceAPIException;
import hk.edu.polyu.intercloud.exceptions.IntercloudException;
import hk.edu.polyu.intercloud.exceptions.NoSuchDataException;
import hk.edu.polyu.intercloud.exceptions.ProtocolException;
import hk.edu.polyu.intercloud.exceptions.StorageException;
import hk.edu.polyu.intercloud.minio.MinioForIntercloud;
import hk.edu.polyu.intercloud.minio.MinioStorageException;
import hk.edu.polyu.intercloud.model.cloud.Cloud;
import hk.edu.polyu.intercloud.model.dns.DNSTXTRecord;
import hk.edu.polyu.intercloud.model.protocol.AdditionalInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionInformation;
import hk.edu.polyu.intercloud.model.protocol.ExceptionProtocol;
import hk.edu.polyu.intercloud.model.protocol.GeneralInformation;
import hk.edu.polyu.intercloud.model.protocol.Protocol;
import hk.edu.polyu.intercloud.model.protocol.RequestInformation;
import hk.edu.polyu.intercloud.model.protocol.ResponseInformation;
import hk.edu.polyu.intercloud.util.DNSUtil;
import hk.edu.polyu.intercloud.util.DatabaseUtil;
import hk.edu.polyu.intercloud.util.LogUtil;
import hk.edu.polyu.intercloud.util.ProtocolUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.vmware.vim25.mo.samples.HostResource;

public class InquireForResource implements Command {
	private Protocol protocol;

	@Override
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	@Override
	public Protocol getProtocol() {
		return this.protocol;
	}

	@Override
	public Protocol execute(List<Object> o) {
		// 0. Get the requirements of the InquireForResource
		Map<String, String> requirements = this.protocol
				.getRequestInformation().getTags();
		try {

			if (Common.my_cloud.getRole().equalsIgnoreCase("Cloud")) {
				if (!this.protocol.getGeneralInformation().getFrom()
						.equals(this.protocol.getGeneralInformation().getTo())) {
					HashMap<String, String> resourceFoundMap = new HashMap<String, String>();

					// 1. Obtain service and corresponding requirements from
					// protocol
					String service_r = requirements.get("Service");
					String serviceProvider_r = null;

					String vendor_r = null;
					String geolocation_r = null;

					String disk_r = null;
					String cpu_r = null;
					String memory_r = null;

					for (Map.Entry<String, String> entry : requirements
							.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("ServiceProvider")) {
							serviceProvider_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Vendor")) {
							vendor_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase(
								"Geolocation")) {
							geolocation_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Disk")) {
							disk_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("CPU")) {
							cpu_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Memory")) {
							memory_r = entry.getValue();
						}
					}

					// 2. Check self service and provider
					boolean service_provider_supported = false;
					for (Map.Entry<String, String> entry : Common.my_service_providers
							.entrySet()) {
						if (service_r.equalsIgnoreCase(entry.getKey())
								&& (serviceProvider_r == null || serviceProvider_r
										.equalsIgnoreCase(entry.getValue()))) {
							service_provider_supported = true;
							resourceFoundMap.put("Service", entry.getKey());
							resourceFoundMap.put("ServiceProvider",
									entry.getValue());
							break;
						}
					}

					if (!service_provider_supported) {
						return this.generateProtocol("NoResourceFound", null,
								null);
					}

					// 3. Check self DNS record for high level requirements
					DNSTXTRecord dnsTXTRecord_self;
					dnsTXTRecord_self = DNSUtil.getTXTRecord(Common.my_name);

					boolean matchedVendor = false;
					boolean matchedGeolocation = false;
					if (vendor_r == null) {
						matchedVendor = true;
						resourceFoundMap.put("Vendor",
								dnsTXTRecord_self.getVendor());
					} else if (dnsTXTRecord_self.getVendor() == null) {
						matchedVendor = false;
					} else if (vendor_r.equalsIgnoreCase(dnsTXTRecord_self
							.getVendor())) {
						matchedVendor = true;
						resourceFoundMap.put("Vendor", vendor_r);
					}
					if (geolocation_r == null) {
						matchedGeolocation = true;
					} else if (dnsTXTRecord_self.getGeolocation() == null) {
						matchedGeolocation = false;
					} else {
						String[] geolocation_s = dnsTXTRecord_self
								.getGeolocation().split(";");
						for (String s : geolocation_s) {
							// String key = s.split(":")[0];
							String value = s.split(":")[1];
							if (geolocation_r.equalsIgnoreCase(value)) {
								matchedGeolocation = true;
								resourceFoundMap.put("Geolocation",
										geolocation_r);
								break;
							}
						}
					}

					if (!(matchedVendor && matchedGeolocation)) {
						return this.generateProtocol("NoResourceFound", null,
								null);
					}

					// 4. Ensure low level requirements of different
					// services(e.g.ObjectStorage, VM)

					if (service_r.equalsIgnoreCase("ObjectStorage")) {
						String serviceProvider_s = Common.my_service_providers
								.get("ObjectStorage");

						if (serviceProvider_s.equalsIgnoreCase("Minio")) {
							// TODO Get unreserved disk space or available disk
							// space
							MinioForIntercloud minioForIntercloud = new MinioForIntercloud();

							// String disk_s = "";
							// Unit conversion to GB
							long disk_number_s = minioForIntercloud
									.getFreeSpace();
							long disk_number_r = 0;

							if (disk_r == null) {
								disk_number_r = 40;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(disk_r);
								if (matcher.find()) {
									disk_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										disk_number_r = disk_number_r * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {

									} else {
										disk_number_r = 40;
									}
								} else {
									disk_number_r = 40;
								}
							}

							if (disk_number_s < disk_number_r) {
								return this.generateProtocol("NoResourceFound",
										null, null);
							} else {
								resourceFoundMap.put("Disk",
										Long.toString(disk_number_r) + "GB");
							}
						} else if (serviceProvider_s.equalsIgnoreCase("Google")
								|| serviceProvider_s.equalsIgnoreCase("Amazon")) {
							// TODO Public Cloud return ResourceFound
							return this.generateProtocol("NoResourceFound",
									null, null);

						} else {
							return this.generateProtocol("NoResourceFound",
									null, null);
						}

					} else if (service_r.equalsIgnoreCase("VM")) {
						String serviceProvider_s = Common.my_service_providers
								.get("VM");

						if (serviceProvider_s.equalsIgnoreCase("VMware")) {
							HostResource vmWareHostResource = new HostResource(
									"vsphere.properties");

							// Get unreserved CPU or available CPU
							long cpuMHz_s = vmWareHostResource
									.getUnreservedCPU();
							long maxCPUMHz_s = vmWareHostResource.getMaxCPU();
							long cpu_number_r = 0;
							boolean sufficientCPU = false;

							// Unit conversion to number of vCPU
							if (cpu_r == null) {
								cpu_number_r = 1;
							} else {
								cpu_number_r = Long.parseLong(cpu_r);
							}

							// Number of vCPU to MHz
							if (cpuMHz_s >= cpu_number_r * 1000
									&& maxCPUMHz_s >= cpu_number_r * 1000) {
								sufficientCPU = true;
								resourceFoundMap.put("CPU",
										Long.toString(cpu_number_r));
							}

							// Get unreserved memory or available CPU
							long memory_s = vmWareHostResource
									.getUnreservedMemory();
							long maxMemory_s = vmWareHostResource
									.getMaxMemory();
							long memory_number_r = 0;
							boolean sufficientMemory = false;

							// Unit conversion to MB
							if (memory_r == null) {
								memory_number_r = 1024;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(memory_r);
								if (matcher.find()) {
									memory_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										memory_number_r = memory_number_r * 1024 * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {
										memory_number_r = memory_number_r * 1024;
									} else if (unit.equalsIgnoreCase("MB")) {

									} else {
										memory_number_r = 1024;
									}
								} else {
									memory_number_r = 1024;
								}
							}

							if (memory_s >= memory_number_r
									&& maxMemory_s >= memory_number_r) {
								sufficientMemory = true;
								resourceFoundMap.put("Memory",
										Long.toString(memory_number_r) + "MB");
							}

							// Get unreserved disk space or available disk space
							long disk_s = vmWareHostResource.getDiskFreeSpace();
							long maxDisk_s = vmWareHostResource
									.getMaxFileSize();
							long disk_number_r = 0;
							boolean sufficientDisk = false;

							// Unit conversion to GB
							if (disk_r == null) {
								disk_number_r = 40;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(disk_r);
								if (matcher.find()) {
									disk_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										disk_number_r = disk_number_r * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {

									} else {
										disk_number_r = 40;
									}
								} else {
									disk_number_r = 40;
								}
							}

							// Number of vCPU to MHz
							if (disk_s >= disk_number_r
									&& maxDisk_s >= disk_number_r) {
								sufficientDisk = true;
								resourceFoundMap.put("Disk",
										Long.toString(disk_number_r) + "GB");
							}

							if (!(sufficientCPU && sufficientMemory && sufficientDisk)) {
								return this.generateProtocol("NoResourceFound",
										null, null);
							}

						} else if (serviceProvider_s.equalsIgnoreCase("HyperV")) {
							// TODO
							return this.generateProtocol("NoResourceFound",
									null, null);
						} else {
							return this.generateProtocol("NoResourceFound",
									null, null);
						}
					}

					return this.generateProtocol("Resource", null,
							resourceFoundMap);
				} else {
					HashMap<String, String> resourceFoundMap = new HashMap<String, String>();

					// 1. Obtain service and corresponding requirements from
					// protocol
					String service_r = requirements.get("Service");
					String serviceProvider_r = null;

					String vendor_r = null;
					String geolocation_r = null;

					String disk_r = null;
					String cpu_r = null;
					String memory_r = null;

					for (Map.Entry<String, String> entry : requirements
							.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("ServiceProvider")) {
							serviceProvider_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Vendor")) {
							vendor_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase(
								"Geolocation")) {
							geolocation_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Disk")) {
							disk_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("CPU")) {
							cpu_r = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase("Memory")) {
							memory_r = entry.getValue();
						}
					}

					// 2. Check self service and provider
					boolean service_provider_supported = false;
					for (Map.Entry<String, String> entry : Common.my_service_providers
							.entrySet()) {
						if (service_r.equalsIgnoreCase(entry.getKey())
								&& (serviceProvider_r == null || serviceProvider_r
										.equalsIgnoreCase(entry.getValue()))) {
							service_provider_supported = true;
							resourceFoundMap.put("Service", entry.getKey());
							resourceFoundMap.put("ServiceProvider",
									entry.getValue());
							break;
						}
					}

					if (!service_provider_supported) {
						selfQueryAction(this.generateProtocol(
								"NoResourceFound", null, null));
						return new Protocol(null, null, null, null, null, null,
								null);
					}

					// 3. Check self DNS record for high level requirements
					DNSTXTRecord dnsTXTRecord_self = DNSUtil
							.getTXTRecord(Common.my_name);

					boolean matchedVendor = false;
					boolean matchedGeolocation = false;
					if (vendor_r == null) {
						matchedVendor = true;
						resourceFoundMap.put("Vendor",
								dnsTXTRecord_self.getVendor());
					} else if (dnsTXTRecord_self.getVendor() == null) {
						matchedVendor = false;
					} else if (vendor_r.equalsIgnoreCase(dnsTXTRecord_self
							.getVendor())) {
						matchedVendor = true;
						resourceFoundMap.put("Vendor", vendor_r);
					}
					if (geolocation_r == null) {
						matchedGeolocation = true;
					} else if (dnsTXTRecord_self.getGeolocation() == null) {
						matchedGeolocation = false;
					} else {
						String[] geolocation_s = dnsTXTRecord_self
								.getGeolocation().split(";");
						for (String s : geolocation_s) {
							// String key = s.split(":")[0];
							String value = s.split(":")[1];
							if (geolocation_r.equalsIgnoreCase(value)) {
								matchedGeolocation = true;
								resourceFoundMap.put("Geolocation",
										geolocation_r);
								break;
							}
						}
					}

					if (!(matchedVendor && matchedGeolocation)) {
						selfQueryAction(this.generateProtocol(
								"NoResourceFound", null, null));
						return new Protocol(null, null, null, null, null, null,
								null);
					}

					// 4. Ensure low level requirements of different
					// services(e.g.ObjectStorage, VM)

					if (service_r.equalsIgnoreCase("ObjectStorage")) {
						String serviceProvider_s = Common.my_service_providers
								.get("ObjectStorage");

						if (serviceProvider_s.equalsIgnoreCase("Minio")) {
							// TODO Get unreserved disk space or available disk
							// space
							MinioForIntercloud minioForIntercloud = new MinioForIntercloud();

							// String disk_s = "";
							// Unit conversion to GB
							long disk_number_s = minioForIntercloud
									.getFreeSpace();
							long disk_number_r = 0;

							if (disk_r == null) {
								disk_number_r = 40;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(disk_r);
								if (matcher.find()) {
									disk_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										disk_number_r = disk_number_r * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {

									} else {
										disk_number_r = 40;
									}
								} else {
									disk_number_r = 40;
								}
							}

							if (disk_number_s < disk_number_r) {
								selfQueryAction(this.generateProtocol(
										"NoResourceFound", null, null));
								return new Protocol(null, null, null, null,
										null, null, null);
							} else {
								resourceFoundMap.put("Disk",
										Long.toString(disk_number_r) + "GB");
							}
						} else if (serviceProvider_s.equalsIgnoreCase("Google")
								|| serviceProvider_s.equalsIgnoreCase("Amazon")) {
							// TODO Public Cloud return ResourceFound
							selfQueryAction(this.generateProtocol(
									"NoResourceFound", null, null));
							return new Protocol(null, null, null, null, null,
									null, null);

						} else {
							selfQueryAction(this.generateProtocol(
									"NoResourceFound", null, null));
							return new Protocol(null, null, null, null, null,
									null, null);
						}

					} else if (service_r.equalsIgnoreCase("VM")) {
						String serviceProvider_s = Common.my_service_providers
								.get("VM");

						if (serviceProvider_s.equalsIgnoreCase("VMware")) {
							HostResource vmWareHostResource = new HostResource(
									"vsphere.properties");

							// Get unreserved CPU or available CPU
							long cpuMHz_s = vmWareHostResource
									.getUnreservedCPU();
							long maxCPUMHz_s = vmWareHostResource.getMaxCPU();
							long cpu_number_r = 0;
							boolean sufficientCPU = false;

							// Unit conversion to number of vCPU
							if (cpu_r == null) {
								cpu_number_r = 1;
							} else {
								cpu_number_r = Long.parseLong(cpu_r);
							}

							// Number of vCPU to MHz
							if (cpuMHz_s >= cpu_number_r * 1000
									&& maxCPUMHz_s >= cpu_number_r * 1000) {
								sufficientCPU = true;
								resourceFoundMap.put("CPU",
										Long.toString(cpu_number_r));
							}

							// Get unreserved memory or available CPU
							long memory_s = vmWareHostResource
									.getUnreservedMemory();
							long maxMemory_s = vmWareHostResource
									.getMaxMemory();
							long memory_number_r = 0;
							boolean sufficientMemory = false;

							// Unit conversion to MB
							if (memory_r == null) {
								memory_number_r = 1024;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(memory_r);
								if (matcher.find()) {
									memory_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										memory_number_r = memory_number_r * 1024 * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {
										memory_number_r = memory_number_r * 1024;
									} else if (unit.equalsIgnoreCase("MB")) {

									} else {
										memory_number_r = 1024;
									}
								} else {
									memory_number_r = 1024;
								}
							}

							if (memory_s >= memory_number_r
									&& maxMemory_s >= memory_number_r) {
								sufficientMemory = true;
								resourceFoundMap.put("Memory",
										Long.toString(memory_number_r) + "MB");
							}

							// Get unreserved disk space or available disk space
							long disk_s = vmWareHostResource.getDiskFreeSpace();
							long maxDisk_s = vmWareHostResource
									.getMaxFileSize();
							long disk_number_r = 0;
							boolean sufficientDisk = false;

							// Unit conversion to GB
							if (disk_r == null) {
								disk_number_r = 40;
							} else {
								Matcher matcher = Pattern.compile(
										"(\\d+)([A-Za-z]+)").matcher(disk_r);
								if (matcher.find()) {
									disk_number_r = Long.parseLong(matcher
											.group(1));
									String unit = matcher.group(2);
									if (unit.equalsIgnoreCase("TB")) {
										disk_number_r = disk_number_r * 1024;
									} else if (unit.equalsIgnoreCase("GB")) {

									} else {
										disk_number_r = 40;
									}
								} else {
									disk_number_r = 40;
								}
							}

							// Number of vCPU to MHz
							if (disk_s >= disk_number_r
									&& maxDisk_s >= disk_number_r) {
								sufficientDisk = true;
								resourceFoundMap.put("Disk",
										Long.toString(disk_number_r) + "GB");
							}

							if (!(sufficientCPU && sufficientMemory && sufficientDisk)) {
								selfQueryAction(this.generateProtocol(
										"NoResourceFound", null, null));
								return new Protocol(null, null, null, null,
										null, null, null);
							}

						} else if (serviceProvider_s.equalsIgnoreCase("HyperV")) {
							// TODO
							selfQueryAction(this.generateProtocol(
									"NoResourceFound", null, null));
							return new Protocol(null, null, null, null, null,
									null, null);
						} else {
							selfQueryAction(this.generateProtocol(
									"NoResourceFound", null, null));
							return new Protocol(null, null, null, null, null,
									null, null);
						}
					}

					selfQueryAction(this.generateProtocol("Resource", null,
							resourceFoundMap));
					return new Protocol(null, null, null, null, null, null,
							null);
				}
			} else if (Common.my_cloud.getRole().equalsIgnoreCase("Exchange")) {
				// 1. Obtain service and corresponding requirements from
				// protocol
				String service_r = requirements.get("Service");
				String serviceProvider_r = null;
				String vendor_r = null;
				String geolocation_r = null;

				for (Map.Entry<String, String> entry : requirements.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("ServiceProvider")) {
						serviceProvider_r = entry.getValue();
					} else if (entry.getKey().equalsIgnoreCase("Vendor")) {
						vendor_r = entry.getValue();
					} else if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						geolocation_r = entry.getValue();
					}
				}

				// 2. Get domain name for all clouds
				ArrayList<String> childCloudList = new ArrayList<String>();

				for (Map.Entry<String, Cloud> entry : Common.my_friends
						.entrySet()) {
					if (entry.getValue().getRole().equalsIgnoreCase("Cloud")
							&& !entry.getKey().equalsIgnoreCase(
									this.protocol.getGeneralInformation()
											.getFrom())) {
						childCloudList.add(entry.getKey());
					}
				}

				// 3. Select suitable cloud by querying DNS TXT record
				/*
				 * Clouds that fulfill the requirement will be stored in
				 * cloudList
				 */
				ArrayList<String> cloudList = new ArrayList<String>();

				for (String cloudName : childCloudList) {
					DNSTXTRecord dnsTXTRecord = DNSUtil.getTXTRecord(cloudName);

					boolean matchedServiceProvider = false;

					if (dnsTXTRecord.getService() == null) {
						continue;
					}

					String[] services = dnsTXTRecord.getService().split(";");
					for (String s : services) {
						String service = s.split(":")[0];
						String provider = s.split(":")[1];
						if (service.equalsIgnoreCase(service_r)
								&& (serviceProvider_r == null || serviceProvider_r
										.equalsIgnoreCase(provider))) {
							matchedServiceProvider = true;
							break;
						}
					}

					boolean matchedVendor = false;
					boolean matchedGeolocation = false;

					if (vendor_r == null) {
						matchedVendor = true;
					} else if (dnsTXTRecord.getVendor() == null) {
						matchedVendor = false;
					} else if (vendor_r.equalsIgnoreCase(dnsTXTRecord
							.getVendor())) {
						matchedVendor = true;
					}
					if (geolocation_r == null) {
						matchedGeolocation = true;
					} else if (dnsTXTRecord.getGeolocation() == null) {
						matchedGeolocation = false;
					} else {
						String[] geolocation = dnsTXTRecord.getGeolocation()
								.split(";");
						for (String s : geolocation) {
							// String key = s.split(":")[0];
							String value = s.split(":")[1];
							if (geolocation_r.equalsIgnoreCase(value)) {
								matchedGeolocation = true;
								break;
							}
						}
					}

					if (matchedServiceProvider && matchedVendor
							&& matchedGeolocation) {
						if (cloudList.size() == 0) {
							cloudList.add(cloudName);
						} else {
							for (int i = 0; i < cloudList.size(); i++) {
								if (cloudName.compareTo(cloudList.get(i)) < 0) {
									cloudList.add(i, cloudName);
									break;
								} else if (i == cloudList.size() - 1) {
									cloudList.add(cloudName);
									break;
								}
							}
						}
					}
				}

				if (cloudList.size() > 1) {
					String from = this.protocol.getGeneralInformation()
							.getFrom();
					for (int i = 0; i < cloudList.size(); i++) {
						if (from.compareTo(cloudList.get(0)) < 0) {
							break;
						} else {
							String temp = cloudList.remove(0);
							cloudList.add(temp);
						}
					}
				}

				if (!cloudList.isEmpty()) {
					return this.generateProtocol("Cloud", cloudList, null);
				}

				// 4. When no cloud fulfill the requirement, query Intercloud
				// Root
				/*
				 * New a InquireResourceAPI to send a InquireForResource request
				 * to Intercloud Root
				 */
				InquireResourceAPI irAPI = new InquireResourceAPI();

				HashMap<String, Object> requirements_clone = new HashMap<>();
				for (Map.Entry<String, String> entry : requirements.entrySet()) {
					if (!entry.getKey().equalsIgnoreCase("Service")) {
						requirements_clone
								.put(entry.getKey(), entry.getValue());
					}
				}
				boolean protocolSecurity = false;
				if (this.protocol.getAdditionalInformation().getTags()
						.containsKey("Signature")) {
					protocolSecurity = true;
				}

				try {
					irAPI.inquire(123456789, Common.my_root, service_r,
							requirements_clone, protocolSecurity, true,
							this.protocol.getId());
				} catch (InquireResourceAPIException e) {
					return this.generateProtocol("NoResourceFound", null, null);
				}

				// 5. Obtain protocolID from Common.InquireResoureceAPIFlag
				String protocolIdFromRoot = null;
				for (Map.Entry<String, String[]> entry : Common.flag.entrySet()) {
					if (entry.getValue()[0].equalsIgnoreCase("2")
							&& entry.getValue()[1]
									.equalsIgnoreCase(this.protocol.getId())) {
						protocolIdFromRoot = entry.getKey();
						Common.flag.remove(entry.getKey());
						break;
					}
				}

				// 6. Get response protocol from Database
				if (protocolIdFromRoot != null) {
					String protocolStringFromRoot = DatabaseUtil
							.getResultTrack(Long.valueOf(protocolIdFromRoot));
					Protocol protocolObjectFromRoot = ProtocolUtil
							.parseProtocolType(protocolStringFromRoot);
					if (protocolObjectFromRoot instanceof ExceptionProtocol) {
						return this.generateProtocol("NoResourceFound", null,
								null);
					} else if (protocolObjectFromRoot.getResponseInformation()
							.getTags().containsKey("Result")) {
						return this.generateProtocol("NoResourceFound", null,
								null);
					} else if (protocolObjectFromRoot.getResponseInformation()
							.getTags().containsKey("Exchange")) {
						String[] exchangeArray = protocolObjectFromRoot
								.getResponseInformation().getTags()
								.get("Exchange").split(";");
						ArrayList<String> exchangeList = new ArrayList<>();
						for (int i = 0; i < exchangeArray.length; i++) {
							exchangeList.add(exchangeArray[i]);
						}
						return this.generateProtocol("Exchange", exchangeList,
								null);
					}
				} else {
					return this.generateProtocol("NoResourceFound", null, null);
				}

			} else if (Common.my_cloud.getRole().equalsIgnoreCase("Root")) {
				// 1. Obtain Geolocation requirements from protocol
				String geolocation_r = null;
				for (Map.Entry<String, String> entry : requirements.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Geolocation")) {
						geolocation_r = entry.getValue();
					}
				}

				// 2. Get domain name for all Exchanges
				ArrayList<String> childExchagneList = new ArrayList<String>();

				for (Map.Entry<String, Cloud> entry : Common.my_friends
						.entrySet()) {
					if (entry.getValue().getRole().equalsIgnoreCase("Exchange")
							&& !entry.getKey().equalsIgnoreCase(
									this.protocol.getGeneralInformation()
											.getFrom())) {
						childExchagneList.add(entry.getKey());
					}
				}

				// 3. Select suitable Exchange by query DNS TXT record
				/*
				 * Exchanges that fulfill the requirement will be stored in
				 * exchangeList
				 */
				ArrayList<String> exchangeList = new ArrayList<>();

				for (String exchangeName : childExchagneList) {
					DNSTXTRecord dnsTXTRecord = DNSUtil
							.getTXTRecord(exchangeName);

					boolean matchedGeolocation = false;
					if (geolocation_r == null) {
						matchedGeolocation = true;
					} else if (dnsTXTRecord.getGeolocation() == null) {
						matchedGeolocation = false;
					} else {
						String[] geolocation = dnsTXTRecord.getGeolocation()
								.split(";");
						for (String s : geolocation) {
							// String key = s.split(":")[0];
							String value = s.split(":")[1];
							if (geolocation_r.equalsIgnoreCase(value)) {
								matchedGeolocation = true;
								break;
							}
						}
					}

					if (matchedGeolocation) {
						exchangeList.add(exchangeName);
					}
				}

				if (exchangeList.isEmpty()) {
					return this.generateProtocol("NoResourceFound", null, null);
				} else {
					return this
							.generateProtocol("Exchange", exchangeList, null);
				}
			}
		} catch (DNSException e) {
			LogUtil.logException(e);
			return this.generateException("101",
					DNSException.class.getSimpleName(), e.getMessage());
		} catch (MinioStorageException e) {
			LogUtil.logException(e);
			return this.generateException("3",
					StorageException.class.getSimpleName(), e.getMessage());
		} catch (ProtocolException e) {
			return this.generateException("102",
					ProtocolException.class.getSimpleName(), e.getMessage());
		} catch (ClassNotFoundException | SQLException | ParseException e) {
			LogUtil.logException(e);
			return this.generateException("2",
					DatabaseException.class.getSimpleName(), e.getMessage());
		} catch (NoSuchDataException e) {
			return this.generateException("7",
					NoSuchDataException.class.getSimpleName(), e.getMessage());
		} catch (Exception e) {
			return this.generateException("0",
					IntercloudException.class.getSimpleName(), e.getMessage());
		}
		return null;
	}

	@Override
	public AdditionalInformation pre_execute(String protocol,
			HashMap<String, String> info, String systemType)
			throws ParserConfigurationException, SAXException, IOException,
			Exception {
		return new AdditionalInformation();
	}

	@Override
	public void initialization() {

	}

	public void selfQueryAction(Protocol p) throws ProtocolException,
			ClassNotFoundException, SQLException, ParseException,
			NoSuchDataException {
		String protocolString = null;
		if (p instanceof ExceptionProtocol) {
			protocolString = ProtocolUtil
					.generateException((ExceptionProtocol) p);
		} else {
			protocolString = ProtocolUtil.generateResponse(p, false);
		}
		DatabaseUtil.insertResultTrack(p.getId(), p.getGeneralInformation()
				.getFrom(), protocolString);
		// Update flag
		if (Common.flag.containsKey(p.getId())) {
			String[] values = Common.flag.get(p.getId());
			values[0] = "2";
			Common.flag.replace(p.getId(), values);
		}
	}

	private Protocol generateProtocol(String resourceType,
			ArrayList<String> domainNameList, HashMap<String, String> resource) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		RequestInformation requestInformation = null;

		ResponseInformation responseInformation = new ResponseInformation();
		responseInformation.setCommand("ReplyForResource");
		responseInformation.setService("InquireResource");

		if (resourceType.equalsIgnoreCase("Cloud")
				|| resourceType.equalsIgnoreCase("Exchange")) {
			String content = "";
			for (int i = 0; i < domainNameList.size(); i++) {
				content += domainNameList.get(i) + ";";
			}
			content = content.substring(0, content.toString().length() - 1);
			responseInformation.addTags(resourceType, content);
		} else if (resourceType.equalsIgnoreCase("NoResourceFound")) {
			responseInformation.addTags("Result", "NoResourceFound");
		} else if (resourceType.equalsIgnoreCase("Resource")) {
			for (Map.Entry<String, String> entry : resource.entrySet()) {
				responseInformation.addTags(entry.getKey(), entry.getValue());
			}
		}

		AdditionalInformation additionalInformation = new AdditionalInformation();

		Protocol protocol = new Protocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation, requestInformation,
				responseInformation, additionalInformation, null);

		return protocol;
	}

	private ExceptionProtocol generateException(String code, String type,
			String message) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String[] dateTime = dateFormat.format(date).split("\\s+");

		GeneralInformation generalInformation = new GeneralInformation(
				this.protocol.getGeneralInformation().getTo(), this.protocol
						.getGeneralInformation().getFrom(), dateTime[0],
				dateTime[1]);

		ExceptionInformation exceptionInformation = new ExceptionInformation();
		exceptionInformation.addTags("Command", "InquireForResource");
		exceptionInformation.addTags("Code", code);
		exceptionInformation.addTags("Type", type);
		exceptionInformation.addTags("Message", message);

		AdditionalInformation additionalInformation = new AdditionalInformation();

		return new ExceptionProtocol(this.protocol.getProtocolVersion(),
				this.protocol.getId(), generalInformation,
				exceptionInformation, additionalInformation);
	}

}

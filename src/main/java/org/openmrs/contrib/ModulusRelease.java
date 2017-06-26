package org.openmrs.contrib;

import java.util.Date;

public class ModulusRelease {

	private Module module;
	private String moduleVersion;
	private String downloadURL;
	private Date dateCreated;
	private String requiredOMRSVersion;
	private Integer downloadCount;

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	public String getModuleVersion() {
		return moduleVersion;
	}

	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}

	public String getDownloadURL() {
		return downloadURL;
	}

	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getRequiredOMRSVersion() {
		return requiredOMRSVersion;
	}

	public void setRequiredOMRSVersion(String requiredOMRSVersion) {
		this.requiredOMRSVersion = requiredOMRSVersion;
	}

	public Integer getDownloadCount() {
		return downloadCount;
	}

	public void setDownloadCount(Integer downloadCount) {
		this.downloadCount = downloadCount;
	}
}

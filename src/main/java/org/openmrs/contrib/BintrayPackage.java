package org.openmrs.contrib;


import org.apache.commons.lang.StringUtils;

import java.util.List;

public class BintrayPackage {
	private String name;
	private String desc;
	private String website_url;
	private String vcs_url;
	private List<String> versions;
	private List<String> licenses;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		if (StringUtils.isBlank(desc)) {
			this.desc = null;
		} else {
			this.desc = desc;
		}
	}

	public String getWebsite_url() {
		return website_url;
	}

	public void setWebsite_url(String website_url) {
		if (StringUtils.isBlank(website_url)) {
			this.website_url = null;
		} else {
			this.website_url = website_url;
		}
	}

	public List<String> getVersions() {
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
	}

	public List<String> getLicenses() {
		return licenses;
	}

	public void setLicenses(List<String> licenses) {
		this.licenses = licenses;
	}

	public String getVcs_url() {
		return vcs_url;
	}

	public void setVcs_url(String vcs_url) {
		this.vcs_url = vcs_url;
	}
}

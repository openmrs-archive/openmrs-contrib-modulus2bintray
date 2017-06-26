package org.openmrs.contrib;

import java.text.SimpleDateFormat;
import java.util.Date;


public class BintrayVersion {

	private String name;
	private Date released;
	private String desc;

	public BintrayVersion() {
	}

	public BintrayVersion(ModulusRelease modulusRelease) {
		this.name = modulusRelease.getModuleVersion();
		this.released = modulusRelease.getDateCreated();
		this.desc = modulusRelease.getModule().getDescription();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getReleased() {
		return released;
	}

	public void setReleased(Date released) {
		this.released = released;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}

package org.openmrs.contrib;

import java.util.List;
import java.util.Map;

public class Module implements Comparable<Module> {

	private String bintrayPackage;
	private Integer id;
	private String legacyId;
	private String documentationURL;
	private String description;
	private String name;
	private List<Map<String, String>> releases;
	private String owner;
	private List<Map<String, String>> maintainers;

	@Override
	public String toString() {
		return name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLegacyId() {
		return legacyId;
	}

	public void setLegacyId(String legacyId) {
		this.legacyId = legacyId;
	}

	public String getBintrayPackage() {
		return bintrayPackage;
	}

	public void setBintrayPackage(String bintrayPackage) {
		this.bintrayPackage = bintrayPackage;
	}

	public String getDocumentationURL() {
		return documentationURL;
	}

	public void setDocumentationURL(String documentationURL) {
		this.documentationURL = documentationURL;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Map<String, String>> getReleases() {
		return releases;
	}

	public void setReleases(List<Map<String, String>> releases) {
		this.releases = releases;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public List<Map<String, String>> getMaintainers() {
		return maintainers;
	}

	public void setMaintainers(List<Map<String, String>> maintainers) {
		this.maintainers = maintainers;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Module module = (Module) o;

		return name.equals(module.name);

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}


	@Override
	public int compareTo(Module o) {
		return name.compareTo(o.name);
	}
}

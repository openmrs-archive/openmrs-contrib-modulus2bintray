package org.openmrs.contrib;

import java.util.Arrays;
import java.util.List;

public class BintrayAttribute {

	private String name;
	private List<String> values;

	public BintrayAttribute(String name, List<String> values) {
		this.name = name;
		this.values = values;
	}

	public BintrayAttribute(String name, String... values) {
		this.name = name;
		this.values = Arrays.asList(values);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public void setValues(String... values) {
		this.values = Arrays.asList(values);
	}
}

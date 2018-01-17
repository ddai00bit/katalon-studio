package com.kms.katalon.selenium.ide.model;

import java.util.List;

public class TestSuite {
	private String filePath;
	private String name;
	private List<TestCase> testCases;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<TestCase> getTestCases() {
		return testCases;
	}
	
	public void setTestCases(List<TestCase> testCases) {
		this.testCases = testCases;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	@Override
	public String toString() {
		return "TestSuite [name" + name + ", testCases=" + testCases + "]";
	}
}

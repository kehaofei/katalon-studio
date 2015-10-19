package com.kms.katalon.entity.report;

import java.io.File;

import com.kms.katalon.entity.file.IntegratedFileEntity;

public class ReportEntity extends IntegratedFileEntity {

	private static final long serialVersionUID = 1228763256416624714L;
	
	public String getRelativePathForUI() {
		if (parentFolder != null) {
			return parentFolder.getRelativePath() + File.separator + this.name;
		}
		return "";
	}

	@Override
	public String getLocation() {
		//return parentFolder.getLocation() + File.separator + this.name + getFileExtension();
		return parentFolder.getLocation() + File.separator + this.name;
	}

	@Override
	public String getFileExtension() {
		return getReportFileExtension();
	}

	public String getHtmlFile(){
		return getLocation() + File.separator + getName() + ".html";
	}
	
	public String getCSVFile(){
		return getLocation() + File.separator + getName() + ".csv";
	}
	
	public static String getReportFileExtension() {
		return "";
	}
}
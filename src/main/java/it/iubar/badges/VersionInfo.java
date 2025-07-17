package it.iubar.badges;

public class VersionInfo {

	private String fileName;
	private String number;

	public VersionInfo(String fileName, String number) {
		this.fileName = fileName;
		this.number = number;
	}

	public String getFilename() {
		return fileName;
	}

	public String getNumber() {
		return this.number;
	}

}

package app.cbreader.demo.model;

public class WrongReferenceItem {
	public String sample;
	public String fileName;
	public WrongReferenceItem(String sample, String fileName) {
		this.sample = sample;
		this.fileName = fileName;
	}
	
	@Override
	public String toString() {
		return String.format("%s %s\r\n", sample, fileName);
	}
}

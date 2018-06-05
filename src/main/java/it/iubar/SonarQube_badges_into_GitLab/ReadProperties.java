package it.iubar.SonarQube_badges_into_GitLab;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadProperties {
	
	public static String getKey(String file, String key){
		
		Properties prop = new Properties();
		String filePath = "././././././" + file;
		
		//try (InputStream inputStream = new FileInputStream(filePath)) {
		try (InputStream inputStream = new FileInputStream(filePath)) {
		;
			prop.load(inputStream);
 			return prop.getProperty(key);
		
		} catch (IOException ex) {
			return "Error";}
				
	}
	
	public static void main(String[] args) {
 
		}
}

package it.iubar.SonarQube_badges_into_GitLab;

import java.util.Properties;
import java.util.logging.Logger;

public class SonarQube_badges_into_GitLab {
	
	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	public static void main(String[] args) {
		PropertiesFile properties = new PropertiesFile();
		Properties config = new Properties();
		config = properties.getPropertiesFile("config.properties");
		if(config.isEmpty())
		{
			LOGGER.warning("Errore nella lettura del file!");
			System.exit(0);
		}
		else
		{
			GitlabApiClient client = new GitlabApiClient();
			client.setProperties(config);
			client.run();
		}
	}
}
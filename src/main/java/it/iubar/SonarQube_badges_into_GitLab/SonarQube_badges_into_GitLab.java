package it.iubar.SonarQube_badges_into_GitLab;

import java.util.Properties;
import java.util.logging.Logger;

public class SonarQube_badges_into_GitLab {

	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	public static void main(String[] args) {

		Properties config = null;
		if(areEnvVarsSet()) {
			// Reading config from enviroment variables....
			config = new Properties();
			config.setProperty("sonar.host", System.getenv("SONAR-HOST"));
			config.setProperty("gitlab.host", System.getenv("GITLAB-HOST"));
			config.setProperty("gitlab.token", System.getenv("GITLAB-TOKEN"));					  
		}else {
			// Reading config from file...
			PropertiesFile properties = new PropertiesFile();
			config = properties.getPropertiesFile("config.properties");
		}
		
		if (config.isEmpty()) {
			LOGGER.warning("File di configurazione vuoto");
			System.exit(0);
		} else {
			GitlabApiClient client = new GitlabApiClient();
			client.setProperties(config);
			client.run();
		}
	}

	private static boolean areEnvVarsSet() {
		if(System.getenv("SONAR-HOST")!=null && System.getenv("GITLAB-HOST")!=null && System.getenv("GITLAB-TOKEN")!=null) {
			return true;
		}
		return false;
	}
}
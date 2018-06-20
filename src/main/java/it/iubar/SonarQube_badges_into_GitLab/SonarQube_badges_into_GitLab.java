package it.iubar.SonarQube_badges_into_GitLab;

import java.util.Properties;
import java.util.logging.Logger;

public class SonarQube_badges_into_GitLab {

	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	public static void main(String[] args) throws Exception {
		try {
			Properties config = null;
			if(areEnvVarsSet()) {
				// Reading config from enviroment variables....
				config = new Properties();
				config.setProperty("sonar.host", System.getenv("SONAR_HOST"));
				config.setProperty("gitlab.host", System.getenv("GITLAB_HOST"));
				config.setProperty("gitlab.token", System.getenv("GITLAB_TOKEN"));					  
			}else {
				// Reading config from file...
				PropertiesFile properties = new PropertiesFile();
				config = properties.getPropertiesFile("config.properties");
			}
			
			if (config.isEmpty()) {
				LOGGER.warning("ERRORE: File di configurazione vuoto");
			} else {
				GitlabApiClient client = new GitlabApiClient();
				client.setProperties(config);
				client.run();
			}	
		} catch (Exception e) {
			LOGGER.severe("ERRORE: " + e.getMessage());
			throw e;
		}
	}

	private static boolean areEnvVarsSet() {
		if(System.getenv("SONAR_HOST")!=null && System.getenv("GITLAB_HOST")!=null && System.getenv("GITLAB_TOKEN")!=null) {
			return true;
		}
		return false;
	}
}

package it.iubar.SonarQube_badges_into_GitLab;

import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

public class SonarQube_badges_into_GitLab {

	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());
	private static final String CONFIG_FILE = "config.properties";

	public static void main(String[] args) throws Exception {
		try {
			Properties config = null;			
			if(areEnvVarsSet()) {				
				// Reading config from enviroment variables....
				LOGGER.info("La configurazione specificata tramite variabili d'ambiente ha la precedenza rispetto a quella indicata nel file " + CONFIG_FILE);				
				config = new Properties();
				config.setProperty("sonar.host", System.getenv("SONAR_HOST"));
				config.setProperty("gitlab.host", System.getenv("GITLAB_HOST"));
				config.setProperty("gitlab.token", System.getenv("GITLAB_TOKEN"));					  
			}else {
				// Reading config from file...
				PropertiesFile properties = new PropertiesFile();
				config = properties.getPropertiesFile(CONFIG_FILE);
			}

			if (config.isEmpty()) {
				LOGGER.warning("ERRORE: Impossibile inizializzare la configurazione del programma");
			} else {
				GitlabApiClient client = new GitlabApiClient();
				client.setProperties(config);
				int statusCode = client.run();	
				if(statusCode!=Status.OK.getStatusCode()) {
					LOGGER.severe("Status code: " + statusCode);
					System.exit(1);
				}else {
					LOGGER.info("Status code: " + statusCode);
					// OK: nothing to do
				}
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

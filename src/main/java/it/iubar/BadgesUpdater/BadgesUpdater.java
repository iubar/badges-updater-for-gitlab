package it.iubar.BadgesUpdater;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

public class BadgesUpdater {

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
				client.run();
				Set<Integer> errors = client.getErrors();
				if(!errors.isEmpty()) {
					LOGGER.severe("Done with errors");
					System.exit(1);
				}else {
					LOGGER.info("All done without errors");
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

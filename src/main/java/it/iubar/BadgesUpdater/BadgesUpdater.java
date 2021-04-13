package it.iubar.BadgesUpdater;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class BadgesUpdater {

	private static final Logger LOGGER = Logger.getLogger(BadgesUpdater.class.getName());

	public static void main(String[] args) throws Exception {
	 
			Properties config = null;			
			if(areEnvVarsSet()) {				
				// Reading config from enviroment variables....
				LOGGER.info("La configurazione specificata tramite variabili d'ambiente ha la precedenza rispetto a quella indicata nel file " + Config.CONFIG_FILE);				
				config = new Properties();
				config.setProperty("sonar.host", System.getenv("SONAR_HOST"));
				config.setProperty("gitlab.host", System.getenv("GITLAB_HOST"));
				config.setProperty("gitlab.token", System.getenv("GITLAB_TOKEN"));					  
			}else {
				// Reading config from file...
				config = PropertiesUtils.loadPropertiesFile(Config.CONFIG_FILE);
			}

			if (config.isEmpty()) {
				LOGGER.severe("ERRORE: Impossibile inizializzare la configurazione del programma");
				System.exit(1);
			} else {
				GitlabApiClient client = new GitlabApiClient();
				client.setProperties(config);
				client.run();
				Set<String> errors = client.getErrors();
				LOGGER.severe("***** SUMMARY *****");
				if(!errors.isEmpty()) {
					LOGGER.severe("Errors found: " + errors.size());
					for (String errorMsg : errors) {
						LOGGER.severe(errorMsg);
					}
					System.exit(1);
				}else {
					LOGGER.info("All done without errors");
				}
			}	
	 
	}

	private static boolean areEnvVarsSet() {
		if(System.getenv("SONAR_HOST")!=null && System.getenv("GITLAB_HOST")!=null && System.getenv("GITLAB_TOKEN")!=null) {
			return true;
		}
		return false;
	}
}

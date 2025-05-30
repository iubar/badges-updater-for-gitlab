package it.iubar.badges;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import it.iubar.badges.Config.UpdateType;

public class ProjectsUpdater {

	private static final Logger LOGGER = Logger.getLogger(ProjectsUpdater.class.getName());

	public static void main(String[] args) throws Exception {
		Properties config = null;

		if (areSomeEnvVarsSet()) {
			// Reading config from enviroment variables....
			LOGGER.info(
				"La configurazione specificata tramite variabili d'ambiente ha la precedenza rispetto a quella indicata nel file " +
				Config.CONFIG_FILE
			);
			config = new Properties();
			setProperty(config, "sonar.host", System.getenv("SONAR_HOST"));
			setProperty(config, "gitlab.host", System.getenv("GITLAB_HOST"));
			setProperty(config, "gitlab.token", System.getenv("GITLAB_TOKEN"));
			setProperty(config, "webhook.url", System.getenv("WEBHOOK_URL"));
		} else {
			// Reading config from file...
			config = PropertiesUtils.loadPropertiesFile(Config.CONFIG_FILE);
		}

		if (config.isEmpty()) {
			LOGGER.severe("ERRORE: Impossibile inizializzare la configurazione del programma");
			System.exit(1);
		} else {
			//client.run();
			BadgesUpdater badgesUpdater = new BadgesUpdater(config);
			PipelinesUpdater pipelinesUpdater = new PipelinesUpdater(config);
			WebhooksUpdater webhooksUpdater = new WebhooksUpdater(config);
			if (Config.UPDATE_BADGES!=UpdateType.DISABLED) {
				badgesUpdater.run();
			}
			if (Config.DELETE_PIPELINES) {
				pipelinesUpdater.run();
			}
			if (Config.UPDATE_WEBHOOKS!=UpdateType.DISABLED) {
				webhooksUpdater.run();
			}
			AbstractUpdater.printErrors();
		}
	}

	public static void setProperty(Properties prop, String key, String value) {
		if (value != null) {
			prop.setProperty(key, value);
		} else {
			if(AbstractUpdater.isNotEmpty(key) && AbstractUpdater.isEmpty(value) ) {
				LOGGER.warning("Valore assente per la chiave: " + key);
			}
			prop.setProperty(key, "");
		}
	}
 
	private static boolean areSomeEnvVarsSet() {
		if (AbstractUpdater.isEmpty(System.getenv("GITLAB_HOST")) || AbstractUpdater.isEmpty(System.getenv("GITLAB_TOKEN"))) {
			return false;
		}
		return true;
	}
}

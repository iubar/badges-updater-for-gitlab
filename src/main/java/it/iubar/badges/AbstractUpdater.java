package it.iubar.badges;

import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

public abstract class AbstractUpdater extends RestClient {

	private static final Logger LOGGER = Logger.getLogger(AbstractUpdater.class.getName());

	protected boolean debug = true;

	protected static Set<String> errors = new HashSet<>();

	protected Properties config = null;

	protected String sonarHost = null;
	protected String gitlabHost = null;
	protected String gitlabToken = null;
	protected String webhookUrl = null;

	public AbstractUpdater(Properties config) {
		super();
		this.config = config;
		initConfig();
	}

	public static Set<String> getErrors() {
		return AbstractUpdater.errors;
	}

	public void setProperties(Properties config) {
		this.config = config;
	}

	private void initConfig() {
		this.sonarHost = String.valueOf(this.config.get("sonar.host"));
		this.gitlabHost = String.valueOf(this.config.get("gitlab.host"));
		this.gitlabToken = String.valueOf(this.config.get("gitlab.token"));
		this.webhookUrl = String.valueOf(this.config.get("webhook.url"));
		LOGGER.info("Configurazione attuale:");
		if(isNotEmpty(this.sonarHost)) {
			LOGGER.info("sonarHost = " + this.sonarHost);
		}else {
			LOGGER.severe("Configurazione assente: " + "sonar.host");
		}
		if(isNotEmpty(this.gitlabHost)) {
			LOGGER.info("gitlabHost = " + this.gitlabHost);
		}else {
			LOGGER.severe("Configurazione assente: " + "gitlab.host");
		}
		if(isNotEmpty(this.gitlabToken)) {
			LOGGER.info("gitlabToken = " + this.gitlabToken);
		}else {
			LOGGER.severe("Configurazione assente: " + "gitlab.token");
		}
		if(isNotEmpty(this.webhookUrl)) {
			LOGGER.info("webhookUrl = " + this.webhookUrl);
		}else {
			LOGGER.severe("Configurazione assente: " + "webhook.url");
		}
	}

	public static boolean isEmpty(String str) {
		if (str != null) {
			if (!str.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	protected static boolean isEmpty2(String str) {
		return (str==null) || (str.length()==0);
	}
	
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/projects.html#list-all-projects
	 */
	protected JsonArray getProjects() {
		JsonArray projects = null;
		String route = "projects" + Config.PER_PAGE;
		Response response = doGet(route);
		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco dei progetti. Status code: " + statusCode);
			System.exit(1);
		} else {
			String jsonString = response.readEntity(String.class);
			projects = JsonUtils.readArray(jsonString);

			if (projects == null || projects.size() == 0) {
				LOGGER.severe("Impossibile recuperare l'elenco dei progetti. Status code: " + statusCode);
				System.exit(1);
			} else {
				if (this.debug) {
					LOGGER.info("Projects read from repository : " + projects.size());
				}
			}
		}
		return projects;
	}

	@Override
	protected URI getBaseURI() {
		return UriBuilder.fromUri(this.gitlabHost + "/api/" + Config.GITLAB_API_VER + "/").build();
	}

	public static void printErrors() {
		Set<String> errors = getErrors();
		if (!errors.isEmpty()) {
			LOGGER.severe("********** RIEPILOGO ERRORI ");
			LOGGER.severe("ERRORS FOUND : " + errors.size());
			for (String errorMsg : errors) {
				LOGGER.severe(errorMsg);
			}
			LOGGER.severe("**********");
			System.exit(1);
		} else {
			LOGGER.info("All done without errors");
		}
	}
}

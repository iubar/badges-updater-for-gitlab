package it.iubar.SonarQube_badges_into_GitLab;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public class GitlabApiClient {

	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	private static final int MAX_PROJECT_PER_PAGE = 200;

	private static final String GITLAB_FILE = ".gitlab-ci.yml";

	private static final String SONAR_FILE = "sonar-project.properties";
	
	private static final boolean DELETE_BADGES = true;
	
	private static final boolean ADD_BADGES = true;
	
	private static final boolean PRINT_PIPELINE = false;
	
	private static final boolean DELETE_PIPELINE = false;
		
	private static final boolean FAST_FAIL = true;

	private String sonarHost = null;
	private String gitlabHost = null;
	private String gitlabToken = null;
	private Properties config = null;

	public void setProperties(Properties config)
	{
		this.config = config;
	}

	public void loadConfig() {
		this.sonarHost = String.valueOf(this.config.get("sonar.host"));
		this.gitlabHost = String.valueOf(this.config.get("gitlab.host"));
		this.gitlabToken = String.valueOf(this.config.get("gitlab.token"));
		LOGGER.info("Configurazione attuale:");
		LOGGER.info("sonarHost = " + this.sonarHost);
		LOGGER.info("gitlabHost = " + this.gitlabHost);
		LOGGER.info("gitlabToken = " + this.gitlabToken);

	}

	public static Client factoryClient() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} 
		};

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			LOGGER.severe("ERRORE: " + e.getMessage());
			throw e;
		} catch (KeyManagementException e) {
			LOGGER.severe("ERRORE: " + e.getMessage());
			throw e;
		}

		ClientBuilder builder = ClientBuilder.newBuilder();
		Client client = builder.sslContext(sslContext).build();
		return client;
	}

	public int run() throws KeyManagementException, NoSuchAlgorithmException {

		loadConfig();

		// Creo la variabile per la rotta
		String route = "projects?per_page=" + MAX_PROJECT_PER_PAGE;

		//Creo il client utilizzando la funzione factoryClient() che ignora la validitï¿½ del certificato SSL
		Client client = factoryClient();

		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base, piï¿½ la rotta
		WebTarget target = client.target(getBaseURI() + route);

		// Effettuo la chiamata GET
		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken).get(Response.class);

		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();
 
		if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco dei progetti. Status code: " + statusCode);
		}else {

			// Dalla chiamata GET prendo il file JSON che ci restituisce e lo scrivo in una stringa
			String json = response.readEntity(String.class);

			// Il file JSON è un array di altri oggetti json, per questo lo vado a mettere dentro un oggetto JSONArray
			JSONArray jsonArray = new JSONArray(json);

			// Stampo il numero di oggetti JSON presenti nell'array, dato che un oggetto corrisponde ad un progetto,
			// il valore stampato corrisponde appunto al numeri di progetti recuperati dalla chiamata GET
			LOGGER.info("Numero di progetti: " + jsonArray.length());

			// Effettuo una serie di operazioni su tutti i progetti
			for (int i = 0; i < jsonArray.length(); i++) {

				JSONObject object = jsonArray.getJSONObject(i);

				// Estraggo il valore della KEY "id", ovvero l'ID del progetto
				int projectId = object.getInt("id");
				String path = object.getString("path_with_namespace");
				LOGGER.info("Progetto " + path + " (id " + projectId + ")");

				if(DELETE_BADGES){
				// Elimino i badges precedenti del progetto
				statusCode = doDelete(projectId);
				if(statusCode!=Status.OK.getStatusCode()) {
					if(FAST_FAIL) {
						System.exit(1);
					}else {
						break;
					}
				}
				}				

				if(ADD_BADGES){
					// Aggiungo i badges relativi al progetto
					List<JSONObject> badges = createBadges(object);
					if(!badges.isEmpty()) {
					statusCode = doPost(projectId, badges);
					if(statusCode!=Status.CREATED.getStatusCode()) {
						if(FAST_FAIL) {
							System.exit(1);
						}else {
							break;
						}
					}
					}
				}

				
				if(PRINT_PIPELINE || DELETE_PIPELINE) {
					String json2 = null;
					// Stampo l'elenco delle pipelines
					// https://docs.gitlab.com/ee/api/pipelines.html#list-project-pipelines				
					String route2 = "/projects/" + projectId + "/pipelines";
					WebTarget target2 = client.target(getBaseURI() + route2);
					Response response2 = target2.request().accept(MediaType.APPLICATION_JSON)
							.header("PRIVATE-TOKEN", this.gitlabToken).get(Response.class);
					statusCode = response2.getStatus();
					if(statusCode!=Status.OK.getStatusCode()) {
						LOGGER.severe("Impossibile recuperare l'elenco delle pipeline per il progetto " + projectId + ". Status code: " + statusCode);
						if(FAST_FAIL) {
							System.exit(1);
						}else {
							break;
						}
					}else {					
						json2 = response2.readEntity(String.class);	
						if(PRINT_PIPELINE) {
							LOGGER.log(Level.INFO, json2);
						}
						 
					}
			 				
				if(DELETE_PIPELINE && json2!=null) {

					//					[
					//					  {
					//					    "id": 47,
					//					    "status": "pending",
					//					    "ref": "new-pipeline",
					//					    "sha": "a91957a858320c0e17f3a0eca7cfacbff50ea29a",
					//					    "web_url": "https://example.com/foo/bar/pipelines/47"
					//					  },
					//					  {
					//					    "id": 48,
					//					    "status": "pending",
					//					    "ref": "new-pipeline",
					//					    "sha": "eb94b618fb5865b26e80fdd8ae531b7a63ad851a",
					//					    "web_url": "https://example.com/foo/bar/pipelines/48"
					//					  }
					//					]


					JSONArray jsonArray2 = new JSONArray(json2);
					LOGGER.info("Numero di pipeline per il progetto " + projectId + ": " + jsonArray.length());
					
					for (int j = 0; j < jsonArray2.length(); j++) {
					JSONObject object2 = jsonArray2.getJSONObject(j);
					int pipelineId = object2.getInt("id");						
					// https://docs.gitlab.com/ee/api/pipelines.html#delete-a-pipeline						
					String route3 = "/projects/" + projectId + "/pipelines/" + pipelineId;
					WebTarget target3 = client.target(getBaseURI() + route3);					
					Response response3 = target3.request().accept(MediaType.APPLICATION_JSON)
							.header("PRIVATE-TOKEN", this.gitlabToken).delete(Response.class);
					statusCode = response3.getStatus();
					if(statusCode!=Status.OK.getStatusCode()) {
						LOGGER.severe("Impossibile eliminare la pipeline " + pipelineId + " del progetto " + projectId + ". Status code: " + statusCode);
						if(FAST_FAIL) {
							System.exit(1);
						}else {
							break;
						}
					}					
					//						Verificare poi su disco se gli artifacts sono stati effettivamente cancellati, il percorso è
					//						/var/opt/gitlab/gitlab-rails/shared/artifacts/<year_month>/<project_id?>/<jobid>
					//						Lo sorage path può variare, vedi:
					//						https://gitlab.com/gitlab-org/gitlab-ce/blob/master/doc/administration/job_artifacts.md#storing-job-artifacts
					//
					//						In alternativa lavorare sui jobs: https://docs.gitlab.com/ee/api/jobs.html#erase-a-job
					//						In alternativa lavorare sui jobs: https://docs.gitlab.com/ee/api/jobs.html#erase-a-job
					//
					}
				}
				}
			}
		}
		return statusCode;
	}

	private int doDelete(int projectId) throws KeyManagementException, NoSuchAlgorithmException {
		Client client = factoryClient();
		WebTarget target = client.target(getBaseURI());

		Response response = target.path("projects")
				.path("" + projectId)
				.path("badges")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken)
				.get(Response.class);

		int statusCode = response.getStatus();
		if(statusCode==Status.NO_CONTENT.getStatusCode()) {
			LOGGER.info("Nessun badge presente per il progetto " + projectId + ". Status code: " + statusCode);
		}else if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.info("Impossibile recuperare l'elenco dei badge per il progetto " + projectId + ". Status code: " + statusCode);			
		}else {
			
			// Inserisco i dati in un JSONArray
			String json = response.readEntity(String.class);
			JSONArray badges = new JSONArray(json);
			// Elimino tutti i badges configurati per il progetto identificato da projectId
			for (int i = 0; i < badges.length(); i++) {						
				JSONObject object = badges.getJSONObject(i);
				int badgeId = object.getInt("id");			
				try {
					WebTarget webTarget = client.target(getBaseURI() + "projects/" + projectId + "/badges/" + badgeId);
					Response response2 = webTarget.request().accept(MediaType.APPLICATION_JSON).header("PRIVATE-TOKEN", gitlabToken).delete(Response.class);
					statusCode = response2.getStatus();
					if(statusCode!=Status.OK.getStatusCode()) {
						LOGGER.severe("Impossibile eliminare il badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode);						 
						break;						 
					}
				} catch (Exception e) {
					LOGGER.severe("ERRORE: " + e.getMessage());
					throw e;
				}
			}
			
		}
		return statusCode;
	}


	private int doPost(int projectId, List<JSONObject> badges) throws KeyManagementException, NoSuchAlgorithmException {
		int statusCode = 0;

		// Creo il client utilizzando la funzione factoryClient() che ignora la validitï¿½ del certificato SSL
		Client client = factoryClient();

		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base
		WebTarget target = client.target(getBaseURI());

		for (JSONObject badge : badges) {			
			// Faccio il post passando l'oggetto JSON sopra creato convertendolo in stringa
			Response response = target.path("projects").path("" + projectId).path("badges").request().accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", this.gitlabToken).post(Entity.json(badge.toString()));
			statusCode = response.getStatus();
			if(statusCode!=Status.CREATED.getStatusCode()) {
				LOGGER.severe("Impossibile aggiungere il badge " + badge.toString() + ". Status code: " + statusCode);
				break;
			}
		}
		return statusCode;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html
	 * 
	 * @param object
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	private List<JSONObject> createBadges(JSONObject object) throws KeyManagementException, NoSuchAlgorithmException {
		List<JSONObject> badges = new ArrayList<JSONObject>();

		// Determino l'id del progetto 
		int id = object.getInt("id");
		// Determino altri valori come il nome ed il gruppo del progetto
		// In alternativa alla lettura dei dati dal parametro "object", potrei utilizzare le variabili gitlab %{project_path} e %{default_branch} nella costruzione del link al pipeline badge.
		// Ad esempio un link valido sarebbe:
		// https://example.gitlab.com/%{project_path}/badges/%{default_branch}/badge.svg 
		// (vedi https://gitlab.iubar.it/help/user/project/badges). 
		// Nota che in Gitlab, il valore di %{project_path} è scollegato dal valore di <project_name> anche se di default %{project_path} assume il seguente formato <nome_gruppo>/<project_name>.			
		String name = object.getString("name");
		JSONObject namespace = object.getJSONObject("namespace");
		String group = namespace.getString("path");

		// Nota che sto generando la chiave di ogni singolo progetto Sonar a partire da una convenzione. 
		// Non ho quindi la certezza matematica che sia proprio la stessa salvata nel file sonar-project.properties
		// Nota che ":" equivale a "%3A"
		String key = group + ":" + name; 

		if(!isGitlabci(id)) {
			LOGGER.warning("File " + GITLAB_FILE + " assente per il progetto " + key + " (" + id + ")");
		}else {
			String link = this.gitlabHost +"/" + group + "/" + name + "/commits/%{default_branch}";
			String  image = this.gitlabHost + "/" + group + "/" + name + "/badges/%{default_branch}/build.svg";			
			JSONObject badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
		}

		if(!isSonar(id)) {
			LOGGER.warning("File " + SONAR_FILE + " assente per il progetto " + key + " (" + id + ")");
		}else {	 		
			String link = this.sonarHost + "/dashboard?id=" + key;
			String image = this.sonarHost + "/api/badges/gate?key=" + key;
			JSONObject badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=Reliability&id=" + key;	
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=bugs";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=code_smells&id=" + key;			
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=code_smells";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=ncloc_language_distribution&id=" + key;
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=ncloc_language_distribution";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=metric=classes&id=" + key;
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=classes";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=metric=functions&id=" + key;
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=functions";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
		}

		return badges;
	}

	private URI getBaseURI() {
		return UriBuilder.fromUri(this.gitlabHost + "/api/v4/").build();
	}

	private boolean isGitlabci(int id) throws KeyManagementException, NoSuchAlgorithmException {
		return isFile(id, GITLAB_FILE);
	}
	private boolean isSonar(int id) throws KeyManagementException, NoSuchAlgorithmException {
		return isFile(id, SONAR_FILE);
	}	
	private boolean isFile(int id, String filename) throws KeyManagementException, NoSuchAlgorithmException {
		boolean b = false;
		Client client = factoryClient();
		WebTarget target = client.target(getBaseURI());
		Response response = target.path("projects")
				.path("" + id)
				.path("repository")
				.path("tree")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken)
				.get(Response.class);
		int statusCode = response.getStatus();
		if (response.getStatus() != Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile determinare se il file " + filename + " è parte del progetto. Status code: " + statusCode);
		}else {
			String json = response.readEntity(String.class);			
			JSONArray files = new JSONArray(json);
			for (int i=0; i<files.length(); i++) {
				JSONObject object = files.getJSONObject(i);
				String fileName = object.getString("name");
				if (fileName.equals(filename)) {
					b=true;
				}
			}
		}

		return b;		

	}	
}

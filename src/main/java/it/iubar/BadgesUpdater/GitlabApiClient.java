package it.iubar.BadgesUpdater;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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

	private static final String GITLAB_FILE = ".gitlab-ci.yml";

	private static final String SONAR_FILE = "sonar-project.properties";

	private static final boolean FAIL_FAST = true;

	private static final boolean DELETE_BADGES = true;

	private static final boolean ADD_BADGES = true;

	private static final boolean PRINT_PIPELINE = false; // attivare solo a scopo di debug

	private static final boolean DELETE_PIPELINE = true;

	// Verranno cancellate tutte le pipelines ad esclusione delle ultime 4
	private static final int SKIP_PIPELINES_QNT = 4;

	// Di default il numero massimo di record restituiti da ogni chiamata all'Api è 20
	private static final int MAX_RECORDS_PER_RESPONSE = 200;	
	private static final String PER_PAGE = "?per_page=" + MAX_RECORDS_PER_RESPONSE;

	private String sonarHost = null;
	private String gitlabHost = null;
	private String gitlabToken = null;
	private Properties config = null;
	private Set<Integer> errors = new HashSet<Integer>();
	private Client client = null;

	public void setProperties(Properties config){
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

	/**
	 * Crea il client e ignora la validità del certificato SSL
	 * 
	 * @return

	 */
	public static Client factoryClient()  {
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
		} catch (KeyManagementException e) {
			LOGGER.severe("ERRORE: " + e.getMessage());
		}

		ClientBuilder builder = ClientBuilder.newBuilder();
		Client client = builder.sslContext(sslContext).build();
		return client;
	}

	private JSONArray getProjects() {

		JSONArray projects = null;				
		String route = getBaseURI() + "projects" + PER_PAGE;
		this.client = factoryClient();
		WebTarget target = this.client.target(route);

		// Effettuo la chiamata GET
		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken).get(Response.class);

		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();

		if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco dei progetti. Status code: " + statusCode);
			System.exit(1);
		}else {

			// Dalla chiamata GET prendo il file JSON che ci restituisce e lo scrivo in una stringa
			String json = response.readEntity(String.class);

			// Il file JSON è un array di altri oggetti json, per questo lo vado a mettere dentro un oggetto JSONArray
			projects = new JSONArray(json);
		}
		return projects;
	}

	public void run(){
		loadConfig();
		JSONArray projects = getProjects();
		// Stampo il numero di oggetti JSON presenti nell'array, dato che un oggetto corrisponde ad un progetto,
		// il valore stampato corrisponde appunto al numeri di progetti recuperati dalla chiamata GET
		LOGGER.info("#" + projects.length() + " projects read from repository");

		// Effettuo una serie di operazioni su tutti i progetti
		for (int i = 0; i < projects.length(); i++) {

			JSONObject project = projects.getJSONObject(i);
			int projectId = project.getInt("id");

			String path = project.getString("path_with_namespace");
			LOGGER.info("Project " + path + " (id " + projectId + ")");

			if(DELETE_BADGES){
				// Elimino i badges precedenti del progetto
				List<Integer> results = removeBadges(projectId);
				if(results.isEmpty()) {
					LOGGER.warning("removeBadges() returns no results for project id " + projectId);
				}else {
					LOGGER.log(Level.INFO, "#" + results.size() + " badges deleted from project id " + projectId);
				}
			}				

			if(ADD_BADGES){
				// Aggiungo i badges relativi al progetto
				List<JSONObject> badges = createBadges(project);
				if(!badges.isEmpty()) {
					List<Integer> results = insertBadges(projectId, badges);
					if(results.isEmpty()) {
						LOGGER.severe("insertBadges() returns no results for project id " + projectId);
					}else {
						LOGGER.log(Level.INFO, "#" + results.size() + " badges added to project id " + projectId);
					}
				}
			}


			if(PRINT_PIPELINE || DELETE_PIPELINE) {

				JSONArray pipelines = getPipelines(projectId);
				if(PRINT_PIPELINE) {
					LOGGER.log(Level.INFO, "Pipelines dump: " + pipelines.toString());
				}if(!DELETE_PIPELINE){			
					LOGGER.log(Level.INFO, "#" + pipelines.length() + " pipelines read for project id " + projectId);
				}

				if(DELETE_PIPELINE && pipelines!=null && !pipelines.isEmpty()) {

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

					List<Integer> results = removePipelines(projectId, pipelines);					
				}
			}
		}
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#list-project-pipelines	
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @return
	 */
	private JSONArray getPipelines(int projectId) {

		String route2 = getBaseURI() + "/projects/" + projectId + "/pipelines" + PER_PAGE;
		WebTarget target2 = this.client.target(route2);
		Response response2 = target2.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken).get(Response.class);
		int statusCode = response2.getStatus();
		if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco delle pipeline per il progetto " + projectId + ". Status code: " + statusCode);
			if(FAIL_FAST) {
				System.exit(1);
			}else {
				this.errors.add(projectId);
			}
		}

		String json2 = response2.readEntity(String.class);		
		JSONArray pipelines = new JSONArray(json2);	

		return pipelines;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#delete-a-pipeline
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param pipelines JSONArray
	 * @return
	 */
	private List<Integer> removePipelines(int projectId, JSONArray pipelines) {
		List<Integer> pipelineIds = new ArrayList<Integer>();		
		if(pipelines.length()<=SKIP_PIPELINES_QNT) {
			LOGGER.info("#" + pipelines.length() + " pipelines found <= " + SKIP_PIPELINES_QNT + ", so there is no pipelines to delete for project id " + projectId);
		}else {
			LOGGER.info("#" + pipelines.length() + " pipelines found for project id " + projectId);
			for (int j = 0 + SKIP_PIPELINES_QNT ; j < pipelines.length(); j++) {
				JSONObject pipeline = pipelines.getJSONObject(j);
				int pipelineId = pipeline.getInt("id");	// The ID of a pipeline									
				String route3 = "/projects/" + projectId + "/pipelines/" + pipelineId;
				WebTarget target3 = this.client.target(getBaseURI() + route3);					
				Response response3 = target3.request().accept(MediaType.APPLICATION_JSON)
						.header("PRIVATE-TOKEN", this.gitlabToken).delete(Response.class);
				int statusCode = response3.getStatus();
				if(statusCode==Status.NO_CONTENT.getStatusCode()) {
					pipelineIds.add(pipelineId);
				}else {
					LOGGER.severe("Impossibile eliminare la pipeline " + pipelineId + " del progetto " + projectId + ". Status code: " + statusCode);
					if(FAIL_FAST) {
						System.exit(1);
					}else {
						this.errors.add(projectId);
						break;
					}				
				}
				//						TODO: Verificare poi su disco se gli artifacts sono stati effettivamente cancellati, il percorso è
				//						/var/opt/gitlab/gitlab-rails/shared/artifacts/<year_month>/<project_id?>/<jobid>
				//						Lo storage path può variare, vedi:
				//						https://gitlab.com/gitlab-org/gitlab-ce/blob/master/doc/administration/job_artifacts.md#storing-job-artifacts
				//						In alternativa lavorare sui jobs: https://docs.gitlab.com/ee/api/jobs.html#erase-a-job
				//
			}
 
			LOGGER.log(Level.INFO, "#" + pipelineIds.size() + " pipelines removed from project id " + projectId);

		}
		return pipelineIds;
	}

	/**
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * 
	 * Elimina tutti i badges configurati per il progetto identificato da projectId
	 * @param projectId
	 * @return
	 */
	private List<Integer> removeBadges(int projectId){
		List<Integer> badgeIds = new ArrayList<Integer>();
		WebTarget target = this.client.target(getBaseURI());
		Response response = target.path("projects")
				.path("" + projectId)
				.path("badges")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken)
				.get(Response.class);

		int statusCode = response.getStatus();
		if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco dei badge per il progetto " + projectId + ". Status code: " + statusCode);			
		}else {
			String json = response.readEntity(String.class);
			JSONArray badges = new JSONArray(json);
			LOGGER.info("#" + badges.length() + " badges read for project id " + projectId);

			for (int i = 0; i < badges.length(); i++) {						
				JSONObject object = badges.getJSONObject(i);
				int badgeId = object.getInt("id");			
				try {
					WebTarget webTarget = this.client.target(getBaseURI() + "projects/" + projectId + "/badges/" + badgeId);
					Response response2 = webTarget.request().accept(MediaType.APPLICATION_JSON).header("PRIVATE-TOKEN", gitlabToken).delete(Response.class);
					statusCode = response2.getStatus();
					if(statusCode==Status.NO_CONTENT.getStatusCode()) {
						// OK
						// LOGGER.info("No content per badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode);
						badgeIds.add(badgeId);
					}else {
						LOGGER.severe("Impossibile eliminare il badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode);						 
						if(FAIL_FAST) {
							System.exit(1);
						}else {
							this.errors.add(projectId);
							break;
						}						 
					}
				} catch (Exception e) {
					LOGGER.severe("ERRORE: " + e.getMessage());
					throw e;
				}
			}

		}
		return badgeIds;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#add-a-badge-to-a-project
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param badges
	 * @return
	 */
	private List<Integer> insertBadges(int projectId, List<JSONObject> badges) {
		List<Integer> badgeIds = new ArrayList<Integer>();
		WebTarget target = this.client.target(getBaseURI());
		for (JSONObject badge : badges) {			
			Response response = target.path("projects").path("" + projectId).path("badges").request().accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", this.gitlabToken).post(Entity.json(badge.toString()));
			int statusCode = response.getStatus();
			if(statusCode==Status.CREATED.getStatusCode()) {
				String json = response.readEntity(String.class);			
				JSONObject object = new JSONObject(json);
				int badgeId = object.getInt("id");
				badgeIds.add(badgeId);
			}else {
				LOGGER.severe("Impossibile aggiungere il badge " + badge.toString() + ". Status code: " + statusCode);
				if(FAIL_FAST) {
					System.exit(1);
				}else {
					this.errors.add(projectId);
					break;
				}								
			}
		}
		return badgeIds;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html
	 * 
	 * @param object
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	private List<JSONObject> createBadges(JSONObject object) {
		List<JSONObject> badges = new ArrayList<JSONObject>();

		// Determino l'id del progetto 
		int projectId = object.getInt("id");
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

		if(!isGitlabci(projectId)) {
			LOGGER.warning("File " + GITLAB_FILE + " assente per il progetto " + key + " (" + projectId + ")");
		}else {
			String link = this.gitlabHost +"/" + group + "/" + name + "/commits/%{default_branch}";
			String  image = this.gitlabHost + "/" + group + "/" + name + "/badges/%{default_branch}/build.svg";			
			JSONObject badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
		}

		if(!isSonar(projectId)) {
			LOGGER.warning("File " + SONAR_FILE + " assente per il progetto " + key + " (" + projectId + ")");
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
			link = this.sonarHost + "/component_measures?metric=classes&id=" + key;
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=classes";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
			link = this.sonarHost + "/component_measures?metric=functions&id=" + key;
			image = this.sonarHost + "/api/badges/measure?key=" + key + "&metric=functions";
			badge = new JSONObject().put("link_url", link).put("image_url", image);
			badges.add(badge);
		}

		return badges;
	}

	private URI getBaseURI() {
		return UriBuilder.fromUri(this.gitlabHost + "/api/v4/").build();
	}

	private boolean isGitlabci(int projectId){
		return isFile(projectId, GITLAB_FILE);
	}

	private boolean isSonar(int projectId){
		return isFile(projectId, SONAR_FILE);
	}	

	/**
	 * 
	 * @see see https://docs.gitlab.com/ee/api/repositories.html#list-repository-tree
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param fileName
	 * @return
	 */
	private boolean isFile(int projectId, String fileName){
		boolean b = false;		
		String route = getBaseURI() + "/projects/" + projectId + "/repository/tree" + PER_PAGE;
		// Altre opzioni con relativo valor edi default
		// path == /
		// ref == master

		WebTarget target = this.client.target(route);
		Response response = target.request()
				.accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken)
				.get(Response.class);

		int statusCode = response.getStatus();
		if (response.getStatus() != Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile determinare se il file " + fileName + " è parte del progetto. Status code: " + statusCode);
		}else {
			String json = response.readEntity(String.class);			
			JSONArray files = new JSONArray(json);		
			for (int i=0; i<files.length(); i++) {
				JSONObject object = files.getJSONObject(i);
				String _fileName = object.getString("name");			
				if (_fileName.equals(fileName)) {
					b=true;
					break;
				}
			}
		}
		return b;		
	}

	public Set<Integer> getErrors() {
		return this.errors;
	}	
}

package it.iubar.BadgesUpdater;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
 
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

public class GitlabApiClient extends RestClient {

	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	private String sonarHost = null;
	private String gitlabHost = null;
	private String gitlabToken = null;
	private Properties config = null;
	private Set<String> errors = new HashSet<>();

	public GitlabApiClient() {
		super();
	}
	
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

	private JsonArray getProjects() {
		JsonArray projects = null;		
		String route = "projects" + Config.PER_PAGE; // @see https://docs.gitlab.com/ee/api/projects.html#list-all-projects
		Response response = doGet(route);
		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();
		if(statusCode!=Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile recuperare l'elenco dei progetti. Status code: " + statusCode);
			System.exit(1);
		}else {
			String jsonString = response.readEntity(String.class);
			projects = readArray(jsonString); 
		}
		return projects;
	}

	private JsonObject readObject(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonObject object = reader.readObject();
		return object;
	}
	
	private JsonArray readArray(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonArray array = reader.readArray();
		return array;
	}	

	public void run(){
		loadConfig();
		 JsonArray projects = getProjects();
		LOGGER.info("#" + projects.size() + " projects read from repository");

		boolean debug = true;
		
		// Effettuo una serie di operazioni su tutti i progetti
		for (int i = 0; i < projects.size(); i++) {
			JsonObject project = projects.getJsonObject(i);						
			if(debug){
				LOGGER.log(Level.INFO, "Pretty printing project info...");
				prettyPrint(project);
			} 
			
			int projectId = project.getInt("id");
			String path = project.getString("path_with_namespace");
			String projectDescAndId =  path + " (id " + projectId + ")";
			LOGGER.info("Project " + projectDescAndId);

			if(Config.UPDATE_BADGES){
				// Rimuovo i badges esistenti dal progetto
				List<Integer> results = removeBadges(projectId);
				if(results.isEmpty()) {
					LOGGER.warning("removeBadges() returns no results for project " + projectDescAndId + ". That could be a BUG.");
				}else {
					LOGGER.log(Level.INFO, "#" + results.size() + " badges deleted successfully from project " + projectDescAndId);
				}

				// Aggiungo i nuovi badges al progetto
				List<JsonObject> badges = createBadges(project);
				if(!badges.isEmpty()) {
					List<Integer> results2 = insertBadges(projectId, badges);
					if(results2.isEmpty()) {
						LOGGER.severe("insertBadges() returns no results for project " + projectDescAndId);
					}else {
						LOGGER.log(Level.INFO, "#" + results2.size() + " badges added to project " + projectDescAndId);
					}
				}
			}

			JsonArray pipelines = getPipelines(projectId, Config.DEFAULT_BRANCH);
			if(Config.DELETE_PIPELINE) {			  
				if(pipelines!=null && !pipelines.isEmpty()) {
					List<Integer> results3 = removePipelines(projectId, pipelines);
					LOGGER.log(Level.INFO, "#" + results3.size() + " pipelines removed successfully from project " + projectDescAndId);
				}
			}else {
				LOGGER.log(Level.INFO, "#" + pipelines.size() + " pipelines found in project " + projectDescAndId);
			}
		}
	}

	private void prettyPrint(JsonObject jsonObject) {
		try {
			String prettyString = prettyPrintFormat(jsonObject);
			System.out.println(prettyString);				
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Can't print json", ex);
		} 
	}

	private String prettyPrintFormat(JsonObject jsonObject) throws IOException {
		String jsonString = null; 
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);		        
		JsonWriterFactory writerFactory = Json.createWriterFactory(config);		        	 
		try(Writer writer = new StringWriter()) {
		    writerFactory.createWriter(writer).write(jsonObject);
		    jsonString = writer.toString();
		}
		return jsonString;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#list-project-pipelines	
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @return
	 */
	private JsonArray getPipelines(int projectId, String branch) {
		JsonArray pipelines = null;	
		String route = "projects/" + projectId + "/pipelines" + Config.PER_PAGE + "&ref=" + branch;
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if(statusCode!=Status.OK.getStatusCode()) {
			String error = "Impossibile recuperare l'elenco delle pipeline per il progetto " + projectId + ". Status code: " + statusCode;
			LOGGER.severe(error);
			if(Config.FAIL_FAST) {
				System.exit(1);
			}else {
				this.errors.add(error);
			}
		}else {
			String jsonString = response.readEntity(String.class);		
			pipelines = readArray(jsonString); 
		}
		return pipelines;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#delete-a-pipeline
	 * 
	 * Se si volesse verificare su disco la presenza o meno degli artifacts relativi a una pipeline, 
	 * bisogna sapere che il percorso dello storage di Gitlab può variare in relazione alla modalità di installazione 
	 * Ad esempio può essere: /var/opt/gitlab/gitlab-rails/shared/artifacts/<year_month>/<project_id?>/<jobid>
	 * @see https://gitlab.com/gitlab-org/gitlab-ce/blob/master/doc/administration/job_artifacts.md#storing-job-artifacts
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param pipelines JSONArray
	 * @return
	 */
	private List<Integer> removePipelines(int projectId, JsonArray pipelines) {
		List<Integer> pipelineIds = new ArrayList<Integer>();		
		if(pipelines.size()<=Config.SKIP_PIPELINES_QNT) {
			LOGGER.info("#" + pipelines.size() + " pipelines found <= " + Config.SKIP_PIPELINES_QNT + ", so there is no pipelines to delete for project id " + projectId);
		}else {
			LOGGER.info("#" + pipelines.size() + " pipelines found for project id " + projectId);
			for (int j = 0 + Config.SKIP_PIPELINES_QNT ; j < pipelines.size(); j++) {
				String msg = "Removing " + (j+1-Config.SKIP_PIPELINES_QNT) + "/" + (pipelines.size()-Config.SKIP_PIPELINES_QNT) + " pipeline for project " + projectId + " (I will keep the last " + Config.SKIP_PIPELINES_QNT + "/" + pipelines.size() + " pipelines)";
				LOGGER.info(msg);
				JsonObject pipeline = pipelines.getJsonObject(j);
				int pipelineId = pipeline.getInt("id");	// The ID of a pipeline									
				String route3 = "projects/" + projectId + "/pipelines/" + pipelineId; 		
				Response response3 = doDelete(route3);
				int statusCode = response3.getStatus();
				if(statusCode==Status.NO_CONTENT.getStatusCode()) {
					pipelineIds.add(pipelineId);
				}else {
					String error = "Impossibile eliminare la pipeline " + pipelineId + " del progetto " + projectId + " (" + getProjectUrl(projectId) + "). Status code: " + statusCode;
					error = error + " (Nota che l'errore si potrebbe manifestare quando la pipeline è in esecuzione oppure un proceddo è archiviato e quindi è in sola lettura).";
					LOGGER.severe(error);
					this.errors.add(error);	
					break; // satrebbe inutile continuare
				}
			}
		}
		return pipelineIds;
	}

	private String getProjectUrl(int projectId) {
		return this.gitlabHost + "/projects/" + projectId;
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
		JsonArray badges = getBadgesList(projectId);
		for (int i = 0; i < badges.size(); i++) {						
			JsonObject object = badges.getJsonObject(i);
			int badgeId = object.getInt("id");			
			try {
				String route = "projects/" + projectId + "/badges/" + badgeId;
				Response response2 = doDelete(route);
				int statusCode = response2.getStatus();
				if(statusCode==Status.NO_CONTENT.getStatusCode()) {
					// OK
					// LOGGER.info("No content per badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode);
					badgeIds.add(badgeId);
				}else {
					String error = "Impossibile eliminare il badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode;
					LOGGER.severe(error);						 
					if(Config.FAIL_FAST) {
						System.exit(1);
					}else {
						this.errors.add(error);
						break;
					}						 
				}
			} catch (Exception e) {
				LOGGER.severe("ERRORE: " + e.getMessage());
				throw e;
			}
		}


		return badgeIds;
	}

	/**
	 * 
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#list-all-badges-of-a-project
	 * 
	 * @param projectId
	 * @return
	 */
	private JsonArray getBadgesList(int projectId) {
		JsonArray badges = null;
		String route = "projects/" + projectId + "/badges";
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if(statusCode!=Status.OK.getStatusCode()) {
			String msg = "Impossibile recuperare l'elenco dei badge per il progetto " + projectId + ". Status code: " + statusCode;
			LOGGER.severe(msg);
			if(Config.FAIL_FAST) {
				System.exit(1);
			}else {
				this.errors.add(msg);
			}
		}else {
			String jsonString = response.readEntity(String.class);
			badges = readArray(jsonString); 
			LOGGER.info("#" + badges.size() + " badges exist for project id " + projectId);
		}
		return badges;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#add-a-badge-to-a-project
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param badges
	 * @return
	 */
	private List<Integer> insertBadges(int projectId, List<JsonObject> badges) {
		List<Integer> badgeIds = new ArrayList<Integer>();
		for (JsonObject badge : badges) {	
			String route = "projects/" + projectId + "/badges";
			Response response = doPost(route, Entity.json(badge.toString()));
			int statusCode = response.getStatus();
			if(statusCode==Status.CREATED.getStatusCode()) {
				String jsonString = response.readEntity(String.class);			
				JsonObject object = readObject(jsonString);
				int badgeId = object.getInt("id");
				badgeIds.add(badgeId);
			}else {
				String msg = "Impossibile aggiungere il badge " + badge.toString() + ". Status code: " + statusCode;
				LOGGER.severe(msg);
				if(Config.FAIL_FAST) {
					System.exit(1);
				}else {
					this.errors.add(msg);
					break;
				}								
			}
		}
		return badgeIds;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html
	 * 
	 * I badge sono associati al progetto e sono indipendenti dal branch.
	 * Tuttavia devo esaminare dei file di un particolare branch per poter determinare quali badge 
	 * bisogna aggiungere al progetto
	 * 
	 * @param object
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	private List<JsonObject> createBadges(JsonObject object) { // TODO: rinominare object in project
		final String branch = Config.DEFAULT_BRANCH;
		List<JsonObject> badges = new ArrayList<>();

		// Determino l'id del progetto 
		int projectId = object.getInt("id");
		// Determino altri valori come il nome ed il gruppo del progetto
		// In alternativa alla lettura dei dati dal parametro "object", potrei utilizzare le variabili gitlab %{project_path} e %{default_branch} nella costruzione del link al pipeline badge.
		// Ad esempio un link valido sarebbe:
		// https://example.gitlab.com/%{project_path}/badges/%{default_branch}/badge.svg 
		// (vedi https://gitlab.iubar.it/help/user/project/badges). 
		// Nota che in Gitlab, il valore di %{project_path} è scollegato dal valore di <project_name> anche se di default %{project_path} assume il seguente formato <nome_gruppo>/<project_name>.			

/*
Esempio oggeto "object" (see https://docs.gitlab.com/ee/api/projects.html#list-all-projects)
....		
   "name": "Diaspora Client",
    "name_with_namespace": "Diaspora / Diaspora Client",
    "path": "diaspora-client",
    "path_with_namespace": "diaspora/diaspora-client",
    "namespace": {
      "id": 3,
      "name": "Diaspora",
      "path": "diaspora",
      "kind": "group",
      "full_path": "diaspora"
    },
....
*/
		String name = object.getString("name");
		JsonObject namespace = object.getJsonObject("namespace");
		String group = namespace.getString("path");
		String pathWithNamespace = object.getString("path_with_namespace");
		
		// TODO: sostituire: 
		// group + "/" + name
		// con: 
		// pathWithNamespace

		if(!isGitlabci(projectId, branch)) {
			LOGGER.warning("File " + Config.GITLAB_FILE + " assente per il progetto " + projectId);
		}else {
			String link = this.gitlabHost +"/" + group + "/" + name + "/commits/%{default_branch}";
			// https://gitlab.com/gitlab-org/gitlab-foss/issues/41174
			String  image = this.gitlabHost + "/" + group + "/" + name + "/badges/%{default_branch}/pipeline.svg";			
			JsonObjectBuilder  builder = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder.build());
		}

		if(!isSonar(projectId, branch)) {
			LOGGER.warning("File " + Config.SONAR_FILE + " assente per il progetto " + projectId);
		}else {

			String sonarProjectContent = getFileContent(projectId, Config.SONAR_FILE, branch);
			Properties properties = PropertiesUtils.parsePropertiesString(sonarProjectContent); // sonar.projectKey è un file di configurazione nel formato Java Properties
			Object obj = properties.get("sonar.projectKey");
			String sonarProjectKeyActual = null;
			if(obj!=null) {
				sonarProjectKeyActual = (String) obj;
			}
			if(sonarProjectKeyActual==null) {
				LOGGER.severe("Impossibile recuperare il valore di sonar.projectKey");
				System.exit(1);
			}
			String sonarProjectKeyExpected = group + ":" + name; // il carattere ":" equivale a "%3A"
			if(!sonarProjectKeyExpected.equals(sonarProjectKeyActual)) {
				LOGGER.warning("Il valore di sonar.projectKey del progetto " + projectId + " non rispetta le nostre linee guida. Valore atteso: " + sonarProjectKeyExpected + " valore attuale: " + sonarProjectKeyActual);
			}

 
			String link = this.sonarHost + "/dashboard?id=" + sonarProjectKeyActual;
			String image = this.sonarHost + "/api/badges/gate?key=" + sonarProjectKeyActual;
			JsonObjectBuilder  builder1 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder1.build());
			link = this.sonarHost + "/component_measures?metric=Reliability&id=" + sonarProjectKeyActual;	
			image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=bugs";
			JsonObjectBuilder  builder2 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder2.build());
			link = this.sonarHost + "/component_measures?metric=code_smells&id=" + sonarProjectKeyActual;			
			image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=code_smells";
			JsonObjectBuilder  builder3 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder3.build());
			link = this.sonarHost + "/component_measures?metric=ncloc_language_distribution&id=" + sonarProjectKeyActual;
			image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=ncloc_language_distribution";
			JsonObjectBuilder  builder4 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder4.build());
			link = this.sonarHost + "/component_measures?metric=classes&id=" + sonarProjectKeyActual;
			image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=classes";
			JsonObjectBuilder  builder5 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder5.build());
			link = this.sonarHost + "/component_measures?metric=functions&id=" + sonarProjectKeyActual;
			image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=functions";
			JsonObjectBuilder  badge6 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(badge6.build());
		}

		return badges;
	}

	@Override
	protected URI getBaseURI() {
		return UriBuilder.fromUri(this.gitlabHost + "/api/" + Config.GITLAB_API_VER + "/").build();
	}

	private boolean isGitlabci(int projectId, String branch){
		return isFile(projectId, Config.GITLAB_FILE, branch);
	}

	private boolean isSonar(int projectId, String branch){
		return isFile(projectId, Config.SONAR_FILE, branch);
	}	

	/**
	 * 
	 * @see see https://docs.gitlab.com/ee/api/repositories.html#list-repository-tree
	 * 
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param fileName
	 * @return
	 */
	private boolean isFile(int projectId, String fileName, String branch){
		boolean b = false;		
		String route = "projects/" + projectId + "/repository/tree" + Config.PER_PAGE + "&ref=" + branch;
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (response.getStatus() != Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile determinare se il file " + fileName + " è parte del progetto. Status code: " + statusCode);
		}else {
			String jsonString = response.readEntity(String.class);	
			JsonArray files = readArray(jsonString); 
 	
			for (int i=0; i<files.size(); i++) {
				 JsonObject jsonObject = files.getJsonObject(i);
				String _fileName = jsonObject.getString("name");			
				if (_fileName.equals(fileName)) {
					b=true;
					break;
				}
			}
		}
		return b;		
	}

	public Set<String> getErrors() {
		return this.errors;
	}

	/**
	 * 
	 * @see https://docs.gitlab.com/ee/api/repository_files.html#get-file-from-repository
	 * 
	 * @param projectId
	 * @param filePath Url encoded full path to new file
	 * @return
	 */
	private String getFileContent(int projectId, String filePath, String branch) {	
		String content = "";
		int statusCode = 0;
		Response response2 = null;
		try {
			String filePathEncoded = URLEncoder.encode(filePath, "UTF-8");
			String route = "projects/" + projectId + "/repository/files/" + filePathEncoded + "?ref=" + branch;
			response2 = doGet(route);
			statusCode = response2.getStatus();			
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		if(statusCode!=Status.OK.getStatusCode()) {
			String msg = "Impossibile recuperare il contenuto del file " + filePath + " per il progetto " + projectId + ". Status code: " + statusCode;
			if(Config.FAIL_FAST) {
				System.exit(1);
			} else {
				this.errors.add(msg);
			}
		}else {
			String jsonString = response2.readEntity(String.class);		
			JsonObject jsonObject = readObject(jsonString);
			// Integer size = jsonObject.getInt("size"); // Integer/int max value is 0x7fffffff = 2.147.483.647			
			JsonNumber size = jsonObject.getJsonNumber("size"); 
			String base64 = jsonObject.getString("content");
			byte[] decoded = Base64.getDecoder().decode(base64);
			try {
				content = new String(decoded, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return content;
	}

	@Override
	protected Builder getBuilder(WebTarget target) {				
		Builder builder = super.getBuilder(target).header("PRIVATE-TOKEN", this.gitlabToken);
		return builder;
	}

}

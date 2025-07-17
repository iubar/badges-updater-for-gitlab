package it.iubar.badges;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.jersey.client.ClientResponse;

import it.iubar.badges.Config.UpdateType;

/**
 * @see https://docs.gitlab.com/ee/api/project_badges.html
 */
public class BadgesUpdater extends AbstractUpdater implements IUpdater {

	private static final Logger LOGGER = Logger.getLogger(BadgesUpdater.class.getName());

	public BadgesUpdater(Properties config) {
		super(config);
		this.client = factoryClient(this.gitlabToken);
	}

	@Override
	public void run() {
		JsonArray projects = getProjects();

		// Effettuo una serie di operazioni su tutti i progetti
		for (int i = 0; i < projects.size(); i++) {
			JsonObject project = projects.getJsonObject(i);
			if (this.debug) {
				LOGGER.log(Level.INFO, "Pretty printing project info...");
				JsonUtils.prettyPrint(project);
			}

			int projectId = project.getInt("id");
			String path = project.getString("path_with_namespace");
			String projectDescAndId = path + " (id " + projectId + ")";
			LOGGER.info("Project " + projectDescAndId);

			// Rimuovo TUTTI i badges esistenti dal progetto
			List<Integer> results = removeBadges(projectId);
			if (results.isEmpty()) {
				LOGGER.warning("removeBadges() returns no results for project " + projectDescAndId + ". That could be a BUG.");
			} else {
				LOGGER.log(Level.INFO, "#" + results.size() + " badges deleted successfully from project " + projectDescAndId);
			}
			
			
			if (Config.UPDATE_BADGES == UpdateType.DELETE_AND_ADD) {
				// Aggiungo i nuovi badges al progetto
				List<JsonObject> badges = createBadges(project);
				if (!badges.isEmpty()) {
					List<Integer> results2 = insertBadges(projectId, badges);
					if (results2.isEmpty()) {
						LOGGER.severe("insertBadges() returns no results for project " + projectDescAndId);
					} else {
						LOGGER.log(Level.INFO, "#" + results2.size() + " badges added to project " + projectDescAndId);
					}
				}
			} 
		}
	}

	private boolean isGitlabci(int projectId, String branch) {
		return isFile(projectId, Config.GITLAB_FILE, branch);
	}

	private boolean isSonar(int projectId, String branch) {
		return isFile(projectId, Config.SONAR_FILE, branch);
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#list-all-badges-of-a-project
	 */
	private JsonArray getBadgesList(int projectId) {
		JsonArray badges = null;
		String route = "projects/" + projectId + "/badges";
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			String msg = "Impossibile recuperare l'elenco dei badge per il progetto " + projectToUrl(projectId) + ". Status code: " + statusCode;
			logError(msg, response);
		} else {
			String jsonString = response.readEntity(String.class);
			badges = JsonUtils.readArray(jsonString);
			LOGGER.info("#" + badges.size() + " badges exist for project id " + projectId);
		}
		return badges;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#add-a-badge-to-a-project
	 *
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param badges
	 */
	private List<Integer> insertBadges(int projectId, List<JsonObject> badges) {
		List<Integer> badgeIds = new ArrayList<Integer>();
		for (JsonObject badge : badges) {
			String route = "projects/" + projectId + "/badges";
			Response response = doPost(route, Entity.json(badge.toString()));
			int statusCode = response.getStatus();
			if (statusCode == Status.CREATED.getStatusCode()) { // OK
				String jsonString = response.readEntity(String.class);
				JsonObject object = JsonUtils.readObject(jsonString);
				int badgeId = object.getInt("id");
				badgeIds.add(badgeId);
			} else {
				String error = "Impossibile aggiungere il badge " + badge.toString() + " per il progetto " + projectToUrl(projectId) + ". Status code: " + statusCode;
				LOGGER.severe(error);
				logError(response);
				if (Config.FAIL_FAST) {
					System.exit(1);
				} else {
					AbstractUpdater.errors.add(error);
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
	private List<JsonObject> createBadges(JsonObject project) {
		String branch = project.getString("default_branch");
		List<JsonObject> badges = new ArrayList<>();

		// Determino l'id del progetto		
		int projectId = project.getInt("id");
		
		boolean archived = project.getBoolean("archived");
		
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
		String name = project.getString("name");
		JsonObject namespaceJsonObj = project.getJsonObject("namespace");
		String namespacePath = namespaceJsonObj.getString("path");
		String pathWithNamespace = project.getString("path_with_namespace");

		if (!isGitlabci(projectId, branch)) {
			LOGGER.warning("File " + Config.GITLAB_FILE + " assente per il progetto " + projectId);
		} else {
			// see https://gitlab.com/gitlab-org/gitlab-foss/issues/41174
			//String link = this.gitlabHost + "/" + pathWithNamespace + "/commits/%{default_branch}";
			String link = this.gitlabHost + "/" + pathWithNamespace + "/-/pipelines";
			String image = this.gitlabHost + "/" + pathWithNamespace + "/badges/%{default_branch}/pipeline.svg";
			JsonObjectBuilder builder = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder.build());
		}

		if (!isSonar(projectId, branch)) {
			LOGGER.warning("File " + Config.SONAR_FILE + " assente per il progetto " + projectId);
		} else {
			
			if(Config.ADD_SONAR_BADGES) {
				
			String sonarProjectContent = getFileContent(projectId, Config.SONAR_FILE, branch);
			Properties properties = PropertiesUtils.parsePropertiesString(sonarProjectContent); // sonar.projectKey è un file di configurazione nel formato Java Properties
			Object obj = properties.get("sonar.projectKey");
			String sonarProjectKeyActual = null;
			if (obj != null) {
				sonarProjectKeyActual = (String) obj;
			}
			if (sonarProjectKeyActual == null) {
				LOGGER.severe("Impossibile recuperare il valore di sonar.projectKey");
				System.exit(1); // TODO: meglio lanciare eccezione ?
			}
			String sonarProjectKeyExpected = namespacePath + ":" + name; // il carattere ":" equivale a "%3A"
			if (!sonarProjectKeyExpected.equals(sonarProjectKeyActual)) {
				LOGGER.warning(
					"Il valore di sonar.projectKey del progetto " +
					projectId +
					" non rispetta le nostre linee guida. Valore atteso: " +
					sonarProjectKeyExpected +
					" valore attuale: " +
					sonarProjectKeyActual
				);
			}


				
			String link = this.sonarHost + "/dashboard?id=" + sonarProjectKeyActual;
			String image = this.sonarHost + "/api/badges/gate?key=" + sonarProjectKeyActual;
			JsonObjectBuilder builder1 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(builder1.build());
			

				link = this.sonarHost + "/component_measures?metric=Reliability&id=" + sonarProjectKeyActual;
				image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=bugs";
				JsonObjectBuilder builder2 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
				badges.add(builder2.build());
				
				link = this.sonarHost + "/component_measures?metric=code_smells&id=" + sonarProjectKeyActual;
				image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=code_smells";
				JsonObjectBuilder builder3 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
				badges.add(builder3.build());
				
				link = this.sonarHost + "/component_measures?metric=ncloc_language_distribution&id=" + sonarProjectKeyActual;
				image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=ncloc_language_distribution";
				JsonObjectBuilder builder4 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
				badges.add(builder4.build());
				
				link = this.sonarHost + "/component_measures?metric=classes&id=" + sonarProjectKeyActual;
				image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=classes";
				JsonObjectBuilder builder5 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
				badges.add(builder5.build());
				
				link = this.sonarHost + "/component_measures?metric=functions&id=" + sonarProjectKeyActual;
				image = this.sonarHost + "/api/badges/measure?key=" + sonarProjectKeyActual + "&metric=functions";
				JsonObjectBuilder badge6 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
				badges.add(badge6.build());
			}		
		}
		
		
		if(Config.ADD_VERSION_BADGES) {
			String link = this.gitlabHost + "/" + pathWithNamespace;
			String image = "https://img.shields.io/badge/version-" + "dev" + "-red";
			VersionInfo versionInfo = getVersion(projectId, branch);
			if(versionInfo!=null) {
				String verNumber = versionInfo.getNumber();
				String verFile = versionInfo.getFilename();						
				link = this.gitlabHost + "/" + pathWithNamespace + "/-/blob/master/" + verFile + "?ref_type=heads";
				image = "https://img.shields.io/badge/version-" + verNumber + "-blue";
			}
			JsonObjectBuilder badge6 = Json.createObjectBuilder().add("link_url", link).add("image_url", image);
			badges.add(badge6.build());
		}

		return badges;
	}

	
	public enum Language {
	    EXPO("app.json"), JAVASCRIPT("package.json"), PHP("composer.json"), JAVA("pom.xml");

		private String filename;

		Language(String filename) {
			this.filename = filename;
		}
		
		public String getFilename() {
			return filename;
		}
 
	}
	
	private VersionInfo getVersion(int projectId, String branch) {
		VersionInfo versionInfo = null;
        for (Language lang : Language.values()) {
        	String fileName = lang.getFilename();
            boolean b = isFile(projectId, fileName, branch);
            if(b) {
            	String content = getFileContent(projectId, fileName, branch);
            	String number = parseText(content, lang);
            	if(number!=null) {
            		versionInfo = new VersionInfo(fileName, number);
            	}
            	break;
            }
        }
		return versionInfo;
	}
 
	private String parseText(String content, Language lang) {	
		String version = null;
		Matcher matcher = null;
		switch (lang) {
			case EXPO:
		        // Regex per trovare il valore di "version" all'interno di "app.json"
		        Pattern patternAppJson = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
		        matcher = patternAppJson.matcher(content);

		        if (matcher.find()) {
		            version = matcher.group(1);
		            System.out.println("Version trovata: " + version);
		        } else {
		            System.out.println("Version non trovata.");
		        }
				break;
			case JAVASCRIPT:
		        // Regex per trovare il valore del campo "version" all'interno di "package.json"
		        Pattern patternPackage = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
		        matcher = patternPackage.matcher(content);

		        if (matcher.find()) {
		            version = matcher.group(1);
		            System.out.println("Version trovata: " + version);
		        } else {
		            System.out.println("Version non trovata.");
		        }
				break;
			case PHP:
		        // Regex per trovare il valore di "version" all'interno di "composer.json"
		        Pattern patternComposer = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
		        matcher = patternComposer.matcher(content);

		        if (matcher.find()) {
		            version = matcher.group(1);
		            System.out.println("Version trovata: " + version);
		        } else {
		            System.out.println("Version non trovata.");
		        }
				break;
			case JAVA:
		        // Espressione regolare per catturare il contenuto del tag <version> fuori da <dependency> nel file "pom.xml"
		        Pattern patternPom = Pattern.compile("<version>(.*?)</version>");
		        matcher = patternPom.matcher(content);

		        while (matcher.find()) {
		            version = matcher.group(1);
		            System.out.println("Version trovata: " + version);
		            break; // prende solo la prima <version> trovata (quella del progetto)
		        }
				break;				
			default:
				break;
		}
		return version;
	}

	/**
	 *
	 * @see https://docs.gitlab.com/ee/api/project_badges.html#remove-a-badge-from-a-project
	 *
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 *
	 * Elimina tutti i badges configurati per il progetto identificato da projectId
	 *
	 */
	private List<Integer> removeBadges(int projectId) {
		List<Integer> badgeIds = new ArrayList<Integer>();
		JsonArray badges = getBadgesList(projectId);
		for (int i = 0; i < badges.size(); i++) {
			JsonObject object = badges.getJsonObject(i);
			int badgeId = object.getInt("id");
			try {
				String route = "projects/" + projectId + "/badges/" + badgeId;
				Response response = doDelete(route);
				int statusCode = response.getStatus();
				if (statusCode == Status.NO_CONTENT.getStatusCode()) { // OK
					// LOGGER.info("No content per badge " + badgeId + " del progetto " + projectId + ". Status code: " + statusCode);
					badgeIds.add(badgeId);
				} else {
					String error = "Impossibile eliminare il badge " + badgeId + " del progetto " + projectToUrl(projectId) + ". Status code: " + statusCode;
					LOGGER.severe(error);
					logError(response);
					if (Config.FAIL_FAST) {
						System.exit(1);
					} else {
						AbstractUpdater.errors.add(error);
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
	 * @see see https://docs.gitlab.com/ee/api/repositories.html#list-repository-tree
	 *
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param fileName
	 * @param branch
	 */
	private boolean isFile(int projectId, String fileName, String branch) {
		boolean b = false;
		String route = "projects/" + projectId + "/repository/tree" + Config.PER_PAGE + "&ref=" + branch;
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (response.getStatus() != Status.OK.getStatusCode()) {
			LOGGER.severe("Impossibile determinare se il file " + fileName + " è parte del progetto. Status code: " + statusCode);
			// TODO: valutare se cambiare il messaggio in "Il file  " + fileName + " non è parte del progetto"
		} else {
			String jsonString = response.readEntity(String.class);
			JsonArray files = JsonUtils.readArray(jsonString);

			for (int i = 0; i < files.size(); i++) {
				JsonObject jsonObject = files.getJsonObject(i);
				String _fileName = jsonObject.getString("name");
				if (_fileName.equals(fileName)) {
					b = true;
					break;
				}
			}
		}
		return b;
	}

	/**
	 *
	 * @see https://docs.gitlab.com/ee/api/repository_files.html#get-file-from-repository
	 * 
	 * Allows you to receive information about file in repository like name, size, and content. File content is Base64 encoded. You can access this endpoint without authentication, if the repository is publicly accessible.
	 * 
	 * Nota che esiste anche soluzione alternativa : https://docs.gitlab.com/api/repository_files/#get-raw-file-from-repository (GET /projects/:id/repository/files/:file_path/raw )
	 * 
	 *
	 * @param projectId
	 * @param filePath Url encoded full path to new file
	 * @param branch
	 */
	private String getFileContent(int projectId, String filePath, String branch) {
		String content = "";
		int statusCode = 0;
		Response response = null;
		try {
			String filePathEncoded = URLEncoder.encode(filePath, "UTF-8");
			String route = "projects/" + projectId + "/repository/files/" + filePathEncoded + "?ref=" + branch;
			response = doGet(route);
			statusCode = response.getStatus();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}

		if (statusCode != Status.OK.getStatusCode()) {
			String msg =
				"Impossibile recuperare il contenuto del file " + filePath + " per il progetto " + projectToUrl(projectId) + ". Status code: " + statusCode;
			logError(msg, response);
		} else {
			String jsonString = response.readEntity(String.class);
			JsonObject jsonObject = JsonUtils.readObject(jsonString);
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
}

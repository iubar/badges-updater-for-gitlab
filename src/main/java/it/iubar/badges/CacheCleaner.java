package it.iubar.badges;

import java.io.UnsupportedEncodingException;

import jakarta.ws.rs.client.ClientBuilder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.iubar.badges.Config.UpdateType;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @see https://docs.gitlab.com/ee/api/project_badges.html
 */
public class CacheCleaner extends AbstractUpdater implements IUpdater {

	private static final Logger LOGGER = Logger.getLogger(CacheCleaner.class.getName());

	public CacheCleaner(Properties config) {
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

			// Pulisco la cache
			String result = clearProjectCache(projectId);
 			LOGGER.log(Level.INFO, "#" + result + " cache deleted successfully from project " + projectDescAndId);
			 					  
		}
	}
 

	/**
	 * Non esiste un endpoint REST equivalente per questo bisogna usare la mutation GraphQL runnerCacheClear
	 * 
	 * @param projectId
	 */
	public   String clearProjectCache(long projectId) {
						
        try {        	          
            String query = "mutation RunnerCacheClear($projectId: ProjectID!) { "
                         + "runnerCacheClear(input: { projectId: $projectId }) { errors } }";

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("projectId", "gid://gitlab/Project/" + projectId);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", query);
            body.put("variables", variables);
 
            // PROBLEMA: Jersey da solo sa serializzare String, byte[], InputStream e poco altro, ma non una Map in application/json. Serve un modulo che fornisca un MessageBodyWriter per JSON.
            // Il modulo jersey-media-json-processing è presente nelle dipendenze.
            // Ma il punto è che quel modulo sa serializzare solo i tipi di jakarta.json (JsonObject, JsonArray, JsonStructure), non le Map. Per questo Jersey non trova un MessageBodyWriter per LinkedHashMap
            // SOLUZIONE 1: Non richiede dipendenze
            // Json.createObjectBuilder(Map<String, Object>) esiste da JSON-P 1.1 e gestisce String, numeri, Boolean, null, Map annidate e Collection. Va bene quindi per un body GraphQL            
            JsonObject json = Json.createObjectBuilder(body).build();
            Response response = doMutate(Entity.entity(json, MediaType.APPLICATION_JSON));
            // SOLUZIONE 2: JSON-B consente la serializzazione di oggetti diversi da quelli indicati sopra
            /*
        <dependency>
            <groupId>org.glassfish.jersey.media</groupId>
            <artifactId>jersey-media-json-binding</artifactId>
            <version>${jersey.client.version}</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse</groupId>
            <artifactId>yasson</artifactId>
            <version>3.0.4</version>
        </dependency>
        */                               
            // Response response = doMutate(Entity.entity(body, MediaType.APPLICATION_JSON));
            
            String result = response.readEntity(String.class);
            System.out.println("Status: " + response.getStatus());
            System.out.println("Response: " + result);

            response.close();
            return result;
        } finally {
            client.close();
        }
    }

	 

	 
}

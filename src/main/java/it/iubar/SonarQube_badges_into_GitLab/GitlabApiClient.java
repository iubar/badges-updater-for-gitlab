package it.iubar.SonarQube_badges_into_GitLab;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
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
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public class GitlabApiClient {


	private static final Logger LOGGER = Logger.getLogger(GitlabApiClient.class.getName());

	private static final int MAX_PROJECT_PER_PAGE = 200;  
	
	private String sonarHost = null;
	private String gitlabHost = null;
	private String gitlabToken = null;
	private Properties config = null;
	
	public void setProperties(Properties config)
	{
		this.config = config;
	}

	public void loadConfig() {

		this.sonarHost = String.valueOf(config.get("sonar.host"));
		this.gitlabHost = String.valueOf(config.get("gitlab.host"));
		this.gitlabToken = String.valueOf(config.get("gitlab.token"));

		LOGGER.info("Ho letto sonarHost = " + this.sonarHost);
		LOGGER.info("Ho letto gitlabHost = " + this.gitlabHost);
		LOGGER.info("Ho letto gitlabToken = " + this.gitlabToken);

	}
	
	public static Client factoryClient() {
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
			e.printStackTrace();			
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		ClientBuilder builder = ClientBuilder.newBuilder();
		Client client = builder.sslContext(sslContext).build();
		return client;
	}

	public void run() {
		
		loadConfig();
		
		// Creo la variabile per la rotta
		String route = "projects?per_page=" + MAX_PROJECT_PER_PAGE;

		//Creo il client utilizzando la funzione factoryClient() che ignora la validit� del certificato SSL
		Client client = factoryClient();

		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base, pi� la rotta
		WebTarget target = client.target(getBaseURI()+route);

		//Effettuo la chiamata GET
		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", this.gitlabToken).get(Response.class);
		
		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();
		LOGGER.info("Lettura dei progetti: CODE(" + statusCode +")");
		
		//Dalla chiamata GET prendo il file JSON che ci restituisce e lo scrivo in una stringa
		String json = response.readEntity(String.class);

		//Il file JSON � un array di altri oggetti json, per questo lo vado a mettere dentro un oggetto JSONArray
		JSONArray jsonArray = new JSONArray(json);
		
		//Stampo il numero di oggetti JSON presenti nell'array, dato che un oggetto corrisponde ad un progetto,
		//il valore stampato corrisponde appunto al numeri di progetti recuperati dalla chiamata GET
		LOGGER.info("Numero di progetti: " + jsonArray.length());
		
		//Ciclo FOR utilizzatto per effettuare una seire di operazioni a tutti i progetti
		for (int i = 0; i < jsonArray.length(); i++) {
			
			//Dall'array JSON estraggo l'ennesimo oggetto JSON
			JSONObject object = jsonArray.getJSONObject(i);
			
			//Estraggo il valore della KEY "id", ovvero l'ID del progetto
			int id = object.getInt("id");
			
			//Elimino i badges precedenti del progetto, passo alla funzione il suo ID e il TOKEN per l'autorizzazione
			doDelete(id);
			
			//Faccio il POST con i 7 badges relativi al progetto, passo alla funzione l'ID del progetto e la mappa dei links generati dalla funzione createBadges
			doPost(id, createBadges(object));
			
		}

	}
	
	private void doDelete (int id) {
	
		Client client = factoryClient();
		//Creo il link per la richiesta
		WebTarget target = client.target(getBaseURI());
		//try {
			//Continuo la richiesta per ricevere i badges di un progetto
			Response response = target.path("projects")
					.path(""+id)
					.path("badges")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", this.gitlabToken)
					.get(Response.class);
			//Inserisco i dati in un JSONArray
			String json = response.readEntity(String.class);
			JSONArray badges = new JSONArray(json);
			//Elimino i badges inserendo nella chiamata l'id di un badges
			for (int i = 0; i < badges.length(); i++) {						
					JSONObject object = badges.getJSONObject(i);
					int id_badge = object.getInt("id");
					String id_badgeStr=""+id_badge;
					
							try {
								WebTarget webTarget = client.target(getBaseURI()+"projects/"+id+"/badges/"+id_badgeStr);
								Response response2 = webTarget.request().accept(MediaType.APPLICATION_JSON).header("PRIVATE-TOKEN", gitlabToken).delete();
		
							} catch (Exception e) {
								LOGGER.severe("Errore: " + e.getMessage());
							}
			}
			LOGGER.info("Eliminati i badges del progetto: " + id);
	}
	
	
	private void doPost(int id, Map<String,List<String>> badges) {
		
		List<String> links = badges.get("links");
		List<String> images = badges.get("images");
		//Creo il client utilizzando la funzione factoryClient() che ignora la validit� del certificato SSL
		Client client = factoryClient();
		
		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base
		WebTarget target = client.target(getBaseURI());
		
		//Creo un ciclo FOR per i 7 badges
		
		
		for (int i=0; i < links.size() ; i++)
		{
			//Creo un oggetto JSON dove metto i links dell'ennesimo badge dalle ArrayLists
			JSONObject badge = new JSONObject()
					.put("link_url", links.get(i))
		            .put("image_url", images.get(i));
			
			// Faccio il post passando l'oggetto JSON sopra creato convertendolo in stringa
			Response response = target.path("projects").path(""+id).path("badges").request().accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", this.gitlabToken).post(Entity.json(badge.toString()));
			
		}
		LOGGER.info("Inseriti i badges del progetto: " + id);
		
	}
		
	private Map<String,List<String>> createBadges(JSONObject object)
	{
		// Creo un mappa con due chiavi, una per le images e una per i link, come valore avranno le relative liste
		Map<String,List<String>> map=new HashMap<String,List<String>>();
		
		//Estraggo l'id del progetto e altri valori come il suo nome ed il gruppo
		int id = object.getInt("id");	
		String name = object.getString("name");
		JSONObject namespace = object.getJSONObject("namespace");
		String group = namespace.getString("path");
		
		//genero la lista delle images
		List<String> images = new ArrayList<String>();
		if(isGitlabci(id)) {
		images.add(this.gitlabHost + "/" + group + "/" + name + "/badges/master/build.svg");
		}
		if(isSonar(id)) {
		images.add(this.sonarHost + "/api/badges/gate?key=" + group + ":" + name);
		images.add(this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=bugs");
		images.add(this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=code_smells");
		images.add(this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=ncloc_language_distribution");
		images.add(this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=classes");
		images.add(this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=functions");
		}
		
		//inserisco la lista appena creata come valore della prima chiave nella mappa
		map.put("images",images);
		
		//genero la lista dei links
		List<String> links = new ArrayList<String>();
		if(isGitlabci(id)) {
		links.add("https://" + this.gitlabHost +"/" + group + "/" + name + "/commits/master");
		}
		if(isSonar(id)) {
		links.add(this.sonarHost + " /dashboard?id=" + group + ":" + name);
		links.add(this.sonarHost + "/component_measures/domain/Reliability?id=" + group + ":" + name);
		links.add(this.sonarHost + "/component_measures/domain/Maintainability?id=" + group + ":" + name);
		links.add(this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		links.add(this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		links.add(this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		}
		
		//inserisco la lista appena creata come valore della seconda chiave nella mappa
		map.put("links",links);
		
		return map;
	}
	


	private URI getBaseURI() {
		return UriBuilder.fromUri("https://" + this.gitlabHost + "/api/v4/").build();
	}
	
	private boolean isGitlabci(int id) {
		return isFile(id, ".gitlab-ci.yml");
	}
	private boolean isSonar(int id) {
		return isFile(id, "sonar-project.properties");
	}	
	private boolean isFile(int id, String filename) {
		boolean b = false;
		 Client client = factoryClient();
		//creo il link per la richiesta
		WebTarget target = client.target(getBaseURI());
		//invio la richiesta di get per avere il tree di un progetto	
		Response response = target.path("projects")
					.path(""+id)
					.path("repository")
					.path("tree")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", this.gitlabToken)
					.get(Response.class);
			String json = response.readEntity(String.class);
			JSONArray files = new JSONArray(json);
			for (int i=0; i<files.length(); i++) {
				JSONObject object = files.getJSONObject(i);
				String file_name = object.getString("name");
				if (file_name.equals(filename)) {
					b=true;
				}
			}
			
		return b;		
		
	}	
}

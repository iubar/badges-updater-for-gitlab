package it.iubar.SonarQube_badges_into_GitLab;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
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
	
	private String sonarHost = "192.168.0.117:9000";
	private String gitlabHost = "gitlab.iubar.it";
	private String gitlabToken = "7ALqC2FSMxyV2zGe2EBu";


	public GitlabApiClient() {
		// Read config.properties
		ReadPropertiesFile objPropertiesFile = new ReadPropertiesFile();
		String configFile = "config.properties";	 
		//this.sonarHost = objPropertiesFile.readKey(configFile, "sonar.host");
		//this.gitlabHost = objPropertiesFile.readKey(configFile, "gitlab.host");
		//this.gitlabToken = objPropertiesFile.readKey(configFile, "gitlab.token");

		LOGGER.config("Ho letto sonarHost = " + this.sonarHost);
	}
	
	public Client factoryClient() {
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

		// Creo la variabile per la rotta
		String route = "projects?per_page=200";

		//Creo il client utilizzando la funzione factoryClient() che ignora la validit� del certificato SSL
		Client client = factoryClient();

		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base, pi� la rotta
		WebTarget target = client.target(getBaseURI()+route);

		//Effettuo la chiamata GET
		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", gitlabToken).get(Response.class);
		
		//Salvo in questa variabile il codice di risposta alla chiamata GET
		int statusCode = response.getStatus();
		
		//Dalla chiamata GET prendo il file JSON che ci restituisce e lo scrivo in una stringa
		String json = response.readEntity(String.class);

		//Il file JSON � un array di altri oggetti json, per questo lo vado a mettere dentro un oggetto JSONArray
		JSONArray jsonArray = new JSONArray(json);
		
		//Stampo il numero di oggetti JSON presenti nell'array, dato che un oggetto corrisponde ad un progetto,
		//il valore stampato corrisponde appunto al numeri di progetti recuperati dalla chiamata GET
		System.out.println("Numero di progetti: " + jsonArray.length());
		
		//Ciclo FOR utilizzatto per effettuare una seire di operazioni a tutti i progetti
		for (int i = 0; i < 1 /* jsonArray.length()*/ ; i++) {
			
			//Dall'array JSON estraggo l'ennesimo oggetto JSON
			JSONObject object = jsonArray.getJSONObject(i);
			
			//Dall'oggetto JSON, ovverro un singolo progetto, etraggo i seguenti valori:
			
			//Estraggo il valore della KEY "id", ovvero l'ID del progetto
			int id = object.getInt("id");
			
			//Creo due ArrayList, una con i links dei 7 badges e una con le relative images
			List<String> badgesImage = new ArrayList<String>();
			List<String> badgesLink = new ArrayList<String>();
			
			//Chiamo le due funzioni che generano i links in base al nome e al gruppo del progetto
			badgesImage = createBadgesImages(object);
			badgesLink = createBadgesLinks(object);

			//Elimino i badges precedenti del progetto, passo alla funzione il suo ID e il TOKEN per l'autorizzazione
			doDelete(id);
			
			//Faccio il POST con i 7 badges relativi al progetto, passo alla funzione il suo ID, il TOKEN per l'autorizzazione,
			//la lista dei links e quella delle images
			doPost(id, badgesLink, badgesImage);
			
		}

	}
	
	private void doDelete (int id) {
	
		Client client = factoryClient();

		WebTarget target = client.target(getBaseURI());
		//try {
			Response response = target.path("projects")
					.path(""+id)
					.path("badges")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", gitlabToken)
					.get(Response.class);
			String json = response.readEntity(String.class);
			LOGGER.info("Elimino i badges del progetto: " + id);
			JSONArray badges = new JSONArray(json);

			for (int i = 0; i < badges.length(); i++) {						
					JSONObject object = badges.getJSONObject(i);
					int id_badge = object.getInt("id");
					String id_badgeStr=""+id_badge;
					//System.out.print("\n\n"+id_badgeStr);
					
							try {
								WebTarget webTarget = client.target(getBaseURI()+"projects/"+id+"/badges/"+id_badgeStr);
								//System.out.print("\n"+webTarget);
								Response response2 = webTarget.request().accept(MediaType.APPLICATION_JSON).header("PRIVATE-TOKEN", gitlabToken).delete();
								//System.out.print("\n"+response2);
								
								
							} catch (Exception e) {
								LOGGER.severe("Errore: " + e.getMessage());
							}
			}
		
	}
	
	
	private void doPost(int id, List<String> links, List<String> images) {
		
		//Creo il client utilizzando la funzione factoryClient() che ignora la validit� del certificato SSL
		Client client = factoryClient();
		
		// Creo il target utilizzando getBaseURI, che ha l'indirizzo base
		WebTarget target = client.target(getBaseURI());
		
		//Stampo a video l'ID del progetto al quale sto inserendo i badges
		LOGGER.info("Inserisco i badges del progetto con ID: " + id);
		
		//Dichiaro una variabile di stato, per controllare lo stato delle chiamate
		
		//Creo un ciclo FOR per i 7 badges
		
		for (int i=0; i<7; i++)
		{
			//Creo un oggetto JSON dove metto i links dell'ennesimo badge dalle ArrayLists
			JSONObject badge = new JSONObject()
					.put("link_url", links.get(i))
		            .put("image_url", images.get(i));
			
			

			// Faccio il post passando l'oggetto JSON sopra creato convertendolo in stringa
			Response response = target.path("projects").path(""+id).path("badges").request().accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", gitlabToken).post(Entity.json(badge.toString()));
					
		}
		
	}
	
	private List<String> createBadgesImages(JSONObject object)
	{

		int id = object.getInt("id");
		
		String name = object.getString("name");
		JSONObject namespace = object.getJSONObject("namespace");
		String group = namespace.getString("path");



		//Creo un'ArrayList con 7 elementi, ogni elemento � il link d'immagine del badge
		List<String> badges = new ArrayList<String>();
		if(isGitlabci(id) || true==true) {
		badges.add("https://" + this.gitlabHost + "/" + group + "/" + name + "/badges/master/build.svg");
		}
		if(isSonar(id) || true==true) {
		badges.add("http://" + this.sonarHost + "/api/badges/gate?key=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=bugs");
		badges.add("http://" + this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=code_smells");
		badges.add("http://" + this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=ncloc_language_distribution");
		badges.add("http://" + this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=classes");
		badges.add("http://" + this.sonarHost + "/api/badges/measure?key=" + group + ":" + name + "&metric=functions");
		}
		return badges;
	}
	
	private List<String> createBadgesLinks(JSONObject object)
	{

		int id = object.getInt("id");
		
		String name = object.getString("name");
		JSONObject namespace = object.getJSONObject("namespace");
		String group = namespace.getString("path");
		
		//Creo un'ArrayList con 7 elementi, ogni elemento � il link del badge
		List<String> badges = new ArrayList<String>();
		if(isGitlabci(id) || true==true) {
		badges.add("https://" + this.gitlabHost +"/" + group + "/" + name + "/commits/master");
		}
		if(isSonar(id) || true==true) {
		badges.add("http://" + this.sonarHost + " /dashboard?id=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/component_measures/domain/Reliability?id=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/component_measures/domain/Maintainability?id=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		badges.add("http://" + this.sonarHost + "/component_measures/domain/Size?id=" + group + ":" + name);
		}
		return badges;
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
	private static boolean isFile(int id, String filename) {
		boolean b = false;
		// ....
		return b;		
		
	}	
}

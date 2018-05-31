package it.iubar.Hello_Maven;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

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

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;
	

public class Hello_Maven {

	public static Client factoryClient() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ClientBuilder builder = ClientBuilder.newBuilder();
		Client client = builder.sslContext(sslContext).build();
		return client;
	}

	public static void main(String[] args) {

		String token = "7ALqC2FSMxyV2zGe2EBu";
		String route = "projects?per_page=200";
		// Client client = ClientBuilder.newClient(new ClientConfig());
		Client client = factoryClient();

		WebTarget target = client.target(getBaseURI()+route);

		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", token).get(Response.class);
		int statusCode = response.getStatus();
		
		System.out.println("STATUS : " + statusCode);
		
		String json = response.readEntity(String.class);
		// System.out.println(json);
		
		JSONArray jsonArray = new JSONArray(json);
		for (int i = 0; i < 1 ; i++) {
			//jsonArray.length()
			JSONObject object = jsonArray.getJSONObject(i);
			
			//System.out.println("#" + (i + 1) + "  " + object.toString());
			System.out.println("Numero di progetti: " + jsonArray.length());
			int id = object.getInt("id");
			String name = object.getString("name");
			JSONObject namespace = object.getJSONObject("namespace");
			String group = namespace.getString("name");
			//System.out.println("GROUP : " + group);
			//System.out.println("ID : " + id);
			
			//Creo due ArrayList, una con i links dei 7 badges e una con le relative images
			ArrayList badgesImage = new ArrayList();
			ArrayList badgesLink = new ArrayList();
			badgesImage = createBadgesImages(name, group);
			badgesLink = createBadgesLinks(name, group);

			// Faccio il posto dei babges
			//doPost(id, token, badgesLink, badgesImage);
			//delateBadge(id);
			
		}

	}
	
	
	private static void doPost(int id, String token, ArrayList links, ArrayList images) {
		//Creo il client
		Client client = factoryClient();
		WebTarget target = client.target(getBaseURI());
		System.out.println("Inserisco i badges del progetto con ID: " + id);
		for (int i=0; i<7; i++)
		{
			//Creo l'oggetto JSON dove copio i links di un singolo badge dalle ArrayList
			JSONObject badge = new JSONObject()
					.put("link_url", links.get(i))
		            .put("image_url", images.get(i));
			
			System.out.println("File JSON:" + badge);
			
			// Faccio il post passando l'oggetto JSON sopra creato come stringa
			Response response = target.path("projects").path(""+id).path("badges").request().accept(MediaType.APPLICATION_JSON)
					.header("PRIVATE-TOKEN", token).post(Entity.json(badge.toString()));

			int statusCode = response.getStatus();
			System.out.println(statusCode);
		}
		//String json = response.readEntity(String.class);
		//JSONObject jsonObject = new JSONObject(json);
		//String message = jsonObject.getString("response");
	}
	
	private static ArrayList createBadgesImages(String name, String group)
	{
		//Creo un'ArrayList con 7 elementi, ogni elemento è il link d'immagine del badge
		ArrayList badges = new ArrayList();
		badges.add("https://gitlab.iubar.it/" + group + "/" + name + "/badges/master/build.svg");
		badges.add("http://192.168.0.117:9000/api/badges/gate?key=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/api/badges/measure?key=" + group + ":" + name + "&metric=bugs");
		badges.add("http://192.168.0.117:9000/api/badges/measure?key=" + group + ":" + name + "&metric=code_smells");
		badges.add("http://192.168.0.117:9000/api/badges/measure?key=" + group + ":" + name + "&metric=ncloc_language_distribution");
		badges.add("http://192.168.0.117:9000/api/badges/measure?key=" + group + ":" + name + "&metric=classes");
		badges.add("http://192.168.0.117:9000/api/badges/measure?key=" + group + ":" + name + "&metric=functions");
		return badges;
	}
	
	private static ArrayList createBadgesLinks(String name, String group)
	{
		//Creo un'ArrayList con 7 elementi, ogni elemento è il link del badge
		ArrayList badges = new ArrayList();
		badges.add("https://gitlab.iubar.it/" + group + "/" + name + "/commits/master");
		badges.add("http://192.168.0.117:9000/dashboard?id=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/component_measures/domain/Reliability?id=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/component_measures/domain/Maintainability?id=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/component_measures/domain/Size?id=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/component_measures/domain/Size?id=" + group + ":" + name);
		badges.add("http://192.168.0.117:9000/component_measures/domain/Size?id=" + group + ":" + name);
		return badges;
	}
	

	private static URI getBaseURI() {
		return UriBuilder.fromUri("https://gitlab.iubar.it/api/v4/").build();
	}
}
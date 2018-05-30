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
		String route = "projects";
		// Client client = ClientBuilder.newClient(new ClientConfig());
		Client client = factoryClient();

		WebTarget target = client.target(getBaseURI());

		Response response = target.path(route).request().accept(MediaType.APPLICATION_JSON)
				.header("PRIVATE-TOKEN", token).get(Response.class);
		int statusCode = response.getStatus();
		System.out.println("STATUS : " + statusCode);
		String json = response.readEntity(String.class);
		// System.out.println(json);
		JSONArray jsonArray = new JSONArray(json);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject object = jsonArray.getJSONObject(i);
			System.out.println("#" + (i + 1) + "  " + object.toString());
			int id = object.getInt("id");
			String name = object.getString("name");
			JSONObject namespace = object.getJSONObject("namespace");
			String group = namespace.getString("name");
			System.out.println("GROUP : " + group);
			System.out.println("ID : " + id);
			// POST: ...
			// doPost(id, );
			// eliminateBadge(id)
			ArrayList badgesImage = new ArrayList();
			ArrayList badgesLink = new ArrayList();
			badgesImage = createBadgesImages(name, group);
			badgesLink = createBadgesLinks(name, group);
			//System.out.println("LINK : " + badgesLink.get(1));
			//System.out.println("IMAGE : " + badgesImage.get(1));
			
			


		}

	}

	private static void doPost(int id, ArrayList links, ArrayList images) {
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		URI baseUri = UriBuilder.fromUri("https://gitlab.example.com/api/v4/projects/" + id + "/badges").build();
		WebTarget target = client.target(baseUri);
		for (int i=0; i<7; i++)
		{
			String input = "link_url="+ links.get(i) + "&image_url=" + images.get(i);
			Response response = target.path("register-client").path("1").request().accept(MediaType.APPLICATION_JSON)
					.post(Entity.json(input));
		}
		

		int statusCode = response.getStatus();

		String json = response.readEntity(String.class);
		JSONObject jsonObject = new JSONObject(json);
		String message = jsonObject.getString("response");
	}
	
	private static ArrayList createBadgesImages(String name, String group)
	{
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
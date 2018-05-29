package it.iubar.Hello_Maven;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

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
			JSONObject namespace = object.getJSONObject("namespace");
			String group = namespace.getString("name");
			System.out.println("GROUP : " + group);
			System.out.println("ID : " + id);
			// POST: ...
			// doPost(id);

		}

	}

	private static void doPost(int id) {
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		URI baseUri = UriBuilder.fromUri("http://iubar.it/crm/api/crm/v1").build();
		WebTarget target = client.target(baseUri);
		String input = "{" + "\"mac\": \"B8-CA-3A-96-BD-03\", " + "\"cf\": DMNLSN95P14D969J\", "
				+ "\"nome\": \"ALESSANDRO\", " + "\"cognome\": \"DAMONTE\"" + "}";
		Response response = target.path("register-client").path("1").request().accept(MediaType.APPLICATION_JSON)
				.post(Entity.json(input));

		int statusCode = response.getStatus();

		String json = response.readEntity(String.class);
		JSONObject jsonObject = new JSONObject(json);
		String message = jsonObject.getString("response");

	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri("https://gitlab.iubar.it/api/v4/").build();
	}
}

package it.iubar.badges;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;

public abstract class RestClient {

	private static final Logger LOGGER = Logger.getLogger(RestClient.class.getName());

	protected Client client = null;

	protected abstract URI getBaseURI();

	/**
	 * Crea il client e ignora la validità del certificato SSL
	 *
	 */
	public static Client factoryClient(String apiToken) {
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			},
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

		//String user = "john";
		//String pass = "changeme";
		//String str = user + ":" + pass;
		//String encodedString = Base64.getEncoder().encodeToString(str.getBytes());

		// Headers for client.
		ClientRequestFilter clientRequestFilter = new ClientRequestFilter() {
			public void filter(ClientRequestContext clientRequestContext) throws IOException {
				//clientRequestContext.getHeaders().add("Token", token);
				//clientRequestContext.getHeaders().add("ServerName", serverName);
				//clientRequestContext.getHeaders().add("Content-Type", contentType);
				//clientRequestContext.getHeaders().add("Content-Length", contentLength);
				//clientRequestContext.getHeaders().add("Connection", connection);

				//clientRequestContext.getHeaders().add("Authorization", "Basic " + encodedString);
				clientRequestContext.getHeaders().add("PRIVATE-TOKEN", apiToken);
				// clientRequestContext.getHeaders().add("Authorization", "Bearer " + apiToken); // OAuth-compliant headers:
			}
		};

		ClientConfig config = new ClientConfig();
		config.register(clientRequestFilter);
		ClientBuilder builder = ClientBuilder.newBuilder();
		Client client = builder.sslContext(sslContext).withConfig(config).build();

		return client;
	}

	private Builder getBuilder(WebTarget target) {
		return target.request().accept(MediaType.APPLICATION_JSON);
	}

	protected Response doGet(String route) {
		String uri = getBaseURI() + route;
		LOGGER.log(Level.INFO, "uri : " + uri);
		WebTarget target = this.client.target(uri);
		Response response = getBuilder(target).get(Response.class);
		return response;
	}

	protected Response doDelete(String route) {
		String uri = getBaseURI() + route;
		LOGGER.log(Level.INFO, "uri : " + uri);
		WebTarget target = this.client.target(uri);
		Response response = getBuilder(target).delete(Response.class);
		return response;
	}

	protected <T> Response doPut(String route, Entity<T> entity) {
		String uri = getBaseURI() + route;
		LOGGER.log(Level.INFO, "uri : " + uri);
		WebTarget target = this.client.target(uri);
		Response response = getBuilder(target).put(entity);
		return response;
	}

	protected <T> Response doPost(String route, Entity<T> entity) {
		String uri = getBaseURI() + route;
		LOGGER.log(Level.INFO, "uri : " + uri);
		WebTarget target = this.client.target(uri);
		Response response = getBuilder(target).post(entity);
		return response;
	}

	protected static void logResponse(Response response) {
		String output = response.readEntity(String.class);
		LOGGER.log(Level.INFO, "Message : " + output);
	}
	
	protected static void logError(Response response) {
		String output = response.readEntity(String.class);
		LOGGER.log(Level.SEVERE, "Error : " + output);
	}
	
	protected void logError(String error, Response response) {
		LOGGER.log(Level.SEVERE, error);
		logError(response);
		if (Config.FAIL_FAST) {
			System.exit(1);
		} else {
			AbstractUpdater.errors.add(error);
		}
	}
	
}

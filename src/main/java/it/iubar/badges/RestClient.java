package it.iubar.badges;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public abstract class RestClient {

	private static final Logger LOGGER = Logger.getLogger(RestClient.class.getName());
	
	private Client client = null;

	protected abstract URI getBaseURI();
	
	public RestClient() {
		this.client = factoryClient();		
	}
	
	/**
	 * Crea il client e ignora la validità del certificato SSL
	 * 
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
	
	protected Builder getBuilder(WebTarget target) {
		return target.request().accept(MediaType.APPLICATION_JSON);
	}
	
	protected Response doGet(String route) {
		WebTarget target = this.client.target(getBaseURI() + route);
		Response response = getBuilder(target).get(Response.class);
		return response;
	}

	protected Response doDelete(String route) {
		WebTarget target = this.client.target(getBaseURI() + route);					
		Response response = getBuilder(target).delete(Response.class);
		return response;
	}
	
	protected <T> Response doPost(String route, Entity<T> entity) {
		WebTarget target = this.client.target(getBaseURI() + route);
		Response response = getBuilder(target).post(entity);
		return response;
	}
	
}

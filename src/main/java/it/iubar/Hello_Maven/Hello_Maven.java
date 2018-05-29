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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public class Hello_Maven {
	
	public static Client factoryClient() {
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
	        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
	        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
	    }};

		SSLContext sslContext = null; 
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		  //HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
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
	
    public static void main( String[] args )
    {
       // Client client = ClientBuilder.newClient(new ClientConfig());
    	Client client = factoryClient();

        WebTarget target = client.target(getBaseURI());

        Response response = target.request().
        		accept(MediaType.APPLICATION_JSON).
        		get(Response.class);
        int statusCode = response.getStatus();
        System.out.println(statusCode);
        String json = response.readEntity(String.class);
        System.out.println(json);
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
        	JSONObject object = jsonArray.getJSONObject(i);
        	 System.out.println(object.toString());
        	 int id = object.getInt("id");
        	 System.out.println(id);
        	 // POST: ...
        	 doPost(id);       	 
        	 
        	}
   
        
    
    }
    
    private static void doPost(int id) {
		// TODO Auto-generated method stub
		
	}

	private static URI getBaseURI() {
        return UriBuilder.fromUri("https://gitlab.iubar.it/api/v4/projects/").build();
    }
}

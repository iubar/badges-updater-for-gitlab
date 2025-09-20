package it.iubar.badges;

import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

public class ResponseUtils {

	private static final Logger LOGGER = Logger.getLogger(ResponseUtils.class.getName());
	
	public static Object readAsObject(Response response, boolean printOut) {
		String contentType = response.getHeaderString("Content-Type");
		Object body;
		if (contentType != null && contentType.startsWith("application/json")) {
			try {
				body = response.readEntity(JsonObject.class);
				if(printOut) {
					System.out.println("JSON body : ");
					JsonUtils.prettyPrint((JsonObject)body);
				}
			} catch (Exception e) {
				// se qualcosa va storto, fallback su stringa
				body = response.readEntity(String.class);
				if(printOut) {
					System.out.println("Plain body (fallback): " + body);
				}
			}
		} else {
			body = response.readEntity(String.class);
			if(printOut) {
				System.out.println("Plain body: " + body);
			}
		}
		return body;
	}

	public static JsonArray readJSonArray(Response response, boolean printOut) {
		JsonArray array = null;
		try {		
			array = response.readEntity(JsonArray.class);
			if(printOut) {
				System.out.println("JSON array : ");
				for (int i = 0; i < array.size(); i++) {
					JsonObject json = array.getJsonObject(i);	        		       
					JsonUtils.prettyPrint(json);
				}
			}
		} catch (Exception e) {
			// se qualcosa va storto...
			String contentType = response.getHeaderString("Content-Type");
			System.out.println("Status code: " + response.getStatus());
			String body = response.readEntity(String.class);
			System.out.println("contentType: " + contentType);
			System.out.println("Plain body (fallback): " + body);
			e.printStackTrace();
		}	        	
		return array;
	}

	public static JsonObject readAsStringToJson(Response response, boolean printOut) {
		JsonObject json = null;
		String jsonString = response.readEntity(String.class); 
		json = JsonUtils.readObject(jsonString);
		if(printOut) {
			JsonUtils.prettyPrint(json);
		}
		return json;
	}
	
	public static JsonObject readAsJson(Response response, boolean printOut) {
		JsonObject json = null;
		try {
			json = response.readEntity(JsonObject.class);
			if(printOut) {
				System.out.println("JSON body: ");
				JsonUtils.prettyPrint(json);
			}
		} catch (Exception e) {
			// se qualcosa va storto...
			String contentType = response.getHeaderString("Content-Type");
			System.out.println("Status code: " + response.getStatus());
			String body = response.readEntity(String.class);
			System.out.println("contentType: " + contentType);
			System.out.println("Plain body (fallback): " + body);
			e.printStackTrace();
		}
		return json;
	}
	

	public static String readAsString(Response response, boolean printOut) {
		String body = response.readEntity(String.class); 
		if(printOut) {
			System.out.println("Plain body : " + body);
		}
		return body;
	}

}

package it.iubar.badges;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;

public class JsonUtils {

	private static final Logger LOGGER = Logger.getLogger(JsonUtils.class.getName());

	public static JsonObject parseJsonString(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonObject = reader.readObject();
		return jsonObject;
	}

	public static String prettyPrintFormat(JsonObject jsonObject) {
		String jsonString = null;
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory writerFactory = Json.createWriterFactory(config);
		try (Writer writer = new StringWriter()) {
			writerFactory.createWriter(writer).write(jsonObject);
			jsonString = writer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonString;
	} 

	public static void prettyPrint(JsonArray jsonArray) {
		if (jsonArray != null) {
			for (JsonValue jsonValue : jsonArray) {
				if (jsonValue instanceof JsonArray) {
					prettyPrint(jsonValue.asJsonArray());
				} else if (jsonValue instanceof JsonObject) {
					prettyPrint(jsonValue.asJsonObject());
				} else {
					throw new RuntimeException("Error in prettyPrint()");
				}
			}
		}
	}

	public static void prettyPrint(HashMap<?, ?> map, int indent) {
	    String pad = "  ".repeat(indent);
	        System.out.println("{");
	        for (Map.Entry<?, ?> entry : map.entrySet()) {
	            System.out.print(pad + "  " + entry.getKey() + ": ");
	            Object value = entry.getValue();
	            if (value instanceof List) {
	                prettyPrint((List)value, indent + 1);
	        	} else if (value instanceof List) {
	        		prettyPrint((List)value, indent + 1);
	            } else {
	                System.out.println(value);
	            }
	        }
	        System.out.println(pad + "}");
	 
	}
	
	public static void prettyPrint(List<?> list, int indent) {
		String pad = "  ".repeat(indent);
        System.out.println("[");
        for (Object item : list) {
            System.out.print(pad + "  ");
            if (item instanceof List) {
                prettyPrint((List)item, indent + 1);
        	} else if (item instanceof List) {
        		prettyPrint((List)item, indent + 1);
            } else {
                System.out.println(item);
            }
        }
        System.out.println(pad + "]");		
	}
	
	
	public static void prettyPrint(JsonObject jsonObject) {
		System.out.println(prettyPrintFormat(jsonObject));
	}

	public static JsonObject readObject(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonObject object = reader.readObject();
		return object;
	}

	public static JsonArray readArray(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonArray array = reader.readArray();
		return array;
	}

	/**
	 * Entity: è l’involucro usato dal client JAX-RS per spedire quel contenuto in una richiesta HTTP, con l’informazione sul Content-Type
	 * Entity è un wrapper per trasportare dati in una richiesta HTTP con Jersey.
	 * 
	 * @param entity
	 */
	public static void prettyPrint(Entity<?>  entity) {
		Object data = entity.getEntity(); // il "vero" contenuto
		if (data instanceof JsonObject) {
			prettyPrint((JsonObject)data);
		} else if (data instanceof JsonArray) {
			prettyPrint((JsonArray)data);
		} else if (data instanceof HashMap) {
			prettyPrint((HashMap)data, 0);			
		} else if (data instanceof List<?>){
			prettyPrint((List<?>)data, 0);				
		} else {
		    System.out.println("Dato non stampabile : " + data.getClass());
		}		
	}
}

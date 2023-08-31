package it.iubar.badges;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtils {

	private static final Logger LOGGER = Logger.getLogger(JsonUtils.class.getName());

	private static JsonObject parseJsonString(String jsonString) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonObject = reader.readObject();
		return jsonObject;
	}

	private static String prettyPrintFormat(JsonObject jsonObject) throws IOException {
		String jsonString = null;
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory writerFactory = Json.createWriterFactory(config);
		try (Writer writer = new StringWriter()) {
			writerFactory.createWriter(writer).write(jsonObject);
			jsonString = writer.toString();
		}
		return jsonString;
	}

	public static void prettyPrint(JsonObject jsonObject) {
		try {
			String prettyString = prettyPrintFormat(jsonObject);
			System.out.println(prettyString);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Can't print json", ex);
		}
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
}

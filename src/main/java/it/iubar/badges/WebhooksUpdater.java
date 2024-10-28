package it.iubar.badges;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see https://docs.gitlab.com/ee/api/project_webhooks.html
 */
public class WebhooksUpdater extends AbstractUpdater implements IUpdater {

	private static final Logger LOGGER = Logger.getLogger(WebhooksUpdater.class.getName());

	public WebhooksUpdater(Properties config) {
		super(config);
		this.client = factoryClient(this.gitlabToken);
	}

	@Override
	public void run() {
		JsonArray projects = getProjects();

		// Effettuo una serie di operazioni su tutti i progetti
		for (int i = 0; i < projects.size(); i++) {
			JsonObject project = projects.getJsonObject(i);
			if (this.debug) {
				LOGGER.log(Level.INFO, "Pretty printing project info...");

				JsonUtils.prettyPrint(project);
			}

			int projectId = project.getInt("id");
			String path = project.getString("path_with_namespace");
			String projectDescAndId = path + " (id " + projectId + ")";
			LOGGER.info("Project " + projectDescAndId);

			JsonArray webhooks = getWebhooks(projectId);

			if (Config.UPDATE_WEBHOOKS) {
				for (int j = 0; j < webhooks.size(); j++) {
					JsonObject webhook = webhooks.getJsonObject(j);
					JsonUtils.prettyPrint(webhook);
					int hookId = Integer.parseInt(webhook.get("id").toString());

					// Recupero i detagli sul webhook, ma forse non mi servono queste informazioni
					JsonObject detail = getDetail(projectId, hookId);
					JsonUtils.prettyPrint(detail);

					if (!webhook.isEmpty()) {
						// Aggiorno il primo webhook e cancello i restanti
						if (j == 0) {
							updateWebhook(projectId, hookId, webhook);
						} else {
							deleteWebhook(projectId, hookId);
						}
					}
					/*
					if() {
						List<Integer> results2 = editWebhook(projectId, webhook);
						if (results2.isEmpty()) {
							LOGGER.severe("No results for project " + projectDescAndId);
						} else {
							LOGGER.log(Level.INFO, "#" + results2.size() + " OK " + projectDescAndId);
						}
					}
					*/

				}
			}
		}
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_webhooks.html#delete-project-webhook
	 */
	private JsonObject deleteWebhook(int projectId, int hookId) {
		JsonObject webhook = null;
		String route = "projects/" + projectId + "/hooks/" + hookId;
		Response response = doDelete(route);
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			String error =
				"Impossibile eliminare il dettaglio del webhook per il progetto " +
				projectId +
				" con hook_id " +
				hookId +
				". Status code: " +
				statusCode +
				". Verificare che la feature CI/CD sia abilitata per il progetto.";
			LOGGER.severe(error);
			if (Config.FAIL_FAST) {
				System.exit(1);
			} else {
				AbstractUpdater.errors.add(error);
			}
		} else {
			String jsonString = response.readEntity(String.class);
			webhook = JsonUtils.readObject(jsonString);
		}
		return webhook;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_webhooks.html#get-a-project-webhook
	 */
	private JsonObject getDetail(int projectId, int hookId) {
		JsonObject webhook = null;
		String route = "projects/" + projectId + "/hooks/" + hookId;
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			String error =
				"Impossibile recuperare il dettaglio del webhook per il progetto " +
				projectId +
				" con hook_id " +
				hookId +
				". Status code: " +
				statusCode +
				". Verificare che la feature CI/CD sia abilitata per il progetto.";
			LOGGER.severe(error);
			if (Config.FAIL_FAST) {
				System.exit(1);
			} else {
				AbstractUpdater.errors.add(error);
			}
		} else {
			String jsonString = response.readEntity(String.class);
			webhook = JsonUtils.readObject(jsonString);
		}
		return webhook;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/project_webhooks.html#list-webhooks-for-a-project
	 */
	private JsonArray getWebhooks(int projectId) {
		JsonArray webhooks = null;
		String route = "projects/" + projectId + "/hooks";
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			String error =
				"Impossibile recuperare l'elenco dei webhooks per il progetto " +
				projectId +
				". Status code: " +
				statusCode +
				". Verificare che la feature CI/CD sia abilitata per il progetto.";
			LOGGER.severe(error);
			if (Config.FAIL_FAST) {
				System.exit(1);
			} else {
				AbstractUpdater.errors.add(error);
			}
		} else {
			String jsonString = response.readEntity(String.class);
			webhooks = JsonUtils.readArray(jsonString);
		}
		return webhooks;
	}

	/*
	 * @see https://docs.gitlab.com/ee/api/project_webhooks.html#edit-a-project-webhook
	 */
	private void updateWebhook(int projectId, int hookId, JsonObject oldValue) {
		JsonObjectBuilder builder = Json.createObjectBuilder().add("url", this.webhookUrl);
		JsonObject jsonObject2 = builder.build();

		String route = "projects/" + projectId + "/hooks/" + hookId;
		Response response = doPut(route, Entity.json(jsonObject2.toString()));
		int statusCode = response.getStatus();
		if (statusCode == Status.OK.getStatusCode()) {
			String jsonString = response.readEntity(String.class);
			JsonObject object = JsonUtils.readObject(jsonString);
			JsonUtils.prettyPrint(object);
			String msg = "OK : updated. Status code: " + statusCode;
			LOGGER.info(msg);
		} else {
			String msg = "Impossibile modificare il webhook " + hookId + ". Status code: " + statusCode;
			LOGGER.severe(msg);
			if (Config.FAIL_FAST) {
				System.exit(1);
			} else {
				AbstractUpdater.errors.add(msg);
			}
		}
	}
}
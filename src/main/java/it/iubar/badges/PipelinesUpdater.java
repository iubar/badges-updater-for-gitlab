package it.iubar.badges;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @see https://docs.gitlab.com/ee/api/pipelines.html
 */
public class PipelinesUpdater extends AbstractUpdater implements IUpdater {

	private static final Logger LOGGER = Logger.getLogger(PipelinesUpdater.class.getName());

	public PipelinesUpdater(Properties config) {
		super(config);
		this.client = factoryClient(this.gitlabToken);
	}

	private String getProjectUrl(int projectId) {
		return this.gitlabHost + "/projects/" + projectId;
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

			JsonArray pipelines = getPipelines(projectId, Config.DEFAULT_BRANCH);
			if (Config.DELETE_PIPELINES) {
				if (pipelines != null && !pipelines.isEmpty()) {
					List<Integer> results3 = removePipelines(projectId, pipelines);
					LOGGER.log(Level.INFO, "#" + results3.size() + " pipelines removed successfully from project " + projectDescAndId);
				}
			} else {
				LOGGER.log(Level.INFO, "#" + pipelines.size() + " pipelines found in project " + projectDescAndId);
			}
		}
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#list-project-pipelines
	 *
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 */
	private JsonArray getPipelines(int projectId, String branch) {
		JsonArray pipelines = null;
		String route = "projects/" + projectId + "/pipelines" + Config.PER_PAGE + "&ref=" + branch;
		Response response = doGet(route);
		int statusCode = response.getStatus();
		if (statusCode != Status.OK.getStatusCode()) {
			String msg =
				"Impossibile recuperare l'elenco delle pipeline per il progetto " +
				projectId +
				". Status code: " +
				statusCode +
				". Verificare che la feature CI/CD sia abilitata per il progetto.";
			logError(msg, response);
		} else {
			String jsonString = response.readEntity(String.class);
			pipelines = JsonUtils.readArray(jsonString);
		}
		return pipelines;
	}

	/**
	 * @see https://docs.gitlab.com/ee/api/pipelines.html#delete-a-pipeline
	 *
	 * Se si volesse verificare su disco la presenza o meno degli artifacts relativi a una pipeline,
	 * bisogna sapere che il percorso dello storage di Gitlab può variare in relazione alla modalità di installazione
	 * Ad esempio può essere: /var/opt/gitlab/gitlab-rails/shared/artifacts/<year_month>/<project_id?>/<jobid>
	 * @see https://gitlab.com/gitlab-org/gitlab-ce/blob/master/doc/administration/job_artifacts.md#storing-job-artifacts
	 *
	 * @param projectId The ID or URL-encoded path of the project owned by the authenticated user
	 * @param pipelines JsonArray
	 */
	private List<Integer> removePipelines(int projectId, JsonArray pipelines) {
		List<Integer> pipelineIds = new ArrayList<Integer>();
		if (pipelines.size() <= Config.SKIP_PIPELINES_QNT) {
			LOGGER.info(
				"Pipelines found #" +
				pipelines.size() +
				" <= " +
				Config.SKIP_PIPELINES_QNT +
				", so there is no pipelines to delete for project id " +
				projectId
			);
		} else {
			LOGGER.info("#" + pipelines.size() + " pipelines found for project id " + projectId);
			for (int j = 0 + Config.SKIP_PIPELINES_QNT; j < pipelines.size(); j++) {
				String msg =
					"Removing " +
					(j + 1 - Config.SKIP_PIPELINES_QNT) +
					"/" +
					(pipelines.size() - Config.SKIP_PIPELINES_QNT) +
					" pipeline for project id " +
					projectId +
					" (I will keep the last " +
					Config.SKIP_PIPELINES_QNT +
					"/" +
					pipelines.size() +
					" pipelines)";
				LOGGER.info(msg);
				JsonObject pipeline = pipelines.getJsonObject(j);
				int pipelineId = pipeline.getInt("id"); // The ID of a pipeline
				String route3 = "projects/" + projectId + "/pipelines/" + pipelineId;
				Response response = doDelete(route3);
				int statusCode = response.getStatus();
				if (statusCode == Status.NO_CONTENT.getStatusCode()) {
					pipelineIds.add(pipelineId);
				} else {
					String error =
						"Impossibile eliminare la pipeline " +
						pipelineId +
						" del progetto " +
						projectId +
						" (" +
						getProjectUrl(projectId) +
						"). Status code: " +
						statusCode;
					error = error +
					" (Nota che l'errore si potrebbe manifestare quando la pipeline è in esecuzione oppure un proceddo è archiviato e quindi è in sola lettura).";
					LOGGER.warning(error);
					logError(error, response);
					//					if(Config.FAIL_FAST) {
					//						System.exit(1);
					//					} else {
					//						this.errors.add(error);
					//						break; // satrebbe inutile continuare
					//					}
				}
			}
		}
		return pipelineIds;
	}
}

package it.iubar.badges;

public class Config {

	public enum UpdateType { 
	    DELETE_AND_ADD,
	    DELETE_ALL, 
	    DISABLED
	}
	
	public static final boolean FAIL_FAST = true;

	public static final UpdateType UPDATE_BADGES = UpdateType.DELETE_AND_ADD;
	
	public static final UpdateType UPDATE_WEBHOOKS = UpdateType.DELETE_AND_ADD;
	
	public static final boolean ADD_SONAR_BADGES = true;

	public static final boolean DELETE_PIPELINES = true;

	public static final String DEFAULT_BRANCH = "master";

	// Verranno cancellate tutte le pipelines ad esclusione delle ultime 5
	public static final int SKIP_PIPELINES_QNT = 5;

	// Di default il numero massimo di record restituiti da ogni chiamata all'Api è 20
	public static final int MAX_RECORDS_PER_RESPONSE = 200;

	public static final String PER_PAGE = "?per_page=" + MAX_RECORDS_PER_RESPONSE; // @see https://docs.gitlab.com/ee/api/#pagination

	public static final String GITLAB_API_VER = "v4";

	public static final String GITLAB_FILE = ".gitlab-ci.yml";

	public static final String SONAR_FILE = "sonar-project.properties";

	public static final String CONFIG_FILE = "config.properties";
}

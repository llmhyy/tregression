package tregression.constants;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import microbat.Activator;
import tregression.preference.TregressionPreference;

public enum Dataset {
	DEFECTS4J("Defects4J"), REGS4J("Regs4J");

	private final String name;
	private static final Map<String, Dataset> LOOKUP = new HashMap<>();

	static {
		for (Dataset dataset : EnumSet.allOf(Dataset.class)) {
			LOOKUP.put(dataset.getName(), dataset);
		}
	}

	private Dataset(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static Dataset getDataset(String name) {
		return LOOKUP.get(name);
	}

	public static Dataset getTypeFromPref() {
		String datasetName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.DATASET_NAME);
		return getDataset(datasetName);
	}

	public static String[] getProjectNames() {
		Dataset datasetType = getTypeFromPref();
		if (datasetType.equals(Dataset.DEFECTS4J)) {
			return new String[] { "Chart", "Closure", "Lang", "Math", "Mockito", "Time" };
		}
		return new String[] { "uniVocity_univocity-parsers", "apache_commons-lang", "jhy_jsoup", "jmrozanec_cron-utils",
				"JSQLParser_JSqlParser", "alibaba_fastjson", "spring-projects_spring-data-commons", "alibaba_druid",
				"classgraph_classgraph", "brettwooldridge_HikariCP", "w3c_epubcheck", "yegor256_cactoos",
				"devnied_Bit-lib4j", "josephw_futoshiki", "codehaus_jettison", "bmwcarit_barefoot",
				"Esri_geometry-api-java", "jdereg_java-util", "DJCordhose_jmte", "EsotericSoftware_reflectasm",
				"haku_dlnatoad", "albertus82_jface-utils", "Commonjava_partyline", "fasseg_exp4j", "joaovicente_Tracy",
				"jeffdcamp_dbtools-query", "mikera_vectorz", "tntim96_JSCover", "wsky_top-push", "addthis_stream-lib",
				"lets-blade_blade", "mrniko_netty-socketio", "redis_jedis", "OfficeDev_ews-java-api",
				"GoogleCloudPlatform_app-maven-plugin", "mooreds_gwt-crypto", "jmurty_java-xmlbuilder",
				"killme2008_aviatorscript", "sboesebeck_morphium", "davidmoten_rtree", "srikanth-lingala_zip4j",
				"davidmoten_rxjava-jdbc", "apache_datasketches-java", "EXIficient_exificient", "osglworks_java-tool",
				"verdict-project_verdict", "PapenfussLab_gridss", "hub4j_github-api", "mybatis_mybatis-3",
				"fabric8io_docker-maven-plugin", "zeromq_jeromq", "zeroturnaround_zt-exec", "scijava_scijava-common",
				"INRIA_spoon", "valotrading_silvertip", "xerial_sqlite-jdbc", "lightblueseas_jcommons-lang",
				"serba_jmxterm", "brianm_config-magic", "khoubyari_spring-boot-rest-example", "dynjs_dynjs",
				"ShifuML_shifu", "apache_commons-jexl", "google_closure-templates", "tzaeschke_zoodb",
				"basho_riak-java-client", "apache_systemds", "cojen_Tupl", "j256_ormlite-core",
				"cantaloupe-project_cantaloupe", "jboss-javassist_javassist",
				"spring-projects_spring-data-elasticsearch", "imglib_imglib2", "jahlborn_jackcess", "imagej_imagej-ops",
				"spring-projects_spring-data-cassandra", "CodeStory_fluent-http", "openshift_openshift-restclient-java",
				"xebialabs_overthere", "vert-x3_vertx-codegen", "apache_commons-validator",
				"jeffheaton_encog-java-core", "vietj_reactive-pg-client", "decorators-squad_eo-yaml",
				"spring-projects_spring-hateoas", "antlr_stringtemplate4", "JOML-CI_JOML", "bkiers_Liqp",
				"influxdata_influxdb-java", "crawler-commons_crawler-commons", "apache_commons-beanutils",
				"neuland_jade4j", "chrisvest_stormpot", "apache_commons-compress", "jenkinsci_junit-plugin",
				"kokorin_Jaffree", "codelibs_jcifs", "graphaware_neo4j-nlp", "marschall_memoryfilesystem",
				"damianszczepanik_cucumber-reporting", "jqno_equalsverifier", "ktuukkan_marine-api",
				"doctau_garbagecat", "rickfast_consul-client", "camueller_SmartApplianceEnabler", "DiUS_java-faker",
				"logic-ng_LogicNG", "bioinform_varsim", "lemire_javaewah", "bguerout_jongo",
				"logfellow_logstash-logback-encoder", "davidmoten_rxjava-extras", "salesforce_storm-dynamic-spout",
				"Multiverse_Multiverse-Core" };
	}

	public static int[] getBugNums() {
		Dataset datasetType = getTypeFromPref();
		if (datasetType.equals(Dataset.DEFECTS4J)) {
			return new int[] { 26, 133, 65, 106, 38, 27 };
		}
		return new int[] { 25, 1, 58, 19, 2, 184, 13, 27, 6, 13, 2, 8, 1, 2, 2, 1, 5, 7, 1, 1, 1, 2, 6, 2, 2, 2, 7, 2,
				1, 4, 3, 1, 3, 2, 2, 1, 1, 3, 14, 2, 2, 1, 20, 2, 2, 53, 3, 3, 9, 3, 2, 1, 1, 5, 1, 2, 1, 1, 3, 1, 1, 2,
				11, 31, 4, 1, 5, 3, 3, 2, 1, 1, 2, 3, 1, 1, 1, 2, 3, 2, 2, 3, 2, 6, 11, 2, 2, 2, 2, 16, 2, 4, 1, 7, 1,
				1, 1, 6, 7, 2, 13, 2, 3, 2, 3, 6, 3, 3, 8, 6, 3, 1, 2, 1 };
	}
}
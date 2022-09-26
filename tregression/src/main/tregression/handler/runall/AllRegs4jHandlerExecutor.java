package tregression.handler.runall;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import experiment.utils.report.ExperimentReportComparisonReporter;
import experiment.utils.report.rules.SimulatorComparisonRule;
import experiment.utils.report.rules.TextComparisonRule;
import microbat.Activator;
import sav.common.core.Pair;
import sav.common.core.utils.SingleTimer;
import tregression.constants.Dataset;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.ReadEmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialReader;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.config.Regs4jProjectConfig;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.handler.paths.PathConfiguration;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.preference.TregressionPreference;

public class AllRegs4jHandlerExecutor extends RunAllInDatasetExecutor {
	@Override
	public void execute() {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int skippedNum = 0;
				int endNum = 500;
				
				String[] projects = {"uniVocity_univocity-parsers", "apache_commons-lang", "jhy_jsoup", "jmrozanec_cron-utils", "JSQLParser_JSqlParser", "alibaba_fastjson", "spring-projects_spring-data-commons", "alibaba_druid", "classgraph_classgraph", "brettwooldridge_HikariCP", "w3c_epubcheck", "yegor256_cactoos", "devnied_Bit-lib4j", "josephw_futoshiki", "codehaus_jettison", "bmwcarit_barefoot", "Esri_geometry-api-java", "jdereg_java-util", "DJCordhose_jmte", "EsotericSoftware_reflectasm", "haku_dlnatoad", "albertus82_jface-utils", "Commonjava_partyline", "fasseg_exp4j", "joaovicente_Tracy", "jeffdcamp_dbtools-query", "mikera_vectorz", "tntim96_JSCover", "wsky_top-push", "amaembo_streamex", "lets-blade_blade", "mrniko_netty-socketio", "redis_jedis", "OfficeDev_ews-java-api", "GoogleCloudPlatform_app-maven-plugin", "mooreds_gwt-crypto", "jmurty_java-xmlbuilder", "killme2008_aviatorscript", "sboesebeck_morphium", "davidmoten_rtree", "srikanth-lingala_zip4j", "davidmoten_rxjava-jdbc", "apache_datasketches-java", "EXIficient_exificient", "osglworks_java-tool", "verdict-project_verdict", "PapenfussLab_gridss", "hub4j_github-api", "mybatis_mybatis-3", "fabric8io_docker-maven-plugin", "zeromq_jeromq", "zeroturnaround_zt-exec", "scijava_scijava-common", "addthis_stream-lib", "INRIA_spoon", "valotrading_silvertip", "xerial_sqlite-jdbc", "lightblueseas_jcommons-lang", "serba_jmxterm", "brianm_config-magic", "khoubyari_spring-boot-rest-example", "dynjs_dynjs", "ShifuML_shifu", "apache_commons-jexl", "google_closure-templates", "tzaeschke_zoodb", "basho_riak-java-client", "apache_systemds", "cojen_Tupl", "j256_ormlite-core", "cantaloupe-project_cantaloupe", "jboss-javassist_javassist", "spring-projects_spring-data-elasticsearch", "imglib_imglib2", "jahlborn_jackcess", "imagej_imagej-ops", "spring-projects_spring-data-cassandra", "CodeStory_fluent-http", "openshift_openshift-restclient-java", "xebialabs_overthere", "vert-x3_vertx-codegen", "apache_commons-validator", "jeffheaton_encog-java-core", "vietj_reactive-pg-client", "decorators-squad_eo-yaml", "spring-projects_spring-hateoas", "antlr_stringtemplate4", "JOML-CI_JOML", "bkiers_Liqp", "influxdata_influxdb-java", "crawler-commons_crawler-commons", "apache_commons-beanutils", "neuland_jade4j", "chrisvest_stormpot", "apache_commons-compress", "jenkinsci_junit-plugin", "kokorin_Jaffree", "codelibs_jcifs", "graphaware_neo4j-nlp", "marschall_memoryfilesystem", "damianszczepanik_cucumber-reporting", "jqno_equalsverifier", "ktuukkan_marine-api", "doctau_garbagecat", "rickfast_consul-client", "camueller_SmartApplianceEnabler", "DiUS_java-faker", "logic-ng_LogicNG", "bioinform_varsim", "lemire_javaewah", "bguerout_jongo", "logfellow_logstash-logback-encoder", "davidmoten_rxjava-extras", "salesforce_storm-dynamic-spout", "Multiverse_Multiverse-Core"};
				
				int[] bugNum = {25, 1, 58, 19, 2, 184, 13, 27, 6, 13, 2, 8, 1, 2, 2, 1, 5, 7, 1, 1, 1, 2, 6, 2, 2, 2, 7, 2, 1, 3, 3, 1, 3, 2, 2, 1, 1, 3, 14, 2, 2, 1, 20, 2, 2, 53, 3, 3, 9, 3, 2, 1, 1, 1, 5, 1, 2, 1, 1, 3, 1, 1, 2, 11, 31, 4, 1, 5, 3, 3, 2, 1, 1, 2, 3, 1,
						 1, 1, 2, 3, 2, 2, 3, 2, 6, 11, 2, 2, 2, 2, 16, 2, 4, 1, 7, 1, 1, 1, 6, 7, 2, 13, 2, 3, 2, 3, 6, 3, 3, 8, 6, 3, 1, 2, 1};
				
//				String fileName = "defects4j0.old.xlsx";
				String fileName = "benchmark" + File.separator + "ben.xlsx";
				Map<ReadEmpiricalTrial, ReadEmpiricalTrial> map = new HashMap<>();
				try {
					map = new TrialReader().readXLSX(fileName);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
//				String[] projects = {"Chart"};
//				int[] bugNum = {2};
				
//				String[] projects = {"Lang"};
//				int[] bugNum = {65};
//				
//				String[] projects = {"Time"};
//				int[] bugNum = {2};
				
				String prefix = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH) + File.separator;
				
				int count = 0;
				File regs4jFile = null;
				try {
					for(int i=0; i<projects.length; i++) {
						
						for(int j=1; j<=bugNum[i]; j++) {
							
							SingleTimer timer = SingleTimer.start("generateTrials");
							if (monitor.isCanceled()) {
								return Status.OK_STATUS;
							}
							count++;
							if(count <= skippedNum || count > endNum) {
								continue;
							}
							
							if(!map.isEmpty()){
								ReadEmpiricalTrial tmp = new ReadEmpiricalTrial();
								tmp.setProject(projects[i]);
								tmp.setBugID(String.valueOf(j));
								
								ReadEmpiricalTrial t = map.get(tmp);
								if(t==null){
									System.err.println(projects[i]+"-"+j+" is missing.");
									continue;
								}
								
//								String deadEndType = t.getDeadEndType();
//								if(deadEndType==null || !(deadEndType.equals("control") || deadEndType.equals("data"))){
//									continue;
//								}
								
//							String exception = t.getException();
//							if(exception==null || !exception.contains("over long")){
//								continue;
//							}
							}
							
							System.out.println("***working on the " + j + "th bug of " + projects[i] + " project.");
							PathConfiguration pathConfig = PathConfigurationFactory.createPathConfiguration(Dataset.REGS4J);
							String buggyPath = pathConfig.getBuggyPath(projects[i], Integer.toString(j));
							String fixPath = pathConfig.getBuggyPath(projects[i], Integer.toString(j));
							
							System.out.println("analyzing the " + j + "th bug in " + projects[i] + " project.");
							
							TrialGenerator generator = new TrialGenerator();
							TrialGenerator0 generator0 = new TrialGenerator0();
							
							ProjectConfig regs4jConfig = Regs4jProjectConfig.getConfig(projects[i], String.valueOf(j));
							List<EmpiricalTrial> trials = generator0.generateTrials(buggyPath, fixPath, 
									false, false, false, 3, false, true, regs4jConfig, null);
							
							TrialRecorder recorder;
							try {
								recorder = new TrialRecorder(Dataset.REGS4J);
								regs4jFile = recorder.export(trials, projects[i], j);
								
//								for(EmpiricalTrial t: trials){
//									
//									if(!t.getDeadEndRecordList().isEmpty()){
//										Repository.clearCache();
//										DeadEndRecord record = t.getDeadEndRecordList().get(0);
////									DED datas = new TrainingDataTransfer().transfer(record, t.getBuggyTrace());
//										DED datas = record.getTransformedData(t.getBuggyTrace());
//										setTestCase(datas, t.getTestcase());						
//										try {
////										new DeadEndReporter().export(datas.getAllData(), projects[i], Integer.valueOf(j));
//											
//											if(!t.getRootCauseFinder().getCausalityGraph().getRoots().isEmpty()){
//												new DeadEndCSVWriter("_d4j", null).export(datas.getAllData(), projects[i], String.valueOf(j));											
//											}
//										} catch (NumberFormatException | IOException e) {
//											e.printStackTrace();
//										}
//									}
//								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				} finally {
					String oldDefects4jFile = Activator.getDefault().getPreferenceStore()
							.getString(TregressionPreference.RESULTS_FILE);
					
					if (regs4jFile != null && regs4jFile.exists() && oldDefects4jFile != null
							&& new File(oldDefects4jFile).exists()) {
						Map<String, List<String>> keys = new HashMap<String, List<String>>();
						keys.put("data", Arrays.asList("project", "bug_ID"));
						ExperimentReportComparisonReporter.reportChange("regs4j_compare.xlsx", oldDefects4jFile, regs4jFile.getAbsolutePath(), 
									Arrays.asList(new TextComparisonRule(null), new SimulatorComparisonRule()), keys);
					}
				}
					
//					System.out.println("all the trials");
//					for(int j=0; j<trials.size(); j++) {
//						System.out.println("Trial " + (j+1));
//						System.out.println(trials.get(j));
//					}
				
				return Status.OK_STATUS;
			}
			
			private void setTestCase(DED datas, String tc) {
				if(datas.getTrueData()!=null){
					datas.getTrueData().testcase = tc;					
				}
				for(DeadEndData data: datas.getFalseDatas()){
					data.testcase = tc;
				}
			}
		};
		job.schedule();
	}
}

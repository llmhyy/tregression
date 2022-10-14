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
				
				String[] projects = Dataset.getProjectNames();
				int[] bugNum = Dataset.getBugNums();
				projects = new String[] {"uniVocity_univocity-parsers"};
				bugNum = new int[] {3};
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
						
						for(int j=3; j<=bugNum[i]; j++) {
							
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
							String fixPath = pathConfig.getCorrectPath(projects[i], Integer.toString(j));
							
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

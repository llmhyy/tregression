package tregression.handler.runall;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import experiment.utils.report.ExperimentReportComparisonReporter;
import experiment.utils.report.rules.SimulatorComparisonRule;
import experiment.utils.report.rules.TextComparisonRule;
import microbat.Activator;
import sav.common.core.utils.SingleTimer;
import tregression.constants.Dataset;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.ReadEmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialReader;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.config.MutationFrameworkProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.handler.paths.PathConfiguration;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.preference.TregressionPreference;

public class AllMutationFrameworkHandlerExecutor extends RunAllInDatasetExecutor {
	public void execute() {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int skippedNum = 0;
				int endNum = 500;
				
				String[] projects = Dataset.getProjectNames();
				int[] bugNum = Dataset.getBugNums();

				String fileName = "benchmark" + File.separator + "ben.xlsx";
				Map<ReadEmpiricalTrial, ReadEmpiricalTrial> map = new HashMap<>();
				try {
					map = new TrialReader().readXLSX(fileName);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				String prefix = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH) + File.separator;
				
				int count = 0;
				File mutationFrameworkReportFile = null;
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
							}
							
							System.out.println("***working on the " + j + "th bug of " + projects[i] + " project.");
							PathConfiguration pathConfig = PathConfigurationFactory.createPathConfiguration(Dataset.MUTATION_FRAMEWORK);
							String buggyPath = pathConfig.getBuggyPath(projects[i], Integer.toString(j));
							String fixPath = pathConfig.getCorrectPath(projects[i], Integer.toString(j));
							
							if (!(Files.exists(Paths.get(buggyPath)) && Files.exists(Paths.get(fixPath)))) {
								continue;
							}
							System.out.println("analyzing the " + j + "th bug in " + projects[i] + " project.");
							
							TrialGenerator generator = new TrialGenerator();
							TrialGenerator0 generator0 = new TrialGenerator0();
							
							ProjectConfig mutationFrameworkConfig = MutationFrameworkProjectConfig.getConfig(projects[i], String.valueOf(j));

							List<EmpiricalTrial> trials = generator0.generateTrials(buggyPath, fixPath, 
									false, false, false, 3, false, true, mutationFrameworkConfig, null);
							
							TrialRecorder recorder;
							try {
								recorder = new TrialRecorder(Dataset.MUTATION_FRAMEWORK);
								mutationFrameworkReportFile = recorder.export(trials, projects[i], j);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				} finally {
					String oldMutationFrameworkReportFile = Activator.getDefault().getPreferenceStore()
							.getString(TregressionPreference.RESULTS_FILE);
					
					if (mutationFrameworkReportFile != null && mutationFrameworkReportFile.exists() && oldMutationFrameworkReportFile != null
							&& new File(oldMutationFrameworkReportFile).exists()) {
						Map<String, List<String>> keys = new HashMap<String, List<String>>();
						keys.put("data", Arrays.asList("project", "bug_ID"));
						ExperimentReportComparisonReporter.reportChange("mutation_framework_compare.xlsx", oldMutationFrameworkReportFile, mutationFrameworkReportFile.getAbsolutePath(), 
									Arrays.asList(new TextComparisonRule(null), new SimulatorComparisonRule()), keys);
					}
				}
				
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

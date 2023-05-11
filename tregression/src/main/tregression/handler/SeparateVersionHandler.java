package tregression.handler;

import java.io.IOException;
import java.util.List;

import org.apache.bcel.Repository;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;

import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.util.JavaUtil;
import tregression.constants.Dataset;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.handler.paths.PathConfiguration;
import tregression.handler.paths.PathConfigurationFactory;
import tregression.preference.TregressionPreference;

public class SeparateVersionHandler extends AbstractHandler{

	TrialGenerator generator = new TrialGenerator();
	TrialGenerator0 generator0 = new TrialGenerator0();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
				String projectPath = preferenceStore.getString(TregressionPreference.PROJECT_NAME);
				String bugID = preferenceStore.getString(TregressionPreference.BUG_ID);
				Dataset datasetType = Dataset.getTypeFromPref();
				
				PathConfiguration pathConfiguration = PathConfigurationFactory.createPathConfiguration(datasetType);
				String buggyPath = pathConfiguration.getBuggyPath(projectPath, bugID);
				String fixPath = pathConfiguration.getCorrectPath(projectPath, bugID);
				
				String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				
				String testcase = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
				
				System.out.println("working on the " + id + "th bug of " + projectName + " project.");
				
				ProjectConfig config = ConfigFactory.createConfig(projectName, id, buggyPath, fixPath, datasetType);
				
				if(config == null) {
					try {
						throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + id);						
					} catch (Exception e) {
						e.printStackTrace();
					}
					return Status.CANCEL_STATUS;
				}
				
				List<EmpiricalTrial> trials = generator0.generateTrials(buggyPath, fixPath, 
						false, false, false, 3, true, true, config, testcase);
				
				if(trials.size() != 0) {
					PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();					
				}
				
				System.out.println("all the trials");
				for(int i=0; i<trials.size(); i++) {
					System.out.println("Trial " + (i+1));
					System.out.println(trials.get(i));
					
					EmpiricalTrial t = trials.get(i);
					Trace trace = t.getBuggyTrace();
					
					if(!t.getDeadEndRecordList().isEmpty()){
						Repository.clearCache();
						DeadEndRecord record = t.getDeadEndRecordList().get(0);
						DED datas = record.getTransformedData(trace);
						setTestCase(datas, t.getTestcase());						
						try {
							String fileSuffix;
							if (datasetType.equals(Dataset.DEFECTS4J)) {
								fileSuffix = "_d4j";
							} else {
								fileSuffix = "_r4j";
							}
							new DeadEndCSVWriter(fileSuffix, null).export(datas.getAllData(), projectName, id);
						} catch (NumberFormatException | IOException e) {
							e.printStackTrace();
						}
					}
					
				}
				
				try {
					TrialRecorder recorder = new TrialRecorder(datasetType);
					recorder.export(trials, projectName, Integer.valueOf(id));
					
				} catch (IOException e) {
					e.printStackTrace();
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
		
		return null;
	}
	
	

}

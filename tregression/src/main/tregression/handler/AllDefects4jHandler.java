package tregression.handler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.Activator;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.ReadEmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialReader;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.empiricalstudy.training.TrainingDataTransfer;
import tregression.preference.TregressionPreference;

public class AllDefects4jHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int skippedNum = 0;
				int endNum = 500;
				
				String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
				int[] bugNum = {26, 133, 65, 106, 38, 27};
				
				String fileName = "defects4j0.old.xlsx";
				Map<ReadEmpiricalTrial, ReadEmpiricalTrial> map = new HashMap<>();
				try {
					map = new TrialReader().readXLSX(fileName);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
//				String[] projects = {"Chart"};
//				int[] bugNum = {2};
				
//				String[] projects = {"Lang"};
//				int[] bugNum = {65};
				
//				String[] projects = {"Time"};
//				int[] bugNum = {27};
				
				String prefix = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH) + File.separator;
				
				int count = 0;
				for(int i=0; i<projects.length; i++) {
					
					for(int j=1; j<=bugNum[i]; j++) {
						
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
							
							String deadEndType = t.getDeadEndType();
							if(deadEndType==null || !deadEndType.equals("control")){
								continue;
							}
						}
						
						System.out.println("***working on the " + j + "th bug of " + projects[i] + " project.");
						
						String buggyPath = prefix + projects[i] + File.separator + j + File.separator + "bug";
						String fixPath = prefix + projects[i] + File.separator + j + File.separator + "fix";
						
						System.out.println("analyzing the " + j + "th bug in " + projects[i] + " project.");
						
						TrialGenerator generator = new TrialGenerator();
						TrialGenerator0 generator0 = new TrialGenerator0();
						
						Defects4jProjectConfig d4jConfig = Defects4jProjectConfig.getD4JConfig(projects[i], j);
						List<EmpiricalTrial> trials = generator0.generateTrials(buggyPath, fixPath, 
								false, true, false, 3, false, true, d4jConfig, null);
						
						TrialRecorder recorder;
						try {
							recorder = new TrialRecorder();
							recorder.export(trials, projects[i], j);
							
							for(EmpiricalTrial t: trials){
								
								if(!t.getDeadEndRecordList().isEmpty()){
									Repository.clearCache();
									DeadEndRecord record = t.getDeadEndRecordList().get(0);
//									DED datas = new TrainingDataTransfer().transfer(record, t.getBuggyTrace());
									DED datas = record.getTransformedData(t.getBuggyTrace());
									setTestCase(datas, t.getTestcase());						
									try {
										//new DeadEndReporter().export(datas.getAllData(), projects[i], Integer.valueOf(j));
										
										if(!t.getRootCauseFinder().getCausalityGraph().getRoots().isEmpty()){
											new DeadEndCSVWriter().export(datas.getAllData(), projects[i], String.valueOf(j));											
										}
									} catch (NumberFormatException | IOException e) {
										e.printStackTrace();
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						
					}
					
					
//					System.out.println("all the trials");
//					for(int j=0; j<trials.size(); j++) {
//						System.out.println("Trial " + (j+1));
//						System.out.println(trials.get(j));
//					}
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

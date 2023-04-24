package tregression.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.bcel.Repository;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.util.JavaUtil;
import tregression.datacollection.DataCollector;
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
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;


public class DataCollectionHandler extends AbstractHandler {

	TrialGenerator generator = new TrialGenerator();
	TrialGenerator0 generator0 = new TrialGenerator0();
	
	BuggyTraceView buggyView;
	CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
//		String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
//		String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
//		DataCollector collector = new DataCollector(buggyView, correctView.getTrace(), PlayRegressionLocalizationHandler.finder);
//		collector.exportFeedbackData(PlayRegressionLocalizationHandler.finder.getRegressionNodeList(), id, projectName);
//		collector.exportVectorData(PlayRegressionLocalizationHandler.finder.getRegressionNodeList(), id, projectName);

		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Integer> list = Arrays.asList(new Integer[]{2,7,12});
				
				for(int bug_id = 1; bug_id<=106; ++bug_id) {
					
					if (list.contains(bug_id)) {
						continue;
					}
					
					String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
					String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
					bugID = String.valueOf(bug_id);
					
					String folderPath = "C:\\Users\\arkwa\\Documents\\NUS\\Dissertation\\Classifier\\Feedback\\data";
					String failReportDirName = "Fail_Reports";
					String failReportFileName = "Fail_Report_" + projectPath + ".txt";
					
					Path failReportDirPath = Paths.get(folderPath, failReportDirName);
					failReportDirPath.toFile().mkdir();
					Path failReportPath = Paths.get(folderPath, failReportDirName, failReportFileName);
					
					try {
						
						String buggyPath = PathConfiguration.getBuggyPath(projectPath, bugID);
						String fixPath = PathConfiguration.getCorrectPath(projectPath, bugID);
						
						String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
						String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
						id = String.valueOf(bug_id);
						
						String testcase = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
						
						System.out.println("working on the " + id + "th bug of " + projectName + " project.");
						
						ProjectConfig config = ConfigFactory.createConfig(projectName, id, buggyPath, fixPath);
						
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
							
							if (!t.getDeadEndRecordList().isEmpty()){
								Repository.clearCache();
								DeadEndRecord record = t.getDeadEndRecordList().get(0);
								DED datas = record.getTransformedData(trace);
								setTestCase(datas, t.getTestcase());						
								try {
									new DeadEndCSVWriter("_d4j", null).export(datas.getAllData(), projectName, id);
								} catch (NumberFormatException | IOException e) {
									e.printStackTrace();
								}
							}
							
						}
						
						try {
							TrialRecorder recorder = new TrialRecorder();
							recorder.export(trials, projectName, Integer.valueOf(id));
							
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						Display.getDefault().syncExec(new Runnable() {
						    @Override
						    public void run() {
								try {
									buggyView = (BuggyTraceView)PlatformUI.getWorkbench().
											getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
									
									correctView = (CorrectTraceView)PlatformUI.getWorkbench().
											getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
									
								} catch (PartInitException e) {
									e.printStackTrace();
								} 
						    }
						});
						
						DataCollector collector = new DataCollector(buggyView, correctView.getTrace(), PlayRegressionLocalizationHandler.finder);
						collector.exportFeedbackData(PlayRegressionLocalizationHandler.finder.getRegressionNodeList(), id, projectName);
						collector.exportVectorData(PlayRegressionLocalizationHandler.finder.getRegressionNodeList(), id, projectName);
						
					} catch (Exception e ) {
						try {
							System.out.println("FailReport: " + bugID);
							File failReport = failReportPath.toFile();
							failReport.createNewFile();
							FileWriter writer = new FileWriter(failReport, true);
							writer.write(bugID + '\n');
							writer.close();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
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
		
		return null;
	}

}

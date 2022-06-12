package tregression.handler;

import java.nio.file.Path;
import java.nio.file.Paths;

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
import microbat.preference.MicrobatPreference;
import microbat.util.JavaUtil;
import tregression.autofeedback.AutoFeedbackMethods;
import tregression.autofeedbackevaluation.AutoDebugEvaluator;
import tregression.autofeedbackevaluation.AvgAccMeasurement;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class AutoFeedbackHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
//				String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
//				String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				System.out.println("Start debugging");
				String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				int bugCount = 26;
				
				AutoDebugEvaluator evaluator = new AutoDebugEvaluator(getMethod());
				evaluator.evaulateAll(projectPath, bugCount);
				
				String folder = "C:\\Users\\arkwa\\Documents\\NUS\\Dissertation\\Measurements";
				String fileName = AutoDebugEvaluator.genFileName(projectPath, getMethod());
				
				Path path = Paths.get(folder, fileName);
				// evaluator.exportCSV(path);
				AvgAccMeasurement avgMeasurement = evaluator.getAvgMeasurement();
				System.out.println(avgMeasurement);

				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}
	
	private void updateTraceView() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	private AutoFeedbackMethods getMethod() {
		String selectedMethodName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_FEEDBACK_METHOD);
		AutoFeedbackMethods selectedMethod = AutoFeedbackMethods.valueOf(selectedMethodName);
		return selectedMethod;
	}
}

package tregression.handler;

import java.nio.charset.StandardCharsets;

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

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.stepvectorizer.StepVectorizer;
import microbat.util.JavaUtil;
import microbat.Activator;
import tregression.autofeedback.AutoFeedbackMethods;
import tregression.autofeedback.BaselineFeedbackGenerator;
import tregression.autofeedback.FeedbackGenerator;
import tregression.autofeedbackevaluation.AccMeasurement;
import tregression.autofeedbackevaluation.AutoDebugEvaluator;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.RootCauseNode;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class TestingHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setup();
				
				if (buggyView == null && correctView == null ) {
					System.out.println("buggyView or correctView is null");
					return null;
				}
				
				AutoFeedbackMethods method = AutoFeedbackMethods.BASELINE;
				AutoDebugEvaluator evaluator = new AutoDebugEvaluator(method);
				evaluator.setup(getProjectName(), getBugID());
				AccMeasurement measurement = evaluator.evaluate(getProjectName(), Integer.parseInt(getBugID()));
				System.out.println(measurement);		
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
	private void setup() {
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
	
	private String getProjectName() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
	}
	
	private String getBugID() {
		return Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
	}
}

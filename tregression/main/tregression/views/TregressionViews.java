package tregression.views;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TregressionViews {
	
	public static CorrectTraceView getCorrectTraceView(){
		CorrectTraceView view = null;
		try {
			view = (CorrectTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
	
	public static BuggyTraceView getBuggyTraceView(){
		BuggyTraceView view = null;
		try {
			view = (BuggyTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
	
	public static StepPropertyView getStepPropertyView(){
		StepPropertyView view = null;
		try {
			view = (StepPropertyView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(StepPropertyView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
}

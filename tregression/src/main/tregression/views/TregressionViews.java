package tregression.views;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TregressionViews {
	public static final String CORRECT_TRACE = "tregression.evalView.correctTraceView";
	public static final String BUGGY_TRACE = "tregression.evalView.buggyTraceView";
	
	public static CorrectTraceView getBeforeTraceView(){
		CorrectTraceView view = null;
		try {
			view = (CorrectTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(CORRECT_TRACE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
	
	public static BuggyTraceView getAfterTraceView(){
		BuggyTraceView view = null;
		try {
			view = (BuggyTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(BUGGY_TRACE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
}

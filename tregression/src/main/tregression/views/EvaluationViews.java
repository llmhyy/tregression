package tregression.views;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class EvaluationViews {
	public static final String BEFORE_TRACE = "tregression.evalView.beforeTraceView";
	public static final String AFTER_TRACE = "tregression.evalView.afterTraceView";
	
	public static CorrectTraceView getBeforeTraceView(){
		CorrectTraceView view = null;
		try {
			view = (CorrectTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(BEFORE_TRACE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
	
	public static BuggyTraceView getAfterTraceView(){
		BuggyTraceView view = null;
		try {
			view = (BuggyTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(AFTER_TRACE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		return view;
	}
}

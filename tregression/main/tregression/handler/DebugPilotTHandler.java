package tregression.handler;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.handler.DebugPilotHandler;
import microbat.views.PathView;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class DebugPilotTHandler extends DebugPilotHandler {
	
	protected CorrectTraceView correctView;
	
	@Override
	protected void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
					pathView = (PathView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(PathView.ID);
					pathView.setBuggyView(buggyView);
					trace = buggyView.getTrace();
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					pathView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
}

package tregression.handler;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.handler.StepwisePropagationHandler;
import microbat.views.MicroBatViews;
import microbat.views.PathView;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class StepwisePropagationTHandler extends StepwisePropagationHandler {

	private CorrectTraceView correctView;
	
	
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

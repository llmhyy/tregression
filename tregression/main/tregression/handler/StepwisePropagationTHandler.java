package tregression.handler;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.handler.StepwisePropagationHandler;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.StepwiseTraceView;

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
					stepwiseTraceView = (StepwiseTraceView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(StepwiseTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					stepwiseTraceView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
 }

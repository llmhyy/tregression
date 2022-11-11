package tregression.handler;

import java.util.ArrayList;
import java.util.List;

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

import microbat.baseline.probpropagation.StepwisePropagator;
import microbat.model.value.VarValue;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class StepwisePropagationHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private static List<VarValue> inputs = new ArrayList<>();
	private static List<VarValue> outputs = new ArrayList<>();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				setup();
				
				if (!isReady()) {
					return Status.OK_STATUS;
				}
				
				StepwisePropagator propagator = new StepwisePropagator(buggyView.getTrace(), StepwisePropagationHandler.inputs, StepwisePropagationHandler.outputs);
				
				System.out.println("Propagation Start");
				propagator.propagate();
				
				System.out.println("Propagation End");
				
				return Status.OK_STATUS;
			}
			
		};
		
		job.schedule();
		return null;
	}
	
	private boolean isReady() {
		if (this.buggyView == null) {
			throw new RuntimeException("StepwisePropagationHandler: Buggy view is not ready");
		}
		
		if (this.correctView == null) {
			throw new RuntimeException("StepwisePropagationHandler: Correct view is not ready");
		}
		
		if (StepwisePropagationHandler.inputs.isEmpty()) {
			throw new RuntimeException("StepwisePropagationHandler: There are no inputs");
		}
		
		if (StepwisePropagationHandler.outputs.isEmpty()) {
			throw new RuntimeException("StepwisePropagationHandler: There are no outputs");
		}
		
		return true;
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

	public static void addInputs(List<VarValue> inputs) {
		StepwisePropagationHandler.inputs.addAll(inputs);		
		for (VarValue input : StepwisePropagationHandler.inputs) {
			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
		}
	}
	
	public static void printIO() {
		for (VarValue input : StepwisePropagationHandler.inputs) {
			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
		}
		for (VarValue output : StepwisePropagationHandler.outputs) {
			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
		}
	}
	
	public static void addOutpus(List<VarValue> outputs) {
		StepwisePropagationHandler.outputs.addAll(outputs);
		
		for (VarValue output : StepwisePropagationHandler.outputs) {
			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
		}
	}
	
	public static void clearData() {
		StepwisePropagationHandler.inputs.clear();
		StepwisePropagationHandler.outputs.clear();
	}
	
}

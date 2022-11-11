package tregression.handler;

import java.util.ArrayList;
import java.util.Collection;
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
import microbat.handler.RequireIO;
import microbat.model.value.VarValue;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;
import tregression.views.StepDetailIOUI;

public class StepwisePropagationHandler extends AbstractHandler implements RequireIO {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	private List<VarValue> inputs = new ArrayList<>();
	private List<VarValue> outputs = new ArrayList<>();
	
	private static boolean registerFlag = false;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				if (!StepwisePropagationHandler.registerFlag) {
					registerHandler();
					return Status.OK_STATUS;
				}
				
				setup();
				
				if (!isReady()) {
					return Status.OK_STATUS;
				}
				
				StepwisePropagator propagator = new StepwisePropagator(buggyView.getTrace(), inputs, outputs);
				
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
		
		if (this.inputs.isEmpty()) {
			throw new RuntimeException("StepwisePropagationHandler: There are no inputs");
		}
		
		if (this.outputs.isEmpty()) {
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

	@Override
	public void registerHandler() {
		StepDetailIOUI.registerHandler(this);
		StepwisePropagationHandler.registerFlag = true;
		
		System.out.println();
		System.out.println("StepwisePropagationHandler is now registered to buttons");
		System.out.println("Please select inputs and outputs");
	}

	@Override
	public void addInputs(Collection<VarValue> inputs) {
		this.inputs.addAll(inputs);		
		for (VarValue input : this.inputs) {
			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
		}
	}

	@Override
	public void addOutputs(Collection<VarValue> outputs) {
		this.outputs.addAll(outputs);
		
		for (VarValue output : this.outputs) {
			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
		}
	}

	@Override
	public void printIO() {
		for (VarValue input : this.inputs) {
			System.out.println("StepwisePropagationHandler: Selected Inputs: " + input.getVarID());
		}
		for (VarValue output : this.outputs) {
			System.out.println("StepwisePropagationHandler: Selected Outputs: " + output.getVarID());
		}
	}

	@Override
	public void clearData() {
		this.inputs.clear();
		this.outputs.clear();
		System.out.println("Clear Data");
	}
	
}

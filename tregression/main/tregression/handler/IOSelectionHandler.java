package tregression.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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

import debuginfo.DebugInfo;
import debuginfo.NodeFeedbacksPair;
import debuginfo.NodeVarPair;
import iodetection.IODetector;
import iodetection.StoredIOParser;
import iodetection.IODetector.InputsAndOutput;
import iodetection.IODetector.NodeVarValPair;
import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class IOSelectionHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();

		Job job = new Job("IO Selection") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setup();
				execute();
				return Status.OK_STATUS;
			}
		};

		job.schedule();
		return null;
	}
	
	protected void execute() {
		boolean inputsSelected = !DebugInfo.getInputs().isEmpty();
		boolean outputSelected = !DebugInfo.getOutputs().isEmpty();
		if (outputSelected) {
			if (inputsSelected) {
				// Store the selected IO
				this.storeIO();
				System.out.println("Inputs and output have been selected and stored");
				return;
			} else {
				System.out.println("Output has been selected, please select the inputs");
				return;
			}
		} else {
			if (inputsSelected) {
				System.out.println("Inputs have been selected, please select the output");
				return;
			} else {
				// If there's no IO selected, automatically detect IO
				Trace buggyTrace = this.buggyView.getTrace();
				PairList pairList = this.buggyView.getPairList();
				Optional<InputsAndOutput> ioOptional = this.getIO(buggyTrace, pairList);
				if (ioOptional.isEmpty()) {
					System.out.println("Cannot extract input and output, please select IO manually");
					return;
				} else {
					InputsAndOutput IO = ioOptional.get();
					List<NodeVarValPair> inputNodeVarPairs = IO.getInputs();
					NodeVarValPair outputNodeVarPair = IO.getOutput();
					
					// add inputs
					List<VarValue> inputs = new ArrayList<>();
					inputNodeVarPairs.forEach((nodeVarValPair) -> {
						inputs.add(nodeVarValPair.getVarVal());
					});
					DebugInfo.addInputs(inputs);
					
					// add output
					VarValue outputVar = outputNodeVarPair.getVarVal();
					DebugInfo.addOutput(outputVar);
					if (outputVar.getVarID().startsWith("CR_")) {
						UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
						NodeFeedbacksPair pair = new NodeFeedbacksPair(outputNodeVarPair.getNode(), feedback);
						DebugInfo.addNodeFeedbacksPair(pair);
					}
				}
			}
		}
	}
	
	protected void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	protected Optional<InputsAndOutput> getIO(Trace trace, PairList pairList) {
		
		String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
		String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
		
		String buggyPath = PathConfiguration.getBuggyPath(projectName, bugID);
		String fixPath = PathConfiguration.getCorrectPath(projectName, bugID);
		ProjectConfig config = ConfigFactory.createConfig(projectName, bugID, buggyPath, fixPath);
		String testSrcPath = config.srcTestFolder;
		
		String IOStoragePath = "D:\\Defects4j_IO";
		
		IODetector ioDetector = new IODetector(trace, testSrcPath, pairList);
		StoredIOParser IOParser = new StoredIOParser(IOStoragePath, projectName, bugID);
		HashMap<String, List<String[]>> storedIO = IOParser.getStoredIO();
		if (storedIO == null) {
			// stored IO not found, detect IO and store
			Optional<InputsAndOutput> result = ioDetector.detect();
			IOParser.storeIO(result);
			return result;
		}
		// read from stored IO
		List<String[]> inputs = storedIO.get(InputsAndOutput.INPUTS_KEY);
		List<String[]> output = storedIO.get(InputsAndOutput.OUTPUT_KEY);
		return ioDetector.detect(inputs, output);
	}
	
	protected void storeIO() {
		String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
		String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
		String IOStoragePath = "D:\\Defects4j_IO";
		
		StoredIOParser IOParser = new StoredIOParser(IOStoragePath, projectName, bugID);
		List<NodeVarPair> inputList = DebugInfo.getInputNodeVarPairs();
		List<NodeVarPair> outputList = DebugInfo.getOutputNodeVarPairs();
		List<NodeVarValPair> inputs = new ArrayList<>();
		inputList.forEach((pair) -> {
			inputs.add(this.convertPairToRequiredClass(pair));
		});
		NodeVarValPair output = this.convertPairToRequiredClass(outputList.get(0));
		InputsAndOutput IO = new InputsAndOutput(inputs, output);
		Optional<InputsAndOutput> result = Optional.of(IO);
		IOParser.storeIO(result);
	}
	
	/**
	 * NodeVarPair and NodeVarValPair are similar classes in Microbat and Tregression respectively.
	 * They are created separately to avoid cyclic dependency.
	 * 
	 * @param pair as debuginfo.NodeVarPair
	 * @return pair as iodetection.IODetector.NodeVarValPair
	 */
	private NodeVarValPair convertPairToRequiredClass(NodeVarPair pair) {
		return new NodeVarValPair(pair.getNode(), pair.getVariable(), pair.getVarContainingNodeID());
	}
}

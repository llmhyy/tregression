package baseline;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import microbat.mutation.MutationAgent;
import sav.strategies.dto.AppJavaClassPath;
import testio.TestIOFramework;
import testio.model.IOModel;
import testio.model.TestIO;
import tracediff.TraceDiff;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

/**
 * MutationAgent_Tregression is the extended version
 * MutationAgent in Microbat. It not only mutate the
 * trace, it will also construct a pair list and the
 * DiffMatcher between the correct trace and buggy
 * trace.
 * @author David
 *
 */
public class MutationAgent_Tregression extends MutationAgent {
	
	protected Trace correctTrace = null;
	protected PairList pairList = null;
	protected DiffMatcher matcher = null;
	
	protected List<VarValue> inputs = new ArrayList<>();
	protected List<VarValue> outputs = new ArrayList<>();
	
	public MutationAgent_Tregression(String projectPath, String java_path, int stepLimit) {
		super(projectPath, java_path, stepLimit);
	}
	
	public void startMutation() {
		
		super.startMutation();

		this.correctTrace = super.result.getOriginalTrace();
		
		// Set up the Class Path
		final String projName = super.result.getMutatedProject().getProjectName();
		final String regressionID = super.testCase.toString();
		
		ProjectConfig config = ConfigFactory.createConfig(projName, regressionID, super.mutatedProjPath, super.originalProjPath);
		tregression.empiricalstudy.TestCase tc = new tregression.empiricalstudy.TestCase(super.result.getTestClass(), result.getTestSimpleName());
		AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(super.mutatedProjPath, tc, config);
		AppJavaClassPath correctApp = AppClassPathInitializer.initialize(super.originalProjPath, tc, config);
		
		this.buggyTrace.setAppJavaClassPath(buggyApp);
		this.correctTrace.setAppJavaClassPath(correctApp);
		
		// Set up the diffMatcher
		this.matcher = new DiffMatcher(srcFolderPath, testFolderPath, super.mutatedProjPath, super.originalProjPath);
		this.matcher.matchCode();
		
		// Convert tracediff.PairList to tregression.PairList
		tracediff.model.PairList pairList_traceDiff = TraceDiff.getTraceAlignment(srcFolderPath, testFolderPath,
                super.mutatedProjPath, super.originalProjPath,
                result.getMutatedTrace(), result.getOriginalTrace());

		
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		this.pairList = traceMatcher.matchTraceNodePair(this.buggyTrace, this.correctTrace, this.matcher);
		
		this.rootCauses = result.getRootCauses();
		
		TestIOFramework testIOFramework = new TestIOFramework();
		TestIO testIO = testIOFramework.getBuggyTestIOs(
				super.result.getOriginalResult(),
				super.result.getOriginalResultWithAssertions(),
				super.result.getMutatedResult(),
				super.result.getMutatedResultWithAssertions(),
				super.result.getOriginalProject().getRoot(),
                super.result.getMutatedProject().getRoot(), 
                pairList_traceDiff, 
                super.result.getTestClass(),
                super.result.getTestSimpleName());
		
		if (testIO != null) {
			for (IOModel model : testIO.getInputs()) {
				this.inputs.add(model.getValue());
			}
			this.outputs.add(testIO.getOutput().getValue());
		}
	}
	
	public Trace getCorrectTrace() {
		return this.correctTrace;
	}
	
	public PairList getPairList() {
		return this.pairList;
	}
	
	public DiffMatcher getMatcher() {
		return this.matcher;
	}
	
	public void reset() {
		super.reset();
		
		this.correctTrace = null;
		this.pairList = null;
		this.matcher = null;
		
		this.inputs.clear();
		this.outputs.clear();
	}
	
	public List<VarValue> getInputs() {
		return this.inputs;
	}
	
	public List<VarValue> getOutputs() {
		return this.outputs;
	}
}

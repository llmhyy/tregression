package baseline;

import java.util.ArrayList;
import java.util.List;

import jmutation.MutationFramework;
import jmutation.model.MutationResult;
import jmutation.model.TestCase;
import jmutation.model.project.Project;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import sav.strategies.dto.AppJavaClassPath;
import testio.TestIOFramework;
import testio.model.IOModel;
import testio.model.TestIO;
import tracediff.TraceDiff;
import tracediff.model.TraceNodePair;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

public class MutationAgent {

	private final int maxMutationLimit = 10;
	private final int maxMutation = 1;
	
	private int mutationCount = 0;
	
	private final String srcFolderPath = "src\\main\\java";
	private final String testFolderPath = "src\\test\\java";
	private final String projectPath;
	private final String dropInDir;
	private final String microbatConfigPath;
	
	private Trace buggyTrace = null;
	private Trace correctTrace = null;
	private PairList pairList = null;
	private DiffMatcher matcher = null;
	private String mutatedProjPath = null;
	private String originalProjPath = null;
	private TestCase testCase = null;
	
	private List<TraceNode> rootCauses = new ArrayList<>();
	private List<VarValue> inputs = new ArrayList<>();
	private List<VarValue> outputs = new ArrayList<>();
	
	private int testCaseID = -1;
	private int seed = 1;
	
	public MutationAgent(String projectPath, String dropInDir, String microbatConfigPath) {
		this.projectPath = projectPath;
		this.dropInDir = dropInDir;
		this.microbatConfigPath = microbatConfigPath;
	}
	
	public void startMutation() {
		
		if (!isReady()) {
			throw new RuntimeException("Mutation Agent is not ready");
		}
		
		System.out.println("Mutating Test Case " + this.testCaseID);
		this.reset();
		
		MutationFramework mutationFramework = new MutationFramework();
		mutationFramework.setProjectPath(projectPath);
		mutationFramework.setDropInsDir(dropInDir);
		mutationFramework.setMicrobatConfigPath(microbatConfigPath);
		mutationFramework.setMaxNumberOfMutations(maxMutation);
		
		this.testCase = mutationFramework.getTestCases().get(this.testCaseID);
		mutationFramework.setTestCase(testCase);
		
		// Mutate project until it fail the test case
		MutationResult result = null;
		boolean testCaseFailed = false;
		for (int i=0; i<maxMutationLimit; i++) {
			this.mutationCount++;
			mutationFramework.setSeed(this.seed);
			result = mutationFramework.startMutationFramework();
			if (!result.mutatedTestCasePassed()) {
				testCaseFailed = true;
				break;
			}
		}
		
		if (!testCaseFailed) {
			throw new RuntimeException(this.genErrorMsg("Cannot fail the test case"));
		}

		Project mutatedProject = result.getMutatedProject();
		Project originalProject = result.getOriginalProject();
		
		this.mutatedProjPath = mutatedProject.getRoot().getAbsolutePath();
		this.originalProjPath = originalProject.getRoot().getAbsolutePath();
		
		this.buggyTrace = result.getMutatedTrace();
		this.buggyTrace.setSourceVersion(true);
		
		this.correctTrace = result.getOriginalTrace();
		
		// Set up the Class Path
		final String projName = result.getMutatedProject().getProjectName();
		final String regressionID = testCase.toString();
		final String buggyPath = mutatedProject.getRoot().getAbsolutePath();
		final String correctPath = originalProject.getRoot().getAbsolutePath();
		
		ProjectConfig config = ConfigFactory.createConfig(projName, regressionID, buggyPath, correctPath);
		tregression.empiricalstudy.TestCase tc = new tregression.empiricalstudy.TestCase(result.getTestClass(), result.getTestSimpleName());
		AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(buggyPath, tc, config);
		AppJavaClassPath correctApp = AppClassPathInitializer.initialize(correctPath, tc, config);
		
		this.buggyTrace.setAppJavaClassPath(buggyApp);
		this.correctTrace.setAppJavaClassPath(correctApp);
		
		// Set up the diffMatcher
		this.matcher = new DiffMatcher(srcFolderPath, testFolderPath, mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath());
		this.matcher.matchCode();
		
		// Convert tracediff.PairList to tregression.PairList
		tracediff.model.PairList pairList_traceDiff = TraceDiff.getTraceAlignment(srcFolderPath, testFolderPath,
                mutatedProject.getRoot().getAbsolutePath(), originalProject.getRoot().getAbsolutePath(),
                result.getMutatedTrace(), result.getOriginalTrace());
//		List<tregression.model.TraceNodePair> pairLTregression = new ArrayList<>();
//		for (TraceNodePair pair : pairList.getPairList()) {
//			pairLTregression.add(new tregression.model.TraceNodePair(pair.getBeforeNode(), pair.getAfterNode()));
//		}
//		this.pairList = new PairList(pairLTregression);
		
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		this.pairList = traceMatcher.matchTraceNodePair(this.buggyTrace, this.correctTrace, this.matcher);
		
		this.rootCauses = result.getRootCauses();
		
		TestIOFramework testIOFramework = new TestIOFramework();
		TestIO testIO = testIOFramework.getBuggyTestIOs(result.getOriginalResult(),
				result.getOriginalResultWithAssertions(),
				result.getMutatedResult(),
				result.getMutatedResultWithAssertions(), originalProject.getRoot(),
                mutatedProject.getRoot(), pairList_traceDiff, result.getTestClass(),
                result.getTestSimpleName());
		
		if (testIO == null) {
//			throw new RuntimeException(this.genErrorMsg("testIO is null"));
		} else {
			if (testIO.getInputs().isEmpty() || testIO.getOutput() == null) {
//				throw new RuntimeException(this.genErrorMsg("No IO"));
			}
			for (IOModel model : testIO.getInputs()) {
				this.inputs.add(model.getValue());
			}
			
			this.outputs.add(testIO.getOutput().getValue());
		}
	}
	
	public void setSeed(int seed) {
		this.seed = seed;
	}
	
	public Trace getBuggyTrace() {
		return this.buggyTrace;
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
	
	public List<TraceNode> getRootCause() {
		return this.rootCauses;
	}
	
	public List<VarValue> getInputs() {
		return this.inputs;
	}
	
	public List<VarValue> getOutputs() {
		return this.outputs;
	}
	
	public String getMutatedProjPath() {
		return this.mutatedProjPath;
	}
	
	public String getOriginalProjPath() {
		return this.originalProjPath;
	}
	
	public TestCase getTestCase() {
		return this.testCase;
	}
	
	public boolean isReady() {
		return this.testCaseID>=0;
	}
	
	public void reset() {
		this.buggyTrace = null;
		this.correctTrace = null;
		this.pairList = null;
		this.matcher = null;
		
		this.mutatedProjPath = "";
		this.originalProjPath = "";
		
		this.rootCauses.clear();
		this.inputs.clear();
		this.outputs.clear();
		
		this.mutationCount = 0;
	}
	
	public void setTestCaseID(int testCaseID) {
		this.testCaseID = testCaseID;
	}
	
	public String genErrorMsg(final String msg) {
		return "MutationAgent: " + msg;
	}
	
	public int getMutationCount() {
		return this.mutationCount;
	}
}

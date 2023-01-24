package mutation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.SystemUtils;

import jmutation.MutationFramework;
import jmutation.constants.ResourcesPath;
import jmutation.model.MicrobatConfig;
import jmutation.model.mutation.MutationFrameworkResult;
import jmutation.model.TestCase;
import jmutation.model.mutation.MutationFrameworkConfig;
import jmutation.model.mutation.MutationFrameworkConfig.MutationFrameworkConfigBuilder;
import jmutation.model.project.Project;
import jmutation.utils.RandomSingleton;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

/**
 * MutationAgent will make use of MutationFramework to mutation
 * the code and fail the targeted test case.
 * 
 * @author David
 *
 */
public class MutationAgent {
	
	/**
	 * Number of time allowed for mutation framework attempt to fail the test case
	 */
	protected final int maxMutationLimit = 10;
	
	/**
	 * Maximum allowed amount of changes in the source code
	 */
	protected final int maxChanges = 1;
	
	/**
	 * Number of attempt that mutation framework needed to fail the test case
	 */
	protected int mutationCount = 0;
	
	// Configuration for mutation framework
	protected final String srcFolderPath = "src\\main\\java";
	protected final String testFolderPath = "src\\test\\java";
	protected final String projectPath;
	protected final String java_path;
	protected final int stepLimit;
	protected int seed = 1;
	protected MutationFramework mutationFramework = null;
	protected MutationFrameworkConfig configuration = null;
	
	// Target Test Case
	protected int testCaseID = -1;
	protected String testCaseClass = null;
	protected String testCaseMethodName = null;		
	
	// Output of the mutation
	protected Trace buggyTrace = null;
	protected Trace correctTrace = null;
	protected String mutatedProjPath = null;
	protected String originalProjPath = null;
	protected TestCase testCase = null;
	protected PairList pairList = null;
	protected DiffMatcher matcher = null;
	
	protected List<TraceNode> rootCauses = new ArrayList<>();
	protected List<VarValue> inputs = new ArrayList<>();
	protected List<VarValue> outputs = new ArrayList<>();
	
	public MutationAgent(String projectPath, String java_path, int stepLimit) {
		this.projectPath = projectPath;
		this.java_path = java_path;
		this.stepLimit = stepLimit;
	}
	
	/**
	 * Setup the configuration for mutation framework
	 */
	public void setup() throws IOException{
		
		this.reset();
		
		MutationFrameworkConfigBuilder configBuilder = new MutationFrameworkConfigBuilder();
		configBuilder.setDropInsPath(Paths.get(ResourcesPath.DEFAULT_RESOURCES_PATH, ResourcesPath.DEFAULT_DROP_INS_DIR).toString());
        configBuilder.setProjectPath(this.projectPath);
        MicrobatConfig microbatConfig = MicrobatConfig.defaultConfig();
        microbatConfig = microbatConfig.setJavaHome(this.java_path);
        microbatConfig = microbatConfig.setStepLimit(this.stepLimit);
        configBuilder.setMicrobatConfig(microbatConfig);
        configuration = configBuilder.build();
        mutationFramework = new MutationFramework(configuration);
		mutationFramework.extractResources();
	}

	public void startMutation() {
		
		if (!isReady()) {
			throw new RuntimeException("Mutation Agent is not ready");
		}
		
		System.out.println("Mutating Test Case " + this.testCaseID);
		if (this.testCaseID>=0) {
			this.testCase = mutationFramework.getTestCases().get(this.testCaseID);
		} else {
			final String qualifiedName = String.format("%s#%s", this.testCaseClass, this.testCaseMethodName);
			for (TestCase testCase : mutationFramework.getTestCases()) {
				if (testCase.qualifiedName().equals(qualifiedName)) {
					this.testCase = testCase;
					break;
				}
			}
		}
		configuration.setTestCase(testCase);
		
		// Mutate project until it fail the test case
		boolean testCaseFailed = false;
		MutationFrameworkResult result = null;
		for (int i=0; i<100; i++) {
			this.mutationCount++;
			RandomSingleton.getSingleton().setSeed(i);
			try {
				result = mutationFramework.startMutationFramework();
			} catch (TimeoutException e) {
				e.printStackTrace();
				continue;
			}
			if (!result.isTestCasePassed()) {
				testCaseFailed = true;
				break;
			}
		}
		
		if (!testCaseFailed) {
			throw new RuntimeException(this.genErrorMsg("Cannot fail the test case"));
		}
		
		// Get the mutation information
		Project mutatedProject = result.getMutatedProject();
		Project originalProject = result.getOriginalProject();
		
		this.mutatedProjPath = mutatedProject.getRoot().getAbsolutePath();
		this.originalProjPath = originalProject.getRoot().getAbsolutePath();
		
		this.buggyTrace = result.getMutatedTrace();
		this.buggyTrace.setSourceVersion(true);

		
		this.rootCauses = result.getRootCauses();

		this.correctTrace = result.getOriginalTrace();
		
		// Set up the Class Path
		final String projName = result.getMutatedProject().getProjectName();
		final String regressionID = testCase.toString();
		
		ProjectConfig config = ConfigFactory.createConfig(projName, regressionID, this.mutatedProjPath, this.originalProjPath);
		tregression.empiricalstudy.TestCase tc = new tregression.empiricalstudy.TestCase(result.getTestClass(), result.getTestSimpleName());
		AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(this.mutatedProjPath, tc, config);
		AppJavaClassPath correctApp = AppClassPathInitializer.initialize(this.originalProjPath, tc, config);
		
		this.buggyTrace.setAppJavaClassPath(buggyApp);
		this.correctTrace.setAppJavaClassPath(correctApp);
		
		// Set up the diffMatcher
		this.matcher = new DiffMatcher(srcFolderPath, testFolderPath, this.mutatedProjPath, this.originalProjPath);
		this.matcher.matchCode();
		
		// Convert tracediff.PairList to tregression.PairList
//		tracediff.model.PairList pairList_traceDiff = TraceDiff.getTraceAlignment(srcFolderPath, testFolderPath,
//				this.mutatedProjPath, this.originalProjPath,
//                result.getMutatedTrace(), result.getOriginalTrace());

		
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		this.pairList = traceMatcher.matchTraceNodePair(this.buggyTrace, this.correctTrace, this.matcher);
		
		this.rootCauses = result.getRootCauses();
		
//		TestIOFramework testIOFramework = new TestIOFramework();
//		TestIO testIO = testIOFramework.getBuggyTestIOs(
//				result.getOriginalResult(),
//				result.getOriginalResultWithAssertions(),
//				result.getMutatedResult(),
//				result.getMutatedResultWithAssertions(),
//				result.getOriginalProject().getRoot(),
//                result.getMutatedProject().getRoot(), 
//                pairList_traceDiff, 
//                result.getTestClass(),
//                result.getTestSimpleName());
//		
//		if (testIO != null) {
//			for (IOModel model : testIO.getInputs()) {
//				this.inputs.add(model.getValue());
//			}
//			this.outputs.add(testIO.getOutput().getValue());
//		}
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
		this.buggyTrace = null;
		this.mutatedProjPath = "";
		this.originalProjPath = "";
		this.rootCauses.clear();
		this.mutationCount = 0;
		
		this.correctTrace = null;
		this.pairList = null;
		this.matcher = null;
		
		this.inputs.clear();
		this.outputs.clear();
		
		this.mutationFramework = null;
	}
	
	public List<VarValue> getInputs() {
		return this.inputs;
	}
	
	public List<VarValue> getOutputs() {
		return this.outputs;
	}
	
	private static String getUserHomePath() {
		return SystemUtils.getUserHome().toString();
	}
	
	
	public void setSeed(int seed) {
		this.seed = seed;
	}
	
	public Trace getBuggyTrace() {
		return this.buggyTrace;
	}
	
	public List<TraceNode> getRootCause() {
		return this.rootCauses;
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
	
	/**
	 * Check is the Mutation Framework ready to mutate the code.
	 * @return True if it is ready.
	 */
	public boolean isReady() {	
		// Check is the test case provided
		if (this.testCaseID < 0 && (this.testCaseClass == null || this.testCaseMethodName == null)) {
			throw new RuntimeException("Targeted Test Case is not provided.");
		}
		
		// Check is the mutation framework correctly set up
		if (this.mutationFramework == null) {
			throw new RuntimeException("Mutation Framework is not set up.");
		}
		
		return true;
	}
	
	public void setTestCaseID(int testCaseID) {
		this.testCaseID = testCaseID;
	}
	
	public void setTestCaseInfo(final String testCaseClass, final String testCaseMethodName) {
		this.testCaseClass = testCaseClass;
		this.testCaseMethodName = testCaseMethodName;
	}
	
	public String genErrorMsg(final String msg) {
		return "MutationAgent: " + msg;
	}
	
	public int getMutationCount() {
		return this.mutationCount;
	}
}

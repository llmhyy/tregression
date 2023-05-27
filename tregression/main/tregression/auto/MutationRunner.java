package tregression.auto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import defects4janalysis.RunResult;
import iodetection.IODetector;
import iodetection.IOWriter;
import iodetection.IODetector.IOResult;
import jmutation.dataset.BugDataset;
import jmutation.dataset.bug.minimize.ProjectMinimizer;
import jmutation.dataset.bug.model.path.MutationFrameworkPathConfiguration;
import jmutation.dataset.bug.model.path.PathConfiguration;
import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import microbat.probability.SPP.vectorization.NodeFeatureRecord;
import microbat.probability.SPP.vectorization.TraceVectorizer;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.MutationDatasetProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.handler.MutationRunnerHandler;

public class MutationRunner extends ProjectsRunner {
	
	private static final String ZIP_EXT = ".zip";
	private static final String IO_FILE_NAME_FORMAT = "io-%d.txt";
	private static final String LINE = "=========";
	
	private final String datasetPath;
	private final PathConfiguration pathConfig;
	
	private final boolean skipProcessedTestCase;
	private final Set<String> processTC = new HashSet<>();
	
	public MutationRunner(String basePath, String resultPath) {
		this(basePath, resultPath, 5, null, false);
	}
	
	public MutationRunner(String basePath, String resultPath, String datasetPath, boolean skipProcessedTestCase) {
		this(basePath, resultPath, 5, datasetPath, skipProcessedTestCase);
	}
	
	public MutationRunner(String basePath, String resultPath, String datasetPath) {
		this(basePath, resultPath, 5, datasetPath, false);
	}

	public MutationRunner(String basePath, String resultPath, int maxThreadCount, String datasetPath, boolean skipProcessedTestCase) {
		super(basePath, resultPath, maxThreadCount);
		this.datasetPath = datasetPath;
		this.pathConfig = new MutationFrameworkPathConfiguration(this.basePath);
		this.skipProcessedTestCase = skipProcessedTestCase;
	}
	
	
	@Override
	protected RunResult runProject(String projectName, String bugID_str) {
		final String projectPath = Paths.get(this.basePath, projectName).toString();
		BugDataset dataset = new BugDataset(projectPath);
		
		if (!bugID_str.endsWith(MutationRunner.ZIP_EXT)) {
			ProjectsRunner.printMsg(bugID_str + " is not zipped sample");
			return null;
		}
		bugID_str = bugID_str.substring(0, bugID_str.indexOf(ZIP_EXT));
		
		int bugId;
		try {
			bugId = Integer.parseInt(bugID_str);
		} catch (NumberFormatException e) {
			ProjectsRunner.printMsg(bugID_str + " is not mutation sample");
			return null;
		}
		
		RunResult result = new RunResult();
		result.projectName = projectName;
		result.bugID = bugId;
		try {
			dataset.unzip(bugId);
			ProjectMinimizer minimizer = dataset.createMinimizer(bugId);
			minimizer.maximise();
			
			final String bugFolder = pathConfig.getBuggyPath(projectName, bugID_str);
			final String fixFolder = pathConfig.getFixPath(projectName, bugID_str);
			final ProjectConfig config = ConfigFactory.createConfig(projectName, bugID_str, bugFolder, fixFolder);
			List<TestCase> tc = null;
			try {
				tc = config.retrieveFailingTestCase(bugFolder);
			} catch (IOException e) {
				result.errorMessage = "[Config] Cannot retrieveFailing test case: " + e.toString();
				return result;
			}
			
			if (this.processTC.contains(tc.get(0).toString()) && this.skipProcessedTestCase) {
				return null;
			}
			
			if (this.skipProcessedTestCase) {
				this.processTC.add(tc.get(0).toString());
			}
			
			MutationDatasetProjectConfig.executeMavenCmd(Paths.get(bugFolder), "test-compile");
			List<EmpiricalTrial> trials = this.generateTrials(bugFolder, fixFolder, config);
			if (trials == null || trials.isEmpty()) {
				result.errorMessage = ProjectsRunner.genMsg("No trials generated");
				return result;
			}
			
			for (int i=0; i<trials.size(); i++) {
				EmpiricalTrial trial = trials.get(i);
				System.out.println(trial);
				Trace trace = trial.getBuggyTrace();
				if (trace == null) {
					result.errorMessage = "[Trials Generation]: " + trial.getExceptionExplanation();
					return result;
				}
				result.traceLen = Long.valueOf(trace.size());
				result.isOmissionBug = trial.getBugType() == EmpiricalTrial.OVER_SKIP;
				result.rootCauseOrder = trial.getRootcauseNode() == null ? -1 : trial.getRootcauseNode().getOrder();
				for (DeadEndRecord record : trial.getDeadEndRecordList()) {
					if (record.getSolutionPattern() != null) {
						result.solutionName += record.getSolutionPattern().getTypeName() + ":";
					}
				}
				
				if (this.datasetPath != null) {
					final String fileName = projectName + "_" + bugID_str + ".txt";
					final String outputPath = Paths.get(this.datasetPath, fileName).toString();
					ProjectsRunner.printMsg("Saving feature to: " + outputPath );
					TraceVectorizer vectorizer = new TraceVectorizer();
					Trace correctTrace = trial.getFixedTrace();
					List<NodeFeatureRecord> records = vectorizer.vectorize(correctTrace);
					vectorizer.wirteToFile(records, outputPath);
				}
				
				IODetector ioDetector = new IODetector(trial.getBuggyTrace(), "src\\test\\java", trial.getPairList());
				Path ioFilePath = Paths.get(pathConfig.getRepoPath(), projectName,
						String.format(IO_FILE_NAME_FORMAT, bugId));
				executeIOPostProcessing(ioDetector, ioFilePath);
			}
		} catch (IOException e) {
			// Crash from unzipping or deleting buggy project
			result.errorMessage = ProjectsRunner.genMsg(e.toString());
		} finally {
			String pathToBug = pathConfig.getBugPath(projectName, Integer.toString(bugId));
			try {
				FileUtils.deleteDirectory(new File(pathToBug));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private void executeIOPostProcessing(IODetector ioDetector, Path ioFilePath) {
		Optional<IOResult> ioOptional = ioDetector.detect();
		if (ioOptional.isEmpty()) {
			System.out.println("IO Detection Failed");
			return;
		}
		IOResult io = ioOptional.get();
		printIOResult(io);
		saveIOResult(io, ioFilePath);
	}
	
	private void printIOResult(IOResult io) {
		List<VarValue> inputs = io.getInputs();
		VarValue output = io.getOutput();
		System.out.println(String.join(" ", LINE, "inputs", LINE));
		for (VarValue input : inputs) {
			System.out.println(input);
		}
		System.out.println(String.join(" ", LINE, "output", LINE));
		System.out.println(output);
	}

	private void saveIOResult(IOResult io, Path path) {
		IOWriter writer = new IOWriter();
		try {
			writer.writeIO(io.getInputs(), io.getOutput(), path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

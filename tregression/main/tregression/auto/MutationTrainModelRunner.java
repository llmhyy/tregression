package tregression.auto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import tregression.rl.TrainModelAgent;
import iodetection.IODetector.InputsAndOutput;
import iodetection.IODetector.NodeVarValPair;
import jmutation.dataset.BugDataset;
import jmutation.dataset.bug.minimize.ProjectMinimizer;
import jmutation.dataset.bug.model.path.MutationFrameworkPathConfiguration;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.MutationDatasetProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import jmutation.dataset.bug.model.path.PathConfiguration;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.probability.SPP.vectorization.NodeFeatureRecord;
import microbat.probability.SPP.vectorization.TraceVectorizer;
import iodetection.IODetector;

public class MutationTrainModelRunner extends ProjectsRunner {

	protected static final String ZIP_EXT = ".zip";
	
	protected final PathConfiguration pathConfig;
	
	public MutationTrainModelRunner(String basePath, String resultPath) {
		super(basePath, resultPath);
		this.pathConfig = new MutationFrameworkPathConfiguration(this.basePath);
	}

	@Override
	public RunResult runProject(String projectName, String bugID_str) {
		final String projectPath = Paths.get(this.basePath, projectName).toString();
		if (!bugID_str.endsWith(MutationTrainModelRunner.ZIP_EXT)) {
			ProjectsRunner.printMsg(bugID_str + " is not zipped sample");
			return null;
		}
		
		bugID_str = bugID_str.substring(0, bugID_str.indexOf(MutationTrainModelRunner.ZIP_EXT));
		if (this.filter.contains(projectName + ":" + bugID_str)) {
			ProjectsRunner.printMsg("Skip: " + projectName + " " + bugID_str);
			return null;
		}
		
		int bugId;
		try {
			bugId = Integer.parseInt(bugID_str);
		} catch (NumberFormatException e) {
			ProjectsRunner.printMsg(bugID_str + " is not mutation sample");
			return null;
		}
		
		
		final RunResult result = new RunResult();
		result.projectName = projectName;
		result.bugID = bugId;
		
		try {
			
			BugDataset dataset = new BugDataset(projectPath);
			dataset.unzip(bugId);
			
			ProjectMinimizer minimizer = dataset.createMinimizer(bugId);
			minimizer.maximise();
			
			final String bugFolder = pathConfig.getBuggyPath(projectName, bugID_str);
			final String fixFolder = pathConfig.getFixPath(projectName, bugID_str);
			final ProjectConfig config = ConfigFactory.createConfig(projectName, bugID_str, bugFolder, fixFolder);
			
			MutationDatasetProjectConfig.executeMavenCmd(Paths.get(bugFolder), "test-compile");
			List<EmpiricalTrial> trials = this.generateTrials(bugFolder, fixFolder, config);
			if (trials == null || trials.isEmpty()) {
				result.errorMessage = ProjectsRunner.genMsg("No trials generated");
				return result;
			}
			
			EmpiricalTrial trial = trials.get(0);
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
			
			// Prepare inputs and outputs for automatic debugging
			List<VarValue> inputs = new ArrayList<>();
			List<VarValue> outputs = new ArrayList<>();
			TraceNode outputNode = null;
			IODetector ioDetector = new IODetector(trace, config.srcTestFolder, trial.getPairList());
			Optional<InputsAndOutput> ioOptional = ioDetector.detect();
			if (ioOptional.isEmpty()) {
				ProjectsRunner.printMsg("Cannot extract input and output");
				result.errorMessage = "[IODection]: Cannot find inputs and outputs";
				return result;
			} else {
				InputsAndOutput io = ioOptional.get();
				List<NodeVarValPair> inputsDetected = io.getInputs();
				NodeVarValPair outputDetected = io.getOutput();
				for (NodeVarValPair input : inputsDetected) {
					VarValue inputVar = input.getVarVal();
					inputs.add(inputVar);
				}
				outputs.add(outputDetected.getVarVal());
				outputNode = outputDetected.getNode();
				
				ProjectsRunner.printMsg("========================");
				ProjectsRunner.printMsg("Inputs: ");
				for (NodeVarValPair input : inputsDetected) {
					ProjectsRunner.printMsg(input.toString());
				}
				ProjectsRunner.printMsg("Outputs: ");
				ProjectsRunner.printMsg(outputDetected.toString());
				
				TrainModelAgent agent = new TrainModelAgent(trial, inputs, outputs, outputNode);
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future<RunResult> future = executor.submit(() -> {
					return agent.startTraining(result);
				});
				RunResult newResult = new RunResult();
				newResult.projectName = result.projectName;
				newResult.bugID = result.bugID;
				try {
					newResult = future.get(30, TimeUnit.MINUTES);
				} catch (Exception e) {
					newResult.errorMessage = e.toString();
				} finally {
		            future.cancel(true);
		            executor.shutdown();
		        }
				return newResult;
			}
		} catch (NumberFormatException e) {
			ProjectsRunner.printMsg(bugID_str + " is not mutation sample");
			return null;
		} catch (IOException e) {
			result.errorMessage = ProjectsRunner.genMsg(e.toString());
		} finally {
			String pathToBug = pathConfig.getBugPath(projectName, Integer.toString(bugId));
			try {
				FileUtils.deleteDirectory(new File(pathToBug));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}

}

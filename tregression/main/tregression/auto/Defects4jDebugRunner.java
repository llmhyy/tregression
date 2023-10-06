package tregression.auto;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import iodetection.IODetector.InputsAndOutput;
import iodetection.IODetector.NodeVarValPair;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.auto.result.DebugResult;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;

public class Defects4jDebugRunner extends ProjectsDebugRunner {

	private List<String> outOfMemoryFilters = new ArrayList<>();
	
	public Defects4jDebugRunner(String basePath, String resultPath, final double mistakeProbability, final long timeLimit) {
		this(basePath, resultPath, 5, mistakeProbability, timeLimit);
	}
	
	public Defects4jDebugRunner(String basePath, String resultPath, int maxThreadsCount, final double mistakeProbability, final long timeLimit) {
		super(basePath, resultPath, maxThreadsCount, mistakeProbability, timeLimit);
	    outOfMemoryFilters.add("Compress:22");
	    outOfMemoryFilters.add("Compress:29");
	    outOfMemoryFilters.add("JacksonCore:4");
	    outOfMemoryFilters.add("JacksonCore:17");
	    outOfMemoryFilters.add("JacksonCore:25");
	    outOfMemoryFilters.add("Jsoup:81");
	}
	
	@Override
	public RunResult runProject(String projectName, String bugID_str) {
		RunResult result = new RunResult();
		try {
			Integer.valueOf(bugID_str);
		} catch (NumberFormatException e) {
			return null;
		}
		
		final String projectID = projectName + ":" + bugID_str;
		if (this.outOfMemoryFilters.contains(projectID)) {
			result.projectName = projectName;
			result.bugID = Integer.valueOf(bugID_str);
			result.errorMessage = ProjectsRunner.genMsg("Out of memory");
		} else {
			result.projectName = projectName;
			result.bugID = Integer.valueOf(bugID_str);
			
			final ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
			if(config == null) {
				result.errorMessage = ProjectsRunner.genMsg("Cannot generate project config");
				return result;
			}
			
			final String bugFolder = Paths.get(basePath, projectName, bugID_str, "bug").toString();
			final String fixFolder = Paths.get(basePath, projectName, bugID_str, "fix").toString();
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
					SolutionPattern solutionPattern = record.getSolutionPattern();
					if (solutionPattern != null) {
//						result.solutionName += record.getSolutionPattern().getTypeName() + ":";
					}
				}
				
				// Prepare inputs and outputs for automatic debugging
				List<VarValue> inputs = new ArrayList<>();
				List<VarValue> outputs = new ArrayList<>();
				TraceNode outputNode = null;
				String IOStoragePath = "D:\\Defects4j_IO";
				Optional<InputsAndOutput> ioOptional = null;
				try {
					ioOptional = this.detectIO(trace, config.srcTestFolder, trial.getPairList());
//					ioOptional = this.getIO(trace, config.srcTestFolder, trial.getPairList(), IOStoragePath, projectName, bugID_str);
				} catch (Exception e) {
					ProjectsRunner.printMsg("Cannot extract input and output");
					DebugResult debugResult = new DebugResult(result);
					debugResult.errorMessage = "[IODetection]: Cannot find inputs and outputs";
					return debugResult;
				}
				if (ioOptional.isEmpty()) {
					ProjectsRunner.printMsg("Cannot extract input and output");
					DebugResult debugResult = new DebugResult(result);
					debugResult.errorMessage = "[IODetection]: Cannot find inputs and outputs";
					return debugResult;
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
					
					AutoDebugPilotAgent agent = new AutoDebugPilotAgent(trial, inputs, outputs, outputNode);
					DebugResult debugResult = new DebugResult(result);
					ExecutorService executor = Executors.newSingleThreadExecutor();
					Future<DebugResult> future = executor.submit(() -> {
						return agent.startDebug(new DebugResult(result));
					});
					try {
						debugResult = future.get(30, TimeUnit.MINUTES);
					} catch (Exception e) {
						debugResult.errorMessage = e.toString();
					} finally {
			            future.cancel(true);
			            executor.shutdown();
			        }
					return debugResult;
				}
			}
		}
		return result;
	}

}

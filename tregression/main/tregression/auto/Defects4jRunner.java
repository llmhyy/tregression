package tregression.auto;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.nio.file.Paths;
import java.util.ArrayList;

import microbat.model.trace.Trace;
import tregression.auto.result.RunResult;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;

public class Defects4jRunner extends ProjectsRunner {
	
	private List<String> outOfMemoryFilters = new ArrayList<>();
	
	public Defects4jRunner(String basePath, String resultPath) {
		this(basePath, resultPath, 5);
	}
	
	public Defects4jRunner(String basePath, String resultPath, int maxThreadsCount) {
		super(basePath, resultPath, maxThreadsCount);
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
						result.solutionName += record.getSolutionPattern().getTypeName() + ":";
					}
				}
			}
		}
		return result;
	}

}
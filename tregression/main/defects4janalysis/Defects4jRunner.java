package defects4janalysis;

import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.handler.PlayRegressionLocalizationHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

import org.apache.bcel.Repository;

import microbat.model.trace.Trace;

public class Defects4jRunner {
	
	public static void main(String[] args) {
		
		final String basePath = "E:\\david\\Defects4j";
		final String resultPath = Paths.get(basePath, "result.txt").toString();
		final String javaHome = "C:\\Program Files\\Java\\jdk1.8.0_341";
		
	    int project_count = 0;
	    int success_count = 0;
	    
		ResultWriter writer = new ResultWriter(resultPath);
	    File baseFolder = new File(basePath);
	    
	    List<String> supportedProjectNames = new ArrayList<>();
	    supportedProjectNames.add("Chart");
	    supportedProjectNames.add("Closure");
	    supportedProjectNames.add("Lang");
	    supportedProjectNames.add("Math");
	    supportedProjectNames.add("Mockito");
	    supportedProjectNames.add("Time");
	    
	    for (String projectName : baseFolder.list()) {
	    	
	    	if (!supportedProjectNames.contains(projectName)) {
	    		continue;
	    	}
	    	
	    	System.out.println("Start running " + projectName);
	    	final String projectPath = Paths.get(basePath, projectName).toString();
	    	File projectFolder = new File(projectPath);

	    	for (String bugID_str : projectFolder.list()) {
	    		project_count++;
	    		System.out.println();
	    		System.out.println("Working on " + projectName + " : " + bugID_str);
	    		
	    		final String bugFolder = Paths.get(projectPath, bugID_str, "bug").toString();
	    		final String fixFolder = Paths.get(projectPath, bugID_str, "fix").toString();
	    		
	    		RunResult result = new RunResult();
	    		result.projectName = projectName;
	    		result.bugID = Integer.valueOf(bugID_str);
	    		
	    		try {
					ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str, javaHome);
					if(config == null) {
						throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + bugID_str);						
					}
					
					TrialGenerator0 generator0 = new TrialGenerator0();
					List<EmpiricalTrial> trials = generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
					
					if (trials.size() != 0) {
						PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();
						for (int i=0; i<trials.size(); i++) {
							EmpiricalTrial t = trials.get(i);
							Trace trace = t.getBuggyTrace();
							result.traceLen = Long.valueOf(trace.size());
							result.rootCauseOrder = t.getRootcauseNode() == null ? -1 : t.getRootcauseNode().getOrder();
							for (DeadEndRecord record : t.getDeadEndRecordList()) {
								result.solutionName = record.getSolutionPattern().getTypeName();
								int solutionType = record.getSolutionPattern().getType();
								if (solutionType == SolutionPattern.MISS_EVALUATED_CONDITION ||
									solutionType == SolutionPattern.MISSING_ASSIGNMENT ||
									solutionType == SolutionPattern.MISSING_IF_BLOCK ||
									solutionType == SolutionPattern.MISSING_IF_RETURN ||
									solutionType == SolutionPattern.MISSING_IF_THROW) {
									result.isOmissionBug = true;
								} else {
									result.isOmissionBug = false;
								}
							}
						}
		    			success_count++;
					}
					
	    		} catch (Exception e) {
	    			System.out.println("Failed");
	    			e.printStackTrace();
	    		}
	    			
	    		writer.writeResult(result);
	    		break;
	    	}
	    	break;
	    }
	    writer.writeResult(success_count, project_count);
	}
	
	
}

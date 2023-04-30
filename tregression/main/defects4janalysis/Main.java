package defects4janalysis;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.handler.PlayRegressionLocalizationHandler;

public class Main {

	public static void main(String[] args) {
		for (int i=0; i<5; i++) {
			System.out.println("Print out something");
		}
//		final String basePath = "E:\\david\\Defects4j";
//		// Write the analysis result to this file
//		final String resultPath = Paths.get(basePath, "result_1.txt").toString();
//		
//	    int project_count = 0;
//	    int success_count = 0;
//	    
//		ResultWriter writer = new ResultWriter(resultPath);
//		writer.writeTitle();
//		
//	    File baseFolder = new File(basePath);
//	    
//	    // Not all projects are supported by Tregression by now
//	    List<String> supportedProjectNames = new ArrayList<>();
//	    supportedProjectNames.add("Chart");
//	    supportedProjectNames.add("Closure");
//	    supportedProjectNames.add("Lang");
//	    supportedProjectNames.add("Math");
//	    supportedProjectNames.add("Mockito");
//	    supportedProjectNames.add("Time");
//
//	    List<String> projectFilters = new ArrayList<>();
//	    projectFilters.add("Closure:44");
//	    
//	    // Loop all projects in the Defects4j folder
//	    for (String projectName : baseFolder.list()) {
//	    	
//	    	// Skip if the project is not supported
//	    	if (!supportedProjectNames.contains(projectName)) {
//	    		continue;
//	    	}
//	    	
//	    	System.out.println("Start running " + projectName);
//	    	final String projectPath = Paths.get(basePath, projectName).toString();
//	    	File projectFolder = new File(projectPath);
//	    	
//	    	// Loop all bug id in the projects folder
//	    	for (String bugID_str : projectFolder.list()) {
//	    		project_count++;
//	    		System.out.println();
//	    		System.out.println("Working on " + projectName + " : " + bugID_str);
//	    		
//	    		if (projectFilters.contains(projectName + ":" + bugID_str)) {
//	    			throw new RuntimeException("Will cause hanging problem");
//	    		}
//	    		
//	    		// Path to the buggy folder and the fixed folder
//	    		final String bugFolder = Paths.get(projectPath, bugID_str, "bug").toString();
//	    		final String fixFolder = Paths.get(projectPath, bugID_str, "fix").toString();
//	    		
//	    		// Result store the analysis result
//	    		RunResult result = new RunResult();
//	    		result.projectName = projectName;
//	    		result.bugID = Integer.valueOf(bugID_str);
//	    		
//    			// Get the configuration of the Defects4j project
//				ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
//
//				
//				// TrailGenerator will generate the buggy trace and fixed trace
//				TrialGenerator0 generator0 = new TrialGenerator0();
//				List<EmpiricalTrial> trials = generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
//				
//				// Record the analysis result
//				if (trials.size() != 0) {
//					PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();
//					for (int i=0; i<trials.size(); i++) {
//						EmpiricalTrial t = trials.get(i);
//						System.out.println(t);
//						Trace trace = t.getBuggyTrace();
//						result.traceLen = Long.valueOf(trace.size());
//						result.isOmissionBug = t.getBugType() == EmpiricalTrial.OVER_SKIP;
//
//						result.rootCauseOrder = t.getRootcauseNode() == null ? -1 : t.getRootcauseNode().getOrder();
//						for (DeadEndRecord record : t.getDeadEndRecordList()) {
//							result.solutionName = record.getSolutionPattern().getTypeName();
//
//						}
//					}
//	    			success_count++;
//				}
//
//	    			
//	    		writer.writeResult(result);
//
//	    	}
//
//	    }
//	    writer.writeResult(success_count, project_count);
	}
}

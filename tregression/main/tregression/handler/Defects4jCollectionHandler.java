package tregression.handler;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import defects4janalysis.ResultWriter;
import defects4janalysis.RunResult;
import microbat.model.trace.Trace;
import microbat.util.JavaUtil;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.Defects4jProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;

public class Defects4jCollectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				final String basePath = "E:\\david\\Defects4j";
				// Write the analysis result to this file
				final String resultPath = Paths.get(basePath, "result.txt").toString();
				
			    int project_count = 0;
			    int success_count = 0;
			    
				ResultWriter writer = new ResultWriter(resultPath);
				writer.writeTitle();
				
			    File baseFolder = new File(basePath);
			    
			    // Not all projects are supported by Tregression by now
			    List<String> supportedProjectNames = new ArrayList<>();
			    supportedProjectNames.add("Chart");
			    supportedProjectNames.add("Closure");
			    supportedProjectNames.add("Lang");
			    supportedProjectNames.add("Math");
			    supportedProjectNames.add("Mockito");
			    supportedProjectNames.add("Time");
			    
			    // Loop all projects in the Defects4j folder
			    for (String projectName : baseFolder.list()) {
			    	
			    	// Skip if the project is not supported
			    	if (!supportedProjectNames.contains(projectName)) {
			    		continue;
			    	}
			    	
			    	System.out.println("Start running " + projectName);
			    	final String projectPath = Paths.get(basePath, projectName).toString();
			    	File projectFolder = new File(projectPath);
			    	
			    	// Loop all bug id in the projects folder
			    	for (String bugID_str : projectFolder.list()) {
			    		project_count++;
			    		System.out.println();
			    		System.out.println("Working on " + projectName + " : " + bugID_str);
			    		
			    		// Path to the buggy folder and the fixed folder
			    		final String bugFolder = Paths.get(projectPath, bugID_str, "bug").toString();
			    		final String fixFolder = Paths.get(projectPath, bugID_str, "fix").toString();
			    		
			    		// Result store the analysis result
			    		RunResult result = new RunResult();
			    		result.projectName = projectName;
			    		result.bugID = Integer.valueOf(bugID_str);
			    		
			    		try {
			    			
			    			// Get the configuration of the Defects4j project
							ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
							if(config == null) {
								throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + bugID_str);						
							}
							
							// TrailGenerator will generate the buggy trace and fixed trace
							TrialGenerator0 generator0 = new TrialGenerator0();
							List<EmpiricalTrial> trials = generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
							
							// Record the analysis result
							if (trials.size() != 0) {
								PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();
								for (int i=0; i<trials.size(); i++) {
									EmpiricalTrial t = trials.get(i);
									System.out.println(t);
									Trace trace = t.getBuggyTrace();
									result.traceLen = Long.valueOf(trace.size());
									result.isOmissionBug = t.getBugType() == EmpiricalTrial.OVER_SKIP;
	
									result.rootCauseOrder = t.getRootcauseNode() == null ? -1 : t.getRootcauseNode().getOrder();
									for (DeadEndRecord record : t.getDeadEndRecordList()) {
										result.solutionName = record.getSolutionPattern().getTypeName();
		
									}
								}
				    			success_count++;
							}
							
			    		} catch (Exception e) {
			    			System.out.println("Failed");
			    			result.errorMessage = e.toString();
			    		}
			    			
			    		writer.writeResult(result);

			    	}

			    }
			    writer.writeResult(success_count, project_count);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
}

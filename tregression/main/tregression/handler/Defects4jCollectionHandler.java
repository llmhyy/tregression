package tregression.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
				final String resultFolderPath = "E:\\hongshu\\Defects4j";
				final String resultPath = Paths.get(resultFolderPath, "result.txt").toString();
				
			    int project_count = 0;
			    int success_count = 0;
			    
			    ResultWriter writer = new ResultWriter(resultPath);
			    List<String> processedProjects = new ArrayList<>();
			    // If file exists, read the records
		    	try {
		    		FileReader fileReader = new FileReader(resultPath);
		    		BufferedReader reader = new BufferedReader(fileReader);
		    		String line = reader.readLine(); // first line is the headers
		    		while ((line = reader.readLine()) != null) {
		    			String[] content = line.split(",");
		    			String record = content[0] + ":" + content[1];
		    			processedProjects.add(record);
		    		}
		    	} catch (FileNotFoundException e) {
		    		writer.writeTitle();
		    	} catch (IOException e) {
		    		e.printStackTrace();
		    	}
				
			    File baseFolder = new File(basePath);
			    
			    // Not all projects are supported by Tregression by now
			    List<String> supportedProjectNames = new ArrayList<>();
			    supportedProjectNames.add("Chart");
			    supportedProjectNames.add("Closure");
			    supportedProjectNames.add("Lang");
			    supportedProjectNames.add("Math");
			    supportedProjectNames.add("Mockito");
			    supportedProjectNames.add("Time");
	
			    //List<String> projectFilters = new ArrayList<>();
			    //projectFilters.add("Closure:44");
			    //projectFilters.add("Closure:51");
			    //projectFilters.add("Closure:52");
			    //projectFilters.add("Closure:59");
			    //projectFilters.add("Closure:65");
			    //projectFilters.add("Closure:73");
			    //projectFilters.add("Closure:77");
			    //projectFilters.add("Closure:107");
			    
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
			    		
			    		if (projectName.equals("Closure") && bugID_str.equals("47")) {
			    			
			    		} else {
			    			continue;
			    		}
			    		
			    		// Skip if the project has been processed
			    		if (processedProjects.contains(projectName + ":" + bugID_str)) {
			    			System.out.println("Skipped: has record in the result file");
			    			continue;
			    		}
			    		
			    		// Path to the buggy folder and the fixed folder
			    		final String bugFolder = Paths.get(projectPath, bugID_str, "bug").toString();
			    		final String fixFolder = Paths.get(projectPath, bugID_str, "fix").toString();
			    		
			    		// Result store the analysis result
			    		RunResult result = new RunResult();
			    		result.projectName = projectName;
			    		result.bugID = Integer.valueOf(bugID_str);
			    		
			    		try {
			    			//if (projectFilters.contains(projectName + ":" + bugID_str)) {
				    		//	throw new RuntimeException("Will cause hanging problem");
				    		//}
			    			
			    			// Get the configuration of the Defects4j project
							final ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
							if(config == null) {
								throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + bugID_str);						
							}
							
							// TrailGenerator will generate the buggy trace and fixed trace
							final TrialGenerator0 generator0 = new TrialGenerator0();
//							ExecutorService executorService = Executors.newSingleThreadExecutor();
//							Future<List<EmpiricalTrial>> getTrials = executorService.submit(new Callable<List<EmpiricalTrial>>() {
//								@Override
//								public List<EmpiricalTrial> call() throws Exception {
//									return generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
//								}
//							});
//							// Timeout: 15 minutes
//							List<EmpiricalTrial> trials = getTrials.get(15, TimeUnit.MINUTES);
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

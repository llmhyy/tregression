package tregression.handler;

import tregression.auto.*;


import java.nio.file.Paths;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.util.JavaUtil;

public class Defects4jCollectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute();
				return Status.OK_STATUS;
			}
			
		};
		job.schedule();
		return null;
	}
	
	private void execute() {
		final String basePath = "E:\\david\\Defects4j";
		final String resultPath = Paths.get(basePath, "result_1.txt").toString();
		final ProjectsRunner runner = new Defects4jRunner(basePath, resultPath);
		runner.run();
	}
}
	
//	private final int maxThreadsCount = 5;
//	private int hangingThreads = 0;
//	
//	
//	@Override
//	public Object execute(ExecutionEvent event) throws ExecutionException {
//		
//		JavaUtil.sourceFile2CUMap.clear();
//		Job job = new Job("Testing Tregression") {
//
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
//				
//				final String basePath = "E:\\david\\Defects4j";
//				// Write the analysis result to this file
//				final String resultFolderPath = "E:\\hongshu\\Defects4j";
//				final String resultPath = Paths.get(resultFolderPath, "result_1.txt").toString();
//				
//			    int project_count = 0;
//			    int success_count = 0;
//			    
//			    ResultWriter writer = new ResultWriter(resultPath);
//			    List<String> processedProjects = new ArrayList<>();
//			    // If file exists, read the records
//		    	try {
//		    		FileReader fileReader = new FileReader(resultPath);
//		    		BufferedReader reader = new BufferedReader(fileReader);
//		    		String line = reader.readLine(); // first line is the headers
//		    		while ((line = reader.readLine()) != null) {
//		    			String[] content = line.split(",");
//		    			String record = content[0] + ":" + content[1];
//		    			processedProjects.add(record);
//		    		}
//		    		reader.close();
//		    	} catch (FileNotFoundException e) {
//		    		writer.writeTitle();
//		    	} catch (IOException e) {
//		    		e.printStackTrace();
//		    	}
//				
//			    File baseFolder = new File(basePath);
//			    
//			    // Not all projects are supported by Tregression by now
//			    List<String> supportedProjectNames = new ArrayList<>();
//			    supportedProjectNames.add("Chart");
//			    supportedProjectNames.add("Cli");
//			    supportedProjectNames.add("Closure");
//			    supportedProjectNames.add("Codec");
//			    supportedProjectNames.add("Compress");
//			    supportedProjectNames.add("Csv");
//			    supportedProjectNames.add("Gson");
//			    supportedProjectNames.add("JacksonCore");
//			    supportedProjectNames.add("JacksonDatabind");
//			    supportedProjectNames.add("JacksonXml");
//			    supportedProjectNames.add("Jsoup");
//			    supportedProjectNames.add("JxPath");
//			    supportedProjectNames.add("Lang");
//			    supportedProjectNames.add("Math");
//			    supportedProjectNames.add("Mockito");
//			    supportedProjectNames.add("Time");
//	
//			    // bugs causing hanging error:
////			    List<String> projectFilters = new ArrayList<>();
////			    projectFilters.add("Closure:44");
////			    projectFilters.add("Closure:51");
////			    projectFilters.add("Closure:52");
////			    projectFilters.add("Closure:59");
////			    projectFilters.add("Closure:65");
////			    projectFilters.add("Closure:73");
////			    projectFilters.add("Closure:77");
////			    projectFilters.add("Closure:107");
//			    
//			    // out of memory:
//			    List<String> outOfMemoryFilters = new ArrayList<>();
//			    outOfMemoryFilters.add("Compress:22");
//			    outOfMemoryFilters.add("Compress:29");
//			    outOfMemoryFilters.add("JacksonCore:4");
//			    outOfMemoryFilters.add("JacksonCore:17");
//			    outOfMemoryFilters.add("JacksonCore:25");
//			    outOfMemoryFilters.add("Jsoup:81");
//			    
//			    int maxThreadsCount = 5;
//			    ExecutorService executorService = Executors.newFixedThreadPool(maxThreadsCount);
//			    int hangingThreads = 0;
//			    
//			    // Loop all projects in the Defects4j folder
//			    for (String projectName : baseFolder.list()) {
//			    	
//			    	// Skip if the project is not supported
//			    	if (!supportedProjectNames.contains(projectName)) {
//			    		continue;
//			    	}
//			    	
//			    	System.out.println("Start running " + projectName);
//			    	final String projectPath = Paths.get(basePath, projectName).toString();
//			    	File projectFolder = new File(projectPath);
//			    	
//			    	// Loop all bug id in the projects folder
//			    	for (String bugID_str : projectFolder.list()) {
//			    		project_count++;
//			    		System.out.println();
//			    		System.out.println("Working on " + projectName + " : " + bugID_str);
//			    		
//			    		// Skip if the project has been processed
//			    		if (processedProjects.contains(projectName + ":" + bugID_str)) {
//			    			System.out.println("Skipped: has record in the result file");
//			    			continue;
//			    		}
//			    		
//			    		// Path to the buggy folder and the fixed folder
//			    		final String bugFolder = Paths.get(projectPath, bugID_str, "bug").toString();
//			    		final String fixFolder = Paths.get(projectPath, bugID_str, "fix").toString();
//			    		
//			    		// Result store the analysis result
//			    		RunResult result = new RunResult();
//			    		result.projectName = projectName;
//			    		try {
//			    			result.bugID = Integer.valueOf(bugID_str);
//			    		} catch (NumberFormatException e) {
//			    			e.printStackTrace();
//			    			System.out.println("Skipped: invalid folder format");
//			    			continue;
//			    		}
//			    		
//			    		try {
////			    			if (projectFilters.contains(projectName + ":" + bugID_str)) {
////				    			throw new RuntimeException("Will cause hanging problem");
////				    		}
//			    			
//			    			if (outOfMemoryFilters.contains(projectName + ":" + bugID_str)) {
//			    				throw new RuntimeException("Out of memory");
//			    			}
//			    			
//			    			// Get the configuration of the Defects4j project
//							final ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
//							if(config == null) {
//								throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + bugID_str);						
//							}
//							
//							// TrailGenerator will generate the buggy trace and fixed trace
//							final TrialGenerator0 generator0 = new TrialGenerator0();
//							Future<List<EmpiricalTrial>> getTrials = executorService.submit(new Callable<List<EmpiricalTrial>>() {
//								@Override
//								public List<EmpiricalTrial> call() throws Exception {
//									return generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
//								}
//							});
//							// Timeout: 15 minutes
//							List<EmpiricalTrial> trials;
//							try {
//								trials = getTrials.get(15, TimeUnit.MINUTES);
//							} catch (TimeoutException e) {
//								getTrials.cancel(true);
//								hangingThreads++;
//								if (hangingThreads >= maxThreadsCount) {
//									executorService.shutdownNow();
//								}
//								throw e;
//							}
//							
//							// Record the analysis result
//							if (trials.size() != 0) {
//								PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();
//								for (int i=0; i<trials.size(); i++) {
//									EmpiricalTrial t = trials.get(i);
//									System.out.println(t);
//									Trace trace = t.getBuggyTrace();
//									if (trace == null) {
//										throw new RuntimeException(t.getExceptionExplanation());
//									}
//									result.traceLen = Long.valueOf(trace.size());
//									result.isOmissionBug = t.getBugType() == EmpiricalTrial.OVER_SKIP;
//	
//									result.rootCauseOrder = t.getRootcauseNode() == null ? -1 : t.getRootcauseNode().getOrder();
//									for (DeadEndRecord record : t.getDeadEndRecordList()) {
//										result.solutionName += record.getSolutionPattern().getTypeName() + ":";
//									}
//								}
//				    			success_count++;
//							}
//							
//			    		} catch (Exception e) {
//			    			System.out.println("Failed");
//			    			result.errorMessage = e.toString();
//			    		}
//			    			
//			    		writer.writeResult(result);
//			    		if (hangingThreads >= maxThreadsCount) {
//			    			break;
//			    		}
//
//			    	}
//
//			    }
//			    writer.writeResult(success_count, project_count);
//				return Status.OK_STATUS;
//			}
//		};
//		job.schedule();
//		return null;
//	}
//	
//	private void execute() {
//		final String basePath = "E:\\david\\Defects4j";
//		final String resultPath = "E:\\david\\Defects4j\\result_1.txt";
//		
//		// Filter out some projects
//		List<RunResult> processedResult = this.loadProcessedResult(resultPath);
////		List<String> filter = processedResult.stream().map(result -> result.projectName + ":" + result.bugID).toList();
//		List<String> filter =  null;
//		List<String> OutOfMemory = new ArrayList<>();
//		OutOfMemory.add("Compress:22");
//		OutOfMemory.add("Compress:29");
//		OutOfMemory.add("JacksonCore:4");
//		OutOfMemory.add("JacksonCore:17");
//		OutOfMemory.add("JacksonCore:25");
//		OutOfMemory.add("Jsoup:81");
//		
//	    // For recording run result
//	    ResultWriter writer = new ResultWriter(resultPath);
//	    
//	    // For setting up timeout
//	    ExecutorService executorService = Executors.newFixedThreadPool(this.maxThreadsCount);
//	    
//	    File baseFolder = new File(basePath);
//	    for (String projectName : baseFolder.list()) {
//	    	Defects4jCollectionHandler.printMsg("String processing: " + projectName);
//	    	final String projectPath = Paths.get(basePath, projectName).toString();
//	    	File projectFolder = new File(projectPath);
//	    	for (String bugID_str : projectFolder.list()) {
//	    		final String projectID = projectName + ":" + bugID_str;
//	    		if (filter.contains(projectID)) {
//	    			continue;
//	    		}
//	    		
//	    		RunResult result = null;
//	    		try {
//	    			Integer.valueOf(bugID_str);
//	    		} catch (NumberFormatException e) {
//	    			continue;
//	    		}
//	    		
//	    		if (OutOfMemory.contains(projectID)) {
//	    			result = new RunResult();
//	    			result.bugID = Integer.valueOf(bugID_str);
//	    			result.errorMessage = Defects4jCollectionHandler.genMsg("Out of memory");
//	    		} else {
//	    			result = this.runProject(basePath, projectName, bugID_str, executorService);
//	    		}
//	    		writer.writeResult(result);
//	    		if (this.hangingThreads > this.maxThreadsCount) {
//	    			break;
//	    		}
//	    	}
//	    }
//	}
//	
//	private RunResult runProject(final String basePath, final String projectName, final String bugID_str, final ExecutorService executorService) {
//		RunResult result = new RunResult();
//		result.projectName = projectName;
//		result.bugID = Integer.valueOf(bugID_str);
//		
//		final ProjectConfig config = Defects4jProjectConfig.getConfig(projectName, bugID_str);
//		if(config == null) {
//			result.errorMessage = Defects4jCollectionHandler.genMsg("Cannot generate project config");
//			return result;
//		}
//		
//		final String bugFolder = Paths.get(basePath, projectName, bugID_str, "bug").toString();
//		final String fixFolder = Paths.get(basePath, projectName, bugID_str, "fix").toString();
//		List<EmpiricalTrial> trials = this.generateTrials(bugFolder, fixFolder, config, executorService);
//		if (trials == null || trials.isEmpty()) {
//			result.errorMessage = Defects4jCollectionHandler.genMsg("No trials generated");
//			return result;
//		}
//		
//		for (int i=0; i<trials.size(); i++) {
//			EmpiricalTrial trial = trials.get(i);
//			System.out.println(trial);
//			Trace trace = trial.getBuggyTrace();
//			if (trace == null) {
//				result.errorMessage = "[Trials Generation]: " + trial.getExceptionExplanation();
//				return result;
//			}
//			result.traceLen = Long.valueOf(trace.size());
//			result.isOmissionBug = trial.getBugType() == EmpiricalTrial.OVER_SKIP;
//			result.rootCauseOrder = trial.getRootcauseNode() == null ? -1 : trial.getRootcauseNode().getOrder();
//			for (DeadEndRecord record : trial.getDeadEndRecordList()) {
//				result.solutionName += record.getSolutionPattern().getTypeName() + ":";
//			}
//		}
//		
//		return result;
//	}
//	
//	private List<EmpiricalTrial> generateTrials(final String bugFolder, final String fixFolder, final ProjectConfig config, final ExecutorService executorService) {
//		final TrialGenerator0 generator0 = new TrialGenerator0();
//		Future<List<EmpiricalTrial>> getTrials = executorService.submit(new Callable<List<EmpiricalTrial>>() {
//			@Override
//			public List<EmpiricalTrial> call() throws Exception {
//				return generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
//			}
//		});
//		
//		// Timeout: 10 minutes
//		List<EmpiricalTrial> trials;
//		try {
//			trials = getTrials.get(10, TimeUnit.MINUTES);
//		} catch (TimeoutException | InterruptedException | java.util.concurrent.ExecutionException e) {
//			getTrials.cancel(true);
//			this.hangingThreads++;
//			if (this.hangingThreads >= this.maxThreadsCount) {
//				executorService.shutdownNow();
//			}
//			return null;
//		}
//		
//		return trials;
//	}
//	
//	private List<RunResult> loadProcessedResult(final String path) {
//		List<RunResult> results = new ArrayList<>();
//    	try {
//    		FileReader fileReader = new FileReader(path);
//    		BufferedReader reader = new BufferedReader(fileReader);
//    		String line = reader.readLine(); // first line is the headers
//    		while ((line = reader.readLine()) != null) {
//    			RunResult result = RunResult.parseString(line);
//    			results.add(result);
//    		}
//    		reader.close();
//    	} catch (Exception e) {
//    		e.printStackTrace();
//    	}
//    	return results;
//	}
//	
//	public static String genMsg(final String message) {
//		return "Defects4j Runner]: " + message;
//	}
//	
//	public static void printMsg(final String message) {
//		System.out.println(Defects4jCollectionHandler.genMsg(message));
//	}


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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

import defects4janalysis.ResultWriter;
import defects4janalysis.RunResult;
import iodetection.IODetector;
import jmutation.dataset.BugDataset;
import jmutation.dataset.bug.minimize.ProjectMinimizer;
import jmutation.dataset.bug.model.path.MutationFrameworkPathConfiguration;
import jmutation.dataset.bug.model.path.PathConfiguration;
import microbat.ActivatorStub;
import microbat.model.trace.Trace;
import microbat.model.value.VarValue;
import microbat.preference.MicrobatPreference;
import microbat.util.ConsoleUtilsStub;
import microbat.util.JavaUtil;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.MutationDatasetProjectConfig;
import tregression.empiricalstudy.config.ProjectConfig;

public class MutationRunnerHandler extends AbstractHandler {
	private static final String ZIP_EXT = ".zip";

	/**
	 * A main method is provided so that we do not need to run this in an Eclipse
	 * Application.
	 * 
	 * Modify Eclipse home in setUpSystem method to your eclipse home.
	 * 
	 * Microbat configurations can be changed in setUpPreferences method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		setUpSystem();
		new ConsoleUtilsStub().setItselfAsSingleton();
		IPreferenceStore preference = new PreferenceStore();
		setUpPreferences(preference);
		new ActivatorStub(preference).setItselfAsPlugin();

		MutationRunnerHandler handler = new MutationRunnerHandler();
		handler.collectResultsInDataset();
	}

	private static void setUpPreferences(IPreferenceStore preference) {
		preference.setDefault(MicrobatPreference.JAVA7HOME_PATH, "C:\\Program Files\\Java\\jdk1.8.0_341");
		preference.setDefault(MicrobatPreference.STEP_LIMIT, 30000);
		preference.setDefault(MicrobatPreference.VARIABLE_LAYER, 5);
		preference.setDefault(MicrobatPreference.RUN_WITH_DEBUG_MODE, false);
	}

	private static void setUpSystem() {
		System.setProperty("eclipse.launcher", "C:\\Users\\Chenghin\\eclipse\\java-2022-06\\eclipse\\eclipse.exe");
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return collectResultsInDataset();
			}
		};
		job.schedule();
		return null;
	}

	private IStatus collectResultsInDataset() {
		// TODO: Use tregression preferences (repo path)
		final String basePath = "E:\\david\\Mutation_Dataset";

		// Write the analysis result to this file
		final String resultPath = Paths.get(basePath, "result.txt").toString();

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
		int total_count = 0;
		int success_count = 0;

		ResultWriter writer = new ResultWriter(resultPath);
		writer.writeTitle();

		File baseFolder = new File(basePath);

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
    		reader.close();
    	} catch (FileNotFoundException e) {
    		writer.writeTitle();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
		// You can filter out some problematic projection. The example is commented
		List<String> projectFilters = new ArrayList<>();
		// projectFilters.add("Closure:44");

		// Loop all projects in the MutationDataset folder
		for (String projectName : baseFolder.list()) {
			System.out.println("Start running " + projectName);
			final String projectPath = Paths.get(basePath, projectName).toString();
			BugDataset dataset = new BugDataset(projectPath);
			File projectFolder = new File(projectPath);
			String[] mutationFolders = projectFolder.list();
			if (mutationFolders == null) {
				String message = "Mutation Folder is null";
				System.out.println(message);
				return Status.warning(message);
			}
			
			// Loop all bug id in the projects folder
			for (String bugIDZipStr : mutationFolders) {
				if (!bugIDZipStr.endsWith(ZIP_EXT)) {
					continue;
				}
				String bugID_str = bugIDZipStr.substring(0, bugIDZipStr.indexOf(ZIP_EXT));
				
	    		// Skip if the project has been processed
	    		if (processedProjects.contains(projectName + ":" + bugID_str)) {
	    			System.out.println("Skipped: has record in the result file");
	    			continue;
	    		}
	    		
				int bugId;
				try {
					bugId = Integer.parseInt(bugID_str);
				} catch (NumberFormatException e) {
					continue;
				}

				total_count++;

				if (projectFilters.contains(projectName + ":" + bugID_str)) {
					throw new RuntimeException("Will cause hanging problem");
				}

				RunResult result;
				try {
					PathConfiguration pathConfig = new MutationFrameworkPathConfiguration(basePath);
					dataset.unzip(bugId);
					ProjectMinimizer minimizer = dataset.createMinimizer(bugId);
					minimizer.maximise();
					result = collectSingleResult(basePath, projectName, bugId, pathConfig, executorService);
					String pathToBug = pathConfig.getBugPath(projectName, Integer.toString(bugId));
					FileUtils.deleteDirectory(new File(pathToBug));
				} catch (IOException e) {
					// Crash from zipping or unzipping
					e.printStackTrace();
					continue;
				}
				if (result.errorMessage.isEmpty()) {
					success_count++;
				}
				writer.writeResult(result);
			}
		}
		writer.writeResult(success_count, total_count);
		return Status.OK_STATUS;
	}

	private RunResult collectSingleResult(String basePath, String projectName, int bugId,
			PathConfiguration pathConfig, ExecutorService executorService) {
		String bugID_str = String.valueOf(bugId);
		System.out.println();
		System.out.println("Working on " + projectName + " : " + bugID_str);

		// Path to the buggy folder and the fixed folder
		final String bugFolder = pathConfig.getBuggyPath(projectName, bugID_str);
		final String fixFolder = pathConfig.getFixPath(projectName, bugID_str);

		// Result store the analysis result
		RunResult result = new RunResult();
		result.projectName = projectName;
		result.bugID = bugId;

		try {
			// Project config of the mutation dataset
			final ProjectConfig config = ConfigFactory.createConfig(projectName, bugID_str, bugFolder, fixFolder);

			if (config == null) {
				throw new Exception(
						"cannot parse the configuration of the project " + projectName + " with id " + bugID_str);
			}

			MutationDatasetProjectConfig.executeMavenCmd(Paths.get(bugFolder), "test-compile");
			// TrailGenerator will generate the buggy trace and fixed trace

			final TrialGenerator0 generator0 = new TrialGenerator0();
			Future<List<EmpiricalTrial>> getTrials = executorService.submit(new Callable<List<EmpiricalTrial>>() {
				@Override
				public List<EmpiricalTrial> call() throws Exception {
					return generator0.generateTrials(bugFolder, fixFolder, false, false, false, 3, true, true, config, "");
				}
			});
			// Timeout: 15 minutes
			List<EmpiricalTrial> trials = getTrials.get(15, TimeUnit.MINUTES);
			getTrials.cancel(true);
			
			// Record the analysis result
			if (trials.size() != 0) {
				PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();
				for (int i = 0; i < trials.size(); i++) {
					EmpiricalTrial t = trials.get(i);
					System.out.println(t);
					Trace trace = t.getBuggyTrace();
					result.traceLen = Long.valueOf(trace.size());
					result.isOmissionBug = t.getBugType() == EmpiricalTrial.OVER_SKIP;
					result.rootCauseOrder = t.getRootcauseNode() == null ? -1 : t.getRootcauseNode().getOrder();
					for (DeadEndRecord record : t.getDeadEndRecordList()) {
						result.solutionName = record.getSolutionPattern().getTypeName();
					}
					IODetector ioDetector = new IODetector(t.getBuggyTrace(), t.getFixedTrace(), "src\\test\\java",
							t.getPairList());
					ioDetector.detect();
					List<VarValue> inputs = ioDetector.getInputs();
					VarValue output = ioDetector.getOutputs();
					String line = "=========";
					System.out.println(String.join(" ", line, "inputs", line));
					for (VarValue input : inputs) {
						System.out.println(input);
					}
					System.out.println(String.join(" ", line, "output", line));
					System.out.println(output);
				}
			} else {
				result.errorMessage = "No trials";
			}
		} catch (Exception e) {
			System.out.println("Failed");
			result.errorMessage = e.toString();
		}
		return result;
	}
}

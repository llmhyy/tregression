package tregression.handler;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

import defects4janalysis.ResultWriter;
import defects4janalysis.RunResult;
import jmutation.dataset.BugDataset;
import jmutation.dataset.bug.minimize.ProjectMinimizer;
import jmutation.dataset.bug.model.path.MutationFrameworkPathConfiguration;
import jmutation.dataset.bug.model.path.PathConfiguration;
import microbat.ActivatorStub;
import microbat.model.trace.Trace;
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

		int project_count = 0;
		int success_count = 0;

		ResultWriter writer = new ResultWriter(resultPath);
		writer.writeTitle();

		File baseFolder = new File(basePath);

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
				int bugId;
				try {
					bugId = Integer.parseInt(bugID_str);
				} catch (NumberFormatException e) {
					continue;
				}

				if (projectFilters.contains(projectName + ":" + bugID_str)) {
					throw new RuntimeException("Will cause hanging problem");
				}

				RunResult result;
				try {
					dataset.unzip(bugId);
					ProjectMinimizer minimizer = dataset.createMinimizer(bugId);
					minimizer.maximise();
					result = collectSingleResult(basePath, projectName, bugId);
					minimizer.minimize();
					dataset.zip(bugId);
				} catch (IOException e) {
					// Crash from zipping or unzipping
					e.printStackTrace();
					continue;
				}
				writer.writeResult(result);
			}
		}
		writer.writeResult(success_count, project_count);
		return Status.OK_STATUS;
	}

	private RunResult collectSingleResult(String basePath, String projectName, int bugId) {
		String bugID_str = String.valueOf(bugId);
		PathConfiguration pathConfig = new MutationFrameworkPathConfiguration(basePath);
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
			ProjectConfig config = ConfigFactory.createConfig(projectName, bugID_str, bugFolder, fixFolder);

			if (config == null) {
				throw new Exception(
						"cannot parse the configuration of the project " + projectName + " with id " + bugID_str);
			}
			
			MutationDatasetProjectConfig.executeMavenCmd(Paths.get(bugFolder), "test-compile");
			
			// TrailGenerator will generate the buggy trace and fixed trace
			List<EmpiricalTrial> trials = new TrialGenerator0().generateTrials(bugFolder, fixFolder, false, false,
					false, 3, true, true, config, "");

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

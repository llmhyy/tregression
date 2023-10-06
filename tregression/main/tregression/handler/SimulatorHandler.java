package tregression.handler;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.runner.Runner;

import microbat.Activator;
import microbat.util.JavaUtil;
import tregression.auto.AutoSimulationMethod;
import tregression.auto.Defects4jMicrobatRunner;
import tregression.auto.Defects4jProfInferRunner;
import tregression.auto.ProjectsRunner;
import tregression.preference.TregressionPreference;

public class SimulatorHandler extends AbstractHandler {

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
	
	protected void execute() {
		final String inputFolder = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.INPUT_FOLDER_KEY);
		File folder = new File(inputFolder);
		if (!(folder.exists() && folder.isDirectory())) {
			throw new RuntimeException("Give input folder does not exist: " + inputFolder);
		}
		
		final String outputFolderStr = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.OUTPUT_PATH_KEY);
		File outputFolder = new File(outputFolderStr);
		if (!(outputFolder.exists() && outputFolder.isDirectory())) {
			throw new RuntimeException("Given output folder does not exist: " + outputFolderStr);
		}
		
		final String mistakePrbabilityStr = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.MISTAKE_PROBABILITY_KEY);
		final double mistakeProbability;
		try {
			mistakeProbability = Double.parseDouble(mistakePrbabilityStr);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Given probability is not a double value:" + mistakePrbabilityStr);
		}
		
		final String autoSimulationMethodStr = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.AUTO_SIMULATION_METHOD_KEY);
		if (autoSimulationMethodStr == null || autoSimulationMethodStr.isEmpty()) {
			throw new RuntimeException("Invalid auto simulation method:" + autoSimulationMethodStr);
		}
		final AutoSimulationMethod simulationMethod = AutoSimulationMethod.valueOf(autoSimulationMethodStr);
		
		final String timeLimitStr = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TIME_LIMIT_KEY);
		final Long timeLimit;
		try {
			timeLimit = Long.parseLong(timeLimitStr);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Given time limit is not a double value:" + timeLimitStr);
		}
		
		final String resultFileName = simulationMethod.name() + "_" + String.valueOf(mistakeProbability).replace(".", "_") + ".txt";
		final String outputPath = Paths.get(outputFolderStr, resultFileName).toString();
		
		ProjectsRunner runner = null;
		switch (simulationMethod) {
		case DEBUG_PILOT:
			runner = new Defects4jMistakeDebugPIlotRunner(inputFolder, outputPath, mistakeProbability, timeLimit);
			break;
		case FG:
			runner = new Defects4jProfInferRunner(inputFolder, outputPath, mistakeProbability, timeLimit);
			break;
		case MICROBAT:
			runner = new Defects4jMicrobatRunner(inputFolder, outputPath, mistakeProbability, timeLimit);
			break;
		default:
			throw new RuntimeException("Unhandled auto method:" + simulationMethod);
		}
		runner.run();
		
		
	}
	
	protected void printSetting(final String inputFolder, final String outputPath, final double mistakeProbability, final long timeLimit, final AutoSimulationMethod method) {
		System.out.println("--------------------------------");
		System.out.println("Input Folder: " + inputFolder);
		System.out.println("Output Path: " + outputPath);
		System.out.println("Mistake Probability: " + mistakeProbability);
		System.out.println("Time Limit (Minutes): " + timeLimit);
		System.out.println("Auto Simulation Method: " + method.name());
		System.out.println("--------------------------------");
	}
}

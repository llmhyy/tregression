package tregression.autofeedbackevaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.bcel.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.Activator;
import microbat.baseline.encoders.ProbabilityEncoder;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.autofeedback.AutoFeedbackMethods;
import tregression.autofeedback.FeedbackGenerator;
import tregression.autofeedback.NodeFeedbackPair;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.empiricalstudy.TrialRecorder;
import tregression.empiricalstudy.config.ConfigFactory;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.handler.PathConfiguration;
import tregression.handler.PlayRegressionLocalizationHandler;
import tregression.preference.TregressionPreference;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class AutoDebugEvaluator {
	
	// Simulate debug process
	private AutoFeedbackMethods method;
	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	/**
	 * Factor that bound the maximum allowable iteration for baseline.
	 * For example, if the given trace with length 100, then allowable iteration will be 100 * factor
	 */
	private final double maxBaselineItrFactor = 0.5; 
	
	/**
	 * List of accuracy measurement of debugging of all bug report
	 */
	List<AccMeasurement> measurements;
	
	public AutoDebugEvaluator(AutoFeedbackMethods method) {
		this.method = method;
		this.measurements = new ArrayList<>();
	}
	
	/**
	 * If the evaluation process is completed and measurement is store in file, then directly giving path to file can allow evaluator skip the evaluation process
	 * @param path Path to measurement data file
	 */
	public AutoDebugEvaluator(Path path) {
		
		this.method = null;
		this.buggyView = null;
		this.correctView = null;
		this.measurements = new ArrayList<>();
		
		try {
			File file = path.toFile();
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while(line != null) {
				AccMeasurement measurement = AccMeasurement.parseAccMeasurement(line);
				this.measurements.add(measurement);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			this.measurements = null;
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Evaluate the performance of each debugging approach for all buggy trace. The measurement is stored in the data member
	 */
	public void evaulateAll(String projectName, int bugCount) {
		
		for (int bugID=1; bugID<=bugCount; bugID++) {
			
			if (bugID != 12) {
				continue;
			}
			System.out.println("Evaluating bug id: " + bugID);
			try {
				
				String bugIDString = String.valueOf(bugID);
				this.setup(projectName, bugIDString);
				
				if(this.checkSetUp()) {
					
					AccMeasurement measurement = this.evaluate(projectName, bugID);
					this.measurements.add(measurement);
				} else {
					System.out.println("Set up error");
				}
				
				// Current set up is only valid for one bug id, so that we need to clear
				// the set up for previous bug id for the next bug id.
				this.clearSetUp();
				
			} catch (Exception e) {
				this.clearSetUp();
				System.out.println("Error occur in bugID: " + bugID);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Evaluate the performance of debugging for one buggy trace.
	 * @return The accuracy measurement of this buggy trace
	 */
	public AccMeasurement evaluate(String projectName, int bugID) {
		int noOfFeedbackNeeded = 0;
		Trace buggyTrace = this.buggyView.getTrace();
		
		if (this.method == AutoFeedbackMethods.BASELINE) {
			noOfFeedbackNeeded = this.evaluateBaseline(buggyTrace);
			AccMeasurement measurement = new AccMeasurement(projectName, bugID);
			measurement.setnoOfFeedbackNeeded(noOfFeedbackNeeded);
			return measurement;
		} else {
			List<NodeFeedbackPair> predictions = this.predictFeedbacks(buggyTrace);
			List<NodeFeedbackPair> refs = this.getRefFeedbacks(this.buggyView, this.correctView);
			
			// Count number of human feedback needed
			for (NodeFeedbackPair prediction : predictions) {
				if(prediction.getFeedback().getFeedbackType() == UserFeedback.UNCLEAR) {
					noOfFeedbackNeeded++;
				}
			}
			
			AccMeasurement measurement = new AccMeasurement(projectName, bugID, noOfFeedbackNeeded, predictions, refs);
			return measurement;
		}
		
	}
	
	/**
	 * Evaluate the baseline performance
	 * @param buggyTrace Buggy trace
	 * @return Number of user feedback needed to reach the root cause
	 */
	private int evaluateBaseline(Trace buggyTrace) {
		
		final int maxItr = (int) (buggyTrace.size() * this.maxBaselineItrFactor);
		int noOfFeedbackNeeded = 0;
		
		ProbabilityEncoder encoder = new ProbabilityEncoder(buggyTrace);
		encoder.setup();
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, this.correctView.getTrace());
		TraceNode ref = this.getFirstDeviationNode(PlayRegressionLocalizationHandler.finder);

		while(noOfFeedbackNeeded < maxItr) {
			encoder.encode();
			
			TraceNode result = encoder.getMostErroneousNode();
			System.out.println("Ground Truth: " + ref.getOrder() + ", Predication: " + result.getOrder());
			
			// Case that baseline find out the root cause
			if (result.getLineNumber() == ref.getLineNumber()) {
				break;
			}
			
			StepChangeType type = typeChecker.getType(result, true, this.buggyView.getPairList(), this.buggyView.getDiffMatcher());
			UserFeedback feedback = this.typeToFeedback(type, result, true, PlayRegressionLocalizationHandler.finder);
			encoder.updateProbability(result, feedback);
			
			noOfFeedbackNeeded++;
		}
		
		return noOfFeedbackNeeded;
	}
	
	public List<AccMeasurement> getMeasurements() {
		return this.measurements;
	}

	/**
	 * After evaluating all the bug in project, this function calculate the average value of all measurement.
	 * Note that the average result is store into the same AccMeasurement Object for convenience. Note that this AccMeasurement
	 * has member bugID to be -1.
	 * @return Average of all measurement. Null if the evaluation is not completed yet.
	 */
	public AvgAccMeasurement getAvgMeasurement() {
		return new AvgAccMeasurement(this.measurements);
	}
	
	/**
	 * Predict feedback of all the trace node in the given buggy trace
	 * @param buggyTrace Target buggy trace
	 * @return List of node feedback pair
	 */
	private List<NodeFeedbackPair> predictFeedbacks(Trace buggyTrace) {
		List<NodeFeedbackPair> prediction = new ArrayList<>();
		
		List<TraceNode> executionList = buggyTrace.getExecutionList();
		executionList = this.reverseOrder(executionList);
		
		// Predict feedback
		FeedbackGenerator generator = FeedbackGenerator.getFeedbackGenerator(buggyTrace, this.method);
		generator.setVerbal(false); // Don't print out debug message
		for (TraceNode node : executionList) {
			System.out.println("Predicting feedback on node " + node.getOrder());
			UserFeedback predictedFeedback = generator.giveFeedback(node);
			if (predictedFeedback.getFeedbackType() == UserFeedback.UNCLEAR) {
				generator.requestUserFeedback(node, buggyView, correctView);
			}
			prediction.add(new NodeFeedbackPair(node, predictedFeedback));
		}
		
		return prediction;
	}
	
	/**
	 * Get the ground truth feedback of all trace node
	 * @param buggyView Tregression buggy view
	 * @param correctView Tregression correct view
	 * @return List of ground truth node feedback pair
	 */
	private List<NodeFeedbackPair> getRefFeedbacks(BuggyTraceView buggyView, CorrectTraceView correctView) {
		List<NodeFeedbackPair> refFeedbacks = new ArrayList<>();
		
		Trace buggyTrace = buggyView.getTrace();
		Trace correctTrace = correctView.getTrace();
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		List<TraceNode> executionList = buggyTrace.getExecutionList();
		executionList = this.reverseOrder(executionList);
		
		for(TraceNode node : executionList) {
			StepChangeType type = typeChecker.getType(node, true, buggyView.getPairList(), buggyView.getDiffMatcher());
			UserFeedback feedback = this.typeToFeedback(type, node, true, PlayRegressionLocalizationHandler.finder);
			refFeedbacks.add(new NodeFeedbackPair(node, feedback));
		}
		
		return refFeedbacks;
	}
	
	/**
	 * Reverse the order of give trace
	 * @param trace List of trace node
	 * @return Reversed trace
	 */
	private List<TraceNode> reverseOrder(List<TraceNode> trace) {
		trace.sort(new Comparator<TraceNode>() {
			@Override
			public int compare(TraceNode o1, TraceNode o2) {
				return o2.getOrder() - o1.getOrder();
			}
		});
		return trace;
	}
	
	/**
	 * Set up the variables needed for evaluation
	 * @param projectPath Project Name
	 * @param bugID Bug id
	 */
	public void setup(String projectPath, String bugID) {
		this.updateFinder(projectPath, bugID);
		this.updateTraceView();
	}
	
	/**
	 * Check is the current set up ready to do evaluation. In order to do evaluation,
	 * the buggy view, correct view and the finder should be available.
	 * @return True if the set up is ready.
	 */
	public boolean checkSetUp() {
		return this.buggyView != null && this.correctView != null && PlayRegressionLocalizationHandler.finder != null;
	}
	
	/**
	 * Clear the set up so that it will not mess up with other bug id.
	 */
	public void clearSetUp() {
		this.buggyView = null;
		this.correctView = null;
		PlayRegressionLocalizationHandler.finder = null;
	}
	
	/**
	 * Export all the measurement to text file in csv format.
	 * @param path Path to the folder to store the text file.
	 */
	public void exportCSV(Path path) {
		try {
			File file = path.toFile();
			file.createNewFile();
			
			FileWriter writer = new FileWriter(file);
			for (AccMeasurement measurement : this.measurements) {
				writer.write(measurement.toCSV() + '\n');
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Failed to export measurement");
			e.printStackTrace();
		}
	}
	
	/**
	 * Generate the file name for storing the measurements
	 * @param projectName Name of the project
	 * @param method Evaluation method
	 * @return Name of the text file
	 */
	public static String genFileName(String projectName, AutoFeedbackMethods method) {
		return "Measurements_" + projectName + "_" + method.name() + ".txt";
	}
	
	private void updateFinder(String projectPath, String bugID) {
		String buggyPath = PathConfiguration.getBuggyPath(projectPath, bugID);
		String fixPath = PathConfiguration.getCorrectPath(projectPath, bugID);
		
		String projectName = projectPath;
		String id = bugID;

		String testcase = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
		
		System.out.println("working on the " + id + "th bug of " + projectName + " project.");
		
		ProjectConfig config = ConfigFactory.createConfig(projectName, id, buggyPath, fixPath);
		
		if(config == null) {
			try {
				throw new Exception("cannot parse the configuration of the project " + projectName + " with id " + id);						
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		TrialGenerator0 generator0 = new TrialGenerator0();
		
		List<EmpiricalTrial> trials = generator0.generateTrials(buggyPath, fixPath, 
				false, false, false, 3, true, true, config, testcase);
		
		if(trials.size() != 0) {
			PlayRegressionLocalizationHandler.finder = trials.get(0).getRootCauseFinder();					
		}
		
		for(int i=0; i<trials.size(); i++) {
			
			EmpiricalTrial t = trials.get(i);
			Trace trace = t.getBuggyTrace();
			
			if(!t.getDeadEndRecordList().isEmpty()){
				Repository.clearCache();
				DeadEndRecord record = t.getDeadEndRecordList().get(0);
				DED datas = record.getTransformedData(trace);
				setTestCase(datas, t.getTestcase());						
				try {
					new DeadEndCSVWriter("_d4j", null).export(datas.getAllData(), projectName, id);
				} catch (NumberFormatException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		
//		try {
//			TrialRecorder recorder = new TrialRecorder();
//			recorder.export(trials, projectName, Integer.valueOf(id));
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	private void updateTraceView() {
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	private void setTestCase(DED datas, String tc) {
		if(datas.getTrueData()!=null){
			datas.getTrueData().testcase = tc;					
		}
		for(DeadEndData data: datas.getFalseDatas()){
			data.testcase = tc;
		}
	}
	
	/**
	 * Convert the StepChangeType to UserFeedback
	 * @param type StepChangeType to be converted 
	 * @param node Variable needed for getting wrong variable
	 * @param isOnBefore Variable needed for getting wrong variable
	 * @param finder Variable needed for getting wrong variable
	 * @return Converted UserFeedback
	 */
	private UserFeedback typeToFeedback(StepChangeType type, TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
		UserFeedback feedback = new UserFeedback();
		switch(type.getType()) {
		case StepChangeType.IDT:
			feedback.setFeedbackType(UserFeedback.CORRECT);
			break;
		case StepChangeType.CTL:
			feedback.setFeedbackType(UserFeedback.WRONG_PATH);
			break;
		case StepChangeType.DAT:
			feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
			VarValue wrongVar = type.getWrongVariable(node, isOnBefore, finder);
			feedback.setOption(new ChosenVariableOption(wrongVar, null));
			break;
		case StepChangeType.SRC:
			feedback.setFeedbackType(UserFeedback.UNCLEAR);
			break;
		}
		return feedback;
	}
	
	/**
	 * Get the first deviation node from buggy trace.
	 * @param finder Root cause finder from PlayRegressionLocalizationHandler
	 * @return First deviation node. Can be null when error occur
	 */
	private TraceNode getFirstDeviationNode(RootCauseFinder finder) {
		
		if (finder == null) {
			System.out.println("BaselineFeedbackGenerator.getFirstDeviationNode Error: finder is null");
			return null;
		}
		
		List<RootCauseNode> deviationPoints = finder.getRealRootCaseList();
		if (deviationPoints.isEmpty()) {
			System.out.println("BaselineFeedbackGenerator.getFirstDeviationNode Error: getRealRootCaseList is empty");
			return null;
		}
		
		TraceNode firstDeviationNode = deviationPoints.get(0).getRoot();
		for (RootCauseNode deviationNode : deviationPoints) {
			if (deviationNode.getRoot().getOrder() < firstDeviationNode.getOrder()) {
				firstDeviationNode = deviationNode.getRoot();
			}
		}
		
		return firstDeviationNode;
	}
}

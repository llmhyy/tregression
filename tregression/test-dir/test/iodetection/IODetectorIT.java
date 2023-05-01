package iodetection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.handler.PlayRegressionLocalizationHandler;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IODetectorIT {

	private static final Path traceDir = Paths.get("test-dir", "files", "iodetection");

	@Test
	void detect_SingleAssertionAndAllInputsInTest_ObtainsIOCorrectly() {
		Trace buggyTrace = RunningInfo
				.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		Trace workingTrace = RunningInfo
				.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		IODetector detector = new IODetector(buggyTrace, workingTrace);
		detector.detect();
		List<VarValue> expectedInputs = new ArrayList<>();
		List<VarValue> expectedOutputs = new ArrayList<>();
		TraceNode outputNode = buggyTrace.getTraceNode(4);
		assertEquals(expectedInputs, detector.getInputs());
		assertEquals(expectedOutputs, detector.getOutputs());
	}

	@Test
	void detect_MultipleAssertionsAndAllInputsInTest_ObtainsIOCorrectly() {

	}

	@Test
	void detect_SingleAssertionAndNotAllInputsInTest_ObtainsIOCorrectly() {

	}

	@Test
	void detect_MultipleAssertionsAndNotAllInputsInTest_ObtainsIOCorrectly() {

	}
	
	@Test
	void detect_MultipleAssertionsAndCrashFailure_ObtainsIOCorrectly() {

	}
	
	// To view the traces in the test case in the Eclipse application. Only works on Server 250.
	void setRootCauseFinder(Trace buggyTrace, Trace workingTrace) {
		// 4 arguments: relative src folder path, relative test folder path, working project root, buggy project root
		// E.g. "src\\main\\java", "src\\test\\java", "E:\\david\\Mutation_Dataset\\math_70\\fix", "E:\\david\\Mutation_Dataset\\math_70\\1\\bug"
		DiffMatcher diffMatcher = new DiffMatcher("", "", "", "");
		diffMatcher.matchCode();
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, workingTrace,
				diffMatcher);
		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, workingTrace);
		PlayRegressionLocalizationHandler.finder = rootcauseFinder;
		// Run the left-most orange "Play the process of Regression Localization" button in eclipse application
	}
}

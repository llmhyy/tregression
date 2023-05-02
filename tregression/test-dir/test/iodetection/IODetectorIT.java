package iodetection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import microbat.instrumentation.output.RunningInfo;
import microbat.model.BreakPoint;
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

	private static final String SAMPLE_TEST_DIR = "src/main/test";

	// math_70 bug ID 1
	@Test
	void detect_SingleAssertionAndAllInputsInTest_ObtainsIOCorrectly() {
		Trace buggyTrace = RunningInfo
				.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		Trace workingTrace = RunningInfo
				.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		IODetector detector = new IODetector(buggyTrace, workingTrace, "");
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

	// To view the traces in the test case in the Eclipse application. Only works on
	// Server 250.
	void setRootCauseFinder(Trace buggyTrace, Trace workingTrace) {
		// 4 arguments: relative src folder path, relative test folder path, working
		// project root, buggy project root
		// E.g. "src\\main\\java", "src\\test\\java",
		// "E:\\david\\Mutation_Dataset\\math_70\\fix",
		// "E:\\david\\Mutation_Dataset\\math_70\\1\\bug"
		DiffMatcher diffMatcher = new DiffMatcher("", "", "", "");
		diffMatcher.matchCode();
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, workingTrace, diffMatcher);
		RootCauseFinder rootcauseFinder = new RootCauseFinder();
		rootcauseFinder.setRootCauseBasedOnDefects4J(pairList, diffMatcher, buggyTrace, workingTrace);
		PlayRegressionLocalizationHandler.finder = rootcauseFinder;
		// Run the left-most orange "Play the process of Regression Localization" button
		// in eclipse application
	}

	@Nested
	class ObtainingInputs {
		@Test
		void detectInputVarValsFromOutput_AnonymousInputs_ObtainsInputs() {
			Trace buggyTrace = RunningInfo
					.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			setJavaPathToBreakPoints(buggyTrace.getExecutionList());
			setJavaPathToBreakPoints(workingTrace.getExecutionList());
			IODetector detector = new IODetector(buggyTrace, workingTrace, SAMPLE_TEST_DIR);
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			for (VarValue input : buggyTrace.getTraceNode(2).getWrittenVariables()) {
				expectedInputs.add(input);
			}
			expectedInputs.add(buggyTrace.getTraceNode(2).getReadVariables().get(0));
			assertEquals(expectedInputs, new HashSet<>(inputs));
		}

		@Test
		void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs() {
			Trace buggyTrace = RunningInfo.readFromFile(
					traceDir.resolve("buggy-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(
					traceDir.resolve("working-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			setJavaPathToBreakPoints(buggyTrace.getExecutionList());
			setJavaPathToBreakPoints(workingTrace.getExecutionList());
			IODetector detector = new IODetector(buggyTrace, workingTrace, SAMPLE_TEST_DIR);
			TraceNode outputNode = buggyTrace.getTraceNode(1538);
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			for (VarValue input : buggyTrace.getTraceNode(2).getWrittenVariables()) {
				expectedInputs.add(input);
			}
			expectedInputs.add(buggyTrace.getTraceNode(2).getReadVariables().get(0));
			assertEquals(expectedInputs, new HashSet<>(inputs));
		}
	}
	
	private void setJavaPathToBreakPoints(List<TraceNode> executionList) {
		Predicate<TraceNode> testNodeFilter = new Predicate<TraceNode>() {
			@Override
			public boolean test(TraceNode node) {
				BreakPoint breakPoint = node.getBreakPoint();
				return breakPoint.getClassCanonicalName().endsWith("Test");
			}
		};
		setJavaPathToBreakPoints(executionList, testNodeFilter);
	}
	
	private void setJavaPathToBreakPoints(List<TraceNode> executionList, final Set<Integer> nodesInTestFile) {
		Predicate<TraceNode> testNodeFilter = new Predicate<TraceNode>() {
			@Override
			public boolean test(TraceNode node) {
				return nodesInTestFile.contains(node.getOrder() - 1);
			}
		};
		setJavaPathToBreakPoints(executionList, testNodeFilter);
	}

	private void setJavaPathToBreakPoints(List<TraceNode> executionList, Predicate<TraceNode> testNodeFilter) {
		for (int i = 0; i < executionList.size(); i++) {
			TraceNode node = executionList.get(i);
			BreakPoint breakPoint = node.getBreakPoint();
			if (testNodeFilter.test(node)) {
				breakPoint.setFullJavaFilePath(SAMPLE_TEST_DIR);
			} else {
				breakPoint.setFullJavaFilePath("");
			}
		}
	}
}

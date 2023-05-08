package iodetection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import iodetection.IODetector.IOModel;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import microbat.instrumentation.output.RunningInfo;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.MinimumASTNodeFinder;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.config.MavenProjectConfig;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: Currently we are using traces from math_70 data set mutations. If the instrumentator & data set is updated, it may be impossible to reproduce the trace files with the exact same mutations (all tests will require updates).
// Thus, the projects used to create the traces should be stored in the test-files directory. However, math_70 is very large.
// We should create a small project that allows us to regenerate the traces used in the tests (and modify the tests to use them instead).
class IODetectorIT {

	private static final Path testFilesDir = Paths.get("test-dir", "files", "iodetection");

	private IODetector constructIODetector(Trace buggyTrace, Trace workingTrace, PairList pairList) {
		return new IODetector(buggyTrace, workingTrace, TEST_DIR, pairList);
	}

	private static final String DATA_SET_BUG_DIR_FORMAT = "E:\\david\\Mutation_Dataset\\math_70\\%d\\bug";
	private static final String DATA_SET_FIX_DIR = "E:\\david\\Mutation_Dataset\\math_70\\fix";
	private static final String SAMPLE_PROJECT_FORMAT = "projects\\%d";
	private static final String SRC_DIR = "src\\main\\java";
	private static final String TEST_DIR = "src\\test\\java";
	private static final String MVN_TEST_COMPILE = "test-compile";

	private Map<String, CompilationUnit> pathToCompilationUnitMap;

	@BeforeEach
	void setUp() {
		pathToCompilationUnitMap = new HashMap<>();
	}

	// math_70 bug ID 1
	@Test
	void detect_SingleAssertionAndAllInputsInTest_ObtainsIOCorrectly() {
		Trace buggyTrace = RunningInfo
				.readFromFile(testFilesDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		Trace workingTrace = RunningInfo
				.readFromFile(testFilesDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 1);
		IODetector detector = constructIODetector(buggyTrace, workingTrace,
				createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
		detector.detect();
		List<VarValue> expectedInputs = new ArrayList<>();
		List<VarValue> expectedOutputs = new ArrayList<>();
		TraceNode outputNode = buggyTrace.getTraceNode(4);
		assertEquals(expectedInputs, detector.getInputs());
		assertEquals(expectedOutputs, detector.getOutputs());
	}

	private PairList createPairList(Trace buggyTrace, Trace workingTrace, String buggyPath, String workingPath) {
		// 4 arguments: relative src folder path, relative test folder path, working
		// project root, buggy project root
		// E.g. "src\\main\\java", "src\\test\\java",
		// "E:\\david\\Mutation_Dataset\\math_70\\fix",
		// "E:\\david\\Mutation_Dataset\\math_70\\1\\bug"
		DiffMatcher diffMatcher = new DiffMatcher(SRC_DIR, TEST_DIR, buggyPath, workingPath);
		diffMatcher.matchCode();
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, workingTrace, diffMatcher);
		return pairList;
		// Run the left-most orange "Play the process of Regression Localization" button
		// in eclipse application
	}

	@Nested
	class ObtainingInputs {

		@Nested
		class Math70 {
			// math_70 bug ID 1
			@Test
			void detectInputVarValsFromOutput_AnonymousInputs_ObtainsInputs() {
				Trace buggyTrace = RunningInfo
						.readFromFile(
								testFilesDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(
								testFilesDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
						.getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 1);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				TraceNode outputNode = buggyTrace.getLatestNode();
				outputNode.getReadVariables().get(0);
				VarValue output = outputNode.getReadVariables().get(0);
				List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
				Set<VarValue> expectedInputs = new HashSet<>();
				for (VarValue input : buggyTrace.getTraceNode(1).getWrittenVariables()) {
					expectedInputs.add(input);
				}
				expectedInputs.add(buggyTrace.getTraceNode(1).getReadVariables().get(0));
				assertEquals(expectedInputs, new HashSet<>(inputs));
			}

			// math_70 bug ID 2
			@Test
			void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir
								.resolve("buggy-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir
								.resolve("working-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
						.getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 2);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				TraceNode outputNode = buggyTrace.getTraceNode(1537);
				outputNode.getReadVariables().get(0);
				VarValue output = outputNode.getReadVariables().get(0);
				List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
				Set<VarValue> expectedInputs = new HashSet<>();
				// x at line 86, Math.cosh, sinh, tanh, log, ceil
//			assertEquals(expectedInputs, new HashSet<>(inputs));
				// Current inputs:
				// CBRT, COSH, EXPM1, TANH, ABS, this, x, conditional result of loop, SQRT, SINH
				// Does not have log, ceil (both used "postCompose", e.g. log.postCompose, while
				// others used "of"), and has additional cbrt.
				// Sometimes, the VarValues' children can form links as well.
				// After fixing bug: sqrt, log, tanh, sinh, abs, ceil, expm1, cosh, cbrt, 0.1,
				// conditional result
			}

			// math_70 bug ID 3
			@Test
			void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs1() {
				Trace buggyTrace = RunningInfo.readFromFile(testFilesDir.resolve("buggy-math_70-3-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-math_70-3-trace.exec").toFile()).getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 3);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				TraceNode outputNode = buggyTrace.getLatestNode();
				outputNode.getReadVariables().get(0);
				VarValue output = outputNode.getReadVariables().get(0);
				List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
				Set<VarValue> expectedInputs = new HashSet<>();
//			assertEquals(expectedInputs, new HashSet<>(inputs));
				// expected ComposableFunction.COS, 3, x
				// After modifying to use getInvocationMethodOrControlDominator:
				// We have COS, 0.1/x at line 357, 3/a, SIN, this at line 23, conditional result
				// in for loop, 5/scaleFactor in line 398
				// - Why did we get sin? Anonymous class data domination is not accurate. It
				// will point to the previous same anonymous class initialization
				// node 45, this$0 -> node 38 (another anonymous ComposableFunction init)
			}

			// math_70 bug ID 5
			// Expected .2, .2, .5, a, b, c
			// Current obtained inputs:
			// We now have .2, .2, .5, a, b, c, drk, Double array.
			// Questions:
			// 1. Why do we have Double array, but not String array?
			// Ans: String array is passed into Arrays.asList, which is excluded from
			// instrumentation.
			// 2. Why do we have drk & its original input? (Should only be original input)
			// Ans: When initialization for drk is done, it is only written to drk. The
			// constructor return value is not read.
			@Test
			void detectInputVarValsFromOutput_ArrayInputs_ObtainsInputs() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-ArrayInput-trace.exec").toFile()).getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-ArrayInput-trace.exec").toFile()).getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 5);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				TraceNode outputNode = buggyTrace.getLatestNode();
				outputNode.getReadVariables().get(0);
				VarValue output = outputNode.getReadVariables().get(0);
				List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
				Set<VarValue> expectedInputs = new HashSet<>();
			}
		}

		@Test
		void detectInputVarValsFromOutput_InputNotReadOnlyWritten_ObtainsInputs() {
			Trace buggyTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("buggy-input-written-only-trace.exec").toFile()).getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("working-input-written-only-trace.exec").toFile())
					.getMainTrace();
			String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 5)).toAbsolutePath()
					.toString();
			String buggyPath = projectRoot + "\\bug";
			String workingPath = projectRoot + "\\fix";
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
			IODetector detector = constructIODetector(buggyTrace, workingTrace,
					createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			expectedInputs.addAll(buggyTrace.getTraceNode(1).getReadVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(4).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(5).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(6).getWrittenVariables());
			assertEquals(expectedInputs, new HashSet<>(inputs));
		}

		@Test
		void detectInputVarValsFromOutput_ArrayInputs_ObtainsInputs() {
			Trace buggyTrace = RunningInfo.readFromFile(testFilesDir.resolve("buggy-array-input-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("working-array-input-trace.exec").toFile()).getMainTrace();
			String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 6)).toAbsolutePath()
					.toString();
			String buggyPath = projectRoot + "\\bug";
			String workingPath = projectRoot + "\\fix";
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
			IODetector detector = constructIODetector(buggyTrace, workingTrace,
					createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			expectedInputs.addAll(buggyTrace.getTraceNode(1).getReadVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(2).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(5).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(6).getWrittenVariables());
			assertEquals(expectedInputs, new HashSet<>(inputs));
			// add the 4 array value
			// LocalVariable [type=regularproject.MainTest, variableName=this]:
			// regularproject/MainTest{7,7}this-0
//			LocalVariable [type=regularproject.Main, variableName=main]: regularproject/MainTest{12,14}main-1
//			ArrayElementVar [type=java.lang.Object, variableName=1993134103[0]]: 1993134103[0]
//			ArrayElementVar [type=java.lang.Object, variableName=1993134103[1]]: 1993134103[1]
		}

		@Test
		void detectInputVarValsFromOutput_WrongIntermediateValue_DoesNotIncludeWrongValueAsInput() {
			Trace buggyTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("buggy-wrong-intermediate-value-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("working-wrong-intermediate-value-trace.exec").toFile())
					.getMainTrace();
			String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 7)).toAbsolutePath()
					.toString();
			String buggyPath = projectRoot + "\\bug";
			String workingPath = projectRoot + "\\fix";
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
			IODetector detector = constructIODetector(buggyTrace, workingTrace,
					createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			expectedInputs.addAll(buggyTrace.getTraceNode(1).getReadVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(4).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(5).getWrittenVariables());
			expectedInputs.add(buggyTrace.getTraceNode(8).getWrittenVariables().get(0));
			assertEquals(expectedInputs, new HashSet<>(inputs));
		}

		@Test
		void detectInputVarValsFromOutput_InputFromAnotherTestClass_ObtainsInputs() {
			Trace buggyTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("buggy-input-from-another-class-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(testFilesDir.resolve("working-input-from-another-class-trace.exec").toFile())
					.getMainTrace();
			String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 8)).toAbsolutePath()
					.toString();
			String buggyPath = projectRoot + "\\bug";
			String workingPath = projectRoot + "\\fix";
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
			IODetector detector = constructIODetector(buggyTrace, workingTrace,
					createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			expectedInputs.addAll(buggyTrace.getTraceNode(1).getReadVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(4).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(8).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(9).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(11).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(12).getWrittenVariables());
			expectedInputs.addAll(buggyTrace.getTraceNode(13).getWrittenVariables());
			assertEquals(expectedInputs, new HashSet<>(inputs));
		}
	}

	@Nested
	class ObtainingOutputs {
		@Nested
		class Math70 {
			// math_70 bug ID 1
			@Test
			void detectOutput_LastNodeAssertionSingleReadVar_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(
								testFilesDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(
								testFilesDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
						.getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 1);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				IOModel output = detector.detectOutput();
				assertEquals(3, output.getNode().getOrder());
				TraceNode outputNode = buggyTrace.getLatestNode();
				VarValue expectedVarVal = outputNode.getReadVariables().get(0);
				assertEquals(expectedVarVal, output.getVarVal());
			}

			// math_70 bug ID 2
			@Test
			void detectOutput_MultiLineAssertionWithEpsilon_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir
								.resolve("buggy-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir
								.resolve("working-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
						.getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 2);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				IOModel output = detector.detectOutput();
				assertEquals(1537, output.getNode().getOrder());
				TraceNode expectedOutputNode = buggyTrace.getTraceNode(1537);
				VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
				assertEquals(expectedVarVal, output.getVarVal());
			}

			// math_70 bug ID 3
			@Test
			void detectOutput_LastNodeAssertion_ObtainsOutput() {
				Trace buggyTrace = RunningInfo.readFromFile(testFilesDir.resolve("buggy-math_70-3-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-math_70-3-trace.exec").toFile()).getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 3);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getLatestNode();
				VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}

			// math_70 bug ID 5
			@Test
			void detectOutput_ArrayInputs_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-ArrayInput-trace.exec").toFile()).getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-ArrayInput-trace.exec").toFile()).getMainTrace();
				String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, 5);
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), DATA_SET_FIX_DIR);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, DATA_SET_FIX_DIR));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getLatestNode();
				VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}
		}

		@Nested
		class ObtainingOutputsAfterException {
			@Test
			void detectOutput_ExceptionThrown_ObtainsControlDominatorAsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-regular-exception-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-regular-exception-trace.exec").toFile())
						.getMainTrace();
				String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 1)).toAbsolutePath()
						.toString();
				String buggyPath = projectRoot + "\\bug";
				String workingPath = projectRoot + "\\fix";
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getTraceNode(6);
				VarValue expectedVarVal = expectedOutputNode.getWrittenVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}

			@Test
			void detectOutput_NullPointerExceptionThrown_ObtainsControlDominatorAsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-null-ptr-exception-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-null-ptr-exception-trace.exec").toFile())
						.getMainTrace();
				String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 2)).toAbsolutePath()
						.toString();
				String buggyPath = projectRoot + "\\bug";
				String workingPath = projectRoot + "\\fix";
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getLatestNode();
				VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}

			@Test
			void detectOutput_DivByZeroExceptionThrown_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-div-by-zero-exception-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-div-by-zero-exception-trace.exec").toFile())
						.getMainTrace();
				String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 3)).toAbsolutePath()
						.toString();
				String buggyPath = projectRoot + "\\bug";
				String workingPath = projectRoot + "\\fix";
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getLatestNode();
				VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}

			@Test
			void detectOutput_OutOfBoundsExceptionThrown_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-out-of-bounds-exception-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-out-of-bounds-exception-trace.exec").toFile())
						.getMainTrace();
				String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 4)).toAbsolutePath()
						.toString();
				String buggyPath = projectRoot + "\\bug";
				String workingPath = projectRoot + "\\fix";
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getTraceNode(14);
				VarValue expectedVarVal = expectedOutputNode.getWrittenVariables().get(0);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}

			@Test
			void detectOutput_OutOfBoundsExceptionThrownDueToDataStructureSize_ObtainsOutput() {
				Trace buggyTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("buggy-out-of-bounds-list-size-trace.exec").toFile())
						.getMainTrace();
				Trace workingTrace = RunningInfo
						.readFromFile(testFilesDir.resolve("working-out-of-bounds-list-size-trace.exec").toFile())
						.getMainTrace();
				String projectRoot = testFilesDir.resolve(String.format(SAMPLE_PROJECT_FORMAT, 9)).toAbsolutePath()
						.toString();
				String buggyPath = projectRoot + "\\bug";
				String workingPath = projectRoot + "\\fix";
				appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
				appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
				IODetector detector = constructIODetector(buggyTrace, workingTrace,
						createPairList(buggyTrace, workingTrace, buggyPath, workingPath));
				IOModel output = detector.detectOutput();
				TraceNode expectedOutputNode = buggyTrace.getTraceNode(7);
				VarValue expectedVarVal = expectedOutputNode.getWrittenVariables().get(1);
				assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
				assertEquals(expectedVarVal, output.getVarVal());
			}
		}
	}

	private void appendMissingInfoToExecutionList(List<TraceNode> executionList, String path) {
		setJavaPathToBreakPoints(executionList, path);
		setMissingReadVariables(executionList);
	}

	private void setJavaPathToBreakPoints(List<TraceNode> executionList, String path) {
		for (int i = 0; i < executionList.size(); i++) {
			TraceNode node = executionList.get(i);
			BreakPoint breakPoint = node.getBreakPoint();
			String className = breakPoint.getClassCanonicalName();
			if (className.contains("$")) {
				className = className.substring(0, className.indexOf("$"));
			}
			String classPath = className.replace(".", File.separator) + ".java";
			String testPath = String.join(File.separator, path, TEST_DIR, classPath);
			if (new File(testPath).exists()) {
				breakPoint.setFullJavaFilePath(testPath);
			} else {
				breakPoint.setFullJavaFilePath(String.join(File.separator, path, SRC_DIR, classPath));
			}
		}
	}

	private void setMissingReadVariables(List<TraceNode> executionList) {
		for (TraceNode node : executionList) {
			if (!node.getInvocationChildren().isEmpty() && node.getReadVariables().isEmpty()) {
				// check AST completeness
				CompilationUnit cu;
				try {
					cu = getCompilationUnit(Paths.get(node.getBreakPoint().getFullJavaFilePath()));
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				MinimumASTNodeFinder finder = new MinimumASTNodeFinder(node.getLineNumber(), cu);
				cu.accept(finder);
				ASTNode astNode = finder.getMinimumNode();

				if (astNode != null) {
					int start = cu.getLineNumber(astNode.getStartPosition());
					int end = cu.getLineNumber(astNode.getStartPosition() + astNode.getLength());

					TraceNode stepOverPrev = node.getStepOverPrevious();
					while (stepOverPrev != null && start <= stepOverPrev.getLineNumber()
							&& stepOverPrev.getLineNumber() <= end) {
						List<VarValue> readVars = stepOverPrev.getReadVariables();
						for (VarValue readVar : readVars) {
							if (!node.getReadVariables().contains(readVar)) {
								node.getReadVariables().add(readVar);
							}
						}
						stepOverPrev = stepOverPrev.getStepOverPrevious();
					}
				}

			}
		}
	}

	private CompilationUnit getCompilationUnit(Path path) throws IOException {
		String pathStr = path.toString();
		if (pathToCompilationUnitMap.containsKey(pathStr))
			return pathToCompilationUnitMap.get(pathStr);
		String fileContent = Files.readString(path);
		CompilationUnit cu = parseCompilationUnit(fileContent);
		pathToCompilationUnitMap.put(pathStr, cu);
		return cu;

	}

	private CompilationUnit parseCompilationUnit(String fileContent) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // handles JDK 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
		parser.setSource(fileContent.toCharArray());
		parser.setResolveBindings(true);
		// In order to parse 1.6 code, some compiler options need to be set to 1.6
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);

		return (CompilationUnit) parser.createAST(null);
	}
}

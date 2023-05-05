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
import java.util.function.Predicate;

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
import microbat.util.JavaUtil;
import microbat.util.MinimumASTNodeFinder;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.handler.PlayRegressionLocalizationHandler;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: Currently we are using traces from math_70 data set mutations. If the instrumentator & data set is updated, it may be impossible to reproduce the trace files with the exact same mutations (all tests will require updates).
// Thus, the projects used to create the traces should be stored in the test-files directory. However, math_70 is very large.
// We should create a small project that allows us to regenerate the traces used in the tests (and modify the tests to use them instead).
class IODetectorIT2 {

	private static final Path traceDir = Paths.get("test-dir", "files", "iodetection");

	private IODetector constructIODetector(Trace buggyTrace, Trace workingTrace, PairList pairList) {
		return new IODetector2(buggyTrace, workingTrace, TEST_DIR, pairList);
	}
	
	private static final String BUG_DIR_FORMAT = "E:\\david\\Mutation_Dataset\\math_70\\%d\\bug";
	private static final String FIX_DIR = "E:\\david\\Mutation_Dataset\\math_70\\fix";
	private static final String SRC_DIR = "src\\main\\java";
	private static final String TEST_DIR = "src\\test\\java";
	
	private Map<String, CompilationUnit> pathToCompilationUnitMap;
	
	@BeforeEach
	void setUp() {
		 pathToCompilationUnitMap = new HashMap<>();
	}
	

	// math_70 bug ID 1
	@Test
	void detect_SingleAssertionAndAllInputsInTest_ObtainsIOCorrectly() {
		Trace buggyTrace = RunningInfo
				.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		Trace workingTrace = RunningInfo
				.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
				.getMainTrace();
		IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 1));
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
	
	private PairList createPairList(Trace buggyTrace, Trace workingTrace, int bugId) {
		// 4 arguments: relative src folder path, relative test folder path, working
		// project root, buggy project root
		// E.g. "src\\main\\java", "src\\test\\java",
		// "E:\\david\\Mutation_Dataset\\math_70\\fix",
		// "E:\\david\\Mutation_Dataset\\math_70\\1\\bug"
		String bugPath = String.format(BUG_DIR_FORMAT, bugId);
		DiffMatcher diffMatcher = new DiffMatcher(SRC_DIR, TEST_DIR, FIX_DIR, bugPath);
		diffMatcher.matchCode();
		ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
		PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, workingTrace, diffMatcher);
		return pairList;
		// Run the left-most orange "Play the process of Regression Localization" button
		// in eclipse application
	}

	@Nested
	class ObtainingInputs {
		// math_70 bug ID 1
		@Test
		void detectInputVarValsFromOutput_AnonymousInputs_ObtainsInputs() {
			Trace buggyTrace = RunningInfo
					.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 1);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 1);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 1));
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
			Trace buggyTrace = RunningInfo.readFromFile(
					traceDir.resolve("buggy-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(
					traceDir.resolve("working-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 2);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 2);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 2));
			TraceNode outputNode = buggyTrace.getTraceNode(1537);
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
			// x at line 86, Math.cosh, sinh, tanh, log, ceil
//			assertEquals(expectedInputs, new HashSet<>(inputs));
			// Current inputs:
			// CBRT, COSH, EXPM1, TANH, ABS, this, x, conditional result of loop, SQRT, SINH
			// Does not have log, ceil (both used "postCompose", e.g. log.postCompose, while others used "of"), and has additional cbrt.
			// Sometimes, the VarValues' children can form links as well.
			// After fixing bug: sqrt, log, tanh, sinh, abs, ceil, expm1, cosh, cbrt
		}

		// math_70 bug ID 3
		@Test
		void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs1() {
			Trace buggyTrace = RunningInfo.readFromFile(traceDir.resolve("buggy-math_70-3-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(traceDir.resolve("working-math_70-3-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 3);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 3);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 3));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
//			assertEquals(expectedInputs, new HashSet<>(inputs));
			// expected ComposableFunction.COS, 3, x
			// After modifying to use getInvocationMethodOrControlDominator:
			// We have COS, 0.1/x at line 357, 3/a, SIN, this at line 23, conditional result in for loop, 5/scaleFactor in line 398
			// - Why did we get sin? Anonymous class data domination is not accurate. It will point to the previous same anonymous class initialization 
			// node 45, this$0 -> node 38 (another anonymous ComposableFunction init)
		}

		// math_70 bug ID 5
		@Test
		void detectInputVarValsFromOutput_ArrayInputs_ObtainsInputs() {
			Trace buggyTrace = RunningInfo.readFromFile(traceDir.resolve("buggy-ArrayInput-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(traceDir.resolve("working-ArrayInput-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 5);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 5);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 5));
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue output = outputNode.getReadVariables().get(0);
			List<VarValue> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
			Set<VarValue> expectedInputs = new HashSet<>();
//			assertEquals(expectedInputs, new HashSet<>(inputs));
			// Expected .2, .2, .5, a, b, c
			// Current obtained inputs:
			// We now have .2, .2, .5, a, b, c, drk, Double array.
			// Questions:
			// 1. Why do we have Double array, but not String array?
			// Ans: String array is passed into Arrays.asList, which is excluded from instrumentation.
			// 2. Why do we have drk & its original input? (Should only be original input)
			// Ans: When initialization for drk is done, it is only written to drk. The constructor return value is not read.
		}
	}

	@Nested
	class ObtainingOutputs {
		// math_70 bug ID 1
		@Test
		void detectOutput_LastNodeAssertionSingleReadVar_ObtainsOutput() {
			Trace buggyTrace = RunningInfo
					.readFromFile(traceDir.resolve("buggy-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo
					.readFromFile(traceDir.resolve("working-SingleAssertionAndAllInputsInTest-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 1);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 1);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 1));
			IOModel output = detector.detectOutput();
			assertEquals(3, output.getNode().getOrder());
			TraceNode outputNode = buggyTrace.getLatestNode();
			outputNode.getReadVariables().get(0);
			VarValue expectedVarVal = outputNode.getReadVariables().get(0);
			assertEquals(expectedVarVal, output.getVarVal());
		}

		// math_70 bug ID 2
		@Test
		void detectOutput_MultiLineAssertionWithEpsilon_ObtainsOutput() {
			Trace buggyTrace = RunningInfo.readFromFile(
					traceDir.resolve("buggy-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(
					traceDir.resolve("working-MultipleAssertionsMultiLineAssertionWithEpsilon-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 2);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 2);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 2));
			IOModel output = detector.detectOutput();
			assertEquals(1537, output.getNode().getOrder());
			TraceNode expectedOutputNode = buggyTrace.getTraceNode(1537);
			expectedOutputNode.getReadVariables().get(0);
			VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
			assertEquals(expectedVarVal, output.getVarVal());
		}

		// math_70 bug ID 3
		@Test
		void detectOutput_LastNodeAssertion_ObtainsOutput() {
			Trace buggyTrace = RunningInfo.readFromFile(traceDir.resolve("buggy-math_70-3-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(traceDir.resolve("working-math_70-3-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 3);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 3);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 3));
			IOModel output = detector.detectOutput();
			TraceNode expectedOutputNode = buggyTrace.getLatestNode();
			expectedOutputNode.getReadVariables().get(0);
			VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
			assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
			assertEquals(expectedVarVal, output.getVarVal());
		}

		// math_70 bug ID 5
		@Test
		void detectOutput_ArrayInputs_ObtainsOutput() {
			Trace buggyTrace = RunningInfo.readFromFile(traceDir.resolve("buggy-ArrayInput-trace.exec").toFile())
					.getMainTrace();
			Trace workingTrace = RunningInfo.readFromFile(traceDir.resolve("working-ArrayInput-trace.exec").toFile())
					.getMainTrace();
			appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), false, 5);
			appendMissingInfoToExecutionList(workingTrace.getExecutionList(), true, 5);
			IODetector detector = constructIODetector(buggyTrace, workingTrace, createPairList(buggyTrace, workingTrace, 5));
			IOModel output = detector.detectOutput();
			TraceNode expectedOutputNode = buggyTrace.getLatestNode();
			expectedOutputNode.getReadVariables().get(0);
			VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
			assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
			assertEquals(expectedVarVal, output.getVarVal());
		}
	}

	private void appendMissingInfoToExecutionList(List<TraceNode> executionList, boolean isWorking, int id) {
		setJavaPathToBreakPoints(executionList, isWorking, id);
		setMissingReadVariables(executionList);
	}
	
	private void setJavaPathToBreakPoints(List<TraceNode> executionList,  boolean isWorking, int id) {
		for (int i = 0; i < executionList.size(); i++) {
			TraceNode node = executionList.get(i);
			BreakPoint breakPoint = node.getBreakPoint();
			String path = "";
			if (isWorking) {
				path = FIX_DIR;
			} else {
				path = String.format(BUG_DIR_FORMAT, id);
			}
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
		for(TraceNode node: executionList){
			if(!node.getInvocationChildren().isEmpty() && 
					node.getReadVariables().isEmpty()) {
				//check AST completeness
				CompilationUnit cu;
				try {
					cu = getCompilationUnit(Paths.get(node.getBreakPoint().getFullJavaFilePath()));
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				MinimumASTNodeFinder finder = new MinimumASTNodeFinder(
						node.getLineNumber(), cu);
				cu.accept(finder);
				ASTNode astNode = finder.getMinimumNode();
				
				if(astNode!=null) {
					int start = cu.getLineNumber(astNode.getStartPosition());
					int end = cu.getLineNumber(astNode.getStartPosition()+astNode.getLength());
					
					TraceNode stepOverPrev = node.getStepOverPrevious();
					while(stepOverPrev!=null && 
							start<=stepOverPrev.getLineNumber() &&
							stepOverPrev.getLineNumber()<=end) {
						List<VarValue> readVars = stepOverPrev.getReadVariables();
						for(VarValue readVar: readVars) {
							if(!node.getReadVariables().contains(readVar)) {
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
		if (pathToCompilationUnitMap.containsKey(pathStr)) return pathToCompilationUnitMap.get(pathStr);
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

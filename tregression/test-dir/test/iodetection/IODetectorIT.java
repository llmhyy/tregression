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

import iodetection.IODetector.NodeVarValPair;
import iodetection.IODetector.InputsAndOutput;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;

import microbat.instrumentation.output.RunningInfo;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.util.MinimumASTNodeFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * To create new tests: 1. Add another project under
 * test-dir/files/io-detection/projects 2. Open Tregression, and "Run Separate
 * Versions" on the new project. 3. Identify the trace file locations (it is
 * printed inside the instrumentation cmd in the console) 4. Copy paste them
 * into test-dir/files/io-detection directory. 5. Rename the traces into
 * buggy-"description"-trace.exec & working-"description"-trace.exec,
 * "description" is up to you. 6. Initialise objects necessary in the test using
 * constructTestObjects("description", projectID). Refer to any of the tests for
 * an example.
 * 
 * @author Chenghin
 *
 */
class IODetectorIT {

    private static final Path TEST_FILES_DIR = Paths.get("test-dir", "files", "iodetection");
    private static final String SAMPLE_PROJECT_FORMAT = "projects\\%d";
    private static final String SRC_DIR = "src\\main\\java";
    private static final String TEST_DIR = "src\\test\\java";
    private static final String BUGGY_TRACE_FMT = "buggy-%s-trace.exec";
    private static final String WORKING_TRACE_FMT = "working-%s-trace.exec";

    private Map<String, CompilationUnit> pathToCompilationUnitMap;

    @BeforeEach
    void setUp() {
        pathToCompilationUnitMap = new HashMap<>();
    }

    @Test
    void detect_InputNotReadOnlyWritten_ObtainsIO() {
        IODetectorTestObjects testObjects = constructTestObjects("input-written-only", 5);
        IODetector detector = testObjects.getIoDetector();
        Trace buggyTrace = testObjects.getBuggyTrace();
        InputsAndOutput result = detector.detect().get();
        TraceNode expectedOutputNode = buggyTrace.getLatestNode();
        expectedOutputNode.getReadVariables().get(0);
        VarValue expectedOutput = expectedOutputNode.getReadVariables().get(0);
        assertEquals(expectedOutputNode, result.getOutput().getNode());
        assertEquals(expectedOutput, result.getOutput().getVarVal());

        Set<NodeVarValPair> expectedInputs = new HashSet<>();
        expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
        expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
        expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
        expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(6), false));
        assertEquals(expectedInputs, new HashSet<>(result.getInputs()));
    }

    @Nested
    class ObtainingInputs {

        @Test
        void detectInputVarValsFromOutput_InputNotReadOnlyWritten_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("input-written-only", 5);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            outputNode.getReadVariables().get(0);
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            TraceNode node1 = buggyTrace.getTraceNode(1);
            node1.getReadVariables().forEach(varVal -> expectedInputs.add(new NodeVarValPair(node1, varVal)));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(6), false));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_ArrayInputs_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("array-input", 6);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            outputNode.getReadVariables().get(0);
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(2), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(6), false));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_WrongIntermediateValue_DoesNotIncludeWrongValueAsInput() {
            IODetectorTestObjects testObjects = constructTestObjects("wrong-intermediate-value", 7);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            outputNode.getReadVariables().get(0);
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            TraceNode node8 = buggyTrace.getTraceNode(8);
            expectedInputs.add(new NodeVarValPair(node8, node8.getWrittenVariables().get(0)));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_InputFromAnotherTestClass_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("input-from-another-class", 8);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            outputNode.getReadVariables().get(0);
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(8), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(9), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(11), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(12), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(13), false));
            TraceNode node14 = buggyTrace.getTraceNode(14);
            expectedInputs.add(new NodeVarValPair(node14, node14.getReadVariables().get(1))); // String value inside invoked method. Technically not supposed to an input, but it's fine.
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_OutputHasNoDataDependencies_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("null-ptr-exception", 2);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_InputIsNotInTestFile_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("input-not-in-test-file", 11);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getLatestNode();
            VarValue output = outputNode.getReadVariables().get(0);
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }

        @Test
        void detectInputVarValsFromOutput_ExceptionThrownResultingInWrongBranch_ObtainsInputs() {
            IODetectorTestObjects testObjects = constructTestObjects("regular-exception", 1);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            TraceNode outputNode = buggyTrace.getTraceNode(7);
            VarValue output = null;
            List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
            Set<NodeVarValPair> expectedInputs = new HashSet<>();
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(1), true));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(4), false));
            expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(buggyTrace.getTraceNode(5), false));
            assertEquals(expectedInputs, new HashSet<>(inputs));
        }
    }

    @Nested
    class ObtainingOutputs {

        @Test
        void detectOutput_WrongIntermediateValue_OnlyTakesWrongValueFromLatestNode() {
            IODetectorTestObjects testObjects = constructTestObjects("wrong-intermediate-value", 7);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            NodeVarValPair output = detector.detectOutput().get();
            TraceNode expectedOutputNode = buggyTrace.getLatestNode();
            VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
            assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
            assertEquals(expectedVarVal, output.getVarVal());
        }

        @Test
        void detectOutput_WrongArrayContents_TakesTheAccessedContent() {
            IODetectorTestObjects testObjects = constructTestObjects("array-contents-diff", 10);
            IODetector detector = testObjects.getIoDetector();
            Trace buggyTrace = testObjects.getBuggyTrace();
            NodeVarValPair output = detector.detectOutput().get();
            TraceNode expectedOutputNode = buggyTrace.getLatestNode();
            VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
            assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
            assertEquals(expectedVarVal, output.getVarVal());
        }

        @Nested
        class ObtainingOutputsAfterException {
            @Test
            void detectOutput_ExceptionThrown_ObtainsControlDominatorAsOutput() {
                IODetectorTestObjects testObjects = constructTestObjects("regular-exception", 1);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getTraceNode(7);
                VarValue expectedVarVal = null;
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            @Test
            void detectOutput_NullPointerExceptionThrown_ObtainsNullAsOutput() {
                IODetectorTestObjects testObjects = constructTestObjects("null-ptr-exception", 2);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            @Test
            void detectOutput_DivByZeroExceptionThrown_ObtainsZeroAsOutput() {
                IODetectorTestObjects testObjects = constructTestObjects("div-by-zero-exception", 3);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            @Test
            void detectOutput_OutOfBoundsExceptionThrown_ObtainsAccessingIndexAsOutput() {
                IODetectorTestObjects testObjects = constructTestObjects("out-of-bounds-exception", 4);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getTraceNode(15);
                VarValue expectedVarVal = null;
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            @Test
            void detectOutput_OutOfBoundsExceptionThrownDueToDataStructureSize_ObtainsDataStructureSizeAsOutput() {
                IODetectorTestObjects testObjects = constructTestObjects("out-of-bounds-list-size", 9);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getTraceNode(10);
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0).getChildren().get(1);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }
        }
    }

    /**
     * Requires unzipping the math_70 dataset on server 250, which may interfere
     * with anyone else using it.
     * 
     * @author Chenghin
     *
     */
    @Disabled
    @Nested
    class Math70 {
        private static final String DATA_SET_BUG_DIR_FORMAT = "E:\\david\\Mutation_Dataset\\math_70\\%d\\bug";
        private static final String DATA_SET_FIX_DIR = "E:\\david\\Mutation_Dataset\\math_70\\fix";

        @Nested
        class ObtainingInputs {
            // math_70 bug ID 1
            @Test
            void detectInputVarValsFromOutput_AnonymousInputs_ObtainsInputs() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("SingleAssertionAndAllInputsInTest", 1);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                TraceNode outputNode = buggyTrace.getLatestNode();
                outputNode.getReadVariables().get(0);
                VarValue output = outputNode.getReadVariables().get(0);
                List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
                Set<NodeVarValPair> expectedInputs = new HashSet<>();
                TraceNode node1 = buggyTrace.getTraceNode(1);
                expectedInputs.addAll(createNodeVarValPairsFromNodeAndVarVals(node1, false));
                expectedInputs.add(new NodeVarValPair(node1, node1.getReadVariables().get(0)));
                assertEquals(expectedInputs, new HashSet<>(inputs));
            }

            // math_70 bug ID 2
            // x at line 86, Math.cosh, sinh, tanh, log, ceil
            // Current inputs:
            // CBRT, COSH, EXPM1, TANH, ABS, this, x, conditional result of loop, SQRT, SINH
            // Does not have log, ceil (both used "postCompose", e.g. log.postCompose, while
            // others used "of"), and has additional cbrt.
            // Sometimes, the VarValues' children can form links as well.
            // After fixing bug: sqrt, log, tanh, sinh, abs, ceil, expm1, cosh, cbrt, 0.1,
            // conditional result
            @Test
            void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70(
                        "MultipleAssertionsMultiLineAssertionWithEpsilon", 2);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                TraceNode outputNode = buggyTrace.getTraceNode(1537);
                outputNode.getReadVariables().get(0);
                VarValue output = outputNode.getReadVariables().get(0);
                List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
                Set<NodeVarValPair> expectedInputs = new HashSet<>();
            }

            // math_70 bug ID 3
            // expected ComposableFunction.COS, 3, x
            // After modifying to use getInvocationMethodOrControlDominator:
            // We have COS, 0.1/x at line 357, 3/a, SIN, this at line 23, conditional result
            // in for loop, 5/scaleFactor in line 398
            // - Why did we get sin? Anonymous class data domination is not accurate. It
            // will point to the previous same anonymous class initialization
            // node 45, this$0 -> node 38 (another anonymous ComposableFunction init)
            @Test
            void detectInputVarValsFromOutput_MultiplePossibleInputs_ObtainsOnlyImportantInputs1() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("math_70-3", 3);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                TraceNode outputNode = buggyTrace.getLatestNode();
                outputNode.getReadVariables().get(0);
                VarValue output = outputNode.getReadVariables().get(0);
                List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
                Set<NodeVarValPair> expectedInputs = new HashSet<>();
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
                IODetectorTestObjects testObjects = constructTestObjectsMath70("ArrayInput", 5);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                TraceNode outputNode = buggyTrace.getLatestNode();
                outputNode.getReadVariables().get(0);
                VarValue output = outputNode.getReadVariables().get(0);
                List<NodeVarValPair> inputs = detector.detectInputVarValsFromOutput(outputNode, output);
                Set<NodeVarValPair> expectedInputs = new HashSet<>();
            }
        }

        @Nested
        class ObtainingOutput {
            // math_70 bug ID 1
            @Test
            void detectOutput_LastNodeAssertionSingleReadVar_ObtainsOutput() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("SingleAssertionAndAllInputsInTest", 1);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                assertEquals(3, output.getNode().getOrder());
                TraceNode outputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = outputNode.getReadVariables().get(0);
                assertEquals(expectedVarVal, output.getVarVal());
            }

            // math_70 bug ID 2
            @Test
            void detectOutput_MultiLineAssertionWithEpsilon_ObtainsOutput() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70(
                        "MultipleAssertionsMultiLineAssertionWithEpsilon", 2);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                assertEquals(1537, output.getNode().getOrder());
                TraceNode expectedOutputNode = buggyTrace.getTraceNode(1537);
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedVarVal, output.getVarVal());
            }

            // math_70 bug ID 3
            @Test
            void detectOutput_LastNodeAssertion_ObtainsOutput() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("math_70-3", 3);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            // math_70 bug ID 5
            @Test
            void detectOutput_ArrayInputs_ObtainsOutput() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("ArrayInput", 5);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }

            // math_70 bug ID 8
            @Test
            void detectOutput_Test_ObtainsOutput() {
                IODetectorTestObjects testObjects = constructTestObjectsMath70("math_70-8", 8);
                IODetector detector = testObjects.getIoDetector();
                Trace buggyTrace = testObjects.getBuggyTrace();
                NodeVarValPair output = detector.detectOutput().get();
                TraceNode expectedOutputNode = buggyTrace.getLatestNode();
                VarValue expectedVarVal = expectedOutputNode.getReadVariables().get(0);
                assertEquals(expectedOutputNode.getOrder(), output.getNode().getOrder());
                assertEquals(expectedVarVal, output.getVarVal());
            }
        }

        private IODetectorTestObjects constructTestObjectsMath70(String traceName, int projectID) {
            String buggyPath = String.format(DATA_SET_BUG_DIR_FORMAT, projectID);
            return constructTestObjects(traceName, buggyPath, DATA_SET_FIX_DIR);
        }

    }

    private static class IODetectorTestObjects {
        private final Trace buggyTrace;
        private final IODetector ioDetector;

        public IODetectorTestObjects(Trace buggyTrace, IODetector ioDetector) {
            super();
            this.buggyTrace = buggyTrace;
            this.ioDetector = ioDetector;
        }

        public Trace getBuggyTrace() {
            return buggyTrace;
        }

        public IODetector getIoDetector() {
            return ioDetector;
        }

    }

    private IODetectorTestObjects constructTestObjects(String traceName, int projectID) {
        String projectRoot = TEST_FILES_DIR.resolve(String.format(SAMPLE_PROJECT_FORMAT, projectID)).toAbsolutePath()
                .toString();
        String buggyPath = projectRoot + "\\bug";
        String workingPath = projectRoot + "\\fix";
        return constructTestObjects(traceName, buggyPath, workingPath);
    }

    private IODetectorTestObjects constructTestObjects(String traceName, String buggyPath, String workingPath) {
        Trace buggyTrace = RunningInfo
                .readFromFile(TEST_FILES_DIR.resolve(String.format(BUGGY_TRACE_FMT, traceName)).toFile())
                .getMainTrace();
        Trace workingTrace = RunningInfo
                .readFromFile(TEST_FILES_DIR.resolve(String.format(WORKING_TRACE_FMT, traceName)).toFile())
                .getMainTrace();
        appendMissingInfoToExecutionList(buggyTrace.getExecutionList(), buggyPath);
        appendMissingInfoToExecutionList(workingTrace.getExecutionList(), workingPath);
        return new IODetectorTestObjects(buggyTrace, constructIODetector(buggyTrace, workingTrace,
                createPairList(buggyTrace, workingTrace, buggyPath, workingPath)));
    }

    private IODetector constructIODetector(Trace buggyTrace, Trace workingTrace, PairList pairList) {
        return new IODetector(buggyTrace, TEST_DIR, pairList);
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

    private PairList createPairList(Trace buggyTrace, Trace workingTrace, String buggyPath, String workingPath) {
        DiffMatcher diffMatcher = new DiffMatcher(SRC_DIR, TEST_DIR, buggyPath, workingPath);
        diffMatcher.matchCode();
        ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
        PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, workingTrace, diffMatcher);
        return pairList;
    }

    private List<NodeVarValPair> createNodeVarValPairsFromNodeAndVarVals(TraceNode node, boolean isRead) {
        List<NodeVarValPair> result = new ArrayList<>();
        List<VarValue> varValues = isRead ? node.getReadVariables() : node.getWrittenVariables();
        for (VarValue varVal : varValues) {
            result.add(new NodeVarValPair(node, varVal));
        }
        return result;
    }
}

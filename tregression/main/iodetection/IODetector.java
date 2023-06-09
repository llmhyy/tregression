package iodetection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

/**
 * Find the input and the output of the test case Okay to include many inputs
 * (eg. setting all the variable before the test case to inputs, but be careful
 * on this) Since we have the correct trace for reference, you may know which
 * variable is correct or not.
 * 
 * To make reference from the correct trace, you may take some reference from
 * the following classes: tregression.handler.SeparateVersionHandler,
 * tregression.StepChangeType, tregression.separatesnapshots.DiffMatcher
 * 
 * Some code example are available at
 * tregression.autofeedbackevaluation.AutoDebugEvaluator#getRefFeedbacks
 */
public class IODetector {

    private final Trace buggyTrace;
    private final String testDir;
    private final PairList pairList;

    public IODetector(Trace buggyTrace, String testDir, PairList pairList) {
        this.buggyTrace = buggyTrace;
        this.testDir = testDir;
        this.pairList = pairList;
    }

    /**
     * Runs IO detection.
     * 
     * @return
     */
    public Optional<InputsAndOutput> detect() {
        Optional<NodeVarValPair> outputNodeAndVarValOpt = detectOutput();
        if (outputNodeAndVarValOpt.isEmpty()) {
            return Optional.empty();
        }
        NodeVarValPair outputNodeAndVarVal = outputNodeAndVarValOpt.get();
        VarValue output = outputNodeAndVarVal.getVarVal();
        List<NodeVarValPair> inputs = detectInputVarValsFromOutput(outputNodeAndVarVal.getNode(), output);
        return Optional.of(new InputsAndOutput(inputs, outputNodeAndVarVal));
    }

    /**
     * Iterates from the last node to first node, and checks for wrong variable
     * value or wrong branch. Once it is found, it is returned.
     * 
     * @return
     */
    public Optional<NodeVarValPair> detectOutput() {
        TraceNode node;
        int lastNodeOrder = buggyTrace.getLatestNode().getOrder();
        for (int i = lastNodeOrder; i >= 1; i--) {
            node = buggyTrace.getTraceNode(i);
            TraceNodePair pair = pairList.findByBeforeNode(node);
            // Check for wrong branch (no corresponding node in correct trace)
            if (pair == null) {
                return Optional.of(new NodeVarValPair(node, null));
            }
            Optional<NodeVarValPair> wrongVariableOptional = getWrongVariableInNode(node);
            if (wrongVariableOptional.isEmpty()) {
                continue;
            }
            return wrongVariableOptional;
        }
        return Optional.empty();
    }

    /**
     * Given an output, it uses data, control and method invocation parent
     * dependencies to identify inputs.
     * 
     * @param outputNode
     * @param output     The VarValue that had wrong value, or null if the output is
     *                   a TraceNode that was wrongly executed.
     * @return
     */
    List<NodeVarValPair> detectInputVarValsFromOutput(TraceNode outputNode, VarValue output) {
        Set<NodeVarValPair> result = new HashSet<>();
        Set<VarValue> inputs = new HashSet<>();
        detectInputVarValsFromOutput(outputNode, inputs, result, new HashSet<>());
        assert !inputs.contains(output);
        return new ArrayList<>(result);
    }

    // For each node, add the following as inputs (Variables in test file only)
    // 1. Written variables.
    // 2. read variables without data dominators
    //
    // Recurse on the following:
    // 1. Data dominator on each read variable
    // 2. Control/Invocation Parent
    void detectInputVarValsFromOutput(TraceNode outputNode, Set<VarValue> inputs, Set<NodeVarValPair> inputsWithNodes,
            Set<Integer> visited) {
        boolean isFirstNode = visited.isEmpty();
        int key = formVisitedKey(outputNode);
        if (visited.contains(key)) {
            return;
        }
        visited.add(key);
        boolean isTestFile = isInTestDir(outputNode.getBreakPoint().getFullJavaFilePath());
        if (isTestFile && !isFirstNode) {
            // If the node is in a test file and is not the node with incorrect variable,
            // check its written variables for inputs.
            List<VarValue> newInputs = new ArrayList<>(outputNode.getWrittenVariables());
            Optional<NodeVarValPair> wrongVariable = getWrongVariableInNode(outputNode);
            if (wrongVariable.isPresent()) {
                VarValue incorrectValue = wrongVariable.get().getVarVal();
                newInputs.remove(incorrectValue);
            }
            newInputs.forEach(newInput -> {
                if (!inputs.contains(newInput)) {
                    inputsWithNodes.add(new NodeVarValPair(outputNode, newInput));
                    inputs.add(newInput);
                }
            });
        }
        boolean shouldCheckForStringInputs = shouldCheckForStringInputs(outputNode);
        for (VarValue readVarVal : outputNode.getReadVariables()) {
            TraceNode dataDominator = buggyTrace.findDataDependency(outputNode, readVarVal);
            if ((dataDominator == null && isTestFile && !inputs.contains(readVarVal)) || 
                    (shouldCheckForStringInputs && 
                    isStringInput(outputNode, readVarVal))) {
                inputs.add(readVarVal);
                inputsWithNodes.add(new NodeVarValPair(outputNode, readVarVal));
            }
            if (dataDominator != null) {
                detectInputVarValsFromOutput(dataDominator, inputs, inputsWithNodes, visited);
            }
        }
        TraceNode controlDominator = outputNode.getInvocationMethodOrDominator();
        if (controlDominator != null) {
            detectInputVarValsFromOutput(controlDominator, inputs, inputsWithNodes, visited);
        }
    }

    private int formVisitedKey(TraceNode node) {
        return node.getOrder();
    }

    private boolean isInTestDir(String filePath) {
        return filePath.contains(testDir);
    }

    /**
     * Use PairList code to get wrong VarValues in the current node. Do not add
     * non-null ReferenceValue, as they are always correct.
     * 
     * @param node
     * @return
     */
    private Optional<NodeVarValPair> getWrongVariableInNode(TraceNode node) {
        TraceNodePair pair = pairList.findByBeforeNode(node);
        if (pair == null) {
            return Optional.empty();
        }
        List<VarValue> result = pair.findSingleWrongWrittenVarID(buggyTrace, pairList);
        Optional<NodeVarValPair> wrongWrittenVar = getWrongVarFromVarList(result, node);
        if (wrongWrittenVar.isPresent()) {
            return wrongWrittenVar;
        }
        result = pair.findSingleWrongReadVar(buggyTrace, pairList);
        return getWrongVarFromVarList(result, node);
    }

    /**
     * Check the "incorrect" var values in the list, and return it if it is null or
     * primitive values.
     * 
     * @param varValues
     * @param node
     * @return
     */
    private Optional<NodeVarValPair> getWrongVarFromVarList(List<VarValue> varValues, TraceNode node) {
        for (VarValue varValue : varValues) {
            if (varValue instanceof ReferenceValue) {
                long addr = ((ReferenceValue) varValue).getUniqueID();
                if (addr != -1) { // If the "incorrect" ref var value is not null, don't return it.
                    continue;
                }
            }
            return Optional.of(new NodeVarValPair(node, varValue));
        }
        return Optional.empty();
    }

    /**
     * Constants (strings) are not recorded in the TraceNode when written, which
     * leads to missed inputs. It is recorded when it is read after a method is
     * invoked and the string is passed as an argument. The method looks at the
     * first inner node of a method invocation, and checks for any read strings with
     * correct values, and returns them as possible inputs.
     *
     */
    private boolean shouldCheckForStringInputs(TraceNode node) {
        TraceNode invocationParent = node.getInvocationParent();
        if (invocationParent == null) {
            return false;
        }
        return isInTestDir(invocationParent.getBreakPoint().getFullJavaFilePath());
    }

    private boolean isStringInput(TraceNode node, VarValue readVarValue) {
        if (!"String".equals(readVarValue.getType())) {
            return false;
        }
        TraceNodePair pair = pairList.findByBeforeNode(node);
        if (pair == null) {
            return true;
        }

        List<VarValue> wrongReadVars = pair.findSingleWrongReadVar(buggyTrace, pairList);
        for (VarValue wrongReadVar : wrongReadVars) {
            if (readVarValue.equals(wrongReadVar)) {
                return false;
            }
        }
        return true;
    }

    public static class NodeVarValPair {
        private final TraceNode node;
        private final VarValue varVal;

        public NodeVarValPair(TraceNode node, VarValue varVal) {
            this.node = node;
            this.varVal = varVal;
        }

        public TraceNode getNode() {
            return node;
        }

        public VarValue getVarVal() {
            return varVal;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, varVal);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NodeVarValPair other = (NodeVarValPair) obj;
            return Objects.equals(node, other.node) && Objects.equals(varVal, other.varVal);
        }

        @Override
        public String toString() {
            return "NodeVarValPair [node=" + node + ", varVal=" + varVal + "]";
        }
    }

    public static class InputsAndOutput {
        private final List<NodeVarValPair> inputs;
        private final NodeVarValPair output;

        public InputsAndOutput(List<NodeVarValPair> inputs, NodeVarValPair output) {
            super();
            this.inputs = inputs;
            this.output = output;
        }

        public List<NodeVarValPair> getInputs() {
            return inputs;
        }

        public NodeVarValPair getOutput() {
            return output;
        }
    }
}

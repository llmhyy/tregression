package iodetection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
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
//        List<NodeVarValPair> inputs = detectInputVarValsFromOutput(outputNodeAndVarVal.getNode(), output);
        List<NodeVarValPair> inputs = detectInputs(outputNodeAndVarVal.getVarContainerNodeOrder());
        return Optional.of(new InputsAndOutput(inputs, outputNodeAndVarVal));
    }
    
    /**
     * Parses IO detection results.
     * 
     * @return
     */
    public Optional<NodeVarValPair> detect(List<String[]> output) {
        NodeVarValPair outputNodeAndVarVal = null;
        int outputNodeOrder = Integer.valueOf(output.get(0)[0]);
        int outputVarContainerNodeOrder = Integer.valueOf(output.get(0)[1]);
        String outputVarID = output.get(0)[2];
        outputNodeAndVarVal = searchForNodeVarPair(outputVarID, outputNodeOrder, outputVarContainerNodeOrder);
    	return Optional.of(outputNodeAndVarVal);
    }
    
    /**
     * Parses IO detection results.
     * 
     * @return
     */
    public Optional<InputsAndOutput> detect(List<String[]> inputs, List<String[]> output) {
        List<NodeVarValPair> inputList = new ArrayList<>();
        NodeVarValPair outputNodeAndVarVal = null;
        for (String[] entry : inputs) {
        	int nodeOrder = Integer.valueOf(entry[0]);
        	String varID = entry[1];
        	NodeVarValPair pair = searchForNodeVarPair(varID, nodeOrder);
        	inputList.add(pair);
        }
        int outputNodeOrder = Integer.valueOf(output.get(0)[0]);
        int outputVarContainerNodeOrder = Integer.valueOf(output.get(0)[1]);
        String outputVarID = output.get(0)[2];
        outputNodeAndVarVal = searchForNodeVarPair(outputVarID, outputNodeOrder, outputVarContainerNodeOrder);
    	return Optional.of(new InputsAndOutput(inputList, outputNodeAndVarVal));
    }
    
    /**
     * Iterates from the first node to the node before the output node, adds 
     * correct written variables in the test case.
     * 
     * @return
     */
    public List<NodeVarValPair> detectInputs(int outputVarContainingNode) {
    	List<NodeVarValPair> inputs = new ArrayList<>();
    	int firstNodeOrder = 1;
    	TraceNode node;
    	boolean previousNodeInTestDir = false;
    	for (int i = firstNodeOrder; i < outputVarContainingNode; i++) {
    		node = buggyTrace.getTraceNode(i);
    		boolean isTestFile = isInTestDir(node.getBreakPoint().getFullJavaFilePath());
    		if (isTestFile) {
    			List<VarValue> writtenVariables = node.getWrittenVariables();
    			// remove wrong variables
    			Optional<NodeVarValPair> wrongVariable = getWrongVariableInNode(node);
                if (wrongVariable.isPresent()) {
                    VarValue incorrectValue = wrongVariable.get().getVarVal();
                    writtenVariables.remove(incorrectValue);
                }
                for (VarValue var : writtenVariables) {
    				inputs.add(new NodeVarValPair(node, var));
    			}
                previousNodeInTestDir = true;
    		} else {
    			if (previousNodeInTestDir) {
    				// check for constants (not captured in test file)
    				List<VarValue> readVariables = node.getReadVariables();
        			Optional<NodeVarValPair> wrongVariable = getWrongVariableInNode(node);
                    if (wrongVariable.isPresent()) {
                        VarValue incorrectValue = wrongVariable.get().getVarVal();
                        readVariables.remove(incorrectValue);
                    }
                    for (VarValue var : readVariables) {
        				inputs.add(new NodeVarValPair(node, var));
        			}
    			}
    			previousNodeInTestDir = false;
    		}
    	}
    	return inputs;
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
        TraceNode outputNode = null;
        for (int i = lastNodeOrder; i >= 1; i--) {
            node = buggyTrace.getTraceNode(i);
            TraceNodePair pair = pairList.findByBeforeNode(node);
            // Check for wrong branch (no corresponding node in correct trace)
            if (pair == null && outputNode == null) {
            	outputNode = node;
            	TraceNode controlDominator = null;
            	// Find the first condition that leads to wrong branch
            	while (pair == null) {
            		controlDominator = node.getControlDominator();
            		if (controlDominator == null) {
            			break;
            		}
            		node = controlDominator;
            		i = node.getOrder();
            		pair = pairList.findByBeforeNode(node);
            	}
            	if (controlDominator == null) {
            		continue;
            	}
            	// the variable-containing node is different from the output node
                return Optional.of(new NodeVarValPair(outputNode, node.getConditionResult(), node.getOrder()));
            }
            Optional<NodeVarValPair> wrongVariableOptional = getWrongVariableInNode(node);
            if (wrongVariableOptional.isEmpty()) {
                continue;
            }
            
            if (outputNode == null) {
            	return wrongVariableOptional;
            } else {
            	// the variable-containing node is different from the output node
            	NodeVarValPair wrongVariable = wrongVariableOptional.get();
            	return Optional.of(new NodeVarValPair(outputNode, wrongVariable.getVarVal(), wrongVariable.getVarContainerNodeOrder()));
            }
//            return wrongVariableOptional;
        }
        return Optional.empty();
    }

    /**
     * Search for node and variable.
     * 
     * @return
     */
    public NodeVarValPair searchForNodeVarPair(String varID, int... relevantNodeOrders) {
    	int outputNodeID = relevantNodeOrders[0];
    	TraceNode node = buggyTrace.getTraceNode(outputNodeID);
        // find wrong variable
        VarValue varValue = searchForVar(node, varID);
        if (varValue != null) {
        	return new NodeVarValPair(node, varValue);
        }
        // output node might be different from node containing output value
        if (relevantNodeOrders.length == 2) {
        	int outputVarNodeOrder = relevantNodeOrders[1];
        	TraceNode outputVarNode = buggyTrace.getTraceNode(outputVarNodeOrder);
        	VarValue outputVarVal = searchForVar(outputVarNode, varID);
        	return new NodeVarValPair(node, outputVarVal, outputVarNodeOrder);
        }
        return new NodeVarValPair(node, null);
    }
    
    private VarValue searchForVar(TraceNode node, String varID) {
    	// find wrong variable
        List<VarValue> variables = new ArrayList<>();
        variables.addAll(node.getReadVariables());
        variables.addAll(node.getWrittenVariables());
        for (VarValue var : variables) {
        	if (var.getVarID().equals(varID)) {
        		return var;
        	}
        }
        return null;
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
    // Iterate on the following:
    // 1. Data dominator on each read variable
    // 2. Control/Invocation Parent
    void detectInputVarValsFromOutput(TraceNode outputNode, Set<VarValue> inputs, Set<NodeVarValPair> inputsWithNodes, 
    		Set<Integer> visited) {
    	Stack<TraceNode> stack = new Stack<>();
    	stack.push(outputNode);
    	// dfs
    	while (!stack.isEmpty()) {
    		TraceNode node = stack.pop();
    		int key = formVisitedKey(node);
    		if (visited.contains(key)) {
    			continue;
    		}
    		visited.add(key);
    		boolean isTestFile = isInTestDir(node.getBreakPoint().getFullJavaFilePath());
    		if (isTestFile && !node.equals(outputNode)) {
    			// If the node is in a test file and is not the node with incorrect variable,
                // check its written variables for inputs.
                List<VarValue> newInputs = new ArrayList<>(node.getWrittenVariables());
                Optional<NodeVarValPair> wrongVariable = getWrongVariableInNode(node);
                if (wrongVariable.isPresent()) {
                    VarValue incorrectValue = wrongVariable.get().getVarVal();
                    newInputs.remove(incorrectValue);
                }
                newInputs.forEach(newInput -> {
                    if (!inputs.contains(newInput)) {
                        inputsWithNodes.add(new NodeVarValPair(node, newInput));
                        inputs.add(newInput);
                    }
                });
    		}
    		boolean shouldCheckForStringInputs = shouldCheckForStringInputs(node);
            for (VarValue readVarVal : node.getReadVariables()) {
                TraceNode dataDominator = buggyTrace.findDataDependency(node, readVarVal);
                if ((dataDominator == null && isTestFile && !inputs.contains(readVarVal)) || 
                        (shouldCheckForStringInputs && 
                        isStringInput(node, readVarVal))) {
                    inputs.add(readVarVal);
                    inputsWithNodes.add(new NodeVarValPair(node, readVarVal));
                }
                if (dataDominator != null) {
                    stack.push(dataDominator);
                }
            }
            TraceNode controlDominator = node.getInvocationMethodOrDominator();
            if (controlDominator != null) {
               stack.push(controlDominator);
            }
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
//        if (wrongWrittenVar.isPresent()) {
//            return wrongWrittenVar;
//        }
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
//                    continue;
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
        private final int varContainerNodeOrder;
        
        public NodeVarValPair(TraceNode node, VarValue varVal, int varContainerNodeOrder) {
            this.node = node;
            this.varVal = varVal;
            this.varContainerNodeOrder = varContainerNodeOrder;
        }

        public NodeVarValPair(TraceNode node, VarValue varVal) {
            this.node = node;
            this.varVal = varVal;
            this.varContainerNodeOrder = node.getOrder();
        }

        public TraceNode getNode() {
            return node;
        }

        public VarValue getVarVal() {
            return varVal;
        }
        
        public int getVarContainerNodeOrder() {
        	return varContainerNodeOrder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, varVal, varContainerNodeOrder);
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
            return Objects.equals(node, other.node) && Objects.equals(varVal, other.varVal) 
            		&& Objects.equals(varContainerNodeOrder, other.varContainerNodeOrder);
        }

        @Override
        public String toString() {
            return "NodeVarValPair [node=" + node + ", varVal=" + varVal 
            		+ ", varContainerNodeOrder=" + varContainerNodeOrder + "]";
        }
    }

    public static class InputsAndOutput {
    	public static String INPUTS_KEY = "Inputs";
    	public static String OUTPUT_KEY = "Output";
    	
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
        
        @Override
        public String toString() {
        	StringBuilder stringBuilder = new StringBuilder();
        	stringBuilder.append(INPUTS_KEY + "\n");
        	for (NodeVarValPair pair : this.inputs) {
        		stringBuilder.append(pair.getNode().getOrder());
        		stringBuilder.append(" ");
        		stringBuilder.append(pair.getVarVal().getVarID());
        		stringBuilder.append("\n");
        	}
        	stringBuilder.append(OUTPUT_KEY + "\n");
        	stringBuilder.append(this.output.getNode().getOrder());
        	stringBuilder.append(" ");
        	stringBuilder.append(this.output.getVarContainerNodeOrder());
        	stringBuilder.append(" ");
    		stringBuilder.append(this.output.getVarVal().getVarID());
    		stringBuilder.append("\n");
        	return stringBuilder.toString();
        }
    }
}

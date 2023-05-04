package iodetection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;

public class IODetector {

	private final Trace buggyTrace;
	private final Trace correctTrace;
	private final String testDir;

	private List<VarValue> inputs = new ArrayList<>();
	private VarValue output;

	public IODetector(Trace buggyTrace, Trace correctTrace, String testDir) {
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.testDir = testDir;
	}

	// TODO find the input and the output of the test case
	// Okay to include many inputs (eg. setting all the variable before the test
	// case to inputs, but be careful on this)
	// Since we have the correct trace for reference, you may know which variable is
	// correct or not
	// To make reference from the correct trace, you may take some reference from
	// the following class:

	// tregression.handler.SeparateVersionHandler
	// tregression.StepChangeType
	// tregression.separatesnapshots.DiffMatcher
	// tregression.separatesnapshots.DiffMatcher

	// Some code example are avaiable at
	// tregression.autofeedbackevaluation.AutoDebugEvaluator#getRefFeedbacks

	public List<VarValue> getInputs() {
		return this.inputs;
	}

	public VarValue getOutputs() {
		return this.output;
	}

	public void detect() {
		IOModel outputNodeAndVarVal = detectOutput();
		output = outputNodeAndVarVal.getVarVal();
		inputs = detectInputVarValsFromOutput(outputNodeAndVarVal.getNode(), output);
	}

	public IOModel detectOutput() {
		// TODO: Go backwards from this node.
		TraceNode node;
		int lastNodeOrder = buggyTrace.getLatestNode().getOrder();
		for (int i = lastNodeOrder; i >= 0; i--) {
			node = buggyTrace.getTraceNode(i);
			if (node.getReadVariables().size() == 1) {
				return new IOModel(node, node.getReadVariables().get(0));
			}
		}
		return null;
	}

	public List<VarValue> detectInputVarValsFromOutput(TraceNode outputNode, VarValue output) {
		Set<VarValue> result = new HashSet<>();
		detectInputVarValsFromOutput(outputNode, output, result, new HashSet<TraceNode>());
//		Map<String, VarValue> primitiveInputs = new HashMap<>();
//		Map<String, VarValue> referenceInputs = new HashMap<>();
//		detectInputVarValsFromOutput(outputNode, output, primitiveInputs, referenceInputs, new HashSet<TraceNode>());
//		for (VarValue input : primitiveInputs.values()) {
//			result.add(input);
//		}		
//		for (VarValue input : referenceInputs.values()) {
//			result.add(input);
//		}
		assert !result.contains(output);
		return new ArrayList<>(result);
	}
	
	void detectInputVarValsFromOutput(TraceNode outputNode, VarValue output, Map<String, VarValue> primitiveInputs, Map<String, VarValue> referenceInputs,
			Set<TraceNode> visited) {
		if (visited.contains(outputNode)) {
			return;
		}
		boolean isTestFile = isInTestDir(outputNode.getBreakPoint().getFullJavaFilePath());
		Set<VarValue> readVariables = new HashSet<>(outputNode.getReadVariables());
		if (isTestFile) {
			// TODO: check if reference, then use aliasID. (math_70 bug id 5)
			// Check primitive variables, how to identify if they were read + written in the same node. (math_70 bug id 2)
			Set<VarValue> writtenVariables = new HashSet<>(outputNode.getWrittenVariables());
			writtenVariables.removeAll(readVariables);
			inputs.addAll(writtenVariables);
		}

		TraceNode dataDominator = buggyTrace.findDataDependency(outputNode, output);
		if (dataDominator == null && isTestFile) {
			inputs.add(output);
		}
		if (dataDominator != null) {
			for (VarValue readVarVal : dataDominator.getReadVariables()) {
				detectInputVarValsFromOutput(dataDominator, readVarVal, primitiveInputs, referenceInputs, visited);
			}
		}
		TraceNode controlDominator = outputNode.getControlDominator();
		if (controlDominator != null) {
			for (VarValue readVarVal : controlDominator.getReadVariables()) {
				detectInputVarValsFromOutput(controlDominator, readVarVal, primitiveInputs, referenceInputs, visited);
			}
		}
	}
	
	private String primitiveValueKey(PrimitiveValue primitiveValue) {
		return primitiveValue.getVariable().getSimpleName();
	}
	
	private String referenceValueKey(ReferenceValue referenceValue) {
		return referenceValue.getAliasVarID();
	}
	
	void detectInputVarValsFromOutput(TraceNode outputNode, VarValue output, Set<VarValue> inputs,
			Set<TraceNode> visited) {
		if (visited.contains(outputNode)) {
			return;
		}
		boolean isTestFile = isInTestDir(outputNode.getBreakPoint().getFullJavaFilePath());
		if (isTestFile) {
			// TODO: check if reference, then use aliasID. (math_70 bug id 5)
			// Check primitive variables, how to identify if they were read + written in the same node. (math_70 bug id 2)
//			Set<VarValue> readVariables = new HashSet<>(outputNode.getReadVariables());
			Set<VarValue> writtenVariables = new HashSet<>(outputNode.getWrittenVariables());
			inputs.addAll(writtenVariables);
		}

		TraceNode dataDominator = buggyTrace.findDataDependency(outputNode, output);
		if (dataDominator == null && isTestFile) {
			inputs.add(output);
		}		
		if (dataDominator != null) {
			for (VarValue readVarVal : dataDominator.getReadVariables()) {
				detectInputVarValsFromOutput(dataDominator, readVarVal, inputs, visited);
			}
		}
		TraceNode controlDominator = outputNode.getControlDominator();
		if (controlDominator != null) {
			for (VarValue readVarVal : controlDominator.getReadVariables()) {
				detectInputVarValsFromOutput(controlDominator, readVarVal, inputs, visited);
			}
		}
	}

	boolean isInTestDir(String filePath) {
		return filePath.contains(testDir);
	}

	static class IOModel {
		private final TraceNode node;
		private final VarValue varVal;

		public IOModel(TraceNode node, VarValue varVal) {
			this.node = node;
			this.varVal = varVal;
		}

		public TraceNode getNode() {
			return node;
		}

		public VarValue getVarVal() {
			return varVal;
		}
	}
}

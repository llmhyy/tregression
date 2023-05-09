package iodetection;

import java.util.List;
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

	public Optional<IOResult> detect() {
		Optional<NodeVarValPair> outputNodeAndVarValOpt = detectOutput();
		if (outputNodeAndVarValOpt.isEmpty()) {
			return Optional.empty();
		}
		NodeVarValPair outputNodeAndVarVal = outputNodeAndVarValOpt.get();
		VarValue output = outputNodeAndVarVal.getVarVal();
		List<VarValue> inputs = detectInputVarValsFromOutput(outputNodeAndVarVal.getNode(), output);
		return Optional.of(new IOResult(inputs, output));
	}

	Optional<NodeVarValPair> detectOutput() {
		TraceNode node;
		int lastNodeOrder = buggyTrace.getLatestNode().getOrder();
		for (int i = lastNodeOrder; i >= 1; i--) {
			node = buggyTrace.getTraceNode(i);
			Optional<NodeVarValPair> wrongVariableOptional = getWrongVariableInNode(node);
			if (wrongVariableOptional.isEmpty()) {
				continue;
			}
			return wrongVariableOptional;
		}
		return Optional.empty();
	}

	List<VarValue> detectInputVarValsFromOutput(TraceNode outputNode, VarValue output) {
		Set<VarValue> result = new HashSet<>();
		detectInputVarValsFromOutput(outputNode, result, new HashSet<String>());
		assert !result.contains(output);
		return new ArrayList<>(result);
	}

	// For each node, add the following as inputs
	// 1. Written vars - read variables.
	// 2. read variables without data dominators
	//
	// Recurse on the following:
	// 1. Data dominator on each read variable
	// 2. Control/Invocation Parent
	void detectInputVarValsFromOutput(TraceNode outputNode, Set<VarValue> inputs, Set<String> visited) {
		String key = formVisitedKey(outputNode);
		if (visited.contains(key)) {
			return;
		}
		visited.add(key);
		boolean isTestFile = isInTestDir(outputNode.getBreakPoint().getFullJavaFilePath());
		if (isTestFile) {
			// TODO: check if reference, then use heap address. (math_70 bug id 5)
			// Check primitive variables, compare with correct trace's aligned node
			List<VarValue> newInputs = new ArrayList<>(outputNode.getWrittenVariables());
			Optional<NodeVarValPair> wrongVariable = getWrongVariableInNode(outputNode);
			if (wrongVariable.isPresent()) {
				VarValue incorrectValue = wrongVariable.get().getVarVal();
				newInputs.remove(incorrectValue);
			}
			inputs.addAll(newInputs);
		}

		for (VarValue readVarVal : outputNode.getReadVariables()) {
			TraceNode dataDominator = buggyTrace.findDataDependency(outputNode, readVarVal);
			if (dataDominator == null && isTestFile) {
				inputs.add(readVarVal);
			}
			if (dataDominator != null) {
				detectInputVarValsFromOutput(dataDominator, inputs, visited);
			}
		}
		TraceNode controlDominator = outputNode.getInvocationMethodOrDominator();
		if (controlDominator != null) {
			detectInputVarValsFromOutput(controlDominator, inputs, visited);
		}
	}

	private String formVisitedKey(TraceNode node) {
		return String.valueOf(node.getOrder());
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
		List<VarValue> result = pair.findSingleWrongWrittenVarID(buggyTrace);
		Optional<NodeVarValPair> wrongWrittenVar = getWrongVarFromVarList(result, node);
		if (wrongWrittenVar.isPresent()) {
			return wrongWrittenVar;
		}
		result = pair.findSingleWrongReadVar(buggyTrace);
		Optional<NodeVarValPair> wrongReadVar = getWrongVarFromVarList(result, node);
		return wrongReadVar;
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
		if (!varValues.isEmpty()) {
			VarValue output = varValues.get(0);
			if (output instanceof ReferenceValue) {
				long addr = ((ReferenceValue) output).getUniqueID();
				if (addr != -1) { // If the "incorrect" ref var value is not null, don't return it.
					return Optional.empty();
				}
			}
			return Optional.of(new NodeVarValPair(node, output));
		}
		return Optional.empty();
	}

	static class NodeVarValPair {
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
	}

	public static class IOResult {
		private final List<VarValue> inputs;
		private final VarValue output;

		public IOResult(List<VarValue> inputs, VarValue output) {
			super();
			this.inputs = inputs;
			this.output = output;
		}

		public List<VarValue> getInputs() {
			return inputs;
		}

		public VarValue getOutput() {
			return output;
		}

	}
}

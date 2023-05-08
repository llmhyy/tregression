package iodetection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

/**
 * We store written Variables, but remove read variables from it & we don't add
 * reference VarValues as inputs based on heap address (alias id), & we don't
 * add primitive VarValues based on whether the corresponding trace node in
 * correct trace has the same stringValue.
 * 
 * @author Chenghin
 *
 */
public class IODetector {

	private final Trace buggyTrace;
	private final Trace correctTrace;
	private final String testDir;
	private final PairList pairList;

	private List<VarValue> inputs = new ArrayList<>();
	private VarValue output;

	public IODetector(Trace buggyTrace, Trace correctTrace, String testDir, PairList pairList) {
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.testDir = testDir;
		this.pairList = pairList;
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
		TraceNode node;
		int lastNodeOrder = buggyTrace.getLatestNode().getOrder();
		for (int i = lastNodeOrder; i >= 1; i--) {
			node = buggyTrace.getTraceNode(i);
			Optional<IOModel> wrongVariableOptional = getWrongVariableInNode(node);
			if (wrongVariableOptional.isEmpty()) {
				continue;
			}
			return wrongVariableOptional.get();
		}
//
//		for (int i = lastNodeOrder; i >= 0; i--) {
//			node = buggyTrace.getTraceNode(i);
//			if (node.getWrittenVariables().size() == 1) {
//				return new IOModel(node, node.getWrittenVariables().get(0));
//			}
//			if (node.getReadVariables().size() == 1) {
//				return new IOModel(node, node.getReadVariables().get(0));
//			}
//		}
		return null;
	}

	public List<VarValue> detectInputVarValsFromOutput(TraceNode outputNode, VarValue output) {
		Set<VarValue> result = new HashSet<>();
		detectInputVarValsFromOutput(outputNode, result, new HashSet<String>());
		assert !result.contains(output);
		return new ArrayList<>(result);
	}

	/**
	 * Currently, it looks at the pair list for the corresponding node in correct
	 * trace. It then checks if the value in the variables written is correct. If it
	 * is, it is added to the result.
	 * 
	 * @param node
	 * @return
	 */
	private List<VarValue> removeReadVariablesFromWritten(TraceNode node) {
		TraceNodePair nodePair = pairList.findByBeforeNode(node);

		List<VarValue> writtenVariables = node.getWrittenVariables();
		if (nodePair == null) {
			return writtenVariables;
		}
		TraceNode correctNode = nodePair.getAfterNode();
		Map<String, VarValue> varNameToPrimitiveVarValBuggyMap = createPrimitiveValueKeyToVarValueMap(writtenVariables);
		Map<String, VarValue> varNameToPrimitiveVarValWorkingMap = createPrimitiveValueKeyToVarValueMap(
				correctNode.getWrittenVariables());
		varNameToPrimitiveVarValBuggyMap.keySet().retainAll(varNameToPrimitiveVarValWorkingMap.keySet());
		List<VarValue> result = new ArrayList<>();
		result.addAll(varNameToPrimitiveVarValBuggyMap.values());

		List<VarValue> readVariables = node.getReadVariables();
		Map<Long, VarValue> addrToWrittenVar = createHeapAddrToVarValueMap(writtenVariables);
		Map<Long, VarValue> addrToReadVar = createHeapAddrToVarValueMap(readVariables);
		for (long key : addrToReadVar.keySet()) {
			addrToWrittenVar.remove(key);
		}

		result.addAll(addrToWrittenVar.values());
		return result;
	}

	private Map<Long, VarValue> createHeapAddrToVarValueMap(List<VarValue> varValues) {
		Map<Long, VarValue> result = new HashMap<>();
		for (VarValue varValue : varValues) {
			if (varValue instanceof ReferenceValue) {
				ReferenceValue refVarVal = (ReferenceValue) varValue;
				result.put(refVarVal.getUniqueID(), refVarVal);
			}
		}
		return result;
	}

	private Map<String, VarValue> createPrimitiveValueKeyToVarValueMap(List<VarValue> varValues) {
		Map<String, VarValue> result = new HashMap<>();
		for (VarValue varValue : varValues) {
			if (varValue instanceof PrimitiveValue) {
				PrimitiveValue primitiveValue = (PrimitiveValue) varValue;
				result.put(createPrimitiveValueKey(primitiveValue), primitiveValue);
			}
		}
		return result;
	}

	/**
	 * Using var name + value does not work since, unnamed variables will use the
	 * address.
	 * 
	 * @param varVal
	 * @return
	 */
	private String createPrimitiveValueKey(PrimitiveValue varVal) {
		String delim = "::";
		return String.join(delim, varVal.getVarName(), varVal.getStringValue());
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
			Optional<IOModel> wrongVariable = getWrongVariableInNode(outputNode);
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
	 * Use PairList code to get wrong VarValues in the current node. Do not add ReferenceValue, as it's
	 * correctness is determined by the address (i.e. always correct.)
	 * 
	 * @param node
	 * @return
	 */
	private Optional<IOModel> getWrongVariableInNode(TraceNode node) {
		TraceNodePair pair = pairList.findByBeforeNode(node);
		if (pair == null) {
			return Optional.empty();
		}
		List<VarValue> result = pair.findSingleWrongWrittenVarID(buggyTrace);
		if (!result.isEmpty() && !(result.get(0) instanceof ReferenceValue)) {
			return Optional.of(new IOModel(node, result.get(0)));
		}
		result = pair.findSingleWrongReadVar(buggyTrace);
		if (!result.isEmpty() && !(result.get(0) instanceof ReferenceValue)) {
			return Optional.of(new IOModel(node, result.get(0)));
		}
		return Optional.empty();
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

package iodetection;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class IODetector {

	private final Trace buggyTrace;
	private final Trace correctTrace;
	private final String testDir;

	private List<VarValue> inputs = new ArrayList<>();
	private List<VarValue> outputs = new ArrayList<>();

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

	public List<VarValue> getOutputs() {
		return this.outputs;
	}

	public void detect() {

	}

	public VarValue detectOutputVarVal() {
		return null;
	}

	public List<VarValue> detectInputVarValsFromOutput(TraceNode outputNode, VarValue output) {
		Set<VarValue> result = new HashSet<>();
		detectInputVarValsFromOutput(outputNode, output, result, new HashSet<TraceNode>());
		assert !result.contains(output);
		return new ArrayList<>(result);
	}

	void detectInputVarValsFromOutput(TraceNode outputNode, VarValue output, Set<VarValue> inputs, Set<TraceNode> visited) {
		if (visited.contains(outputNode)) {
			return;
		}
		boolean isTestFile = isInTestDir(outputNode.getBreakPoint().getFullJavaFilePath());
		TraceNode dataDominator = buggyTrace.findDataDependency(outputNode, output);
		if (dataDominator != null) {
			addInputsInTraceNode(dataDominator, inputs, visited);
		} else if (isTestFile) {
			inputs.add(output);
		}
		TraceNode controlDominator = outputNode.getControlDominator();
		if (controlDominator != null) {
			addInputsInTraceNode(controlDominator, inputs, visited);
		}
	}

	boolean isInTestDir(String filePath) {
		return filePath.contains(testDir);
	}

	void addInputsInTraceNode(TraceNode node, Set<VarValue> inputs, Set<TraceNode> visited) {
		boolean isTestFile = isInTestDir(node.getBreakPoint().getFullJavaFilePath());
		Set<VarValue> readVariables = new HashSet<>(node.getReadVariables());
		if (isTestFile) {
			Set<VarValue> writtenVariables = new HashSet<>(node.getWrittenVariables());
			writtenVariables.removeAll(readVariables);
			inputs.addAll(writtenVariables);
		}
		for (VarValue readVariable : readVariables) {
			detectInputVarValsFromOutput(node, readVariable, inputs, visited);
		}
	}
}

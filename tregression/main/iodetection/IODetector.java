package iodetection;

import java.util.List;
import java.util.ArrayList;

import microbat.model.trace.Trace;
import microbat.model.value.VarValue;

public class IODetector {
	
	private final Trace buggyTrace;
	private final Trace correctTrace;
	
	private List<VarValue> inputs = new ArrayList<>();
	private List<VarValue> outputs = new ArrayList<>();
	
	public IODetector(Trace buggyTrace, Trace correctTrace) {
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
	}
	
	// TODO find the input and the output of the test case
	// Okay to include many inputs (eg. setting all the variable before the test case to inputs, but be careful on this)
	// Since we have the correct trace for reference, you may know which variable is correct or not
	// To make reference from the correct trace, you may take some reference from the following class:
	
	// tregression.handler.SeparateVersionHandler
	// tregression.StepChangeType
	// tregression.separatesnapshots.DiffMatcher
	// tregression.separatesnapshots.DiffMatcher
	
	// Some code example are avaiable at tregression.autofeedbackevaluation.AutoDebugEvaluator#getRefFeedbacks

	public List<VarValue> getInputs() {
		return this.inputs;
	}
	
	public List<VarValue> getOutputs() {
		return this.outputs;
	}
	
}

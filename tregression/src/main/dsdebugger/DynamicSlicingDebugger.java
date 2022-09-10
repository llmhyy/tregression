package dsdebugger;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

/**
 * DynamicSlicingDebugger will apply dynamic slicing
 * to simulate the debugging process.
 * 
 * It is used to estimate the performance of using dynamic slicing
 * 
 * @author David
 *
 */
public class DynamicSlicingDebugger {

	private Trace buggyTrace;
	private Trace correctTrace;
	private List<TraceNode> rootCauses;
	
	/**
	 * Output variable that assume to be wrong
	 */
	private VarValue output = null;
	
	private int slicingCount = 0;
	
	/**
	 * Constructor
	 * @param buggyTrace Buggy Trace
	 * @param correctTrace Correct Trace
	 * @param rootCauses List of root causes
	 */
	public DynamicSlicingDebugger(Trace buggyTrace, Trace correctTrace, List<TraceNode> rootCauses) {
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.rootCauses = rootCauses;
	}
	
	/**
	 * Constructor
	 * @param buggyTrace Buggy Trace
	 * @param correctTrace Correct Trace
	 * @param rootCause Root Cause Node
	 */
	public DynamicSlicingDebugger(Trace buggyTrace, Trace correctTrace, TraceNode rootCause) {
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.rootCauses = new ArrayList<>();
		rootCauses.add(rootCause);
	}
	
	public int getSlicingCount() {
		return this.slicingCount;
	}
	
	public void setOutput(VarValue output) {
		this.output = output;
	}
}

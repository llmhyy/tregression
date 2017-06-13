package tregression.separatesnapshots;

import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;

public class RunningResult {
	private Trace runningTrace;
	private List<BreakPoint> executedStatements;

	public RunningResult(Trace runningTrace, List<BreakPoint> executedStatements) {
		super();
		this.runningTrace = runningTrace;
		this.executedStatements = executedStatements;
	}

	public Trace getRunningTrace() {
		return runningTrace;
	}

	public void setRunningTrace(Trace runningTrace) {
		this.runningTrace = runningTrace;
	}

	public List<BreakPoint> getExecutedStatements() {
		return executedStatements;
	}

	public void setExecutedStatements(List<BreakPoint> executedStatements) {
		this.executedStatements = executedStatements;
	}

	@Override
	public String toString() {
		return "RunningResult [runningTrace=" + runningTrace + ", executedStatements=" + executedStatements + "]";
	}

}

package tregression.separatesnapshots;

import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.TrialGenerator;
import tregression.junit.TestCaseRunner;

public class RunningResult {
	private Trace runningTrace;
	private List<BreakPoint> executedStatements;
	private TestCaseRunner checker;
	private AppJavaClassPath appClassPath;
	
	private int failureType = TrialGenerator.NORMAL;

	public RunningResult() {
	}
	
	public RunningResult(Trace runningTrace, List<BreakPoint> executedStatements, TestCaseRunner checker, AppJavaClassPath appClassPath) {
		super();
		this.runningTrace = runningTrace;
		this.executedStatements = executedStatements;
		this.setChecker(checker);
		this.appClassPath = appClassPath;
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

	public TestCaseRunner getChecker() {
		return checker;
	}

	public void setChecker(TestCaseRunner checker) {
		this.checker = checker;
	}

	public AppJavaClassPath getAppClassPath() {
		return appClassPath;
	}

	public void setAppClassPath(AppJavaClassPath appClassPath) {
		this.appClassPath = appClassPath;
	}

	public int getRunningType() {
		return failureType;
	}

	public void setFailureType(int failureType) {
		this.failureType = failureType;
	}

}

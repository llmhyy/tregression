package tregression.separatesnapshots;

import java.util.List;

import microbat.codeanalysis.runtime.ExecutionStatementCollector;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.TrialGenerator;

public class RunningResult {
	private Trace runningTrace;
	private List<BreakPoint> executedStatements;
	private ExecutionStatementCollector checker;
	private AppJavaClassPath appClassPath;
	
	private PreCheckInformation precheckInfo;
	private RunningInfo runningInfo;
	
	private int failureType = TrialGenerator.NORMAL;

	public RunningResult() {
	}
	
	public RunningResult(Trace runningTrace, List<BreakPoint> executedStatements, ExecutionStatementCollector checker, 
			PreCheckInformation precheckInfo,
			AppJavaClassPath appClassPath) {
		super();
		this.runningTrace = runningTrace;
		this.executedStatements = executedStatements;
		this.setChecker(checker);
		this.appClassPath = appClassPath;
		this.precheckInfo = precheckInfo;
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

	public ExecutionStatementCollector getChecker() {
		return checker;
	}

	public void setChecker(ExecutionStatementCollector checker) {
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

	public PreCheckInformation getPrecheckInfo() {
		return precheckInfo;
	}

	public void setPrecheckInfo(PreCheckInformation precheckInfo) {
		this.precheckInfo = precheckInfo;
	}

	public RunningInfo getRunningInfo() {
		return runningInfo;
	}

	public void setRunningInfo(RunningInfo runningInfo) {
		this.runningInfo = runningInfo;
	}

}

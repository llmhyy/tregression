/**
 * 
 */
package traceagent.report;

import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator0;
import tregression.separatesnapshots.RunningResult;

/**
 * @author LLT
 *
 */
public class BugCaseTrial {
	private String projectName;
	private String bugID;
	private TestCase tc;
	private TraceTrial fixedTrace;
	private TraceTrial bugTrace;

	public BugCaseTrial(String projectName, String bugID, TestCase tc) {
		this.projectName = projectName;
		this.bugID = bugID;
		this.tc = tc;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getBugID() {
		return bugID;
	}

	public TestCase getTc() {
		return tc;
	}

	public TraceTrial getFixedTrace() {
		return fixedTrace;
	}

	public void setFixedTrace(TraceTrial fixedTrace) {
		this.fixedTrace = fixedTrace;
	}

	public TraceTrial getBugTrace() {
		return bugTrace;
	}

	public void setBugTrace(TraceTrial bugTrace) {
		this.bugTrace = bugTrace;
	}

	public static class TraceTrial {
		private PreCheckInformation precheckInfo;
		private RunningInformation runningInfo;
		private String failureType;
		private long executionTime;
		private String workingDir;

		public TraceTrial(String workingDir, RunningResult rs, long executionTime) {
			this.precheckInfo = rs.getPrecheckInfo();
			this.runningInfo = rs.getRunningInfo();
			this.failureType = TrialGenerator0.getProblemType(rs.getRunningType());
			this.executionTime = executionTime;
			this.workingDir = workingDir;
		}

		public PreCheckInformation getPrecheckInfo() {
			return precheckInfo;
		}

		public void setPrecheckInfo(PreCheckInformation precheckInfo) {
			this.precheckInfo = precheckInfo;
		}

		public RunningInformation getRunningInfo() {
			return runningInfo;
		}

		public void setRunningInfo(RunningInformation runningInfo) {
			this.runningInfo = runningInfo;
		}

		public String getFailureType() {
			return failureType;
		}

		public void setFailureType(String failureType) {
			this.failureType = failureType;
		}

		public long getExecutionTime() {
			return executionTime;
		}

		public void setExecutionTime(long executionTime) {
			this.executionTime = executionTime;
		}

		public String getWorkingDir() {
			return workingDir;
		}
	}
}

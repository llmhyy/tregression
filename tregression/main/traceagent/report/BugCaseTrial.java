/**
 * 
 */
package traceagent.report;

import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.instrumentation.output.RunningInfo;
import tregression.empiricalstudy.TestCase;

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
	private String exception;

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
	
	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public static class TraceTrial {
		private PreCheckInformation precheckInfo;
		private RunningInfo runningInfo;
		private long executionTime;
		private String workingDir;
		private boolean isBuggy;

		public TraceTrial(String workingDir, PreCheckInformation precheckInfo, RunningInfo runningInfo,
				long executionTime, boolean isBuggy) {
			this.precheckInfo = precheckInfo;
			this.runningInfo = runningInfo;
			this.executionTime = executionTime;
			this.workingDir = workingDir;
			this.isBuggy = isBuggy;
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

		public long getExecutionTime() {
			return executionTime;
		}

		public void setExecutionTime(long executionTime) {
			this.executionTime = executionTime;
		}

		public String getWorkingDir() {
			return workingDir;
		}
		
		public String getSummary() {
			StringBuilder sb = new StringBuilder();
			if (precheckInfo == null) {
				return "Precheck fails!";
			}
			if (precheckInfo.isOverLong()) {
				sb.append("Overlong!; ");
			} else if (precheckInfo.isUndeterministic()) {
				sb.append("Undeterministic!; ");
			} else if (runningInfo != null) {
				boolean pass = "true;no fail".equals(runningInfo.getProgramMsg());
				if ((isBuggy && pass)
						|| (!isBuggy && !pass)) {
					sb.append("Test result is not correct!; ");
				}
				if (precheckInfo.getStepNum() != runningInfo.getCollectedSteps()) {
					sb.append("Steps mismatched! {")
						.append(runningInfo.getCollectedSteps() - precheckInfo.getStepNum())
						.append("}; ");
				}
			} else {
				sb.append("Missing runningInfo!");
			}
			String sumary = sb.toString();
			if (sumary.isEmpty()) {
				sumary = "Ok";
			}
			return sumary;
		}
	}
}

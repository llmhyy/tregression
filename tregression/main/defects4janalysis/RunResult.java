package defects4janalysis;

public class RunResult {
	
	public String projectName = "";
	public int bugID = -1;
	public long traceLen = -1;
	public long rootCauseOrder = -1;
	public boolean isOmissionBug = false;
	public String solutionName = "";
	public String errorMessage = "";
	
	public final static String DELIMITER = ",";
	
	public boolean isSuccess() {
		return this.traceLen != -1;
	}
	
	public static RunResult parseString(final String string) {
		RunResult result = new RunResult();
		String[] tokens = string.split(RunResult.DELIMITER);
		
		final String projName = tokens[0];
		result.projectName = projName;
		
		final String bugID_str = tokens[1];
		result.bugID = Integer.valueOf(bugID_str);
		
		final String traceLen_str = tokens[2];
		result.traceLen = Integer.valueOf(traceLen_str);
		
		final String rootCauseOrder_str = tokens[3];
		result.rootCauseOrder = Integer.valueOf(rootCauseOrder_str);
		
		final String isOmissionBug_str = tokens[4];
		result.isOmissionBug = Boolean.valueOf(isOmissionBug_str);
		
		final String solutionName = tokens[5];
		result.solutionName = solutionName;
		
		final String errMsg = tokens[6];
		result.errorMessage = errMsg;
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		this.appendStr(strBuilder, this.projectName);
		this.appendStr(strBuilder, String.valueOf(this.bugID));
		this.appendStr(strBuilder, String.valueOf(this.traceLen));
		this.appendStr(strBuilder, String.valueOf(this.rootCauseOrder));
		this.appendStr(strBuilder, String.valueOf(this.isOmissionBug));
		this.appendStr(strBuilder, this.solutionName);
		this.appendStr(strBuilder, this.errorMessage);
		return strBuilder.toString();
	}
	
	private void appendStr(final StringBuilder buffer, final String string) {
		buffer.append(string);
		buffer.append(RunResult.DELIMITER);
	}


}

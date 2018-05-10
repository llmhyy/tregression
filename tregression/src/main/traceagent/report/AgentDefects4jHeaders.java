/**
 * 
 */
package traceagent.report;

import traceagent.report.excel.ExcelHeader;

/**
 * @author LLT
 *
 */
public enum AgentDefects4jHeaders implements ExcelHeader {
	PROJECT_NAME,
	BUG_ID,
	TEST_CASE,
	IS_BUG_TRACE,
	WORKING_DIR,
	EXECUTION_TIME,
	THREAD_NUM,
	IS_OVERLONG,
	VISITED_LOCS,
	OVERLONG_METHODS,
	IS_PASSTEST,
	LOADED_CLASSES,
	PRECHECK_STEP_NUM,
	RUN_STEP_NUM,
	PROGRAM_MSG,
	SUMMARY;
	
	@Override
	public String getTitle() {
		return name();
	}

	@Override
	public int getCellIdx() {
		return ordinal();
	}
	
}

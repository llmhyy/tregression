/**
 * 
 */
package traceagent.report;

import static traceagent.report.AgentDefects4jHeaders.*;

import java.io.File;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.instrumentation.output.RunningInfo;
import sav.common.core.utils.StringUtils;
import traceagent.report.BugCaseTrial.TraceTrial;
import traceagent.report.excel.AbstractExcelWriter;

/**
 * @author LLT
 *
 */
public class AgentDefects4jReport extends AbstractExcelWriter {

	public AgentDefects4jReport(File file) throws Exception {
		super(file);
	}

	public void record(BugCaseTrial trial) throws IOException {
		Sheet sheet = getSheet("testcase", AgentDefects4jHeaders.values(), 0);
		int rowNum = sheet.getLastRowNum() + 1;
		if (trial.getBugTrace() != null) {
			Row row = sheet.createRow(rowNum++);
			writeTestcase(row, trial, trial.getBugTrace(), true);
		}
		if (trial.getFixedTrace() != null) {
			Row row = sheet.createRow(rowNum);
			writeTestcase(row, trial, trial.getFixedTrace(), false);
		}
		writeWorkbook();
	}

	private void writeTestcase(Row row, BugCaseTrial trial, TraceTrial traceTrial, boolean isBuggy) {
		addCell(row, PROJECT_NAME, trial.getProjectName());
		addCell(row, BUG_ID, trial.getBugID());
		addCell(row, TEST_CASE, trial.getTc().getName());
		addCell(row, IS_BUG_TRACE, isBuggy);
		addCell(row, WORKING_DIR, traceTrial.getWorkingDir());
		addCell(row, EXECUTION_TIME, traceTrial.getExecutionTime());
		PreCheckInformation precheck = traceTrial.getPrecheckInfo();
		if (precheck != null) {
			addCell(row, THREAD_NUM, precheck.getThreadNum());
			addCell(row, VISITED_LOCS, precheck.getVisitedLocations().size());
			addCell(row, OVERLONG_METHODS, StringUtils.join(precheck.getOverLongMethods(), "; "));
			addCell(row, IS_OVERLONG, precheck.isOverLong());
			addCell(row, IS_PASSTEST, precheck.isPassTest());
			addCell(row, LOADED_CLASSES, precheck.getLoadedClasses().size());
			addCell(row, PRECHECK_STEP_NUM, precheck.getStepNum());
		}
		RunningInfo runningInfo = traceTrial.getRunningInfo();
		if (runningInfo != null) {
			addCell(row, RUN_STEP_NUM, runningInfo.getCollectedSteps());
			addCell(row, PROGRAM_MSG, runningInfo.getProgramMsg());
		}
		addCell(row, SUMMARY, traceTrial.getSummary());
	}
	
	
}

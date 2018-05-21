package traceagent.handler;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import experiment.utils.report.excel.ExcelUtils;
import sav.common.core.utils.StringUtils;
import traceagent.handler.TestcaseFilter.IFilter;
import traceagent.report.AgentDefects4jHeaders;

public class SucceededCasesFilter implements IFilter {
	private static final String SEPARATOR = "_";
	private List<String> tcs;
	
	public SucceededCasesFilter(String excelFile) {
		if (excelFile == null || !new File(excelFile).exists()) {
			tcs = Collections.emptyList();
		} else {
			tcs = ExcelUtils.collectKeys(excelFile, "testcase", 
					Arrays.asList(AgentDefects4jHeaders.PROJECT_NAME.name(),
							AgentDefects4jHeaders.BUG_ID.name(),
							AgentDefects4jHeaders.TEST_CASE.name(),
							AgentDefects4jHeaders.IS_BUG_TRACE.name()),
					SEPARATOR);
		}
	}

	public boolean filter(String projectName, String bugID, String name, boolean isBuggy) {
		String caseId = StringUtils.join(SEPARATOR, projectName, bugID, name, isBuggy);
		boolean filtered = tcs.contains(caseId);
		if (filtered) {
			System.out.println("ignore: " + caseId);
		}
		return filtered;
	}
}

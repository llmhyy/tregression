package traceagent.handler;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import experiment.utils.report.excel.ExcelReader;
import sav.common.core.utils.StringUtils;
import traceagent.handler.TestcaseFilter.IFilter;

public class DiscardedCasesFilter implements IFilter {
	private static final String SEPARATOR = "_";
	private Set<String> tcs = new HashSet<>();
	
	public DiscardedCasesFilter(String defects4jExcelFile) {
		if (defects4jExcelFile == null || !new File(defects4jExcelFile).exists()) {
			return;
		}
		ExcelReader checkReader = new ExcelReader(new File(defects4jExcelFile), 0);
		List<List<Object>> data = checkReader.listData("all");
		Iterator<List<Object>> rowIt = data.iterator();
		while (rowIt.hasNext()) {
			List<Object> rowData = rowIt.next();
			Object tc = rowData.get(3);
			if (tc == null || "".equals(tc)) {
				tcs.add(rowData.get(0) + SEPARATOR + rowData.get(1));
			}
		}
	}

	@Override
	public boolean filter(String projectName, String bugID, String name, boolean isBuggy) {
		String caseId = StringUtils.join(SEPARATOR, projectName, bugID);
		return !tcs.contains(caseId);
	}
}

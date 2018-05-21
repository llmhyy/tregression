/**
 * 
 */
package traceagent.handler;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 *
 */
public class TestcaseFilter {
	private List<IFilter> filters;
	private boolean disable;
	
	public TestcaseFilter(boolean enable) {
		this.disable = !enable;
		if (enable) {
			filters = new ArrayList<>();
//			filters.add(new DiscardedCasesFilter("defects4j0.old.xlsx"));
			filters.add(new SucceededCasesFilter("Agent_Defect4j.xlsx"));
//			filters.add(new TestcaseFilterByName());
		}
	}

	public boolean filter(String projectName, String bugID, String name, boolean isBuggy) {
		if (disable) {
			return false;
		}
		boolean filtered = false;
		for (IFilter filter : filters) {
			filtered = filter.filter(projectName, bugID, name, isBuggy);
			if (filtered) {
				System.out.println("ignore: " + StringUtils.join("_", projectName, bugID, name, isBuggy));
				break;
			}
		}
		return filtered;
	}
	
	public static interface IFilter {
		public boolean filter(String projectName, String bugID, String name, boolean isBuggy);
	}
}

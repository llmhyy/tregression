package traceagent.handler;

import traceagent.handler.TestcaseFilter.IFilter;

public class TestcaseFilterByName implements IFilter {

	@Override
	public boolean filter(String projectName, String bugID, String name, boolean isBuggy) {
		return ("Chart".equals(projectName) && "26".equals(bugID))
				|| "com.google.javascript.jscomp.CommandLineRunnerTest#testSimpleModeLeavesUnusedParams".equals(name);
	}


}

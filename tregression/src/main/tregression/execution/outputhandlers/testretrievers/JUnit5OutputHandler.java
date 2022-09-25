package tregression.execution.outputhandlers.testretrievers;

import tregression.empiricalstudy.TestCase;

public class JUnit5OutputHandler extends TestRetrieverOutputHandler {

	void addTestCase(String line) {
		if (!line.contains(FAILURE_MSG)) {
			return;
		}
		int openBracketIdx = line.indexOf('(');
		if (openBracketIdx == -1) {
			return;
		}
		int lastFullStop = line.lastIndexOf('.');
		String testName = line.substring(lastFullStop, openBracketIdx);
		String className = line.substring(0, lastFullStop);
		failingTests.add(new TestCase(testName, className));
	}
}

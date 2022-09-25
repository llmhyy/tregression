package tregression.execution.outputhandlers.testretrievers;

import tregression.empiricalstudy.TestCase;

public class JUnit4OutputHandler extends TestRetrieverOutputHandler {
	private boolean startRecordingFailures = false;
	private final static String FAILURE_LIST_HEADER = "Failed tests:";
	
	void addTestCase(String line) {
		if (line.equals(FAILURE_LIST_HEADER)) {
			startRecordingFailures = true;
			return;
		}
		if (!startRecordingFailures) {
			return;
		}
		int openBracketIdx = line.indexOf('(');
		String testName = line.substring(2, openBracketIdx);
		String className = line.substring(openBracketIdx + 1, line.length());
		failingTests.add(new TestCase(testName, className));
	}
}

package tregression.execution.outputhandlers.testretrievers;

import tregression.empiricalstudy.TestCase;

public class TestNGOutputHandler extends TestRetrieverOutputHandler {
	
	@Override
	public void output(String line) {
		super.output(line);
		if (!line.contains(FAILURE_MSG)) {
			return;
		}
		int openBracketIdx = line.indexOf('(');
		int closeBracketIdx = line.indexOf(')');
		String testName = line.substring(0, openBracketIdx);
		String className = line.substring(openBracketIdx, closeBracketIdx);
		failingTests.add(new TestCase(testName, className));
	}
}

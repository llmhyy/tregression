package tregression.execution.outputhandlers.testretrievers;

import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;
import tregression.execution.outputhandlers.OutputHandler;

public abstract class TestRetrieverOutputHandler extends OutputHandler {
	protected final static String FAILURE_MSG = "FAILURE!";
	protected List<TestCase> failingTests = new ArrayList();
	public List<TestCase> getFailingTests() {
		return failingTests;
	}
	
	@Override
	public void output(String line) {
		super.output(line);
		addTestCase(line);
	}
	
	/**
	 * Processes an output line, and adds a failing test to failingTests list if the line specifies
	 * @param line
	 */
	abstract void addTestCase(String line);
}

package tregression.execution.outputhandlers.testretrievers;

import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;
import tregression.execution.outputhandlers.OutputHandler;

public class TestRetrieverOutputHandler extends OutputHandler {
	protected final static String FAILURE_MSG = "FAILURE!";
	protected List<TestCase> failingTests = new ArrayList();
	public List<TestCase> getFailingTests() {
		return failingTests;
	}
}

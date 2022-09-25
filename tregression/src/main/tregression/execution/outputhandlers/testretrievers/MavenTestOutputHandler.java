package tregression.execution.outputhandlers.testretrievers;

import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.TestCase;

public class MavenTestOutputHandler extends TestRetrieverOutputHandler {
	private List<TestRetrieverOutputHandler> testRetrievers = new ArrayList<>();
	
	public MavenTestOutputHandler() {
		testRetrievers.add(new JUnit4OutputHandler());
		testRetrievers.add(new JUnit5OutputHandler());
		testRetrievers.add(new TestNGOutputHandler());
	}
	
	void addTestCase(String line) {
		for (TestRetrieverOutputHandler testRetriever : testRetrievers) {
			testRetriever.addTestCase(line);
		}
	}
	
	@Override
	public List<TestCase> getFailingTests()  {
		if (!failingTests.isEmpty()) {
			return failingTests;
		}
		
		for (TestRetrieverOutputHandler testRetriever : testRetrievers) {
			failingTests.addAll(testRetriever.getFailingTests());
		}
		
		return failingTests;
	}
}

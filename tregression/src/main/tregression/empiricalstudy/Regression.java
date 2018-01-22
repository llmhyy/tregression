package tregression.empiricalstudy;

import java.io.File;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import sav.strategies.dto.AppJavaClassPath;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;

public class Regression {
	private String testClass;
	private String testMethod;
	private Trace buggyTrace;
	private Trace correctTrace;
	private PairList pairList;

	public Regression(Trace buggyTrace, Trace correctTrace, PairList pairList) {
		super();
		this.buggyTrace = buggyTrace;
		this.correctTrace = correctTrace;
		this.pairList = pairList;
	}

	public Trace getBuggyTrace() {
		return buggyTrace;
	}

	public void setBuggyTrace(Trace buggyTrace) {
		this.buggyTrace = buggyTrace;
	}

	public Trace getCorrectTrace() {
		return correctTrace;
	}

	public void setCorrectTrace(Trace correctTrace) {
		this.correctTrace = correctTrace;
	}

	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}

	public void fillMissingInfor(Defects4jProjectConfig config, String buggyPath, String fixPath) {
		fillMissingInfor(buggyTrace, AppClassPathInitializer.initialize(buggyPath, new TestCase(testClass, testMethod), config));
		fillMissingInfor(correctTrace, AppClassPathInitializer.initialize(fixPath, new TestCase(testClass, testMethod), config));
	}

	public void fillMissingInfor(Trace trace, AppJavaClassPath appClassPath) {
		trace.setAppJavaClassPath(appClassPath);
		for (TraceNode node : trace.getExecutionList()) {
			BreakPoint point = node.getBreakPoint();
			if (point.getFullJavaFilePath() != null) {
				continue;
			}
			String relativePath = point.getDeclaringCompilationUnitName().replace(".", File.separator) + ".java";
			String sourcePath = appClassPath.getSoureCodePath() + File.separator + relativePath;
			String testPath = appClassPath.getTestCodePath() + File.separator + relativePath;
			if (new File(sourcePath).exists()) {
				point.setFullJavaFilePath(sourcePath);
			} else if (new File(testPath).exists()) {
				point.setFullJavaFilePath(testPath);
			} else {
				System.err.println("cannot find the source code file for " + point);
			}
		}
	}

	public void setTestCase(String testClass, String testMethod) {
		this.testClass = testClass;
		this.testMethod = testMethod;
	}

	public String getTestClass() {
		return testClass;
	}
}

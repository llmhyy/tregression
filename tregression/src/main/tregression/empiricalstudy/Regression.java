package tregression.empiricalstudy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.preference.AnalysisScopePreference;
import sav.strategies.dto.AppJavaClassPath;
import tregression.model.PairList;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.TraceCollector0;

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

	public void fillMissingInfo(Defects4jProjectConfig config, String buggyPath, String fixPath) {
		fillMissingInfo(buggyTrace, AppClassPathInitializer.initialize(buggyPath, new TestCase(testClass, testMethod), config));
		fillMissingInfo(correctTrace, AppClassPathInitializer.initialize(fixPath, new TestCase(testClass, testMethod), config));
	}

	public static void fillMissingInfo(Trace trace, AppJavaClassPath appClassPath) {
		trace.setAppJavaClassPath(appClassPath);
		
		InstrumentationExecutor.appendMissingInfo(trace, appClassPath);
		
//		Map<String, String> classNameMap = new HashMap<>();
//		Map<String, String> pathMap = new HashMap<>();
//		
//		for (TraceNode node : trace.getExecutionList()) {
//			BreakPoint point = node.getBreakPoint();
//			if (point.getFullJavaFilePath() != null) {
//				continue;
//			}
//			
//			new InstrumentationExecutor(appClassPath, null, null, 
//					includedClassNames, excludedClassNames).attachFullPathInfo(point, appClassPath, classNameMap, pathMap);
//			
//		}
	}

	public void setTestCase(String testClass, String testMethod) {
		this.testClass = testClass;
		this.testMethod = testMethod;
	}

	public String getTestClass() {
		return testClass;
	}
}

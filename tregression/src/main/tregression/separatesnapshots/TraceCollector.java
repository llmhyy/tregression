package tregression.separatesnapshots;

import java.io.File;
import java.util.List;

import microbat.codeanalysis.runtime.ExecutionStatementCollector;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.util.MicroBatUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.TraceModelConstructor;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TrialGenerator;

public class TraceCollector {
	
	public RunningResult preCheck(String workingDir, String testClass, String testMethod, 
			Defects4jProjectConfig config, boolean isRunInTestCaseMode, boolean allowMultiThread) {
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, testClass, testMethod, config);
		if(!isRunInTestCaseMode) {
			appClassPath.setLaunchClass(appClassPath.getOptionalTestClass());
			System.currentTimeMillis();
		}
		
		List<String> libJars = appClassPath.getExternalLibPaths();
		List<String> exlcudes = MicroBatUtil.extractExcludeFiles("", libJars);
		
		ExecutionStatementCollector checker = new ExecutionStatementCollector();
		
		checker.addLibExcludeList(exlcudes);
		List<BreakPoint> executingStatements = checker.collectBreakPoints(appClassPath, isRunInTestCaseMode);
		
		if(checker.isOverLong()) {
			System.out.println("The trace is over long!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator.OVER_LONG);
			return rs;
		}
		
		if(checker.isMultiThread() && !allowMultiThread) {
			System.out.println("It is multi-thread program!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator.MULTI_THREAD);
			return rs;
		}
		
		System.out.println("There are " + checker.getExecutionOrderList().size() + " steps for this trace.");
		
		for(BreakPoint point: executingStatements){
			String relativePath = point.getDeclaringCompilationUnitName().replace(".", File.separator) + ".java";
			String sourcePath = appClassPath.getSoureCodePath() + File.separator + relativePath;
			String testPath = appClassPath.getTestCodePath() + File.separator + relativePath;
			
			if(new File(sourcePath).exists()) {
				point.setFullJavaFilePath(sourcePath);
			}
			else if(new File(testPath).exists()) {
				point.setFullJavaFilePath(testPath);
			}
			else {
				System.err.println("cannot find the source code file for " + point);
			}
		}
		
		RunningResult rs = new RunningResult(null, executingStatements, checker, appClassPath);
		return rs;
	}

	public RunningResult run(RunningResult result, boolean isRunInTestCaseMode){
		
		TraceModelConstructor constructor = new TraceModelConstructor();
		ExecutionStatementCollector checker = result.getChecker();
		
		long t1 = System.currentTimeMillis();
		Trace trace = constructor.constructTraceModel(result.getAppClassPath(), result.getExecutedStatements(), 
				checker.getExecutionOrderList(), checker.getStepNum(), false, isRunInTestCaseMode);
		long t2 = System.currentTimeMillis();
		int time = (int) (t2-t1);
		trace.setConstructTime(time);
		
		result.setRunningTrace(trace);
		return result;
	}

	
}

package tregression.separatesnapshots;

import java.io.File;
import java.util.List;

import microbat.codeanalysis.runtime.ExecutionStatementCollector;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;

public class TraceCollector0 {
	
	public RunningResult preCheck(String workingDir, TestCase tc, 
			Defects4jProjectConfig config, boolean isRunInTestCaseMode, boolean allowMultiThread) {
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, tc, config);
		if(!isRunInTestCaseMode) {
			appClassPath.setLaunchClass(appClassPath.getOptionalTestClass());
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
		
//		System.out.println("There are " + checker.getExecutionOrderList().size() + " steps for this trace.");
		
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

	public RunningResult run(String workingDir, TestCase tc, 
			Defects4jProjectConfig config, boolean isRunInTestCaseMode, boolean allowMultiThread){
		
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, tc, config);
		if(!isRunInTestCaseMode) {
			appClassPath.setLaunchClass(appClassPath.getOptionalTestClass());
		}
		
		InstrumentationExecutor exectuor = new InstrumentationExecutor(appClassPath);
		Trace trace = exectuor.run();
		
		PreCheckInformation precheckInfo = exectuor.getPrecheckInfo();
		//TODO
		if(/*precheckInfo.isOverLong()*/precheckInfo.getStepNum()>Settings.stepLimit) {
			System.out.println("The trace is over long!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator.OVER_LONG);
			return rs;
		}
		
		boolean isMultiThread = precheckInfo.getThreadNum()!=1;
		if(isMultiThread && !allowMultiThread) {
			System.out.println("It is multi-thread program!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator.MULTI_THREAD);
			return rs;
		}
		
		trace.setMultiThread(isMultiThread);
		trace.setAppJavaClassPath(appClassPath);
		for(TraceNode node: trace.getExecutionList()){
			BreakPoint point = node.getBreakPoint();
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
		
		RunningResult rs = new RunningResult(trace, null, null, appClassPath);
		rs.setRunningTrace(trace);
		return rs;
	}

	
}
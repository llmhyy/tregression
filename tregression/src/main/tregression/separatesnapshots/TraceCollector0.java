package tregression.separatesnapshots;

import java.util.List;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.model.trace.Trace;
import microbat.util.MicroBatUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator0;

public class TraceCollector0 {
	private boolean isBuggy;
	
	public TraceCollector0(boolean buggy) {
		this.isBuggy = buggy;
	}
	
	public RunningResult run(String workingDir, TestCase tc, 
			Defects4jProjectConfig config, boolean isRunInTestCaseMode, boolean allowMultiThread, 
			List<String> includeLibs, List<String> excludeLibs){
		
		AppJavaClassPath appClassPath = AppClassPathInitializer.initialize(workingDir, tc, config);
		if(!isRunInTestCaseMode) {
			appClassPath.setLaunchClass(appClassPath.getOptionalTestClass());
		}
		
		String traceDir = MicroBatUtil.generateTraceDir(config.projectName, String.valueOf(config.bugID));
		String traceName = isBuggy ? "bug" : "fix";
		InstrumentationExecutor exectuor = new InstrumentationExecutor(appClassPath,
				traceDir, traceName, includeLibs, excludeLibs);
		
		RunningInformation info = exectuor.run();
		
		PreCheckInformation precheckInfo = exectuor.getPrecheckInfo();
		System.out.println("There are " + precheckInfo.getStepNum() + " steps in this trace");
		if(precheckInfo.isOverLong()) {
			System.out.println("The trace is over long!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.OVER_LONG);
			return rs;
		}
		
//		if(!precheckInfo.getOverLongMethods().isEmpty()) {
//			String method = precheckInfo.getOverLongMethods().get(0);
//			System.out.println("Method " + method + " is over long after instrumentation!");
//			RunningResult rs = new RunningResult();
//			rs.setFailureType(TrialGenerator0.OVER_LONG_INSTRUMENTATION_METHOD);
//			return rs;
//		}
		
		if(precheckInfo.isUndeterministic()){
			System.out.println("This is undeterministic testcase!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.UNDETERMINISTIC);
			return rs;
		}
		
		
		boolean isMultiThread = precheckInfo.getThreadNum()!=1;
		if(isMultiThread && !allowMultiThread) {
			System.out.println("It is multi-thread program!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.MULTI_THREAD);
			return rs;
		}
		
//		if(!info.isExpectedStepsMet()){
//			System.out.println("The expected steps are not met by normal run");
//			RunningResult rs = new RunningResult();
//			rs.setFailureType(TrialGenerator0.EXPECTED_STEP_NOT_MET);
//			return rs;
//		}
		
		Trace trace = info.getTrace();
		trace.setSourceVersion(isBuggy);
		
		RunningResult rs = new RunningResult(trace, null, null, precheckInfo, appClassPath);
		rs.setRunningTrace(trace);
		return rs;
	}
	
}

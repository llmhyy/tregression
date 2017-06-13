package tregression.separatesnapshots;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import sav.commons.TestConfiguration;
import sav.strategies.dto.AppJavaClassPath;
import tregression.TraceModelConstructor;
import tregression.junit.TestCaseAnalyzer;
import tregression.junit.TestCaseRunner;

public class TraceCollector {
	public AppJavaClassPath initialize(String workingDir, String testClass, String testMethod){
		AppJavaClassPath appClassPath = new AppJavaClassPath();
		
		String testTargetPath = workingDir + File.separator + "build-tests";
		String codeTargetPath = workingDir + File.separator + "build";
		
		appClassPath.addClasspath(testTargetPath);
		appClassPath.addClasspath(codeTargetPath);
		
		String sourceCodePath = workingDir + File.separator + "source";
		appClassPath.setSourceCodePath(sourceCodePath);
		
		String testCodePath = workingDir + File.separator + "tests";
		appClassPath.setTestCodePath(testCodePath);
		
		/**
		 * setting junit lib into classpath
		 */
		String userDir = System.getProperty("eclipse.launcher");
		
//		Bundle plugin = Platform.getBundle("tregression");
//		URL url = plugin.getEntry ("/");
//
//		File file = null;
//		try {
//		// Resolve the URL
//			URL resolvedURL = Platform.resolve (url);
//			file = new File (resolvedURL.getFile ());
//		} catch (Exception e) {
//		// Something sensible if an error occurs
//		}
//		
//		userDir = file.getAbsolutePath();
//		userDir = userDir.substring(0, userDir.indexOf("..")-1);
		
		String junitDir = userDir + File.separator + "dropins" + File.separator + "junit_lib";
		String junitPath = junitDir + File.separator + "junit.jar";
		String hamcrestCorePath = junitDir + File.separator + "org.hamcrest.core.jar";
		String testRunnerPath = junitDir + File.separator + "testrunner.jar";
		appClassPath.addClasspath(junitPath);
		appClassPath.addClasspath(hamcrestCorePath);
		appClassPath.addClasspath(testRunnerPath);
		
		appClassPath.setJavaHome(TestConfiguration.getJavaHome());
		appClassPath.setWorkingDirectory(workingDir);
		
		appClassPath.setOptionalTestClass(testClass);
		appClassPath.setOptionalTestMethod(testMethod);
		
		appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
		
		return appClassPath;
	}
	
	public RunningResult run(String workingDir, String testClass, String testMethod){
		
		AppJavaClassPath appClassPath = initialize(workingDir, testClass, testMethod);
		
		TestCaseRunner checker = new TestCaseRunner();
		checker.checkValidity(appClassPath);
		
		TraceModelConstructor constructor = new TraceModelConstructor();
		
		List<BreakPoint> executingStatements = checker.collectBreakPoints(appClassPath, true);
		
		Trace trace = null;
		
//		long t1 = System.currentTimeMillis();
//		Trace trace = constructor.constructTraceModel(appClassPath, executingStatements, checker.getStepNum(), false);
//		long t2 = System.currentTimeMillis();
//		int time = (int) ((t2-t1)/1000);
//		trace.setConstructTime(time);
		
		RunningResult rs = new RunningResult(trace, executingStatements);
		return rs;
	}
}

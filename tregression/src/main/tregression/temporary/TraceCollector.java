package tregression.temporary;

import java.io.File;

import sav.commons.TestConfiguration;
import sav.strategies.dto.AppJavaClassPath;
import tregression.junit.TestCaseAnalyzer;
import tregression.junit.TestCaseRunner;

public class TraceCollector {
	public AppJavaClassPath initialize(String workingDir, String testClass, String testMethod){
		AppJavaClassPath appClassPath = new AppJavaClassPath();
		
		String testTargetPath = workingDir + File.pathSeparator + "build-tests";
		appClassPath.addClasspath(testTargetPath);
		
		/**
		 * setting junit lib into classpath
		 */
		String userDir = System.getProperty("user.dir");
		String junitDir = userDir + File.separator + "dropins" + File.separator + "junit_lib";
		String junitPath = junitDir + File.separator + "junit.jar";
		String hamcrestCorePath = junitDir + File.separator + "org.hamcrest.core.jar";
		appClassPath.addClasspath(junitPath);
		appClassPath.addClasspath(hamcrestCorePath);
		
		appClassPath.setJavaHome(TestConfiguration.getJavaHome());
		appClassPath.setWorkingDirectory(workingDir);
		
		appClassPath.setOptionalTestClass(testClass);
		appClassPath.setOptionalTestMethod(testMethod);
		
		appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
		
		return appClassPath;
	}
	
	public void run(String workingDir, String testClass, String testMethod){
		
		AppJavaClassPath appClassPath = initialize(workingDir, testClass, testMethod);
		
		TestCaseRunner checker = new TestCaseRunner();
		checker.checkValidity(appClassPath);
		
		System.currentTimeMillis();
	}
}

package tregression.separatesnapshots;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.util.MicroBatUtil;
import sav.commons.TestConfiguration;
import sav.strategies.dto.AppJavaClassPath;
import tregression.TraceModelConstructor;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TrialGenerator;
import tregression.junit.TestCaseAnalyzer;
import tregression.junit.TestCaseRunner;

public class TraceCollector {
	public AppJavaClassPath initialize(String workingDir, String testClass, String testMethod, Defects4jProjectConfig config){
		AppJavaClassPath appClassPath = new AppJavaClassPath();
		
		String testTargetPath = workingDir + File.separator + config.bytecodeTestFolder;
		String codeTargetPath = workingDir + File.separator + config.bytecodeSourceFolder;
		
		appClassPath.addClasspath(testTargetPath);
		appClassPath.addClasspath(codeTargetPath);
		
		List<String> libJars = findLibJars(workingDir+File.separator+"lib");
		for(String libJar: libJars) {
			appClassPath.addClasspath(libJar);
			appClassPath.addExternalLibPath(libJar);
		}
		
		List<String> compileLibJars = findLibJars(workingDir+File.separator+"compileLib");
		for(String libJar: compileLibJars) {
			appClassPath.addClasspath(libJar);
			appClassPath.addExternalLibPath(libJar);
		}
		
		List<String> extraLibs0 = findLibJars(workingDir+File.separator+config.buildFolder+File.separator+"lib");
		for(String lib: extraLibs0) {
			appClassPath.addClasspath(lib);
			appClassPath.addExternalLibPath(lib);
		}
		
		List<String> extraLibs1 = findLibJars(workingDir+File.separator+config.buildFolder+File.separator+"libs");
		for(String lib: extraLibs1) {
			appClassPath.addClasspath(lib);
			appClassPath.addExternalLibPath(lib);
		}
		
		String parentLibDir = workingDir.substring(0, workingDir.lastIndexOf(File.separator));
		parentLibDir = workingDir.substring(0, parentLibDir.lastIndexOf(File.separator)) + File.separator + "lib";
		List<String> extraLibs2 = findLibJars(parentLibDir);
		for(String lib: extraLibs2) {
			appClassPath.addClasspath(lib);
			appClassPath.addExternalLibPath(lib);
		}
		
		String sourceCodePath = workingDir + File.separator + config.srcSourceFolder;
		appClassPath.setSourceCodePath(sourceCodePath);
		
		String testCodePath = workingDir + File.separator + config.srcTestFolder;
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
		
		userDir = userDir.substring(0, userDir.lastIndexOf(File.separator+"eclipse"));
		
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
	
	public List<String> findLibJars(String workingDir) {
		List<String> libJars = new ArrayList<>();
		
		File file = new File(workingDir);
		if(file.exists() && file.isDirectory()) {
			for(File childFile: file.listFiles()) {
				String childString = childFile.getAbsolutePath();
				if(childFile.isDirectory()) {
					String newWorkingDir = childString;
					List<String> childLibJars = findLibJars(newWorkingDir);
					libJars.addAll(childLibJars);
				}
				else {
					if (childString.endsWith("jar")) {
						libJars.add(childString);
					}					
				}
			}
		}
		return libJars;
	}
	
	public RunningResult preCheck(String workingDir, String testClass, String testMethod, Defects4jProjectConfig config) {
		AppJavaClassPath appClassPath = initialize(workingDir, testClass, testMethod, config);
		
		List<String> libJars = appClassPath.getExternalLibPaths();
		List<String> exlcudes = MicroBatUtil.extractExcludeFiles("", libJars);
		
		TestCaseRunner checker = new TestCaseRunner();
		
		checker.addLibExcludeList(exlcudes);
		List<BreakPoint> executingStatements = checker.collectBreakPoints(appClassPath, true);
		
		if(checker.isOverLong()) {
			System.out.println("The trace is over long!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator.OVER_LONG);
			return rs;
		}
		
		if(checker.isMultiThread()) {
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

	public RunningResult run(RunningResult result){
		
		TraceModelConstructor constructor = new TraceModelConstructor();
		TestCaseRunner checker = result.getChecker();
		
		long t1 = System.currentTimeMillis();
		Trace trace = constructor.constructTraceModel(result.getAppClassPath(), result.getExecutedStatements(), 
				checker.getExecutionOrderList(), checker.getStepNum(), false);
		long t2 = System.currentTimeMillis();
		int time = (int) (t2-t1);
		trace.setConstructTime(time);
		
		result.setRunningTrace(trace);
		return result;
	}

	
}

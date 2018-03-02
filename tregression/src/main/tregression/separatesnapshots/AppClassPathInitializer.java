package tregression.separatesnapshots;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.junit.TestCaseAnalyzer;

public class AppClassPathInitializer {
	public static AppJavaClassPath initialize(String workingDir, TestCase testCase, Defects4jProjectConfig config){
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
		
		for(int i=0; i<config.additionalSourceFolder.size(); i++){
			String relativePath = config.additionalSourceFolder.get(i);
			String path = workingDir + File.separator + relativePath;
			appClassPath.getAdditionalSourceFolders().add(path);
		}
		
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
		
		appClassPath.setJavaHome(config.javaHome);
		appClassPath.setWorkingDirectory(workingDir);
		
		appClassPath.setOptionalTestClass(testCase.testClass);
		appClassPath.setOptionalTestMethod(testCase.testMethod);
		
		appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
		
		/**
		 * setting bcel lib (for instrumentation) into classpath
		 */
		String bcelDir = junitDir + File.separator + "bcel-6.0.jar";
		appClassPath.addClasspath(bcelDir);
		String javassitDir = junitDir + File.separator + "javassist.jar";
		appClassPath.addClasspath(javassitDir);
		
		/**
		 * setting java agent lib 
		 */
		String agentLib = junitDir + File.separator + "instrumentator.jar";
		appClassPath.setAgentLib(agentLib);
		
		return appClassPath;
	}
	
	public static List<String> findLibJars(String workingDir) {
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
}

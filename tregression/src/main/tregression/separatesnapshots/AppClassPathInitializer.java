package tregression.separatesnapshots;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import microbat.util.MicroBatUtil;
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
		
		MicroBatUtil.setSystemJars(appClassPath);
		
		/**
		 * setting junit lib into classpath
		 */
		appClassPath.setJavaHome(config.javaHome);
		appClassPath.setWorkingDirectory(workingDir);
		
		appClassPath.setOptionalTestClass(testCase.testClass);
		appClassPath.setOptionalTestMethod(testCase.testMethod);
		
		appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
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

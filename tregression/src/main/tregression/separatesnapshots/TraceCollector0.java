package tregression.separatesnapshots;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import microbat.codeanalysis.runtime.ExecutionStatementCollector;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.util.JavaUtil;
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
			rs.setFailureType(TrialGenerator0.OVER_LONG);
			return rs;
		}
		
		if(checker.isMultiThread() && !allowMultiThread) {
			System.out.println("It is multi-thread program!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.MULTI_THREAD);
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
		
		InstrumentationExecutor exectuor = new InstrumentationExecutor(appClassPath,
				MicroBatUtil.generateTraceDir(config.projectName, String.valueOf(config.bugID)),
				isBuggy ? "bug" : "fix");
		
		RunningInformation info = exectuor.run();
		if(!info.isExpectedStepsMet()){
			System.out.println("The expected steps are not met by normal run");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.EXPECTED_STEP_NOT_MET);
			return rs;
		}
		
		Trace trace = info.getTrace();
		trace.setSourceVersion(isBuggy);
		
		PreCheckInformation precheckInfo = exectuor.getPrecheckInfo();
		System.out.println("There are " + precheckInfo.getStepNum() + " steps in this trace");
		if(precheckInfo.isOverLong()) {
			System.out.println("The trace is over long!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.OVER_LONG);
			return rs;
		}
		
		if(!precheckInfo.getOverLongMethods().isEmpty()) {
			String method = precheckInfo.getOverLongMethods().get(0);
			System.out.println("Method " + method + " is over long after instrumentation!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.OVER_LONG_INSTRUMENTATION_METHOD);
			return rs;
		}
		
		boolean isMultiThread = precheckInfo.getThreadNum()!=1;
		if(isMultiThread && !allowMultiThread) {
			System.out.println("It is multi-thread program!");
			RunningResult rs = new RunningResult();
			rs.setFailureType(TrialGenerator0.MULTI_THREAD);
			return rs;
		}
		
		trace.setMultiThread(isMultiThread);
		trace.setAppJavaClassPath(appClassPath);
		
		Map<String, String> classNameMap = new HashMap<>();
		Map<String, String> pathMap = new HashMap<>();
		
		for(TraceNode node: trace.getExecutionList()){
			BreakPoint point = node.getBreakPoint();
			attachFullPathInfo(point, appClassPath, classNameMap, pathMap);
		}
		
		RunningResult rs = new RunningResult(trace, null, null, appClassPath);
		rs.setRunningTrace(trace);
		return rs;
	}
	
	public void attachFullPathInfo(BreakPoint point, AppJavaClassPath appClassPath, 
			Map<String, String> classNameMap, Map<String, String> pathMap){
		String relativePath = point.getDeclaringCompilationUnitName().replace(".", File.separator) + ".java";
		List<String> candidateSourceFolders = appClassPath.getAllSourceFolders();
		for(String candidateSourceFolder: candidateSourceFolders){
			String filePath = candidateSourceFolder + File.separator + relativePath;
			if(new File(filePath).exists()){
				point.setFullJavaFilePath(filePath);
			}
		}
		
		//indicate the declaring compilation name is not correct
		if(point.getFullJavaFilePath()==null){
			String canonicalClassName = point.getClassCanonicalName(); 
			String declaringCompilationUnitName = classNameMap.get(canonicalClassName);
			String fullPath = pathMap.get(canonicalClassName);
			
			if(declaringCompilationUnitName==null){
				String packageName = point.getPackageName();
				String packageRelativePath = packageName.replace(".", File.separator);
				for(String candidateSourceFolder: candidateSourceFolders){
					String packageFullPath = candidateSourceFolder + File.separator + packageRelativePath;
					declaringCompilationUnitName = findDeclaringCompilationUnitName(packageFullPath, canonicalClassName);
					if(declaringCompilationUnitName!=null){
						fullPath = candidateSourceFolder + File.separator + 
								declaringCompilationUnitName.replace(".", File.separator) + ".java";
						break;
					}
				}
			}
			
			classNameMap.put(canonicalClassName, declaringCompilationUnitName);
			pathMap.put(canonicalClassName, fullPath);
			
			point.setDeclaringCompilationUnitName(declaringCompilationUnitName);
			point.setFullJavaFilePath(fullPath);
			
			if(fullPath==null){
				System.err.println("cannot find the source code file for " + point);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private String findDeclaringCompilationUnitName(String packagePath, String canonicalClassName) {
		File packageFolder = new File(packagePath);
		
		if(!packageFolder.exists()){
			return null;
		}
		
		Collection javaFiles = FileUtils.listFiles(packageFolder, new String[]{"java"}, false);;
		for(Object javaFileObject: javaFiles){
			String javaFile = ((File)javaFileObject).getAbsolutePath();
			CompilationUnit cu = JavaUtil.parseCompilationUnit(javaFile);
			TypeNameFinder finder = new TypeNameFinder(cu, canonicalClassName);
			cu.accept(finder);
			if(finder.isFind){
				return JavaUtil.getFullNameOfCompilationUnit(cu);
			}
		}
		
		return null;
	}

	class TypeNameFinder extends ASTVisitor{
		CompilationUnit cu;
		boolean isFind = false;
		String canonicalClassName;

		public TypeNameFinder(CompilationUnit cu, String canonicalClassName) {
			super();
			this.cu = cu;
			this.canonicalClassName = canonicalClassName;
		}
		
		public boolean visit(TypeDeclaration type){
			String simpleName = canonicalClassName;
			if(canonicalClassName.contains(".")){
				simpleName = canonicalClassName.substring(
						canonicalClassName.lastIndexOf(".")+1, canonicalClassName.length());
			}
			if(type.getName().getFullyQualifiedName().equals(simpleName)){
				this.isFind = true;
			}
			
			return false;
		}
		
	}
	
}

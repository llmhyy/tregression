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
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.util.JavaUtil;
import microbat.util.MicroBatUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;

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
		
		InstrumentationExecutor exectuor = new InstrumentationExecutor(appClassPath, generateTraceDir(config),
				isBuggy ? "bug" : "fix");
		Trace trace = exectuor.run();
		
		PreCheckInformation precheckInfo = exectuor.getPrecheckInfo();
		if(precheckInfo.isOverLong()) {
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
		
		Map<String, String> classNameMap = new HashMap<>();
		
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
				String canonicalClassName = point.getClassCanonicalName(); 
				String declaringCompilationUnitName = classNameMap.get(canonicalClassName);
				
				if(declaringCompilationUnitName==null){
					String packageName = point.getPackageName();
					String packageRelativePath = packageName.replace(".", File.separator);
					String sourcePackagePath = appClassPath.getSoureCodePath() + File.separator + packageRelativePath;
					String testPackagePath = appClassPath.getTestCodePath() + File.separator + packageRelativePath;
					
					declaringCompilationUnitName = findDeclaringCompilationUnitName(sourcePackagePath, canonicalClassName);
					if(declaringCompilationUnitName==null){
						declaringCompilationUnitName = findDeclaringCompilationUnitName(testPackagePath, canonicalClassName);
					}
				}
				classNameMap.put(canonicalClassName, declaringCompilationUnitName);
				
				if(declaringCompilationUnitName!=null){
					point.setDeclaringCompilationUnitName(declaringCompilationUnitName);
					relativePath = point.getDeclaringCompilationUnitName().replace(".", File.separator) + ".java";
					sourcePath = appClassPath.getSoureCodePath() + File.separator + relativePath;
					testPath = appClassPath.getSoureCodePath() + File.separator + relativePath;
					
					if(new File(sourcePath).exists()) {
						point.setFullJavaFilePath(sourcePath);
					}
					else if(new File(testPath).exists()) {
						point.setFullJavaFilePath(testPath);
					}
				}
				else{
					System.err.println("cannot find the source code file for " + point);					
				}
			}
		}
		
		RunningResult rs = new RunningResult(trace, null, null, appClassPath);
		rs.setRunningTrace(trace);
		return rs;
	}

	@SuppressWarnings("rawtypes")
	private String findDeclaringCompilationUnitName(String packagePath, String canonicalClassName) {
		File packageFolder = new File(packagePath);
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

	private String generateTraceDir(Defects4jProjectConfig config) {
		String traceFolder = sav.common.core.utils.FileUtils.getFilePath(MicroBatUtil.getTraceFolder(), config.projectName, 
				String.valueOf(config.bugID));
		sav.common.core.utils.FileUtils.createFolder(traceFolder);
		return traceFolder;
	}
	
}

package tregression.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import jmutation.execution.ProjectExecutor;
import jmutation.model.ExecutionResult;
import jmutation.model.MicrobatConfig;
import jmutation.model.PrecheckExecutionResult;
import jmutation.model.Project;
import jmutation.model.ProjectConfig;
import jmutation.model.TestCase;
import jmutation.model.ast.JdtMethodRetriever;
import jmutation.parser.ProjectParser;
import microbat.Activator;
import microbat.model.trace.Trace;
import microbat.util.JavaUtil;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.DiffMatcher;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public class RegressionBugHandler extends AbstractHandler {

	private BuggyTraceView buggyView;
	private CorrectTraceView correctView;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavaUtil.sourceFile2CUMap.clear();
		Job job = new Job("Testing Tregression") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				final String pathSeperator = "\\";
				
				final String projectPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.REPO_PATH);
				final String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
				final String bugID = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
				final String testCaseName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.TEST_CASE);
				
				final String buggyPath = projectPath + pathSeperator + projectName + pathSeperator + bugID + pathSeperator + "ric";
				final String correctPath = projectPath + pathSeperator + projectName + pathSeperator + bugID + pathSeperator + "rfc";
				
				final String dropinPath = "C:\\Users\\arkwa\\git\\java-mutation-framework\\lib";
				
				final String microbatConfigPath = "C:\\Users\\arkwa\\git\\java-mutation-framework\\sampleMicrobatConfig.json";
				ProjectConfig config_bug = new ProjectConfig(buggyPath, dropinPath);
				Project project_bug = config_bug.getProject();
				List<TestCase> testCases = project_bug.getTestCases();
				
				MicrobatConfig microbatConfig = MicrobatConfig.parse(microbatConfigPath, projectPath);
				
				for (TestCase testCase : testCases) {
					
					if (testCase.qualifiedName().equals(testCaseName)) {
						ProjectExecutor projectExecutor = new ProjectExecutor(microbatConfig, config_bug);
						ExecutionResult result = projectExecutor.exec(testCase);
						
					}
//					ProjectExecutor projectExecutor = new ProjectExecutor(microbatConfig, config_bug);
//					PrecheckExecutionResult precheckExecutionResult = projectExecutor.execPrecheck(testCase);
//					if (precheckExecutionResult.isOverLong()) {
//						throw new RuntimeException("TestCase trace is over long: " + precheckExecutionResult.getTotalSteps() + "/" + microbatConfig.getStepLimit());
//					}
//					System.out.println("Normal precheck done");
//					
//					ExecutionResult result = projectExecutor.exec(testCase);
//					if (!result.isSuccessful()) {
						
//					}
				}
				
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
//	/**
//     * Returns a list of methods from the String representation of a file.
//     * @param codeContent file content as String
//     * @return List of methods
//     */
//    public List<TestCase> getAllMethod(String codeContent) {
//        // code taken from regminer
//        List<TestCase> methods = new ArrayList<>();
//        JdtMethodRetriever retriever = new JdtMethodRetriever();
//        CompilationUnit unit = parseCompilationUnit(codeContent);
//        unit.accept(retriever);
//        List<MethodDeclaration> methodNodes = retriever.getMethods();
//        PackageDeclaration packageDeclaration = unit.getPackage();
//        String className;
//        if (packageDeclaration == null) {
//            className = retriever.getClassName();
//        } else {
//            className = unit.getPackage().getName() + "." + retriever.getClassName();
//        }
//        for (MethodDeclaration node : methodNodes) {
//            if (!(node.getParent().getParent() instanceof CompilationUnit) ){
//                continue;
//            }
//            if (isIgnoredMethod(node) || !isTestMethod(node)) {
//                // skip nodes with @Ignore annotation
//                // skip nodes without @Test annotation
//                continue;
//            }
//
//            String simpleName = node.getName().toString();
//            StringJoiner sj = new StringJoiner(",", simpleName + "(", ")");
//            node.parameters().stream().forEach(param -> sj.add(param.toString()));
//            String signature = sj.toString();
//
//            int startLine = unit.getLineNumber(node.getStartPosition()) - 1;
//            int endLine = unit.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
//            methods.add(new TestCase(signature, startLine, endLine, simpleName, className, node));
//        }
//        return methods;
//    }
//    
//    private static boolean isIgnoredMethod(MethodDeclaration node) {
//        return matchAnnotation(node, "@Ignore");
//    }
//
//    private static boolean isTestMethod(MethodDeclaration node) {
//        return matchAnnotation(node, "@Test");
//    }
//    
//    private static boolean matchAnnotation(MethodDeclaration node, String annotation) {
//        return node.modifiers().stream().filter(mod -> mod instanceof Annotation).anyMatch(an -> an.toString().equals(annotation));
//    }
//    
//    public static CompilationUnit parseCompilationUnit(String fileContent) {
//
//        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // handles JDK 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
//        parser.setSource(fileContent.toCharArray());
//        parser.setResolveBindings(true);
//        // In order to parse 1.6 code, some compiler options need to be set to 1.6
//        Map<String, String> options = JavaCore.getOptions();
//        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
//        parser.setCompilerOptions(options);
//
//        CompilationUnit result = (CompilationUnit) parser.createAST(null);
//        return result;
//    }
	
	private void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					buggyView = (BuggyTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
					correctView = (CorrectTraceView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
				} catch (PartInitException e) {
					buggyView = null;
					correctView = null;
					System.out.println("Fail to get the view");
				}
			}
		});
	}
	
	private void updateView(final Trace buggyTrace, final Trace correctTrace, final tregression.model.PairList pairListTregression, final DiffMatcher matcher) {
		if (this.buggyView != null && this.correctView != null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					buggyView.setMainTrace(buggyTrace);
					buggyView.updateData();
					buggyView.setPairList(pairListTregression);
					buggyView.setDiffMatcher(matcher);
					
					correctView.setMainTrace(correctTrace);
					correctView.updateData();
					correctView.setPairList(pairListTregression);
					correctView.setDiffMatcher(matcher);
				}
			});
		} else {
			System.out.println("buggyView or correctView is null");
		}
	}
	
	
}

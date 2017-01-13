package tregression.handler;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import microbat.util.JavaUtil;
import microbat.util.Settings;
import tregression.io.ExcelReporter;
import tregression.io.IgnoredTestCaseFiles;
import tregression.junit.ParsedTrials;
import tregression.junit.TestCaseAnalyzer;

public class EvaluationAllHandler extends AbstractHandler {

	private IgnoredTestCaseFiles ignoredTestCaseFiles;
	private ParsedTrials parsedTrials;
	
	private int trialNumPerTestCase = 3;
	private double[] unclearRates = {0};
	
	
	private boolean isLimitTrialNum = false;
	private int optionSearchLimit = 100;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ignoredTestCaseFiles = new IgnoredTestCaseFiles();
		parsedTrials = new ParsedTrials();
		
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				TestCaseAnalyzer analyzer = new TestCaseAnalyzer();
				
				try {
					ExcelReporter reporter = new ExcelReporter(Settings.projectName, unclearRates);
					IPackageFragmentRoot testRoot = JavaUtil.findTestPackageRootInProject();
					
					for(IJavaElement element: testRoot.getChildren()){
						if(element instanceof IPackageFragment){
							analyzer.runEvaluation((IPackageFragment)element, reporter, isLimitTrialNum, 
									ignoredTestCaseFiles, parsedTrials, trialNumPerTestCase, unclearRates, 
									optionSearchLimit, monitor);				
						}
					}
				} catch (JavaModelException | IOException e) {
					e.printStackTrace();
				}
				
				
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}

	
//	private void archievedSimulation(){
//		SimulatedMicroBat simulator = new SimulatedMicroBat();
//		try {
//			simulator.startSimulation();
//		} catch (GenerateRootCauseException e) {
//			e.printStackTrace();
//		}
//	}
}

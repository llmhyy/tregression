package tregression.handler;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaModelException;

import microbat.util.Settings;
import tregression.io.ExcelReporter;
import tregression.io.IgnoredTestCaseFiles;
import tregression.junit.ParsedTrials;
import tregression.junit.TestCaseAnalyzer;

public class EvaluationTestCaseHandler extends AbstractHandler {

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
					
//					String testCase = "org.apache.commons.math.linear.SparseFieldMatrixTest#testScalarAdd";
					String testCase = "org.apache.commons.math.ode.events.EventStateTest#closeEvents";
//					String testCase = "org.apache.commons.math.analysis.integration.RombergIntegratorTest#testSinFunction";
					String testClassName = testCase.substring(0, testCase.indexOf("#"));
					String testMethodName = testCase.substring(testCase.indexOf("#")+1);
					
					analyzer.runEvaluationForSingleTestCase(testClassName, testMethodName, reporter, isLimitTrialNum,
							ignoredTestCaseFiles, parsedTrials, trialNumPerTestCase, unclearRates, optionSearchLimit, monitor);
				} catch (JavaModelException | IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}

}

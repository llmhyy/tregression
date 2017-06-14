package tregression.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.PathConfiguration;
import tregression.separatesnapshots.RunningResult;
import tregression.separatesnapshots.TraceCollector;

public class SeparateVersionHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Do evaluation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				TraceCollector collector = new TraceCollector();
				
				try {
					TestCase tc = retrieveD4jFailingTestCase(PathConfiguration.buggyPath);
					
					RunningResult buggyRS = collector.run(PathConfiguration.buggyPath, tc.testClass, tc.testMethod);
					RunningResult correctRs = collector.run(PathConfiguration.fixPath, tc.testClass, tc.testMethod);
					
					DiffMatcher diffMatcher = new DiffMatcher("source", 
							PathConfiguration.buggyPath+File.separator+"source", 
							PathConfiguration.fixPath+File.separator+"source");
					diffMatcher.matchCode();
					
					//TODO match trace
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
		
		return null;
	}

	class TestCase{
		public String testClass;
		public String testMethod;
		public TestCase(String testClass, String testMethod) {
			super();
			this.testClass = testClass;
			this.testMethod = testMethod;
		}
	}
	
	public TestCase retrieveD4jFailingTestCase(String buggyVersionPath) throws IOException{
		String failingFile = buggyVersionPath + File.separator + "failing_tests";
		File file = new File(failingFile);
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		reader.close();
		
		String testClass = line.substring(line.indexOf(" ")+1, line.indexOf("::"));
		String testMethod = line.substring(line.indexOf("::")+2, line.length());
		
		TestCase tc = new TestCase(testClass, testMethod);
		return tc;
	}

}

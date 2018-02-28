package tregression.handler;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import microbat.Activator;
import microbat.agent.ExecTraceFileReader;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.model.trace.Trace;
import microbat.util.MicroBatUtil;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.views.Visualizer;

public class RegressionRetrieveHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String buggyPath = PathConfiguration.getBuggyPath();
		String fixPath = PathConfiguration.getCorrectPath();
		
		String projectName = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.PROJECT_NAME);
		String id = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUG_ID);
		
		Defects4jProjectConfig config = Defects4jProjectConfig.getD4JConfig(projectName, Integer.valueOf(id));
		
		DiffMatcher diffMatcher = new DiffMatcher(config.srcSourceFolder, config.srcTestFolder, buggyPath, fixPath);
		diffMatcher.matchCode();
		
		try {
			List<TestCase> list = new TrialGenerator().retrieveD4jFailingTestCase(buggyPath);
			TestCase tc = list.get(0);		
			
			AppJavaClassPath buggyApp = AppClassPathInitializer.initialize(buggyPath, tc, config);
			AppJavaClassPath fixApp = AppClassPathInitializer.initialize(fixPath, tc, config);
			
//			Regression regression = new RegressionRetriever().retriveRegression(projectName, id);
			Regression regression = retrieveRegression(config, buggyPath, fixPath);
			
			Trace buggyTrace = regression.getBuggyTrace();
			buggyTrace.setAppJavaClassPath(buggyApp);
			Trace correctTrace = regression.getCorrectTrace();
			correctTrace.setAppJavaClassPath(fixApp);
			
//			PairList pairList = regression.getPairList();
			regression.fillMissingInfor(config, buggyPath, fixPath);

			/* PairList */
			System.out.println("start matching trace..., buggy trace length: " + buggyTrace.size()
					+ ", correct trace length: " + correctTrace.size());
			long time1 = System.currentTimeMillis();

			ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
			PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, correctTrace, diffMatcher);
			long time2 = System.currentTimeMillis();
			int matchTime = (int) (time2 - time1);
			System.out.println("finish matching trace, taking " + matchTime + "ms");
			
			Visualizer visualizer = new Visualizer();
			visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		return null;
	}
	
	private Regression retrieveRegression(Defects4jProjectConfig config, String buggyPath, String fixPath) {
		String projectName = config.projectName; 
		String bugId = String.valueOf(config.bugID);
		ExecTraceFileReader execTraceReader = new ExecTraceFileReader();
		String buggyExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "bug");
		Trace buggyTrace = execTraceReader.read(buggyExec);
		String fixExec = InstrumentationExecutor
				.generateTraceFilePath(MicroBatUtil.generateTraceDir(projectName, bugId), "fix");
		Trace fixTrace = execTraceReader.read(fixExec);
		
		Regression regression = new Regression(buggyTrace, fixTrace, null);
		return regression;
	}

}

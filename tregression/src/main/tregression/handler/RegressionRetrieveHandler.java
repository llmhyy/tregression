package tregression.handler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import microbat.Activator;
import microbat.model.trace.Trace;
import sav.strategies.dto.AppJavaClassPath;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.TestCase;
import tregression.empiricalstudy.TrialGenerator;
import tregression.io.RegressionRetriever;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
import tregression.separatesnapshots.AppClassPathInitializer;
import tregression.separatesnapshots.DiffMatcher;
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
			
			Regression regression = new RegressionRetriever().retriveRegression(projectName, id);
			
			Trace buggyTrace = regression.getBuggyTrace();
			buggyTrace.setAppJavaClassPath(buggyApp);
			Trace correctTrace = regression.getCorrectTrace();
			correctTrace.setAppJavaClassPath(fixApp);
			
			PairList pairList = regression.getPairList();
			regression.fillMissingInfor(config, buggyPath, fixPath);
			Visualizer visualizer = new Visualizer();
			visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}	
		
		return null;
	}

}

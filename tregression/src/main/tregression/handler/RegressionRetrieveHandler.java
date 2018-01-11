package tregression.handler;

import java.sql.SQLException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import microbat.Activator;
import microbat.model.trace.Trace;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.Regression;
import tregression.io.RegressionRetriever;
import tregression.model.PairList;
import tregression.preference.TregressionPreference;
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
		Regression regression;
		try {
			regression = new RegressionRetriever().retriveRegression(projectName, id);
			Trace buggyTrace = regression.getBuggyTrace();
			Trace correctTrace = regression.getCorrectTrace();
			PairList pairList = regression.getPairList();
			regression.fillMissingInfor(config, buggyPath, fixPath);
			Visualizer visualizer = new Visualizer();
			visualizer.visualize(buggyTrace, correctTrace, pairList, diffMatcher);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}

}

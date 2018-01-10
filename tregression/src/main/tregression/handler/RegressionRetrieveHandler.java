package tregression.handler;

import java.io.File;
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
		String buggyPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.BUGGY_PATH);
		String fixPath = Activator.getDefault().getPreferenceStore().getString(TregressionPreference.CORRECT_PATH);
		
		int startIndex = buggyPath.indexOf(File.separator, buggyPath.indexOf("bug_repo")+1);
		String projectName = buggyPath.substring(startIndex+1, buggyPath.indexOf(File.separator, startIndex+1));
		
		startIndex = buggyPath.indexOf(File.separator, buggyPath.indexOf(projectName)+1);
		String id = buggyPath.substring(startIndex+1, buggyPath.indexOf(File.separator, startIndex+1));
		
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

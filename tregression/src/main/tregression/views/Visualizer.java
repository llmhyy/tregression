package tregression.views;

import org.eclipse.swt.widgets.Display;

import microbat.model.trace.Trace;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class Visualizer {
	public void visualize(final Trace killingMutatantTrace, final Trace correctTrace, 
			final PairList pairList, final DiffMatcher diffMatcher) {

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				CorrectTraceView correctView = EvaluationViews.getBeforeTraceView();
				correctView.setTrace(correctTrace);
				correctView.updateData();
				correctView.setPairList(pairList);

				BuggyTraceView buggyView = EvaluationViews.getAfterTraceView();
				buggyView.setTrace(killingMutatantTrace);
				buggyView.updateData();
				buggyView.setPairList(pairList);
				buggyView.setDiffMatcher(diffMatcher);
			}

		});

	}
}

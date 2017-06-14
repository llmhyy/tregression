package tregression.views;

import org.eclipse.swt.widgets.Display;

import microbat.model.trace.Trace;
import tregression.model.PairList;

public class Visualizer {
	public void visualize(final Trace killingMutatantTrace, final Trace correctTrace, final PairList pairList) {

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				BeforeTraceView beforeView = EvaluationViews.getBeforeTraceView();
				beforeView.setTrace(correctTrace);
				beforeView.updateData();
				beforeView.setPairList(pairList);

				AfterTraceView afterView = EvaluationViews.getAfterTraceView();
				afterView.setTrace(killingMutatantTrace);
				afterView.updateData();
				afterView.setPairList(pairList);
			}

		});

	}
}

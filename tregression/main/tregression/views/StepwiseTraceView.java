package tregression.views;

import org.eclipse.jface.action.Action;

import microbat.views.TraceView;

public class StepwiseTraceView extends TregressionTraceView {

	public static final String ID = "tregression.evalView.stepwiseTraceView";

	public StepwiseTraceView() {

	}

	@Override
	protected Action createControlMendingAction() {
		// TODO Auto-generated method stub
		Action action = new Action() {
			public void run() {
				
			}
		};
		return action;
	}
	
	// functionality for visualisation:
	// when line is selected in the list,
	// we want to display the comparison
	// basically -> what happens when we fire the
	// line event in
	// what diff should be shown?
	// should be equivalent to selecting the
	// line in tregression trace view
	

}

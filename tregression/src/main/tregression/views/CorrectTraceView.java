package tregression.views;

import microbat.model.trace.TraceNode;
import microbat.views.DebugFeedbackView;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import tregression.model.PairList;

public class CorrectTraceView extends TraceView {

	public static final String ID = "tregression.evalView.correctTraceView";
	
	private PairList pairList;
	
	public CorrectTraceView() {
	}

	@Override
	protected void otherViewsBehavior(TraceNode node) {
		
		if(this.refreshProgramState){
			DebugFeedbackView feedbackView = MicroBatViews.getDebugFeedbackView();
			feedbackView.setTraceView(CorrectTraceView.this);
			feedbackView.refresh(node);			
		}
		
		markJavaEditor(node);
	}

	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}

}

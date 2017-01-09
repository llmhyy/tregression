package tregression.views;

import microbat.model.trace.TraceNode;
import microbat.views.DebugFeedbackView;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class AfterTraceView extends TraceView {

	private PairList pairList;
	
	public AfterTraceView() {
	}

	@Override
	protected void otherViewsBehavior(TraceNode node) {
		if(this.refreshProgramState){
			DebugFeedbackView feedbackView = MicroBatViews.getDebugFeedbackView();
			feedbackView.setTraceView(AfterTraceView.this);
			feedbackView.refresh(node);			
		}
		
		TraceNodePair pair = pairList.findByMutatedNode(node);
		
		if(pair != null){
			TraceNode originalNode = pair.getOriginalNode();
			
			if(originalNode != null){
				BeforeTraceView view = EvaluationViews.getBeforeTraceView();	
				view.jumpToNode(view.getTrace(), originalNode.getOrder(), false);
			}
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

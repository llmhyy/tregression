package tregression.auto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class CarelessAutoFeedbackAgent extends AutoFeedbackAgent {

	protected double wrongProb;

	public CarelessAutoFeedbackAgent(EmpiricalTrial trail) {
		this(trail, 0.05d);
	}

	public CarelessAutoFeedbackAgent(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher,
			RootCauseFinder finder) {
		this(buggyTrace, correctTrace, pairList, matcher, finder, 0.05d);
	}

	public CarelessAutoFeedbackAgent(EmpiricalTrial trial, final double wrongProb) {
		super(trial);
		this.wrongProb = wrongProb;
	}

	public CarelessAutoFeedbackAgent(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher,
			RootCauseFinder finder, double wrongProb) {
		super(buggyTrace, correctTrace, pairList, matcher, finder);
		this.wrongProb = wrongProb;
	}

	public DPUserFeedback giveFeedback(final TraceNode node, final Trace trace) {
		List<DPUserFeedback> possibleFeedbacks = this.getPossibleFeedbacks(node, trace);
		DPUserFeedback gtFeedback = super.giveGTFeedback(node);

		if (possibleFeedbacks.size() <= 1 || Math.random() > this.wrongProb) {
			return gtFeedback;
		}
		possibleFeedbacks.remove(gtFeedback);

		Random random = new Random();
		int randomIdx = random.nextInt(possibleFeedbacks.size());
		return possibleFeedbacks.get(randomIdx);
	}

	protected List<DPUserFeedback> getPossibleFeedbacks(final TraceNode node, final Trace trace) {
		List<DPUserFeedback> feedbacks = new ArrayList<>();
		
		DPUserFeedback controlFeedback = new DPUserFeedback(DPUserFeedbackType.WRONG_PATH, node);
		feedbacks.add(controlFeedback);
		
		for (VarValue readVar : node.getReadVariables()) {
			DPUserFeedback feedback = new DPUserFeedback(DPUserFeedbackType.WRONG_VARIABLE, node);
			feedback.addWrongVar(readVar);
		}
//		UserFeedback controlFeedback = new UserFeedback(UserFeedback.WRONG_PATH);
//		feedbacks.add(controlFeedback);

//		// Data Slicing
//		for (VarValue readVar : node.getReadVariables()) {
//			UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
//			feedback.setOption(new ChosenVariableOption(readVar, null));
//			if (TraceUtil.findNextNode(node, feedback, trace) != null) {				
//				feedbacks.add(feedback);
//			}
//		}
		

		return feedbacks;
	}

}

package tregression.auto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class CarelessAutoFeedbackAgent extends AutoFeedbackAgent {

	protected double wrongProb;

	public CarelessAutoFeedbackAgent(EmpiricalTrial trail) {
		this(trail, 0.1d);
	}

	public CarelessAutoFeedbackAgent(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher,
			RootCauseFinder finder) {
		this(buggyTrace, correctTrace, pairList, matcher, finder, 0.1d);
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

	@Override
	public UserFeedback giveFeedback(final TraceNode node) {
		List<UserFeedback> possibleFeedbacks = this.getPossibleFeedbacks(node);
		UserFeedback gtFeedback = super.giveFeedback(node);

		if (possibleFeedbacks.size() <= 1 || Math.random() > this.wrongProb) {
			return gtFeedback;
		}
		possibleFeedbacks.remove(gtFeedback);

		Random random = new Random();
		int randomIdx = random.nextInt(possibleFeedbacks.size());
		return possibleFeedbacks.get(randomIdx);
	}

	protected List<UserFeedback> getPossibleFeedbacks(final TraceNode node) {
		List<UserFeedback> feedbacks = new ArrayList<>();

		// Control Slicing
		if (node.getControlDominator() != null) {
			UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
			feedbacks.add(feedback);
		}

		// Data Slicing
		for (VarValue readVar : node.getReadVariables()) {
			UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
			feedback.setOption(new ChosenVariableOption(readVar, null));
		}

		return feedbacks;
	}

}

package tregression.autofeedback;

import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;

/**
 * RandomFeedbackGenerator is one of the feedback generator and it randomly give feedback
 * @author David
 *
 */
public final class RandomFeedbackGenerator extends FeedbackGenerator {

	/**
	 * Probability that the node is CORRECT
	 */
	private final double correctProb;
	
	/**
	 * Probability that the node is CONTROL_INCORRECT
	 */
	private final double controlIncorrectProb;
	
	public RandomFeedbackGenerator(Trace trace, AutoFeedbackMethod method) {
		this(trace, method, 0.1, 0.5);
	}
	
	public RandomFeedbackGenerator(Trace trace, AutoFeedbackMethod method, double correctProb, double controlIncorrectProb) {
		super(trace, method);
		this.correctProb = correctProb;
		this.controlIncorrectProb = controlIncorrectProb;
	}

	/**
	 * Give random feedback for the given trace node.
	 * It will not give UNCLEAR feedback.
	 * It will not give CORRECT feedback to the last trace node, which is assumed to be wrong.
	 */
	@Override
	public UserFeedback giveFeedback(TraceNode node) {

		UserFeedback feedback = null;
		
		final int readVarCount = node.getReadVariables().size();
		final boolean haveControlDom = node.getControlDominator() != null;
		
		/**
		 * Guess the node is correct or not.
		 * Note that the last node in trace will not be correct.
		 */
		if (node != this.trace.getLatestNode()) {
			if (this.guessIsCorrect()) {
				feedback = this.genCorrectFeedback();
				this.printFeedbackMessage(node, feedback);
				return feedback;
			}
		}
		
		/**
		 * If the node do not have any read variables
		 * or control dominator, no feedback can be given
		 */
		if (readVarCount == 0 && !haveControlDom) {
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		/*
		 * If the node do not have read variable, then
		 * the feedback back will be Control Incorrect.
		 */
		if (readVarCount == 0) {
			feedback = this.genCIFeedback();
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		/*
		 * If the node do not have control dominator,
		 * then the feedback back will be Data Incorrect.
		 */
		if (!haveControlDom) {
			feedback = this.genRandDIFeedback(node);
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		/*
		 * Guess if the node Control Incorrect
		 */
		if (this.guessIsControlIncorrect()) {
			feedback = this.genCIFeedback();
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		/*
		 * Define the feedback to be Data Incorrect
		 */
		VarValue wrongVar = this.getRandVar(node.getReadVariables(), false);
		feedback = this.genDIFeedback(wrongVar, null);
		this.printFeedbackMessage(node, feedback);
		return feedback;
	}
	
	/**
	 * Randomly guess is the statement correct or not
	 * @return True if the statement is correct
	 */
	private boolean guessIsCorrect() {
		return Math.random() < this.correctProb;
	}
	
	/**
	 * Randomly guess is the statement control incorrect or not
	 * @return True if the statement is control incorrect
	 */
	private boolean guessIsControlIncorrect() {
		return Math.random() < this.controlIncorrectProb;
	}

}
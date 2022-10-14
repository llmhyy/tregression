package tregression.autofeedback;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.recommendation.UserFeedback;

public final class NaiveFeedbackGenerator extends FeedbackGenerator {

	/**
	 * Feedback of previous node in trace. Need to be initialize by users.
	 */
	private final String initFeedbackType;
	
	/**
	 * Constructor
	 * @param trace	Target trace of the program
	 * @param method Debugging method
	 * @param initFeedbackType Initial feedback for naive feedback. It must be either WRONG_PATH or WRONG_VARIABLE_VALUE
	 */
	public NaiveFeedbackGenerator(Trace trace, AutoFeedbackMethod method, String initFeedbackType) {
		super(trace, method);
		if (initFeedbackType == UserFeedback.WRONG_PATH || initFeedbackType == UserFeedback.WRONG_VARIABLE_VALUE) {
			this.initFeedbackType = initFeedbackType;
		} else {
			throw new IllegalArgumentException("NaiveFeedbackGenerator can only accept initial feedback as WRONG_PATH or WRONG_VARIABLE_VALUE. However, " + initFeedbackType + " is given");
		}
		
	}

	@Override
	public UserFeedback giveFeedback(TraceNode node) {
		
		UserFeedback feedback = null;
		
		final int readVarCount = node.getReadVariables().size();
		final boolean haveControlDom = node.getControlDominator() != null;
		
		if (readVarCount == 0 && !haveControlDom) {
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		if (readVarCount == 0) {
			feedback = this.genCIFeedback();
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		if (!haveControlDom) {
			feedback = this.genRandDIFeedback(node);
			this.printFeedbackMessage(node, feedback);
			return feedback;
		}
		
		if (this.initFeedbackType == UserFeedback.WRONG_PATH) {
			feedback = this.genCIFeedback();
		} else if (this.initFeedbackType == UserFeedback.WRONG_VARIABLE_VALUE) {
			feedback = this.genRandDIFeedback(node);
		}
		
		this.printFeedbackMessage(node, feedback);
		return feedback;
	}
}
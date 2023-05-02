package tregression.autofeedback;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class AutoFeedbackAgent {
	
	private final StepChangeTypeChecker typeChecker;
	private final PairList pairList;
	private final DiffMatcher matcher;
	private final RootCauseFinder finder;
	
	
	public AutoFeedbackAgent(EmpiricalTrial trail) {
		final Trace buggyTrace = trail.getBuggyTrace();
		final Trace correctTrace = trail.getFixedTrace();
		this.typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		this.pairList = trail.getPairList();
		this.matcher = trail.getDiffMatcher();
		this.finder = trail.getRootCauseFinder();
	}
	
	public UserFeedback giveFeedback(final TraceNode node) {
		StepChangeType type = this.typeChecker.getType(node, true, this.pairList, this.matcher);
		return this.typeToFeedback(type, node, true, this.finder);
	}
	
	/**
	 * Convert the StepChangeType to UserFeedback
	 * @param type StepChangeType to be converted 
	 * @param node Variable needed for getting wrong variable
	 * @param isOnBefore Variable needed for getting wrong variable
	 * @param finder Variable needed for getting wrong variable
	 * @return Converted UserFeedback
	 */
	private UserFeedback typeToFeedback(StepChangeType type, TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
		UserFeedback feedback = new UserFeedback();
		switch(type.getType()) {
		case StepChangeType.IDT:
			feedback.setFeedbackType(UserFeedback.CORRECT);
			break;
		case StepChangeType.CTL:
			feedback.setFeedbackType(UserFeedback.WRONG_PATH);
			break;
		case StepChangeType.DAT:
			feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
			VarValue wrongVar = type.getWrongVariable(node, isOnBefore, finder);
			feedback.setOption(new ChosenVariableOption(wrongVar, null));
			break;
		case StepChangeType.SRC:
			feedback.setFeedbackType(UserFeedback.UNCLEAR);
			break;
		}
		return feedback;
	}
}

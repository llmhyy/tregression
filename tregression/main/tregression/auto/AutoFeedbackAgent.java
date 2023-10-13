package tregression.auto;

import java.util.List;
import java.util.Map;

import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import sav.common.core.Pair;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class AutoFeedbackAgent {
	
	protected final StepChangeTypeChecker typeChecker;
	protected final PairList pairList;
	protected final DiffMatcher matcher;
	protected final RootCauseFinder finder;
	
	public AutoFeedbackAgent(EmpiricalTrial trail) {
		final Trace buggyTrace = trail.getBuggyTrace();
		final Trace correctTrace = trail.getFixedTrace();
		this.typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		this.pairList = trail.getPairList();
		this.matcher = trail.getDiffMatcher();
		this.finder = trail.getRootCauseFinder();
	}
	
	public AutoFeedbackAgent(final Trace buggyTrace, final Trace correctTrace, final PairList pairList, final DiffMatcher matcher, final RootCauseFinder finder) {
		this.typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		this.pairList = pairList;
		this.matcher = matcher;
		this.finder = finder;
	}
	
	public DPUserFeedback giveGTFeedback(final TraceNode node) {
		StepChangeType type = this.typeChecker.getType(node, true, this.pairList, this.matcher);
		return this.typeToFeedback(type, node, true, this.finder);
	}
	
//	/**
//	 * Get the ground truth feedback of the given node
//	 * @param node Target node
//	 * @return Ground truth feedback
//	 */
//	public UserFeedback giveGTFeedback(final TraceNode node) {
//		StepChangeType type = this.typeChecker.getType(node, true, this.pairList, this.matcher);
//		return this.typeToFeedback(type, node, true, this.finder);
//	}
	
	
	public StepChangeType getStepChangeType(final TraceNode node) {
		return this.typeChecker.getType(node, true, this.pairList, this.matcher);
	}
	
//	/**
//	 * Convert the StepChangeType to UserFeedback
//	 * @param type StepChangeType to be converted 
//	 * @param node Variable needed for getting wrong variable
//	 * @param isOnBefore Variable needed for getting wrong variable
//	 * @param finder Variable needed for getting wrong variable
//	 * @return Converted UserFeedback
//	 */
//	private UserFeedback typeToFeedback(StepChangeType type, TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
//		UserFeedback feedback = new UserFeedback();
//		switch(type.getType()) {
//		case StepChangeType.IDT:
//			feedback.setFeedbackType(UserFeedback.CORRECT);
//			break;
//		case StepChangeType.CTL:
//			feedback.setFeedbackType(UserFeedback.WRONG_PATH);
//			break;
//		case StepChangeType.DAT:
//			feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
//			final List<Pair<VarValue, VarValue>> wrongVariableList = type.getWrongVariableList();
//			if (wrongVariableList.size() == 1) {
//				VarValue wrongVar = type.getWrongVariable(node, isOnBefore, finder);
//				feedback.setOption(new ChosenVariableOption(wrongVar, null));
//			} else {
//				// If there are multiple variable to choose, do not pick the "this" variable
//				List<Pair<VarValue, VarValue>> filteredList = wrongVariableList.stream().filter(pair -> !pair.first().isThisVariable()).toList();
//				final VarValue wrongVar = filteredList.get(0).first();
//				feedback.setOption(new ChosenVariableOption(wrongVar, null));
//			}
//			break;
//		case StepChangeType.SRC:
//			feedback.setFeedbackType(UserFeedback.UNCLEAR);
//			break;
//		}
//		return feedback;
//	}
	
	protected DPUserFeedback typeToFeedback(StepChangeType type, TraceNode node, boolean isOnBefore, RootCauseFinder finder) {
		switch (type.getType()) {
		case StepChangeType.IDT:
			return new DPUserFeedback(DPUserFeedbackType.CORRECT, node);
		case StepChangeType.CTL:
			return new DPUserFeedback(DPUserFeedbackType.WRONG_PATH, node);
		case StepChangeType.DAT:
			DPUserFeedback feedback = new DPUserFeedback(DPUserFeedbackType.WRONG_VARIABLE, node);
			final List<VarValue> wrongVars = type.getWrongVariableList().stream().map(pair -> pair.first()).toList();
			for (VarValue readVar : node.getReadVariables()) {
				if (wrongVars.contains(readVar) ) {
					feedback.addWrongVar(readVar);
				} else {
					feedback.addCorrectVar(readVar);
				}
			}
			return feedback;
		case StepChangeType.SRC:
			return new DPUserFeedback(DPUserFeedbackType.ROOT_CAUSE, node);
		default:
			throw new RuntimeException(Log.genMsg(getClass(), "Unhandled step change type: " + type.getType()));	
		}
	}
}

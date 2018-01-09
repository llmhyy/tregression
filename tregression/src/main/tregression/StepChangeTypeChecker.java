package tregression;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import microbat.model.value.VirtualValue;
import microbat.model.variable.Variable;
import tregression.empiricalstudy.MatchStepFinder;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;

public class StepChangeTypeChecker {
	
	private Trace buggyTrace;
	private Trace correctTrace;
	
	
	public StepChangeTypeChecker(Trace buggyTrace, Trace correctTrace) {
		super();
		this.setBuggyTrace(buggyTrace);
		this.setCorrectTrace(correctTrace);
	}

	/**
	 * By default, buggy code is the before/source and the correct code is the
	 * after/target. This convention corresponds to the UI design and defects4j
	 * patches.
	 * 
	 * @param step
	 * @param isOnBeforeTrace
	 * @param pairList
	 * @param matcher
	 * @return
	 */
	public StepChangeType getType(TraceNode step, boolean isOnBeforeTrace,
			PairList pairList, DiffMatcher matcher) {
		
		
		TraceNode matchedStep = MatchStepFinder.findMatchedStep(isOnBeforeTrace, step, pairList);
		
//		if(matchedStep==null) {
//			TraceNode stepOverStep = findPreviousStepOverStepWithSameLine(step);
//			if(stepOverStep!=null) {
//				matchedStep = MatchStepFinder.findMatchedStep(isOnBeforeTrace, stepOverStep, pairList);				
//			}
//		}
		
		boolean isSourceDiff = matcher.checkSourceDiff(step.getBreakPoint(), isOnBeforeTrace);
		if(isSourceDiff){
			return new StepChangeType(StepChangeType.SRC, matchedStep);
		}
		
		
		if (matchedStep == null) {
			return new StepChangeType(StepChangeType.CTL, matchedStep);
		}
		else{
			System.currentTimeMillis();
			List<VarValue> wrongVariableList = checkWrongVariable(isOnBeforeTrace, step, matchedStep, pairList);
			if(wrongVariableList.isEmpty()){
				return new StepChangeType(StepChangeType.IDT, matchedStep);
			}
			else{
				return new StepChangeType(StepChangeType.DAT, matchedStep, wrongVariableList);
			}
		}

	}
	
	/**
	 * When invoking a method m() at line k, we will have two steps running into line k, i.e., a step s_1 
	 * invoking m() and a step s_2 returning from the invocation. Suppose s_1 is matched and s_2 is not, 
	 * we should not check s_2's control dominator, instead, we need to check s_1 instead.
	 * 
	 * In this implementation, we only consider case of non-cascading invocation. In other word, we do not
	 * consider m0(m1(m2(...))). In such case, we need to consider a list of previous-step-over-step.
	 * @param step
	 * @return
	 */
	private TraceNode findPreviousStepOverStepWithSameLine(TraceNode step) {
		TraceNode node = step.getStepOverPrevious();
		if(node != null && node.getLineNumber()==step.getLineNumber()) {
			return node;
		}
		
		return null;
	}

	private List<VarValue> checkWrongVariable(boolean isOnBefore,
			TraceNode thisStep, TraceNode thatStep, PairList pairList) {
		List<VarValue> wrongVariableList = new ArrayList<>();
		for(VarValue readVar: thisStep.getReadVariables()){
			if(!canbeMatched(isOnBefore, readVar, thisStep, thatStep, pairList)){
				wrongVariableList.add(readVar);
			}
		}
		return wrongVariableList;
	}
	
	private Trace getCorrespondingTrace(boolean isOnBeforeTrace, Trace buggyTrace, Trace correctTrace) {
		return isOnBeforeTrace ? buggyTrace : correctTrace;
	}
	
	private Trace getOtherCorrespondingTrace(boolean isOnBeforeTrace, Trace buggyTrace, Trace correctTrace) {
		return !isOnBeforeTrace ? buggyTrace : correctTrace;
	}

	private boolean canbeMatched(boolean isOnBeforeTrace, 
			VarValue thisVar, TraceNode thisStep, TraceNode thatStep, PairList pairList) {
		Trace thisTrace = getCorrespondingTrace(isOnBeforeTrace, buggyTrace, correctTrace);
		Trace thatTrace = getOtherCorrespondingTrace(isOnBeforeTrace, buggyTrace, correctTrace);
		System.currentTimeMillis();
//		boolean containsVirtual = checkReturnVariable(thisStep, thatStep);
		
		List<VarValue> synonymVarList = findSynonymousVarList(thatStep.getReadVariables(), thisVar);
		
		for(VarValue thatVar: synonymVarList){
			TraceNode thisDom = thisTrace.findDataDominator(thisStep, thisVar);
			TraceNode thatDom = thatTrace.findDataDominator(thatStep, thatVar);
			
			if(thatVar instanceof ReferenceValue && thisVar instanceof ReferenceValue) {
//				if(containsVirtual){
//					return true;
//				}
				
				List<TraceNode> thisAssignChain = new ArrayList<>();
				getAssignChain(thisDom, thisVar, thisAssignChain);
				List<TraceNode> thatAssignChain = new ArrayList<>(); 
				getAssignChain(thatDom, thatVar, thatAssignChain);
				
				boolean isAssignChainMatch = isAssignChainMatch(thisAssignChain, thatAssignChain, isOnBeforeTrace, pairList);
				if(isAssignChainMatch){
					return true;
				}
			}
			else {
				String thisString = (thisVar.getStringValue()==null)?"null":thisVar.getStringValue();
				String thatString = (thatVar.getStringValue()==null)?"null":thatVar.getStringValue();
				
				boolean equal = thisString.equals(thatString);
				if(!equal) {
					continue;
				}
				else {
					return equal;					
				}
			}
		}
		
		/**
		 * if a variable does not have corresponding variable in the other trace, we do not record it.
		 */
		return false;
	}

	private boolean isAssignChainMatch(List<TraceNode> thisAssignChain, List<TraceNode> thatAssignChain,
			boolean isOnBeforeTrace, PairList pairList) {
		if(thisAssignChain.size() != thatAssignChain.size()){
			return false;
		}
		
		if(!isOnBeforeTrace){
			List<TraceNode> tmp = null;
			tmp = thisAssignChain;
			thisAssignChain = thatAssignChain;
			thatAssignChain = tmp;
		}
		
		for(int i=0; i<thisAssignChain.size(); i++){
			TraceNode thisDom = thisAssignChain.get(i);
			TraceNode thatDom = thatAssignChain.get(i);
			
			TraceNodePair pair = pairList.findByAfterNode(thatDom);
			if(pair==null){
				return false;
			}
			
			if(pair.getBeforeNode()==null){
				return false;
			}
			
			if(thisDom.getOrder()!=pair.getBeforeNode().getOrder()){
				return false;
			}
		}

		return true;
	}

	private void getAssignChain(TraceNode dom, VarValue var, List<TraceNode> chain) {
		if(dom==null){
			return;
		}
		
		chain.add(dom);
		
		String varID = Variable.truncateSimpleID(var.getVarID());
		for(VarValue readVar: dom.getReadVariables()){
			String readVarID = readVar.getVarID();
			String simpleReadVarID = Variable.truncateSimpleID(readVarID);
			if(varID.equals(simpleReadVarID)){
				TraceNode newDom = dom.findDataDominator(readVar);
				if(!chain.contains(newDom)){
					getAssignChain(newDom, readVar, chain);					
				}
				break;
			}
		}
	}

	private List<VarValue> findSynonymousVarList(List<VarValue> readVariables, VarValue var) {
		List<VarValue> synonymousList = new ArrayList<>();
		for(VarValue readVar: readVariables) {
			if(readVar.getVarName().equals(var.getVarName())) {
				synonymousList.add(readVar);
			}
		}
		return synonymousList;
	}

	private boolean checkReturnVariable(TraceNode thisStep, TraceNode thatStep) {
		boolean isThisStepContainVirtual = false;
		boolean isThatStepContainVirtual = false;
		for(VarValue readVar: thisStep.getReadVariables()){
			if(readVar instanceof VirtualValue){
				isThisStepContainVirtual = true;
			}
		}
		
		for(VarValue readVar: thatStep.getReadVariables()){
			if(readVar instanceof VirtualValue){
				isThatStepContainVirtual = true;
			}
		}
		
		return isThisStepContainVirtual&&isThatStepContainVirtual;
	}

	public Trace getBuggyTrace() {
		return buggyTrace;
	}

	public void setBuggyTrace(Trace buggyTrace) {
		this.buggyTrace = buggyTrace;
	}

	public Trace getCorrectTrace() {
		return correctTrace;
	}

	public void setCorrectTrace(Trace correctTrace) {
		this.correctTrace = correctTrace;
	}
}

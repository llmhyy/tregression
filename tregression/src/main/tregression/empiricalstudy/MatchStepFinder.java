package tregression.empiricalstudy;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class MatchStepFinder {
	public static TraceNode findMatchedStep(boolean isOnBeforeTrace, TraceNode step, PairList pairList){
		TraceNode matchedStep = null;
		
		TraceNodePair pair = null;
		if (isOnBeforeTrace) {
			pair = pairList.findByBeforeNode(step);
			if (pair != null) {
				matchedStep = pair.getAfterNode();
			}

		} else {
			pair = pairList.findByAfterNode(step);
			if (pair != null) {
				matchedStep = pair.getBeforeNode();
			}

		}
		
		return matchedStep;
	}

	public static VarValue findMatchVariable(VarValue readVar, TraceNode matchedStep) {
		for(VarValue var: matchedStep.getReadVariables()){
			if(var.getVarName().equals(readVar.getVarName())){
				return var;
			}
		}
		
		return null;
	}
}

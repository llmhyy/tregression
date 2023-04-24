package tregression.empiricalstudy.solutionpattern.data;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.model.TraceNodePair;

public class MissEvaluedCondition extends PatternDetector {

	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		int breakStepOrder = deadEndRecord.getBreakStepOrder();
		Trace buggyTrace = trial.getBuggyTrace();
		
		TraceNode breakStep = buggyTrace.getTraceNode(breakStepOrder);
		
		if(!breakStep.isConditional()){
			return false;
		}
		
		TraceNodePair pair = trial.getPairList().findByBeforeNode(breakStep);
		if(pair != null && pair.getAfterNode()!=null){
			StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, trial.getFixedTrace());
			StepChangeType diffType = checker.getType(breakStep, true, trial.getPairList(), trial.getDiffMatcher());
			
			if(diffType.getType()==StepChangeType.DAT){
				return true;
			}
		}
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.MISS_EVALUATED_CONDITION);
	}

}

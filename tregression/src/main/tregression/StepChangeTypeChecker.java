package tregression;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class StepChangeTypeChecker {
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
	public StepChangeType getType(TraceNode step, boolean isOnBeforeTrace, PairList pairList, DiffMatcher matcher) {
		
		boolean isSourceDiff = checkSourceDiff(step, isOnBeforeTrace, matcher);
		if(isSourceDiff){
			return new StepChangeType(StepChangeType.SRC);
		}
		
		TraceNode matchedStep = MatchStepFinder.findMatchedStep(isOnBeforeTrace, step, pairList);
		
		if (matchedStep == null) {
			return new StepChangeType(StepChangeType.CTL);
		}
		else{
			List<VarValue> wrongVariableList = checkWrongVariable(step, matchedStep);
			if(wrongVariableList.isEmpty()){
				return new StepChangeType(StepChangeType.IDT);
			}
			else{
				return new StepChangeType(StepChangeType.DAT, wrongVariableList);
			}
		}

	}
	
	private List<VarValue> checkWrongVariable(TraceNode step, TraceNode matchedStep) {
		List<VarValue> wrongVariableList = new ArrayList<>();
		for(VarValue readVar: step.getReadVariables()){
			if(!canbeMatched(readVar, matchedStep)){
				wrongVariableList.add(readVar);
			}
		}
		return wrongVariableList;
	}

	private boolean canbeMatched(VarValue readVar, TraceNode matchedStep) {
		for(VarValue var: matchedStep.getReadVariables()){
			if(var.getVarName().equals(readVar.getVarName())){
				String thisStringValue = (var.getStringValue()==null)?"null":var.getStringValue();
				String thatStringValue = (readVar.getStringValue()==null)?"null":readVar.getStringValue();
				if(thisStringValue.equals(thatStringValue)){
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkSourceDiff(TraceNode step, boolean isOnBeforeTrace, DiffMatcher matcher) {
		if (isOnBeforeTrace) {
			FilePairWithDiff diff = matcher.findDiffBySourceFile(step.getBreakPoint());
			if(diff != null){
				for (DiffChunk chunk : diff.getChunks()) {
					int start = chunk.getStartLineInSource();
					int end = start + chunk.getChunkLengthInSource() - 1;
					int type = findLineChange(step, chunk, start, end, isOnBeforeTrace);
					if(type == StepChangeType.SRC){
						return true;
					}
					else if(type == -1){
						break;
					}
				}
			}
		} else {
			FilePairWithDiff diff = matcher.findDiffByTargetFile(step.getBreakPoint());
			if(diff != null){
				for (DiffChunk chunk : diff.getChunks()) {
					int start = chunk.getStartLineInTarget();
					int end = start + chunk.getChunkLengthInTarget() - 1;
					int type = findLineChange(step, chunk, start, end, isOnBeforeTrace);
					if(type == StepChangeType.SRC){
						return true;
					}
					else if(type == -1){
						break;
					}
				}
				
			}
		}
		
		return false;
	}

	/**
	 * return SRC if the code of step is contained in chunk and the code is a diff
	 * return -1 if the code of step is contained in chunk and the code is not a diff
	 * return -2 if the code of step is not contained in chunk.
	 * 
	 * @param step
	 * @param chunk
	 * @param start
	 * @param end
	 * @return
	 */
	private int findLineChange(TraceNode step, DiffChunk chunk, int start, int end, boolean isOnBeforeTrace) {
		int stepLineNo = step.getBreakPoint().getLineNumber();
		if (start <= stepLineNo && stepLineNo <= end) {
			int count = 0;
			for (int i = 0; i < chunk.getChangeList().size(); i++) {
				LineChange lineChange = chunk.getChangeList().get(i);
				if(isOnBeforeTrace){
					if(lineChange.getType() != LineChange.ADD){
						count++;
					}
				}
				else{
					if(lineChange.getType() != LineChange.REMOVE){
						count++;
					}
				}
				
				int currentLineNo = start + count - 1;
				if (stepLineNo == currentLineNo) {
					if(lineChange.getType() != LineChange.UNCHANGE){
						return StepChangeType.SRC;
					}
					else{
						return -1;
					}
				}
			}
		}
		
		return -2;
	}
}

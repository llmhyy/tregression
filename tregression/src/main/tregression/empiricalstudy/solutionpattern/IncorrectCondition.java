package tregression.empiricalstudy.solutionpattern;

import org.eclipse.swt.dnd.RTFTransfer;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class IncorrectCondition extends PatternDetector{

	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		if(deadEndRecord.getType()==DeadEndRecord.CONTROL){
			return false;
		}
		
		RootCauseNode rootCause = trial.getRealcauseNode();
		if(rootCause.isOnBefore()){
			DiffMatcher matcher = trial.getDiffMatcher();
			for(FilePairWithDiff filePair: matcher.getFileDiffList()){
				for(DiffChunk chunk: filePair.getChunks()){
					boolean ifChanged = isIfChanged(chunk, rootCause.getRoot().getLineNumber());
					if(ifChanged){
						return true;
					}
				}
			}
		}
		
		return false;
	}

	private boolean isIfChanged(DiffChunk chunk, int lineNumber) {
		StringBuffer buffer = new StringBuffer();
		boolean isHit = false;
		for(LineChange lineChange: chunk.getChangeList()){
			if(lineChange.getType()==LineChange.REMOVE){
				String content = lineChange.getLineContent();
				buffer.append(content.substring(1, content.length())+"\n");
				
				int line = chunk.getLineNumberInSource(lineChange);
				if(line==lineNumber){
					isHit = true;
				}
			}
		}
		
		if(isHit){
			String code = buffer.toString();
			return code.contains("if");
		}
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.INCORRECT_CONDITION);
	}
	
}

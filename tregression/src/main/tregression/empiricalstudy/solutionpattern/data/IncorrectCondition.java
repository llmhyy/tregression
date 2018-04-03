package tregression.empiricalstudy.solutionpattern.data;

import java.util.ArrayList;
import java.util.List;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
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
		
		for(RootCauseNode rootCause: trial.getRootCauseFinder().getRealRootCaseList()){
			if(rootCause.isOnBefore()){
				DiffMatcher matcher = trial.getDiffMatcher();
				for(FilePairWithDiff filePair: matcher.getFileDiffList()){
					for(DiffChunk chunk: filePair.getChunks()){
						boolean ifChanged = isIfChanged(chunk, filePair);
						if(ifChanged){
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}

	private boolean isIfChanged(DiffChunk chunk, FilePairWithDiff filePair) {
		StringBuffer buffer = new StringBuffer();
		List<Integer> removedIfs = new ArrayList<>();
		List<Integer> addedIfs = new ArrayList<>();
		for(LineChange lineChange: chunk.getChangeList()){
			if(lineChange.getType()==LineChange.REMOVE){
				String content = lineChange.getLineContent();
				buffer.append(content.substring(1, content.length())+"\n");
				
				if(content.contains("if")){
					int line = chunk.getLineNumberInSource(lineChange);
					removedIfs.add(line);
				}
			}
			
			if(lineChange.getType()==LineChange.ADD){
				String content = lineChange.getLineContent();
				buffer.append(content.substring(1, content.length())+"\n");
				if(content.contains("if")){
					int line = chunk.getLineNumberInTarget(lineChange);
					addedIfs.add(line);
				}
			}
		}
		System.currentTimeMillis();
		if(!removedIfs.isEmpty() && !addedIfs.isEmpty()){
			for(Integer removedLine: removedIfs){
				for(Integer addedLine: addedIfs){
					List<Integer> targetLines = filePair.getSourceToTargetMap().get(removedLine);
					if(targetLines!=null && targetLines.contains(addedLine)){
						return true;
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.INCORRECT_CONDITION);
	}
	
}

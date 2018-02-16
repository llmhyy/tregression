package tregression.empiricalstudy.solutionpattern.data;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IfStatement;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class ExtraNestedIfBlock extends PatternDetector{
	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		if(deadEndRecord.getType()==DeadEndRecord.CONTROL){
			return false;
		}
		
//		RootCauseNode rootCause = trial.getRealcauseNode();
		for(RootCauseNode rootCause: trial.getRootCauseFinder().getRealRootCaseList()){
			if(rootCause.isOnBefore()){
				DiffMatcher matcher = trial.getDiffMatcher();
				for(FilePairWithDiff filePair: matcher.getFileDiffList()){
					for(DiffChunk chunk: filePair.getChunks()){
						boolean ifRemoved = isIfRemoved(chunk, rootCause.getRoot().getLineNumber());
						if(ifRemoved){
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}

	public class IfBlockFinder extends ASTVisitor{
		
		boolean isFound = false;
		
		@Override
		public boolean visit(IfStatement state){
			isFound = true;
			return false;
		}
	}
	
	private boolean isIfRemoved(DiffChunk chunk, int lineNumber) {
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
			ASTNode node = parseAST(code);
			IfBlockFinder finder = new IfBlockFinder();
			node.accept(finder);
			boolean isFound = finder.isFound;
			return isFound;
		}
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.EXTRA_NESTED_IF_BLOCK);
	}
}

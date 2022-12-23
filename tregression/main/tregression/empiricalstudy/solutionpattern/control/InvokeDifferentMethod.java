package tregression.empiricalstudy.solutionpattern.control;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class InvokeDifferentMethod extends PatternDetector{
	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		if(deadEndRecord.getType()==DeadEndRecord.DATA){
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

	class MethodInvocationFinder extends ASTVisitor{
		boolean isFound = false;
		String methodName;
		
		@Override
		public boolean visit(MethodInvocation invocation){
			isFound = true;
			methodName = invocation.getName().getFullyQualifiedName();
			return false;
		}
	}
	
	private boolean isIfChanged(DiffChunk chunk, FilePairWithDiff filePair) {
		Map<Integer, String> removedInvocations = new HashMap<>();
		Map<Integer, String> addedInvocations = new HashMap<>();
		for(LineChange lineChange: chunk.getChangeList()){
			if(lineChange.getType()==LineChange.REMOVE){
				String content = lineChange.getLineContent();
				content = content.substring(1, content.length());
				
				ASTNode node = parseAST(content);
				MethodInvocationFinder finder = new MethodInvocationFinder();
				node.accept(finder);
				
				if(finder.isFound){
					int line = chunk.getLineNumberInSource(lineChange);
					removedInvocations.put(line, finder.methodName);
				}
			}
			
			if(lineChange.getType()==LineChange.ADD){
				String content = lineChange.getLineContent();
				content = content.substring(1, content.length());
				
				ASTNode node = parseAST(content);
				MethodInvocationFinder finder = new MethodInvocationFinder();
				node.accept(finder);
				
				if(finder.isFound){
					int line = chunk.getLineNumberInTarget(lineChange);
					addedInvocations.put(line, finder.methodName);
				}
			}
		}

		if(!removedInvocations.isEmpty() && !addedInvocations.isEmpty()){
			for(Integer removedLine: removedInvocations.keySet()){
				for(Integer addedLine: addedInvocations.keySet()){
					List<Integer> targetLines = filePair.getSourceToTargetMap().get(removedLine);
					if(targetLines!=null && targetLines.contains(addedLine)){
						String methodBefore = removedInvocations.get(removedLine);
						String methodAfter = addedInvocations.get(addedLine);
						return !methodBefore.equals(methodAfter);
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.INVOKE_DIFFERENT_METHOD);
	}
}

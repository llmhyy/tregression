package tregression.empiricalstudy.solutionpattern.data;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class MissingAssignment extends PatternDetector {
	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
		if (deadEndRecord.getType() == DeadEndRecord.CONTROL) {
			return false;
		}

		for (RootCauseNode rootCause : trial.getRootCauseFinder().getRealRootCaseList()) {
			if (!rootCause.isOnBefore()) {
				DiffMatcher matcher = trial.getDiffMatcher();
				for (FilePairWithDiff filePair : matcher.getFileDiffList()) {
					for (DiffChunk chunk : filePair.getChunks()) {
						boolean ifChanged = isIfChanged(chunk, rootCause.getRoot().getLineNumber());
						if (ifChanged) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public class AssignmentFinder extends ASTVisitor {

		boolean isFound = false;

		@Override
		public boolean visit(Assignment assignment) {
			isFound = true;
			return false;
		}
		
		@Override
		public boolean visit(VariableDeclarationFragment frag) {
			isFound = true;
			return false;
		}
	}

	private boolean isIfChanged(DiffChunk chunk, int lineNumber) {
		StringBuffer buffer = new StringBuffer();
		boolean isHit = false;
		for (LineChange lineChange : chunk.getChangeList()) {
			if (lineChange.getType() == LineChange.ADD) {
				String content = lineChange.getLineContent();
				buffer.append(content.substring(1, content.length()) + "\n");

				int line = chunk.getLineNumberInTarget(lineChange);
				if (line == lineNumber) {
					isHit = true;
				}
			}
		}

		if (isHit) {
			String code = buffer.toString();
			ASTNode node = parseAST(code);
			AssignmentFinder finder = new AssignmentFinder();
			node.accept(finder);
			boolean isFound = finder.isFound;
			return isFound;
		}

		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.MISSING_ASSIGNMENT);
	}
}

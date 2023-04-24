package tregression.empiricalstudy.solutionpattern.control;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ThrowStatement;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.solutionpattern.PatternDetector;
import tregression.empiricalstudy.solutionpattern.SolutionPattern;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class MissingIfThrow extends PatternDetector {

	@Override
	public boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial) {
//		if (deadEndRecord.getType() == DeadEndRecord.DATA) {
//			return false;
//		}

		for (RootCauseNode rootCause : trial.getRootCauseFinder().getRealRootCaseList()) {
			if (!rootCause.isOnBefore()) {
				DiffMatcher matcher = trial.getDiffMatcher();
				for (FilePairWithDiff fileDiff : matcher.getFileDiffList()) {
					for (DiffChunk chunk : fileDiff.getChunks()) {
						boolean ifThrowFound = isIfThrowFound(chunk, rootCause.getRoot().getLineNumber());
						if (ifThrowFound) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public class ThrowFinder extends ASTVisitor {

		boolean isFound = false;

		@Override
		public boolean visit(ThrowStatement state) {
			isFound = true;
			return false;
		}
	}

	private boolean isIfThrowFound(DiffChunk chunk, int lineNumber) {
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
			ThrowFinder finder = new ThrowFinder();
			node.accept(finder);
			boolean isFound = finder.isFound;
			return isFound;
		}

		return false;
	}

	@Override
	public SolutionPattern getSolutionPattern() {
		return new SolutionPattern(SolutionPattern.MISSING_IF_THROW);
	}

}

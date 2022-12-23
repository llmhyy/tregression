package tregression.empiricalstudy.solutionpattern;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.EmpiricalTrial;

public abstract class PatternDetector {
	public abstract boolean detect(DeadEndRecord deadEndRecord, EmpiricalTrial trial);
	public abstract SolutionPattern getSolutionPattern();
	
	protected ASTNode parseAST(String code){
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_STATEMENTS);
		parser.setSource(code.toCharArray()); // set source
		ASTNode node = parser.createAST(null);
		
		return node;
	}
}

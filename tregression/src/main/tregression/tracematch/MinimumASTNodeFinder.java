package tregression.tracematch;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

class MinimumASTNodeFinder extends ASTVisitor{
	private int line;
	private CompilationUnit cu;
	
	private int startLine = -1;
	private int endLine = -1;
	
	ASTNode minimumNode = null;

	public MinimumASTNodeFinder(int line, CompilationUnit cu) {
		super();
		this.line = line;
		this.cu = cu;
	}

	@Override
	public void preVisit(ASTNode node) {
		
		int start = cu.getLineNumber(node.getStartPosition());
		int end = cu.getLineNumber(node.getStartPosition()+node.getLength());
		
		if(start<=line && line<=end){
			if(minimumNode == null){
				startLine = start;
				endLine = end;
				minimumNode = node;
			}
			else{
				boolean flag = false;
				
				if(startLine<start){
					startLine = start;
					flag = true;
				}
				
				if(endLine>end){
					endLine = end;
					flag = true;
				}
				
				if(flag){
					minimumNode = node;						
				}
			}
		}
	}
}
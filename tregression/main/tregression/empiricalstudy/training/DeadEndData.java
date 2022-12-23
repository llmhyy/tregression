package tregression.empiricalstudy.training;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.codeanalysis.ast.ASTEncoder;
import microbat.model.trace.TraceNode;
import microbat.util.JavaUtil;
import microbat.util.MinimumASTNodeFinder;

public class DeadEndData {
	public int isBreakStep;
	public String testcase;
	public int traceOrder;
	
	public int astMoveUps;
	public int astMoveDowns;
	public int astMoveRights;
	
	public int traceMoveOuts;
	public int traceMoveIns;
	public int traceMoveDowns;
	
	public int[] stepAST;
	public int[] stepContextAST;
	public int[] occurStepAST;
	public int[] occurStepContextAST;
	public int[] deadEndStepAST;
	public int[] deadEndStepContextAST;
	
	public int deadEndLength;
	
	public String getPlainText(String project, String bugID){
		String plainText = fillCommonRowInfomation();
		return plainText;
	}
	
	protected String fillTraverseInfomation(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(String.valueOf(this.astMoveUps)+",");
		buffer.append(String.valueOf(this.astMoveDowns)+",");
		buffer.append(String.valueOf(this.astMoveRights)+",");
		
//		buffer.append(String.valueOf(this.traceMoveOuts)+",");
//		buffer.append(String.valueOf(this.traceMoveIns)+",");
//		buffer.append(String.valueOf(this.traceMoveDowns)+",");
		
		return buffer.toString();
	}
	
	protected String fillCommonRowInfomation(){
		StringBuffer buffer = new StringBuffer();
		for(int value: this.stepAST){
			buffer.append(String.valueOf(value)+",");
		}
		
		for(int value: this.stepContextAST){
			buffer.append(String.valueOf(value)+",");
		}
		
		for(int value: this.occurStepAST){
			buffer.append(String.valueOf(value)+",");
		}
		
		for(int value: this.occurStepContextAST){
			buffer.append(String.valueOf(value)+",");
		}
		
		for(int value: this.deadEndStepAST){
			buffer.append(String.valueOf(value)+",");
		}
		
		for(int i=0; i<this.deadEndStepContextAST.length; i++) {
			int value = this.deadEndStepContextAST[i];
			if(i != this.deadEndStepContextAST.length-1) {
				buffer.append(String.valueOf(value)+",");				
			}
			else {
				buffer.append(String.valueOf(value));
			}
		}
		
		buffer.append("\n");
		return buffer.toString();
	}
	
	public void setASTInfo(TraceNode step, TraceNode occurStep, TraceNode deadEndStep){
		ASTInfo stepAST = encodeAST(step);
		ASTInfo occurStepAST = encodeAST(occurStep);
		ASTInfo deadEndStepAST = encodeAST(deadEndStep);
		
		this.stepAST = stepAST.astCode;
		this.stepContextAST = stepAST.contextCode;
		this.occurStepAST = occurStepAST.astCode;
		this.occurStepContextAST = occurStepAST.contextCode;
		this.deadEndStepAST = deadEndStepAST.astCode;
		this.deadEndStepContextAST = deadEndStepAST.contextCode;
		
		this.deadEndLength = occurStep.getOrder() - deadEndStep.getOrder();
	}
	
	class ASTInfo{
		int[] astCode;
		int[] contextCode;
		public ASTInfo(int[] astCode, int[] contextCode) {
			super();
			this.astCode = astCode;
			this.contextCode = contextCode;
		}
	}

	private ASTInfo encodeAST(TraceNode step) {
		CompilationUnit cu = JavaUtil.findCompiltionUnitBySourcePath(
				step.getBreakPoint().getFullJavaFilePath(), step.getDeclaringCompilationUnitName());
		MinimumASTNodeFinder finder = new MinimumASTNodeFinder(step.getLineNumber(), cu);
		cu.accept(finder);
		ASTNode node = finder.getMinimumNode();
		int[] astCode = ASTEncoder.encode(node);
		
		ASTNode parent = node.getParent();
		int[] contextCode = ASTEncoder.encode(parent);
		
		return new ASTInfo(astCode, contextCode);
	}
}

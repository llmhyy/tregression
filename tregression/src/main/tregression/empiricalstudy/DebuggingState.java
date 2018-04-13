package tregression.empiricalstudy;

import java.util.List;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.model.StepOperationTuple;

public class DebuggingState {
	TraceNode currentNode;
	List<StepOperationTuple> checkingList;
	VarValue wrongReadVar;

	public DebuggingState(TraceNode currentNode, List<StepOperationTuple> checkingList, VarValue wrongReadVar) {
		super();
		this.currentNode = currentNode;
		this.checkingList = checkingList;
		this.wrongReadVar = wrongReadVar;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DebuggingState) {
			DebuggingState thatState = (DebuggingState)obj;
			if(thatState.currentNode.getOrder()==currentNode.getOrder() &&
					thatState.wrongReadVar.getVarName().equals(wrongReadVar.getVarName())) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		int hashCode1 = currentNode.getOrder();
		int hashCode2 = wrongReadVar.getVarName().hashCode();
		return hashCode1*hashCode2;
	}

}

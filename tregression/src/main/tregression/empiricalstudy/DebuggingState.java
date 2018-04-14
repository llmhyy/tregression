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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentNode == null) ? 0 : currentNode.hashCode());
		result = prime * result + ((wrongReadVar == null) ? 0 : wrongReadVar.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DebuggingState other = (DebuggingState) obj;
		if (currentNode == null) {
			if (other.currentNode != null)
				return false;
		} else if (!currentNode.equals(other.currentNode))
			return false;
		if (wrongReadVar == null) {
			if (other.wrongReadVar != null)
				return false;
		} else if (!wrongReadVar.equals(other.wrongReadVar))
			return false;
		return true;
	}
	
	

}

package tregression.baseline;

import java.util.List;

import microbat.baseline.Configs;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class BaselineSimulator {
	private Trace trace;
	private PairList pairs;
	public BaselineSimulator(Trace buggyTrace, PairList pairs) {
		this.trace = buggyTrace;
		this.pairs = pairs;
	}
	
	public boolean feedback(TraceNode tn) {
		TraceNodePair pair = pairs.findByBeforeNode(tn);
		if (pair.isExactSame()) {
			pair.getBeforeNode().setProbability(Configs.HIGH);
			return false;
		} else {
			List<VarValue> wrongRead = pair.findSingleWrongReadVar(trace);
			if (wrongRead.size() == 0) {
				return true;
			}
			List<VarValue> wrongWrite = pair.findSingleWrongWrittenVarID(trace);
			wrongRead.addAll(wrongWrite);
			for (VarValue v : wrongRead) {
				v.setProbability(Configs.LOW);
			}
			return false;
		}
	}
}

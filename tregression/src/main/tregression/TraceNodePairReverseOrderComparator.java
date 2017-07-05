package tregression;

import java.util.Comparator;

import tregression.model.TraceNodePair;

public class TraceNodePairReverseOrderComparator implements Comparator<TraceNodePair>{

	@Override
	public int compare(TraceNodePair pair1, TraceNodePair pair2) {
		return pair2.getBuggyNode().getOrder() - pair1.getBuggyNode().getOrder();
	}

	
	
}

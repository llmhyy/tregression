package tregression;

import java.util.Comparator;

import tregression.model.TraceNodePair;

public class TraceNodePairReverseOrderComparator implements Comparator<TraceNodePair>{

	@Override
	public int compare(TraceNodePair pair1, TraceNodePair pair2) {
		return pair2.getAfterNode().getOrder() - pair1.getAfterNode().getOrder();
	}

	
	
}

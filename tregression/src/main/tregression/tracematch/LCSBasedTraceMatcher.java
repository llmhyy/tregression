package tregression.tracematch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import microbat.algorithm.graphdiff.GraphDiff;
import microbat.algorithm.graphdiff.HierarchyGraphDiffer;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import tracediff.TraceNodePairReverseOrderComparator;
import tracediff.model.PairList;
import tracediff.model.TraceNodePair;
import tregression.model.TraceNodeWrapper;
import tracediff.separatesnapshots.DiffMatcher;
import tregression.util.TraceNodeComprehensiveSimilarityComparator;

public class LCSBasedTraceMatcher {
	public PairList matchTraceNodePair(Trace mutatedTrace, Trace correctTrace, DiffMatcher matcher) {

		TraceNodeWrapper mutatedTraceNodeWrapper = initVirtualRootWrapper(mutatedTrace);
		TraceNodeWrapper correctTraceNodeWrapper = initVirtualRootWrapper(correctTrace);

		HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
		differ.diff(mutatedTraceNodeWrapper, correctTraceNodeWrapper, false,
				new LCSMatcher(matcher, new TraceNodeComprehensiveSimilarityComparator(matcher)), -1);

		List<GraphDiff> diffList = differ.getDiffs();
		List<TraceNodePair> pList = new ArrayList<>();
		for (GraphDiff diff : diffList) {
			if (diff.getDiffType().equals(GraphDiff.UPDATE)) {
				TraceNodeWrapper wrapperBefore = (TraceNodeWrapper) diff.getNodeBefore();
				TraceNodeWrapper wrapperAfter = (TraceNodeWrapper) diff.getNodeAfter();

				TraceNodePair pair = new TraceNodePair(wrapperBefore.getTraceNode(), wrapperAfter.getTraceNode());
				pair.setExactSame(false);
				pList.add(pair);
			}
		}

		for (GraphDiff common : differ.getCommons()) {
			TraceNodeWrapper wrapperBefore = (TraceNodeWrapper) common.getNodeBefore();
			TraceNodeWrapper wrapperAfter = (TraceNodeWrapper) common.getNodeAfter();

			TraceNodePair pair = new TraceNodePair(wrapperBefore.getTraceNode(), wrapperAfter.getTraceNode());
			pair.setExactSame(true);
			pList.add(pair);
		}

		Collections.sort(pList, new TraceNodePairReverseOrderComparator());
		PairList pairList = new PairList(pList);
		return pairList;
	}
	
	private TraceNodeWrapper initVirtualRootWrapper(Trace trace) {
		TraceNode virtualNode = new TraceNode(null, null, -1, trace, null);
//		List<TraceNode> topList = trace.getTopMethodLevelNodes();
		List<TraceNode> topList = trace.getTopAbstractionLevelNodes();
		virtualNode.setInvocationChildren(topList);
		
		TraceNodeWrapper wrapper = new TraceNodeWrapper(virtualNode);
		
		return wrapper;
	}
}

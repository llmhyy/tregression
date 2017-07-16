package tregression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import tregression.model.PairList;
import tregression.model.Trial;
import tregression.separatesnapshots.DiffMatcher;

/**
 * This class is for empirical study. I will check 
 * (1) whether and when a miss-alignment bug happens and 
 * (2) what is the possible fix for that bug.
 * 
 * @author linyun
 *
 */
public class SimulatorWithCompilcatedModification extends Simulator{
	
	List<TraceNode> rootCauseNodes;
	
	public void prepare(Trace buggyTrace, Trace correctTrace, PairList pairList, Object sourceDiffInfo){
		this.pairList = pairList;
		setObservedFaultNode(buggyTrace);
		
//		System.currentTimeMillis();
	}


	private void setObservedFaultNode(Trace buggyTrace) {
		Map<Integer, TraceNode> allWrongNodeMap = findAllWrongNodes(getPairList(), buggyTrace);
		
		if(!allWrongNodeMap.isEmpty()){
			List<TraceNode> wrongNodeList = new ArrayList<>(allWrongNodeMap.values());
			Collections.sort(wrongNodeList, new TraceNodeReverseOrderComparator());
			observedFaultNode = findObservedFault(wrongNodeList, getPairList());
		}
	}

	
	public Trial detectMutatedBug(Trace buggyTrace, Trace correctTrace, DiffMatcher matcher, int optionSearchLimit) 
					throws SimulationFailException {
		if(observedFaultNode != null){
			
			RootCauseFinder finder = new RootCauseFinder();
			finder.checkRootCause(observedFaultNode, buggyTrace, correctTrace, pairList, matcher);
			
			
			Trial trial = startSimulation(observedFaultNode, buggyTrace, correctTrace, getPairList(), optionSearchLimit);
			return trial;
		}
		
		return null;
	}

	private Trial startSimulation(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace, PairList pairList,
			int optionSearchLimit) {
		
		
		
		
		return null;
	}


	
}

package tregression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import microbat.model.ClassLocation;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeOrderComparator;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import microbat.model.value.VarValue;
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

	
	public EmpiricalTrial detectMutatedBug(Trace buggyTrace, Trace correctTrace, DiffMatcher matcher, int optionSearchLimit) 
					throws SimulationFailException {
		if(observedFaultNode != null){
			
			RootCauseFinder finder = new RootCauseFinder();
			finder.checkRootCause(observedFaultNode, buggyTrace, correctTrace, pairList, matcher);
			
			
			EmpiricalTrial trial = startSimulation(observedFaultNode, buggyTrace, correctTrace, getPairList(), matcher, finder);
			return trial;
		}
		
		return null;
	}

	private EmpiricalTrial startSimulation(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace, PairList pairList,
			DiffMatcher matcher, RootCauseFinder rootCauseFinder) {
		
		List<TraceNode> checkingList = new ArrayList<>();
		checkingList.add(observedFaultNode);
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker();
		
		TraceNode currentNode = observedFaultNode;
		while(true) {
			StepChangeType changeType = typeChecker.getType(currentNode, true, pairList, matcher);
			
			if(changeType.getType()==StepChangeType.SRC){
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.FIND_BUG, 0, checkingList);
				return trial;
			}
			else if(changeType.getType()==StepChangeType.DAT){
				/**
				 * TODO we can generate more trials here, a new wrong variable can create a trial
				 */
				for(VarValue readVar: changeType.getWrongVariableList()){
					TraceNode dataDom = buggyTrace.getLatestProducer(currentNode.getOrder(), readVar.getVarID());
					checkingList.add(dataDom);
					
					currentNode = dataDom;
					break;
				}
			}
			else if(changeType.getType()==StepChangeType.CTL){
				TraceNode controlDom = currentNode.getControlDominator();
				checkingList.add(controlDom);
				
				currentNode = controlDom;
			}
			/**
			 * when it is a correct node
			 */
			else {
				int overskipLen = 0;
				
				TraceNode latestNode = checkingList.get(checkingList.size()-1);
				
				List<TraceNode> regressionNodes = rootCauseFinder.getRegressionNodeList();
				List<TraceNode> correctNodes = rootCauseFinder.getCorrectNodeList();
				
				if(!regressionNodes.isEmpty()) {
					Collections.sort(regressionNodes, new TraceNodeOrderComparator());
					TraceNode regressionNode = regressionNodes.get(0);
					overskipLen = latestNode.getOrder() - regressionNode.getOrder() + 1;
				}
				else if(!correctNodes.isEmpty()){
					Collections.sort(correctNodes, new TraceNodeOrderComparator());
					TraceNode correctNode = correctNodes.get(0);
					
					int startOrder  = rootCauseFinder.findStartOrderInOtherTrace(correctNode, pairList, false);
					overskipLen = latestNode.getOrder() - startOrder + 1;
				}
				
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, overskipLen, checkingList);
				return trial;
			}
			
		}
	}


	
}

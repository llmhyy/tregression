package tregression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeOrderComparator;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
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
		
		List<StepOperationTuple> checkingList = new ArrayList<>();
//		checkingList.add(observedFaultNode);
		
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker();
		
		TraceNode currentNode = observedFaultNode;
		while(true) {
			StepChangeType changeType = typeChecker.getType(currentNode, true, pairList, matcher);
			
			if(changeType.getType()==StepChangeType.SRC){
				StepOperationTuple operation = new StepOperationTuple(currentNode, new UserFeedback(UserFeedback.UNCLEAR), null);
				checkingList.add(operation);
				
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.FIND_BUG, 0, checkingList);
				return trial;
			}
			else if(changeType.getType()==StepChangeType.DAT){
				
				/**
				 * TODO we can generate more trials here, a new wrong variable can create a trial
				 */
				for(VarValue readVar: changeType.getWrongVariableList()){
					StepOperationTuple operation = generateDataFeedback(currentNode, changeType, readVar);
					checkingList.add(operation);
					
					TraceNode dataDom = buggyTrace.getStepVariableTable().get(readVar.getVarID()).getProducers().get(0); 
					currentNode = dataDom;
					
					break;
				}
			}
			else if(changeType.getType()==StepChangeType.CTL){
				TraceNode controlDom = currentNode.getControlDominator();
				
				StepOperationTuple operation = new StepOperationTuple(currentNode, new UserFeedback(UserFeedback.WRONG_PATH), null);
				checkingList.add(operation);
				
				currentNode = controlDom;
			}
			/**
			 * when it is a correct node
			 */
			else {
				StepOperationTuple operation = new StepOperationTuple(currentNode, new UserFeedback(UserFeedback.CORRECT), null);
				checkingList.add(operation);
				
				int overskipLen = checkOverskipLength(pairList, rootCauseFinder, checkingList);
				
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, overskipLen, checkingList);
				System.out.println(trial);
				return trial;
			}
			
		}
	}


	private StepOperationTuple generateDataFeedback(TraceNode currentNode, StepChangeType changeType,
			VarValue readVar) {
		UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
		ChosenVariableOption option = new ChosenVariableOption(readVar, null);
		feedback.setOption(option);
		StepOperationTuple operation = new StepOperationTuple(currentNode, feedback, changeType.getMatchingStep());
		return operation;
	}


	private int checkOverskipLength(PairList pairList, RootCauseFinder rootCauseFinder, List<StepOperationTuple> checkingList) {
		TraceNode latestNode = checkingList.get(checkingList.size()-1).getNode();
		
		List<TraceNode> regressionNodes = rootCauseFinder.getRegressionNodeList();
		List<TraceNode> correctNodes = rootCauseFinder.getCorrectNodeList();
		
		if(!regressionNodes.isEmpty()) {
			Collections.sort(regressionNodes, new TraceNodeOrderComparator());
			TraceNode regressionNode = regressionNodes.get(0);
			int overskipLen = regressionNode.getOrder() - latestNode.getOrder();
			return overskipLen;
		}
		else if(!correctNodes.isEmpty()){
			Collections.sort(correctNodes, new TraceNodeOrderComparator());
			TraceNode correctNode = correctNodes.get(0);
			
			int startOrder  = rootCauseFinder.findStartOrderInOtherTrace(correctNode, pairList, false);
			int overskipLen = startOrder - latestNode.getOrder();
			return overskipLen;
		}
		return 0;
	}


	
}

package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.RootCauseFinder;
import tregression.SimulationFailException;
import tregression.Simulator;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
import tregression.separatesnapshots.DiffMatcher;

/**
 * This class is for empirical study. I will check (1) whether and when a
 * miss-alignment bug happens and (2) what is the possible fix for that bug.
 * 
 * @author linyun
 *
 */
public class SimulatorWithCompilcatedModification extends Simulator {

	List<TraceNode> rootCauseNodes;

	public void prepare(Trace buggyTrace, Trace correctTrace, PairList pairList, Object sourceDiffInfo) {
		this.pairList = pairList;
		setObservedFaultNode(buggyTrace);

		// System.currentTimeMillis();
	}

	private void setObservedFaultNode(Trace buggyTrace) {
		Map<Integer, TraceNode> allWrongNodeMap = findAllWrongNodes(getPairList(), buggyTrace);

		if (!allWrongNodeMap.isEmpty()) {
			List<TraceNode> wrongNodeList = new ArrayList<>(allWrongNodeMap.values());
			Collections.sort(wrongNodeList, new TraceNodeReverseOrderComparator());
//			observedFaultNode = wrongNodeList.get(0);
			observedFaultNode = findObservedFault(wrongNodeList, getPairList());
		}
	}
	
	public List<EmpiricalTrial> detectMutatedBug(Trace buggyTrace, Trace correctTrace, DiffMatcher matcher,
			int optionSearchLimit) throws SimulationFailException {
		if (observedFaultNode != null) {
			RootCauseFinder finder = new RootCauseFinder();
			
			long start = System.currentTimeMillis();
			finder.checkRootCause(observedFaultNode, buggyTrace, correctTrace, pairList, matcher);
			long end = System.currentTimeMillis();
			int checkTime = (int) (end-start);

			List<EmpiricalTrial> trials = startSimulation(observedFaultNode, buggyTrace, correctTrace, getPairList(), matcher, finder);
			if(trials!=null) {
				for(EmpiricalTrial trial: trials) {
					trial.setSimulationTime(checkTime);
				}
			}
			
			return trials;
		}

		return null;
	}

	class DebuggingState {
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

	private List<EmpiricalTrial> startSimulation(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace,
			PairList pairList, DiffMatcher matcher, RootCauseFinder rootCauseFinder) {

		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		List<EmpiricalTrial> trials = new ArrayList<>();
		TraceNode currentNode = observedFaultNode;
		
		Stack<DebuggingState> stack = new Stack<>();
		stack.push(new DebuggingState(currentNode, new ArrayList<StepOperationTuple>(), null));
		Set<DebuggingState> visitedStates = new HashSet<>();
		
		while (!stack.isEmpty()){
			DebuggingState state = stack.pop();
			
			EmpiricalTrial trial = workSingleTrial(buggyTrace, correctTrace, pairList, matcher, 
					rootCauseFinder, typeChecker, currentNode, stack, visitedStates, state);
			trials.add(trial);
		} 
		
		return trials;
	}

	/**
	 * This method returns a debugging trial, and backup all the new debugging state in the input stack.
	 * 
	 * visitedStates records all the backed up debugging state so that we do not repetitively debug the same step with
	 * the same wrong variable twice.
	 * 
	 * stack is used to backup the new debugging state.
	 * 
	 * @param buggyTrace
	 * @param pairList
	 * @param matcher
	 * @param rootCauseFinder
	 * @param typeChecker
	 * @param currentNode
	 * @param stack
	 * @param visitedStates
	 * @param state
	 * @return
	 */
	private EmpiricalTrial workSingleTrial(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher,
			RootCauseFinder rootCauseFinder, StepChangeTypeChecker typeChecker,
			TraceNode currentNode, Stack<DebuggingState> stack, Set<DebuggingState> visitedStates,
			DebuggingState state) {
		/**
		 * recover the debugging state
		 */
		VarValue wrongReadVar = state.wrongReadVar;
		List<StepOperationTuple> checkingList = state.checkingList;
		currentNode = state.currentNode;
		
		TraceNode rootcauseNode = rootCauseFinder.retrieveRootCause(pairList, matcher, buggyTrace, correctTrace);
		TraceNode realcauseNode = rootCauseFinder.getRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
		
		long startTime = System.currentTimeMillis();
		
		/**
		 * start debugging
		 */
		while (true) {
			if(currentNode==null){
				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, 0, rootcauseNode, 
						realcauseNode, checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder.getRegressionNodeList(), rootCauseFinder.getCorrectNodeList(), 
						rootCauseFinder.getRegressionNodeList().size()+rootCauseFinder.getCorrectNodeList().size());
				return trial;
			}
			
			StepChangeType changeType = typeChecker.getType(currentNode, true, pairList, matcher);

			if (changeType.getType() == StepChangeType.SRC) {
				StepOperationTuple operation = new StepOperationTuple(currentNode,
						new UserFeedback(UserFeedback.UNCLEAR), null);
				checkingList.add(operation);
				
				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.FIND_BUG, 0, rootcauseNode, 
						realcauseNode, checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder.getRegressionNodeList(), rootCauseFinder.getCorrectNodeList(), 
						rootCauseFinder.getRegressionNodeList().size()+rootCauseFinder.getCorrectNodeList().size());
				return trial;
			} else if (changeType.getType() == StepChangeType.DAT) {
				if(wrongReadVar == null) {
					for(int i=0; i<changeType.getWrongVariableList().size(); i++) {
						VarValue readVar = changeType.getWrongVariableList().get(i);
						if(i!=0) {
							backupDebuggingState(currentNode, stack, visitedStates, checkingList, readVar);
						}
					}
					
					VarValue readVar = changeType.getWrongVariableList().get(0);
					StepOperationTuple operation = generateDataFeedback(currentNode, changeType, readVar);
					checkingList.add(operation);
					
					TraceNode dataDom = buggyTrace.findDataDominator(currentNode, readVar);
					
					currentNode = dataDom;
					
				}
				else {
					/**
					 * use the designated variable from the recovered state for debugging
					 */
					VarValue readVar = wrongReadVar;
					StepOperationTuple operation = generateDataFeedback(currentNode, changeType, readVar);
					checkingList.add(operation);
					
					TraceNode dataDom = buggyTrace.findDataDominator(currentNode, readVar);
					currentNode = dataDom;
					
					wrongReadVar = null;
				}
			} else if (changeType.getType() == StepChangeType.CTL) {
				TraceNode controlDom = currentNode.getControlDominator();
				if(controlDom==null) {
					controlDom = currentNode.getInvocationParent();
				}

				StepOperationTuple operation = new StepOperationTuple(currentNode,
						new UserFeedback(UserFeedback.WRONG_PATH), null);
				checkingList.add(operation);

				currentNode = controlDom;
			}
			/**
			 * when it is a correct node
			 */
			else {
				StepOperationTuple operation = new StepOperationTuple(currentNode,
						new UserFeedback(UserFeedback.CORRECT), null);
				checkingList.add(operation);

				int overskipLen = checkOverskipLength(pairList, matcher, buggyTrace, rootcauseNode, checkingList);

				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, overskipLen, rootcauseNode, 
						realcauseNode, checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder.getRegressionNodeList(), rootCauseFinder.getCorrectNodeList(), 
						rootCauseFinder.getRegressionNodeList().size()+rootCauseFinder.getCorrectNodeList().size());
				return trial;
			}

		}
		
	}

	private void backupDebuggingState(TraceNode currentNode, Stack<DebuggingState> stack,
			Set<DebuggingState> visitedStates, List<StepOperationTuple> checkingList, VarValue readVar) {
		List<StepOperationTuple> clonedCheckingList = cloneList(checkingList);
		DebuggingState backupState = new DebuggingState(currentNode, clonedCheckingList, readVar);
		if(!visitedStates.contains(backupState)) {
			stack.push(backupState);
			visitedStates.add(backupState);
		}
	}

	private List<StepOperationTuple> cloneList(List<StepOperationTuple> checkingList) {
		List<StepOperationTuple> list = new ArrayList<>();
		for(StepOperationTuple t: checkingList) {
			list.add(t);
		}
		return list;
	}

	private StepOperationTuple generateDataFeedback(TraceNode currentNode, StepChangeType changeType,
			VarValue readVar) {
		UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_VARIABLE_VALUE);
		ChosenVariableOption option = new ChosenVariableOption(readVar, null);
		feedback.setOption(option);
		StepOperationTuple operation = new StepOperationTuple(currentNode, feedback, changeType.getMatchingStep());
		return operation;
	}

	private int checkOverskipLength(PairList pairList, DiffMatcher matcher, Trace buggyTrace, TraceNode rootcauseNode,
			List<StepOperationTuple> checkingList) {
		TraceNode latestNode = checkingList.get(checkingList.size() - 1).getNode();

		if (rootcauseNode != null) {
			return rootcauseNode.getOrder() - latestNode.getOrder();
		}

		return 0;
	}

}

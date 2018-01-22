package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import tregression.SimulationFailException;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.model.PairList;
import tregression.model.StepOperationTuple;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;

/**
 * This class is for empirical study. I will check (1) whether and when a
 * miss-alignment bug happens and (2) what is the possible fix for that bug.
 * 
 * @author linyun
 *
 */
public class Simulator  {

	protected PairList pairList;
	protected DiffMatcher matcher;
	private TraceNode observedFault;
	
	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList, DiffMatcher matcher) {
		this.pairList = pairList;
	}
	
	protected TraceNode findObservedFault(TraceNode node, Trace buggyTrace, Trace correctTrace){
		StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		while(node != null) {
			StepChangeType changeType = checker.getType(node, true, pairList, matcher);
			if (isInvokedByTearDownMethod(node)) {
				
			}
			else if(changeType.getType()==StepChangeType.CTL && node.getControlDominator()==null) {
				if(node.isException()) {
					return node;
				}
			}
			else if(changeType.getType()!=StepChangeType.IDT){
				return node;
			}
			
			node = node.getStepInPrevious();
		}
		
		return null;
	}
	
	private boolean isInvokedByTearDownMethod(TraceNode node) {
		TraceNode n = node;
		while(n!=null) {
			if(n.getMethodSign()!=null && n.getMethodSign().contains(".tearDown()V")) {
				return true;
			}
			else {
				n = n.getInvocationParent();
			}
		}
		
		return false;
	}


	private TraceNode findExceptionNode(List<TraceNode> wrongNodeList) {
		for(TraceNode node: wrongNodeList) {
			if(node.isException()) {
				return node;
			}
		}
		return null;
	}


	protected boolean isObservedFaultWrongPath(TraceNode observableNode, PairList pairList){
		TraceNodePair pair = pairList.findByBeforeNode(observableNode);
		if(pair == null){
			return true;
		}
		
		if(pair.getBeforeNode() == null){
			return true;
		}
		
		return false;
	}
	
	List<TraceNode> rootCauseNodes;

	public void prepare(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher) {
		this.pairList = pairList;
		this.matcher = matcher;
		TraceNode initialStep = buggyTrace.getLatestNode();
		this.setObservedFault(findObservedFault(initialStep, buggyTrace, correctTrace));
	}

	public List<EmpiricalTrial> detectMutatedBug(Trace buggyTrace, Trace correctTrace, DiffMatcher matcher,
			int optionSearchLimit) throws SimulationFailException {
		if (getObservedFault() != null) {
			RootCauseFinder finder = new RootCauseFinder();
			
			long start = System.currentTimeMillis();
			finder.checkRootCause(getObservedFault(), buggyTrace, correctTrace, pairList, matcher);
			long end = System.currentTimeMillis();
			int checkTime = (int) (end-start);

			List<EmpiricalTrial> trials = startSimulation(getObservedFault(), buggyTrace, correctTrace, getPairList(), matcher, finder);
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
		
		int count = 0;
		
		while (!stack.isEmpty() && count<10){
			DebuggingState state = stack.pop();
			
			EmpiricalTrial trial = workSingleTrial(buggyTrace, correctTrace, pairList, matcher, 
					rootCauseFinder, typeChecker, currentNode, stack, visitedStates, state);
			trials.add(trial);
			count++;
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
		RootCauseNode realcauseNode = rootCauseFinder.getRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
		
		boolean isMultiThread = buggyTrace.isMultiThread() || correctTrace.isMultiThread();
		
		long startTime = System.currentTimeMillis();
		
		/**
		 * start debugging
		 */
		while (true) {
			TraceNode previousNode = null;
			if(!checkingList.isEmpty()){
				StepOperationTuple lastTuple = checkingList.get(checkingList.size()-1);
				previousNode = lastTuple.getNode();
			}
			
			if(currentNode==null || (previousNode!=null && currentNode.getOrder()==previousNode.getOrder())){
				long endTime = System.currentTimeMillis();
				
				int order = -1;
				if(realcauseNode!=null){
					order = realcauseNode.getRoot().getOrder();
				}
				
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, order, rootcauseNode, 
						realcauseNode, checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
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
						rootCauseFinder, isMultiThread);
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
					if(controlDom==null){
						controlDom = currentNode.getStepInPrevious();
					}
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
						rootCauseFinder, isMultiThread);
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

	public TraceNode getObservedFault() {
		return observedFault;
	}

	public void setObservedFault(TraceNode observedFault) {
		this.observedFault = observedFault;
	}

}

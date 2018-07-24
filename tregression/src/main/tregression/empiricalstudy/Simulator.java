package tregression.empiricalstudy;

import java.io.IOException;
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
import sav.common.core.SavException;
import tregression.SimulationFailException;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.empiricalstudy.recommendation.BreakerRecommender;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.TrainingDataTransfer;
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
	private List<TraceNode> observedFaultList = new ArrayList<>();
	
	private boolean useSliceBreaker;
	private boolean enableRandom;
	private int breakerTrialLimit;
	
	private boolean allowCacheCriticalConditionalStep = false;
	
	public Simulator(boolean useSlicerBreaker, boolean enableRandom, int breakerTrialLimit){
		this.useSliceBreaker = useSlicerBreaker;
		this.breakerTrialLimit = breakerTrialLimit;
		this.enableRandom = enableRandom;
	}
	
	
	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList, DiffMatcher matcher) {
		this.pairList = pairList;
	}
	
	protected TraceNode findObservedFault(TraceNode node, Trace buggyTrace, Trace correctTrace){
		StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		TraceNode firstTearDownNode = firstPreviousNodeInvokedByTearDown(node);
		System.currentTimeMillis();
		if(firstTearDownNode!=null){
			node = firstTearDownNode.getStepInPrevious();
		}
		
		while(node != null) {
			StepChangeType changeType = checker.getType(node, true, pairList, matcher);
			if(changeType.getType()==StepChangeType.CTL) {
				TraceNode cDom = node.getInvocationMethodOrDominator();
				if(cDom==null){
					if(node.isException()) {
						return node;
					}	
					else{
						node = node.getStepInPrevious();
						continue;
					}
				}
				
				StepChangeType cDomType = checker.getType(cDom, true, pairList, matcher);
				if(cDomType.getType()==StepChangeType.IDT){
					TraceNode stepOverPrev = node.getStepOverPrevious();
					if(stepOverPrev!=null){
						if(stepOverPrev.equals(cDom) && stepOverPrev.isBranch() && !stepOverPrev.isConditional()){
							node = node.getStepInPrevious();
							continue;
						}
					}
				}
				
				return node;
			}
			else if(changeType.getType()!=StepChangeType.IDT){
				return node;
			}
			
			node = node.getStepInPrevious();
		}
		
		return null;
	}
	
	private TraceNode firstPreviousNodeInvokedByTearDown(TraceNode node) {
		TraceNode prev = node.getStepInPrevious();
		if(prev==null) {
			return null;
		}
		
		TraceNode returnNode = null;
		
		boolean isInvoked = isInvokedByTearDownMethod(prev);
		if(isInvoked){
			returnNode = prev;
		}
		else{
			return null;
		}
		
		while(prev != null){
			prev = prev.getStepInPrevious();
			if(prev==null){
				return null;
			}
			
			isInvoked = isInvokedByTearDownMethod(prev);
			if(isInvoked){
				if(returnNode==null){
					returnNode = prev;
				}
				else if(prev.getOrder()<returnNode.getOrder()){
					returnNode = prev;					
				}
			}
			else{
				returnNode = prev;
				break;
			}
		}
		
		return returnNode;
	}

	private boolean isInvokedByTearDownMethod(TraceNode node) {
		TraceNode n = node;
		while(n!=null) {
			if(n.getMethodSign()!=null && n.getMethodSign().contains("tearDown()V")) {
				return true;
			}
			else {
				n = n.getInvocationParent();
			}
		}
		
		return false;
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
	
//	List<TraceNode> rootCauseNodes;
	public void prepare(Trace buggyTrace, Trace correctTrace, PairList pairList, DiffMatcher matcher) {
		this.pairList = pairList;
		this.matcher = matcher;
		TraceNode initialStep = buggyTrace.getLatestNode();
		TraceNode lastObservableFault = findObservedFault(initialStep, buggyTrace, correctTrace);
		
		if(lastObservableFault!=null){
			observedFaultList.add(lastObservableFault);

			StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
			TraceNode node = lastObservableFault.getStepOverPrevious();
			
			int times = 5;
			while(observedFaultList.size() < times && node!= null){
				
				StepChangeType changeType = checker.getType(node, true, pairList, matcher);
				if(changeType.getType()!=StepChangeType.IDT){
					observedFaultList.add(node);
				}
				
				node = node.getStepOverPrevious();
			}
		}
	}

	public List<EmpiricalTrial> detectMutatedBug(Trace buggyTrace, Trace correctTrace, DiffMatcher matcher,
			int optionSearchLimit) throws SimulationFailException {
		List<EmpiricalTrial> trials = null;
		for (TraceNode observedFault: observedFaultList) {
			RootCauseFinder finder = new RootCauseFinder();
			
			long start = System.currentTimeMillis();
			finder.checkRootCause(observedFault, buggyTrace, correctTrace, pairList, matcher);
			long end = System.currentTimeMillis();
			int checkTime = (int) (end-start);

			System.out.println("use slice breaker: " + useSliceBreaker);
			if(useSliceBreaker) {
				trials = startSimulationWithCachedState(observedFault, buggyTrace, correctTrace, getPairList(), matcher, finder);
			}
			else {
				trials = startSimulation(observedFault, buggyTrace, correctTrace, getPairList(), matcher, finder);				
			}
			
			if(trials!=null) {
				boolean rootcauseFind = false;
				for(EmpiricalTrial trial: trials) {
					if(!rootcauseFind && trial.getRootcauseNode()!=null){
						rootcauseFind = true;
					}
					trial.setSimulationTime(checkTime);
				}
				
				if(rootcauseFind){
					observedFaultList.clear();
					observedFaultList.add(observedFault);
					return trials;
				}
			}
		}

		return trials;
	}

	private List<EmpiricalTrial> startSimulationWithCachedState(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace,
			PairList pairList, DiffMatcher matcher, RootCauseFinder rootCauseFinder) {
		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		List<EmpiricalTrial> trials = new ArrayList<>();
		TraceNode currentNode = observedFaultNode;
		
		Stack<DebuggingState> stack = new Stack<>();
		stack.push(new DebuggingState(currentNode, new ArrayList<StepOperationTuple>(), null));
		Set<DebuggingState> visitedStates = new HashSet<>();
		
		while (!stack.isEmpty()){
			DebuggingState state = stack.pop();
			
			EmpiricalTrial trial = workSingleTrialWithCachedState(buggyTrace, correctTrace, pairList, matcher, 
					rootCauseFinder, typeChecker, currentNode, stack, visitedStates, state);
			trials.add(trial);
			
			if(trial.isBreakSlice()){
				break;
			}
		} 
		
		return trials;
	}
	
	private List<EmpiricalTrial> startSimulation(TraceNode observedFaultNode, Trace buggyTrace, Trace correctTrace,
			PairList pairList, DiffMatcher matcher, RootCauseFinder rootCauseFinder) {

		StepChangeTypeChecker typeChecker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		List<EmpiricalTrial> trials = new ArrayList<>();
		TraceNode currentNode = observedFaultNode;
		
		EmpiricalTrial trial = workSingleTrial(buggyTrace, correctTrace, pairList, matcher, 
				rootCauseFinder, typeChecker, currentNode);
		trials.add(trial);
		
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
			TraceNode currentNode) {
		
		List<StepOperationTuple> checkingList = new ArrayList<>();
		
		TraceNode rootcauseNode = rootCauseFinder.retrieveRootCause(pairList, matcher, buggyTrace, correctTrace);
		rootCauseFinder.setRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
		
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
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, -1, rootcauseNode, 
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
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
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
				return trial;
			} else if (changeType.getType() == StepChangeType.DAT) {
				VarValue readVar = changeType.getWrongVariable(currentNode, true, rootCauseFinder);
				StepOperationTuple operation = generateDataFeedback(currentNode, changeType, readVar);
				checkingList.add(operation);
				
				TraceNode dataDom = buggyTrace.findDataDominator(currentNode, readVar);
				
				currentNode = dataDom;
			} else if (changeType.getType() == StepChangeType.CTL) {
				TraceNode controlDom = null;
				if(currentNode.insideException()){
					controlDom = currentNode.getStepInPrevious();
				}
				else{
					controlDom = currentNode.getInvocationMethodOrDominator();
					//indicate the control flow is caused by try-catch
					if(controlDom!=null && !controlDom.isConditional() && controlDom.isBranch()
							&& !controlDom.equals(currentNode.getInvocationParent())){
						StepChangeType t = typeChecker.getType(controlDom, true, pairList, matcher);
						if(t.getType()==StepChangeType.IDT){
							controlDom = findLatestControlDifferent(currentNode, controlDom, 
									typeChecker, pairList, matcher);
						}
					}
					
					if(controlDom==null) {
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
				
				if(currentNode.isException()){
					currentNode = currentNode.getStepInPrevious();
					continue;
				}

				int overskipLen = checkOverskipLength(pairList, matcher, buggyTrace, rootcauseNode, checkingList);
				if(overskipLen<0 && checkingList.size()>=2){
					int size = checkingList.size();
					if(checkingList.get(size-2).getUserFeedback().getFeedbackType().equals(UserFeedback.WRONG_PATH)){
						overskipLen = 1;
					}
				}

				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, overskipLen, rootcauseNode, 
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
				
				if(previousNode!=null){
					StepChangeType prevChangeType = typeChecker.getType(previousNode, true, pairList, matcher);
					List<DeadEndRecord> list = null;
					if(prevChangeType.getType()==StepChangeType.CTL){
						list = createControlRecord(currentNode, previousNode, typeChecker, pairList, matcher);
						trial.setDeadEndRecordList(list);
					}
					else if(prevChangeType.getType()==StepChangeType.DAT){
						list = createDataRecord(currentNode, previousNode, typeChecker, pairList, matcher, rootCauseFinder);
						trial.setDeadEndRecordList(list);
					}
					
					if(trial.getBugType()==EmpiricalTrial.OVER_SKIP && trial.getOverskipLength()==0){
						if(list != null && !list.isEmpty()){
							DeadEndRecord record = list.get(0);
							int len = currentNode.getOrder() - record.getBreakStepOrder();
							trial.setOverskipLength(len);
						}
					}
				}
				
				return trial;	
			}

		}
		
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
	private EmpiricalTrial workSingleTrialWithCachedState(Trace buggyTrace, Trace correctTrace, 
			PairList pairList, DiffMatcher matcher,
			RootCauseFinder rootCauseFinder, StepChangeTypeChecker typeChecker,
			TraceNode currentNode, Stack<DebuggingState> stack, Set<DebuggingState> visitedStates,
			DebuggingState state) {
		/**
		 * recover the debugging state
		 */
		List<StepOperationTuple> checkingList = state.checkingList;
		currentNode = state.currentNode;
		
		TraceNode rootcauseNode = rootCauseFinder.retrieveRootCause(pairList, matcher, buggyTrace, correctTrace);
		rootCauseFinder.setRootCauseBasedOnDefects4J(pairList, matcher, buggyTrace, correctTrace);
		
		boolean isMultiThread = buggyTrace.isMultiThread() || correctTrace.isMultiThread();
		
		long startTime = System.currentTimeMillis();
		
		Set<TraceNode> occuringNodes = new HashSet<>();
		
		EmpiricalTrial overskipTrial = null; 
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
				
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, -1, rootcauseNode, 
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
				/*
				 * LLT: this also can happen for the stage of verifying sliceBreaker,
				 * in such case, original deadEndRecordList need to restore in trial.
				 */
				currentNode = recoverFromBackedState(stack, pairList, matcher, occuringNodes, checkingList,
						typeChecker);
				if (currentNode != null) {
					continue;
				}
				if(overskipTrial!=null){
					trial.setOverskipLength(overskipTrial.getOverskipLength());
					trial.setDeadEndRecordList(overskipTrial.getDeadEndRecordList());
					trial.setCheckList(overskipTrial.getCheckList());
				}
				return trial;
			}
			
			StepChangeType changeType = typeChecker.getType(currentNode, true, pairList, matcher);

			if (changeType.getType() == StepChangeType.SRC) {
				StepOperationTuple operation = new StepOperationTuple(currentNode,
						new UserFeedback(UserFeedback.UNCLEAR), null);
				checkingList.add(operation);
				
				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.FIND_BUG, 0, rootcauseNode, 
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
				if(overskipTrial!=null){
					trial.setBugType(overskipTrial.getBugType());
					trial.setOverskipLength(overskipTrial.getOverskipLength());
					trial.setDeadEndRecordList(overskipTrial.getDeadEndRecordList());
					trial.setBreakSlice(true);
				}
				return trial;
			} else if (changeType.getType() == StepChangeType.DAT) {
				VarValue readVar = changeType.getWrongVariable(currentNode, true, rootCauseFinder);
				StepOperationTuple operation = generateDataFeedback(currentNode, changeType, readVar);
				checkingList.add(operation);
				
				TraceNode dataDom = buggyTrace.findDataDominator(currentNode, readVar);
				
				currentNode = dataDom;
			} else if (changeType.getType() == StepChangeType.CTL) {
				TraceNode controlDom = null;
				if(currentNode.insideException()){
					controlDom = currentNode.getStepInPrevious();
				}
				else{
					controlDom = currentNode.getInvocationMethodOrDominator();
					//indicate the control flow is caused by try-catch
					if(controlDom!=null && !controlDom.isConditional() && controlDom.isBranch()
							&& !controlDom.equals(currentNode.getInvocationParent())){
						StepChangeType t = typeChecker.getType(controlDom, true, pairList, matcher);
						if(t.getType()==StepChangeType.IDT){
							controlDom = findLatestControlDifferent(currentNode, controlDom, 
									typeChecker, pairList, matcher);
						}
					}
					
					if(controlDom==null) {
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
				
				if(currentNode.isException()){
					currentNode = currentNode.getStepInPrevious();
					continue;
				}

				int overskipLen = checkOverskipLength(pairList, matcher, buggyTrace, rootcauseNode, checkingList);
				if(overskipLen<0 && checkingList.size()>=2){
					int size = checkingList.size();
					if(checkingList.get(size-2).getUserFeedback().getFeedbackType().equals(UserFeedback.WRONG_PATH)){
						overskipLen = 1;
					}
				}

				long endTime = System.currentTimeMillis();
				EmpiricalTrial trial = new EmpiricalTrial(EmpiricalTrial.OVER_SKIP, overskipLen, rootcauseNode, 
						checkingList, -1, -1, (int)(endTime-startTime), buggyTrace.size(), correctTrace.size(),
						rootCauseFinder, isMultiThread);
				if(overskipTrial==null){
					overskipTrial = trial;					
				}
				
				List<DeadEndRecord> list = new ArrayList<>();
				if(previousNode!=null){
					StepChangeType prevChangeType = typeChecker.getType(previousNode, true, pairList, matcher);
					if(prevChangeType.getType()==StepChangeType.CTL){
						list = createControlRecord(currentNode, previousNode, typeChecker, pairList, matcher);
						if(trial.getDeadEndDataList().isEmpty()){
							trial.setDeadEndRecordList(list);							
						}
					}
					else if(prevChangeType.getType()==StepChangeType.DAT){
						list = createDataRecord(currentNode, previousNode, typeChecker, pairList, matcher, rootCauseFinder);
						if(trial.getDeadEndDataList().isEmpty()){
							trial.setDeadEndRecordList(list);							
						}
					}
					
					if(list != null && !list.isEmpty()){
						rootcauseNode = buggyTrace.getTraceNode(list.get(0).getBreakStepOrder());
						if(trial.getBugType()==EmpiricalTrial.OVER_SKIP){
							DeadEndRecord record = list.get(0);
							int len = record.getBreakStepOrder() - record.getDeadEndOrder();
							trial.setOverskipLength(len);
						}
					}
				}
				
				List<TraceNode> sliceBreakers = new ArrayList<>();
				if(!enableRandom){
					sliceBreakers = findBreaker(list, breakerTrialLimit, buggyTrace, rootCauseFinder);
				}
				else{
					sliceBreakers = findRandomBreaker(list, breakerTrialLimit, buggyTrace, rootCauseFinder);
				}
				
				if((sliceBreakers.isEmpty() || occuringNodes.contains(currentNode)) 
						&& !stack.empty()){
					currentNode = recoverFromBackedState(stack, pairList, matcher, occuringNodes, checkingList, typeChecker);
					if(currentNode==null){
						overskipTrial.setCheckList(trial.getCheckList());
						return overskipTrial;
					}
				}
				else if(!sliceBreakers.isEmpty()){
					if(includeRootCause(sliceBreakers, rootcauseNode, buggyTrace, correctTrace)){
						trial.setBreakSlice(true);
						stack.clear();
						overskipTrial.setCheckList(trial.getCheckList());
						return overskipTrial;	
					}
					else{ 
						int start = 0;
						currentNode = sliceBreakers.get(start);
						while (occuringNodes.contains(currentNode) && start < (sliceBreakers.size() - 1)) {
							start++;
							currentNode = sliceBreakers.get(start);
						}
						if (!occuringNodes.contains(currentNode)) {
							for (int i = start + 1; i < sliceBreakers.size(); i++) {
								backupDebuggingState(sliceBreakers.get(i), stack, visitedStates, checkingList, null);
							}
							continue;
						}
						currentNode = recoverFromBackedState(stack, pairList, matcher, occuringNodes, checkingList,
								typeChecker);
						if (currentNode == null) {
							return overskipTrial;
						}
					}
				}
				else{
					overskipTrial.setCheckList(trial.getCheckList());
					return overskipTrial;					
				}
			}
		}
		
	}
	
	
	
	private TraceNode recoverFromBackedState(Stack<DebuggingState> stack, PairList pairList2, DiffMatcher matcher2,
			Set<TraceNode> occuringNodes, List<StepOperationTuple> checkingList, StepChangeTypeChecker typeChecker) {
		if (stack.isEmpty()) {
			return null;
		}
		DebuggingState backedState = stack.pop();
		checkingList = backedState.checkingList;
		TraceNode currentNode = backedState.currentNode;
		StepChangeType t = typeChecker.getType(currentNode, true, pairList, matcher);
		
		while(t.getType()==StepChangeType.IDT && !stack.isEmpty()){
			backedState = stack.pop();
			checkingList = backedState.checkingList;
			currentNode = backedState.currentNode;
			occuringNodes.add(currentNode);
			t = typeChecker.getType(currentNode, true, pairList, matcher);
		}
		
		if(t.getType()==StepChangeType.IDT && stack.isEmpty()){
			return null;
		}
		else{
			return currentNode;
		}
	}


	private boolean includeRootCause(List<TraceNode> sliceBreakers, TraceNode rootCauseNode, 
			Trace buggyTrace, Trace correctTrace) {
		for(TraceNode breaker: sliceBreakers){
			if(breaker.getBreakPoint().equals(rootCauseNode.getBreakPoint())){
				return true;
			}
		}
		
		return false;
	}

	private TrainingDataTransfer transfer = new TrainingDataTransfer();

	private List<TraceNode> findBreaker(List<DeadEndRecord> list, int breakerTrialLimit, 
			Trace buggyTrace, RootCauseFinder rootCauseFinder) {
		if(list==null){
			return new ArrayList<>();
		}
		
		for(DeadEndRecord record: list){
			DED ded = null;
			if(this.allowCacheCriticalConditionalStep){
				transfer.setUsingCache(true);
				ded = transfer.transfer(record, buggyTrace);
			}
			else{
				transfer = new TrainingDataTransfer();
				ded = transfer.transfer(record, buggyTrace);
			}
			
			record.setTransformedData(ded);
			try {
				List<TraceNode> breakerCandidates = new BreakerRecommender().
						recommend(ded.getAllData(), buggyTrace, breakerTrialLimit); 
				return breakerCandidates;
			} catch (SavException | IOException e) {
				System.out.println("findBreaker: exception: ");
				e.printStackTrace();
			} 
		}
		
		return new ArrayList<>();
	}
	
	private List<TraceNode> findRandomBreaker(List<DeadEndRecord> list, int breakerTrialLimit, Trace buggyTrace,
			RootCauseFinder rootCauseFinder) {
		if(list==null || list.isEmpty()){
			return new ArrayList<>();
		}
		
		List<TraceNode> breakers = new ArrayList<>();
		
		DeadEndRecord record = list.get(0);
		int occur = record.getOccurOrder();
		int deadend = record.getDeadEndOrder();
		
		int length = occur - deadend;
		
		for(int i=0; i<breakerTrialLimit; i++){
			double ran = Math.random();
			int index = (int) (ran * length);
			int order = deadend + index;
			
			TraceNode node = buggyTrace.getTraceNode(order);
			if(!breakers.contains(node)){
				breakers.add(node);
			}
		}
		
		return breakers;
	}


	private TraceNode findLatestControlDifferent(TraceNode currentNode, TraceNode controlDom, 
			StepChangeTypeChecker checker, PairList pairList, DiffMatcher matcher) {
		TraceNode n = currentNode.getStepInPrevious();
		StepChangeType t = checker.getType(n, true, pairList, matcher);
		while((t.getType()==StepChangeType.CTL || t.getType()==StepChangeType.SRC) && n.getOrder()>controlDom.getOrder()){
			TraceNode dom = n.getInvocationMethodOrDominator();
			if(dom.getMethodSign().equals(n.getMethodSign())){
				return n;
			}
			
			n = n.getStepInPrevious();
			t = checker.getType(n, true, pairList, matcher);
		}
		return controlDom;
	}

	private List<DeadEndRecord> createControlRecord(TraceNode currentNode, TraceNode latestBugNode, StepChangeTypeChecker typeChecker,
			PairList pairList, DiffMatcher matcher) {
		List<DeadEndRecord> deadEndRecords = new ArrayList<>();
		
		Trace trace = currentNode.getTrace();
		for(int i=currentNode.getOrder()+1; i<=latestBugNode.getOrder(); i++){
			TraceNode node = trace.getTraceNode(i);
			StepChangeType changeType = typeChecker.getType(node, true, pairList, matcher);
			if(changeType.getType()==StepChangeType.CTL){
				DeadEndRecord record = new DeadEndRecord(DeadEndRecord.CONTROL, 
						latestBugNode.getOrder(), currentNode.getOrder(), -1, node.getOrder());
				deadEndRecords.add(record);
				
				TraceNode equivalentNode = node.getStepOverNext();
				while(equivalentNode!=null && equivalentNode.getBreakPoint().equals(node.getBreakPoint())){
					DeadEndRecord addRecord = new DeadEndRecord(DeadEndRecord.CONTROL, 
							latestBugNode.getOrder(), currentNode.getOrder(), -1, equivalentNode.getOrder());
					deadEndRecords.add(addRecord);
					equivalentNode = equivalentNode.getStepOverNext();
				}
				
				break;
			}
		}
		
		return deadEndRecords;
	}

	private List<TraceNode> findTheNearestCorrespondence(TraceNode domOnRef, PairList pairList, Trace buggyTrace, Trace correctTrace) {
		List<TraceNode> list = new ArrayList<>();
		
		List<TraceNode> sameLineSteps = findSameLineSteps(domOnRef);
		for(TraceNode sameLineStep: sameLineSteps){
			TraceNodePair pair = pairList.findByAfterNode(sameLineStep);
			if(pair!=null){
				TraceNode beforeNode = pair.getBeforeNode();
				if(beforeNode!=null){
					list.add(beforeNode);
				}
			}
		}
		if(!list.isEmpty()){
			return list;
		}
		
		int endOrder = new RootCauseFinder().findEndOrderInOtherTrace(domOnRef, pairList, false, correctTrace);
		TraceNode startNode = buggyTrace.getTraceNode(endOrder);
		list.add(startNode);
		while(startNode.getStepOverPrevious()!=null && 
				startNode.getStepOverPrevious().getBreakPoint().equals(startNode.getBreakPoint())){
			startNode = startNode.getStepOverPrevious();
			list.add(startNode);
		}
		
//		TraceNode end = buggyTrace.getTraceNode(endOrder);
//		TraceNode n = end.getStepOverNext();
//		while(n!=null && (n.getLineNumber()==end.getLineNumber())){
//			list.add(n);
//			n = n.getStepOverNext();
//		}
		
		return list;
	}
	
	private List<TraceNode> findSameLineSteps(TraceNode domOnRef) {
		List<TraceNode> list = new ArrayList<>();
		list.add(domOnRef);
		
		TraceNode node = domOnRef.getStepOverPrevious();
		while(node!=null && node.getLineNumber()==domOnRef.getLineNumber()){
			list.add(node);
			node = node.getStepOverPrevious();
		}
		
		node = domOnRef.getStepOverNext();
		while(node!=null && node.getLineNumber()==domOnRef.getLineNumber()){
			list.add(node);
			node = node.getStepOverNext();
		}
		
		return list;
	}

	private List<DeadEndRecord> createDataRecord(TraceNode currentNode, TraceNode buggyNode,
			StepChangeTypeChecker typeChecker, PairList pairList, DiffMatcher matcher, RootCauseFinder rootCauseFinder) {
		
		List<DeadEndRecord> deadEndlist = new ArrayList<>();
		TraceNodePair pair = pairList.findByBeforeNode(buggyNode);
		TraceNode matchingStep = pair.getAfterNode();
		
		TraceNode domOnRef = null;
		StepChangeType matchingStepType = typeChecker.getType(matchingStep, false, pairList, matcher);
		System.currentTimeMillis();
		if(matchingStepType.getWrongVariableList()==null) {
			return deadEndlist;
		}
		
		VarValue wrongVar = matchingStepType.getWrongVariable(currentNode, false, rootCauseFinder);
		domOnRef = matchingStep.getDataDominator(wrongVar);
		
		List<TraceNode> breakSteps = new ArrayList<>();
		 
		while(domOnRef != null){
			StepChangeType changeType = typeChecker.getType(domOnRef, false, pairList, matcher);
			if(changeType.getType()==StepChangeType.SRC){
				breakSteps = findTheNearestCorrespondence(domOnRef, pairList, buggyNode.getTrace(), matchingStep.getTrace());
				break;
			}
			else{
				TraceNodePair conPair = pairList.findByAfterNode(domOnRef);
				if(conPair != null && conPair.getBeforeNode() != null){
					/**
					 * if we find a matched step on buggy trace, then we find the first incorrect step starting at the matched
					 * step as the break step.
					 */
					TraceNode matchingPoint = conPair.getBeforeNode();
					for(int order=matchingPoint.getOrder(); order<=matchingPoint.getTrace().size(); order++){
						TraceNode potentialPoint = matchingPoint.getTrace().getTraceNode(order);
						StepChangeType ct = typeChecker.getType(potentialPoint, true, pairList, matcher);
						if(ct.getType()!=StepChangeType.IDT){
							breakSteps.add(potentialPoint);
							break;
						}
					}
					
					break;
				}
				else{
					domOnRef = domOnRef.getInvocationMethodOrDominator();
				}
			}
		}
		
		VarValue wrongVarOnBuggyTrace = matchingStepType.getWrongVariable(currentNode, true, rootCauseFinder);
		for(TraceNode breakStep: breakSteps){
			DeadEndRecord record = new DeadEndRecord(DeadEndRecord.DATA, buggyNode.getOrder(), 
					currentNode.getOrder(), -1, breakStep.getOrder());
			record.setVarValue(wrongVarOnBuggyTrace);
			if(!deadEndlist.contains(record)) {
				deadEndlist.add(record);						
			}
		}
		
		return deadEndlist;
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
		if(this.observedFaultList.isEmpty()){
			return null;
		}
		
		int size = this.observedFaultList.size();
		return this.observedFaultList.get(size-1);
	}

	public void addObservedFault(TraceNode observedFault) {
		this.observedFaultList.add(observedFault);
	}


	public int getBreakerTrialLimit() {
		return breakerTrialLimit;
	}


	public void setBreakerTrialLimit(int breakerTrialLimit) {
		this.breakerTrialLimit = breakerTrialLimit;
	}


	public boolean isEnableRandom() {
		return enableRandom;
	}


	public void setEnableRandom(boolean enableRandom) {
		this.enableRandom = enableRandom;
	}


	public boolean isUseSliceBreaker() {
		return useSliceBreaker;
	}


	public void setUseSliceBreaker(boolean useSliceBreaker) {
		this.useSliceBreaker = useSliceBreaker;
	}


	public boolean isAllowCacheCriticalConditionalStep() {
		return allowCacheCriticalConditionalStep;
	}


	public void setAllowCacheCriticalConditionalStep(boolean allowCacheCriticalConditionalStep) {
		this.allowCacheCriticalConditionalStep = allowCacheCriticalConditionalStep;
	}

}

package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import microbat.handler.CheckingState;
import microbat.model.UserInterestedVariables;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.StepRecommender;
import microbat.recommendation.UserFeedback;
import microbat.util.Settings;
import sav.strategies.dto.ClassLocation;
import tregression.SimulatedUser;
import tregression.SimulationFailException;
import tregression.Simulator;
import tregression.model.PairList;
import tregression.model.StateWrapper;
import tregression.model.StepOperationTuple;
import tregression.model.TraceNodePair;
import tregression.model.Trial;

public class SimulatorWithSingleLineModification extends Simulator{
	private SimulatedUser user = new SimulatedUser();
	private StepRecommender recommender;
	
	protected TraceNode rootCause;
	
	public void prepare(Trace mutatedTrace, Trace correctTrace, PairList pairList, Object sourceDiffInfo){
		
		ClassLocation mutatedLocation = null;
		if(sourceDiffInfo instanceof ClassLocation){
			mutatedLocation = (ClassLocation)sourceDiffInfo;
		}
		
		this.pairList = pairList;
		
		rootCause = findRootCause(mutatedLocation.getClassCanonicalName(), 
				mutatedLocation.getLineNo(), mutatedTrace, getPairList());
		
		Map<Integer, TraceNode> allWrongNodeMap = findAllWrongNodes(getPairList(), mutatedTrace);
		
		if(!allWrongNodeMap.isEmpty()){
			List<TraceNode> wrongNodeList = new ArrayList<>(allWrongNodeMap.values());
			Collections.sort(wrongNodeList, new TraceNodeReverseOrderComparator());
			observedFaultNode = findObservedFault(wrongNodeList, getPairList());
		}
		
	}
	
	public Trial detectMutatedBug(Trace mutatedTrace, Trace correctTrace, ClassLocation mutatedLocation, 
			String testCaseName, String mutatedFile, double unclearRate, boolean enableLoopInference, int optionSearchLimit) 
					throws SimulationFailException {
		mutatedTrace.resetCheckTime();
		if(observedFaultNode != null){
			try {
				Trial trial = startSimulation(observedFaultNode, rootCause, mutatedTrace, correctTrace, 
						getPairList(), testCaseName, mutatedFile, unclearRate, 
						enableLoopInference, optionSearchLimit);
//				System.currentTimeMillis();
				return trial;			
			} catch (Exception e) {
				String errorMsg = "Test case: " + testCaseName + 
						" has exception when simulating debugging\n" + "Mutated File: " + mutatedFile +
						", unclearRate: " + unclearRate + ", enableLoopInference: " + enableLoopInference;
				System.err.println(errorMsg);
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	class Attempt{
		int suspiciousNodeOrder;
		ChosenVariableOption option;
		public Attempt(int suspiciousNodeOrder, ChosenVariableOption option) {
			super();
			this.suspiciousNodeOrder = suspiciousNodeOrder;
			this.option = option;
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Attempt){
				Attempt other = (Attempt)obj;
				if(this.suspiciousNodeOrder == other.suspiciousNodeOrder){
					
					String thisReadVarID = (this.option.getReadVar()!=null)? this.option.getReadVar().getVarID() : "null";
					String thatReadVarID = (other.option.getReadVar()!=null)? other.option.getReadVar().getVarID() : "null";
					
					String thisWrittenVarID = (this.option.getWrittenVar()!=null)? this.option.getWrittenVar().getVarID() : "null";
					String thatWrittenVarID = (other.option.getWrittenVar()!=null)? other.option.getWrittenVar().getVarID() : "null";
					
					if(thisReadVarID.equals(thatReadVarID) && thisWrittenVarID.equals(thatWrittenVarID)){
						return true;
					}
				}
			}
			
			return false;
		}
		
		@Override
		public int hashCode(){
			int readVarCode = (this.option.getReadVar() != null) ? this.option.getReadVar().getVarID().hashCode() : 0;
			int writtenVarCode = (this.option.getWrittenVar() != null) ? this.option.getWrittenVar().getVarID().hashCode() : 0;
			
			return this.suspiciousNodeOrder + readVarCode + writtenVarCode;
		}
		
		@Override
		public String toString(){
			StringBuffer buffer = new StringBuffer();
			buffer.append("order: " + this.suspiciousNodeOrder + "\n");
			buffer.append(this.option.toString());
			return buffer.toString();
		}
	}
	
	class FeedbackResult{
		TraceNode suspiciousNode;
		UserFeedback userFeedback;
		
		public FeedbackResult(TraceNode suspiciousNode, UserFeedback feedback) {
			this.suspiciousNode = suspiciousNode;
			this.userFeedback = feedback;
		}
	}
	
	private Trial startSimulation(TraceNode observedFaultNode, TraceNode rootCause, Trace mutatedTrace, Trace originalTrace,
			PairList pairList, String testCaseName, String mutatedFile, 
			double unclearRate, boolean enableLoopInference, int optionSearchLimit) 
					throws SimulationFailException {
		
		Settings.interestedVariables.clear();
		Settings.localVariableScopes.clear();
		Settings.potentialCorrectPatterns.clear();
		
		/**
		 * this variable is for optimization. 
		 * when a suspicious node with certain option (i.e., wrong variables) is popped out
		 * of the confusing stack, it means that it is not possible to find the mutated bug
		 * with this option on the suspicious node. Therefore, there is no need to try such
		 * attempt again.
		 */
		HashSet<Attempt> failedAttempts = new HashSet<>();
		
		recommender = new StepRecommender(enableLoopInference);
		user = new SimulatedUser();
		
		int traceLength = mutatedTrace.getExectionList().size();
		int maxUnclearFeedbackNum = (int)(traceLength*unclearRate);
		if(unclearRate == -1){
			maxUnclearFeedbackNum = traceLength;
		}
		
		Stack<StateWrapper> confusingStack = new Stack<>();
		ArrayList<StepOperationTuple> jumpingSteps = new ArrayList<>();
		
		try{
			TraceNode lastNode = observedFaultNode;
			TraceNode suspiciousNode = observedFaultNode;
			
			UserFeedback feedback = operateFeedback(observedFaultNode,
					mutatedTrace, pairList, maxUnclearFeedbackNum, confusingStack,
					jumpingSteps, true, failedAttempts);
			
			TraceNodePair pair = pairList.findByAfterNode(suspiciousNode);
			TraceNode referenceNode = (pair==null)? null : pair.getBeforeNode();
			
			jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedback, referenceNode, recommender.getState()));
			
			if(!feedback.getFeedbackType().equals(UserFeedback.UNCLEAR)){
				setCurrentNodeChecked(mutatedTrace, suspiciousNode);		
				updateVariableCheckTime(mutatedTrace, suspiciousNode);
			}
			
			int optionSearchTime = 0;
			StateWrapper currentConfusingState = null;
			
			boolean isBugFound = rootCause.getLineNumber()==suspiciousNode.getLineNumber();
			while(!isBugFound){
				TraceNode originalSuspiciousNode = suspiciousNode;
				
				suspiciousNode = findSuspicioiusNode(suspiciousNode, mutatedTrace, feedback);	
				
				if(suspiciousNode==null && feedback.getFeedbackType().equals(UserFeedback.WRONG_PATH)){
					UserFeedback f = new UserFeedback();
					f.setFeedbackType("Missing Control Dominator!");
					jumpingSteps.add(new StepOperationTuple(originalSuspiciousNode, f, null, recommender.getState()));
					break;
				}
				
				/** It means that the bug cannot be found now */
				if((jumpingSteps.size() > mutatedTrace.size())  
						|| (isContainedInJump(suspiciousNode, jumpingSteps) && unclearRate==0) 
						|| (lastNode.getOrder()==suspiciousNode.getOrder() && !feedback.getFeedbackType().equals(UserFeedback.UNCLEAR))
						/*|| cannotConverge(jumpingSteps)*/){
//					break;
					
					if(currentConfusingState != null){
						Attempt attempt = new Attempt(currentConfusingState.getState().getCurrentNodeOrder(), 
								currentConfusingState.getVariableOption());
						failedAttempts.add(attempt);
					}
					
					System.out.println("=========An attempt fails=========");
					for(StepOperationTuple t: jumpingSteps){
						System.err.println(t);	
						Thread.sleep(10);
					}
					System.out.println();
					
					if(!confusingStack.isEmpty()){
						if(optionSearchTime > optionSearchLimit){
							return null;
						}
						else{
							optionSearchTime++;
						}
						
						/** recover */
						StateWrapper stateWrapper = confusingStack.pop();
						currentConfusingState = stateWrapper;
						
						jumpingSteps = stateWrapper.getJumpingSteps();

						CheckingState state = stateWrapper.getState();
						int checkTime = state.getTraceCheckTime();
						
						mutatedTrace.setCheckTime(state.getTraceCheckTime());
						suspiciousNode = mutatedTrace.getExectionList().get(state.getCurrentNodeOrder()-1);
						suspiciousNode.setSuspicousScoreMap(state.getCurrentNodeSuspicousScoreMap());
						suspiciousNode.setCheckTime(state.getCurrentNodeCheckTime());
						
						Settings.interestedVariables = state.getInterestedVariables();
						Settings.potentialCorrectPatterns = state.getPotentialCorrectPatterns();
						Settings.wrongPathNodeOrder = state.getWrongPathNodeOrder();
						recommender = state.getRecommender();
						
						ChosenVariableOption option = stateWrapper.getVariableOption();
						for(String wrongVarID: option.getIncludedWrongVarID()){
							Settings.interestedVariables.add(wrongVarID, checkTime);
						}
						feedback = new UserFeedback(option, UserFeedback.WRONG_VARIABLE_VALUE);
						
						pair = pairList.findByAfterNode(suspiciousNode);
						referenceNode = (pair==null)? null : pair.getBeforeNode();
						jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedback, referenceNode, recommender.getState()));
					}
					else{
						break;						
					}
				}
				else{
					isBugFound = rootCause.getLineNumber()==suspiciousNode.getLineNumber();
					
					if(!isBugFound){
						if(suspiciousNode.getOrder() == 132){
//							System.currentTimeMillis();
						}
						
						
						feedback = operateFeedback(suspiciousNode,
								mutatedTrace, pairList, maxUnclearFeedbackNum, confusingStack,
								jumpingSteps, false, failedAttempts);

						/**
						 * the logic here is different from microbat
						 */
						if(feedback.getFeedbackType().equals(UserFeedback.CORRECT)){
							
							FeedbackResult result = applyAlignmentSlicing(jumpingSteps, suspiciousNode, 
									feedback, mutatedTrace, pairList, confusingStack, failedAttempts, maxUnclearFeedbackNum);
							if(result != null){
								suspiciousNode = result.suspiciousNode;
								feedback = result.userFeedback;
							}
						}
						else{
							pair = pairList.findByAfterNode(suspiciousNode);
							referenceNode = (pair==null)? null : pair.getBeforeNode();
							
							jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedback, referenceNode, recommender.getState()));	
							
							if(!feedback.getFeedbackType().equals(UserFeedback.UNCLEAR)){
								setCurrentNodeChecked(mutatedTrace, suspiciousNode);		
								updateVariableCheckTime(mutatedTrace, suspiciousNode);
							}
						}
					}
					else{
						UserFeedback f = new UserFeedback();
						f.setFeedbackType("Bug Found!");
						jumpingSteps.add(new StepOperationTuple(suspiciousNode, f, null, recommender.getState()));
					}
					
				}
				
				lastNode = suspiciousNode;
			}
			
			System.out.println("number of attempts: " + optionSearchTime);
			Trial trial = constructTrial(rootCause, mutatedTrace, originalTrace, testCaseName,
					mutatedFile, isBugFound, jumpingSteps);
			
			return trial;
		}
		catch(Exception e){
			e.printStackTrace();
			for(StepOperationTuple t: jumpingSteps){
				System.err.println(t);				
			}
			System.out.println();
			String msg = "The program stuck in " + testCaseName +", the mutated line is " + rootCause.getLineNumber();
			SimulationFailException ex = new SimulationFailException(msg);
			throw ex;
		}
	}
	
	private FeedbackResult applyAlignmentSlicing(ArrayList<StepOperationTuple> jumpingSteps, TraceNode suspiciousNode, 
			UserFeedback feedback, Trace mutatedTrace, PairList pairList, Stack<StateWrapper> confusingStack, 
			HashSet<Attempt> failedAttempts, int maxUnclearFeedbackNum) {
		
		StepOperationTuple tuple = jumpingSteps.get(jumpingSteps.size()-1);
		TraceNode oldSuspiciousNode = tuple.getNode();
		UserFeedback oldFeedback = tuple.getUserFeedback();
		if(oldFeedback.getFeedbackType().equals(UserFeedback.WRONG_VARIABLE_VALUE)){
			TraceNodePair oldPair = pairList.findByAfterNode(oldSuspiciousNode);
			TraceNode originalNode = oldPair.getBeforeNode();
			VarValue readVarInMutation = oldFeedback.getOption().getReadVar();
			
			VarValue readVarInOrigin = findMatchOriginalVar(readVarInMutation, originalNode);
			
			TraceNode dataDominator = originalNode.findDataDominator(readVarInOrigin);
			
			/**
			 * for now, the dataDominator must not find a corresponding mutated node.
			 */
			if(dataDominator != null){
				TraceNode controlDom = dataDominator.getControlDominator();
				TraceNodePair conPair = pairList.findByBeforeNode(controlDom);
				while(conPair == null){
					controlDom = controlDom.getControlDominator();
					conPair = pairList.findByBeforeNode(controlDom);
				}
				
				suspiciousNode = conPair.getAfterNode();
				feedback = operateFeedback(suspiciousNode,
						mutatedTrace, pairList, maxUnclearFeedbackNum, confusingStack,
						jumpingSteps, false, failedAttempts);
				
				jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedback, controlDom, recommender.getState()));
				
				if(!feedback.getFeedbackType().equals(UserFeedback.UNCLEAR)){
					setCurrentNodeChecked(mutatedTrace, suspiciousNode);		
					updateVariableCheckTime(mutatedTrace, suspiciousNode);
				}
				
				FeedbackResult result = new FeedbackResult(suspiciousNode, feedback);
				return result;
			}
			
		}
		else{
			System.out.println("correct feedback follows wrong-path feedback");
			
		}
		
		return null;
	}


	private VarValue findMatchOriginalVar(VarValue readVarInMutation, TraceNode originalNode) {
		for(VarValue readVar: originalNode.getReadVariables()){
			if(readVar.getVarName().equals(readVarInMutation.getVarName())){
				return readVar;
			}
			else{
				List<VarValue> children = readVar.getAllDescedentChildren();
				for(VarValue child: children){
					if(child.getVarName().equals(readVarInMutation.getVarName())){
						return child;
					}
				}
			}
		}
		return null;
	}


	private boolean cannotConverge(ArrayList<StepOperationTuple> jumpingSteps) {
		if(jumpingSteps.size() > 10){
			int size = jumpingSteps.size();
			StepOperationTuple checkingStep = jumpingSteps.get(size-1);
			
			boolean canFindRecurringStep = canFindRecurringStep(checkingStep, jumpingSteps, size-1);
			if(canFindRecurringStep){
				return true;
			}
		}
		
		return false;
	}


	private boolean canFindRecurringStep(StepOperationTuple checkingStep, ArrayList<StepOperationTuple> jumpingSteps,
			int limit) {
		
		if(checkingStep.getUserFeedback().getFeedbackType().equals(UserFeedback.UNCLEAR) ||
				checkingStep.getUserFeedback().getFeedbackType().equals(UserFeedback.CORRECT)){
			return false;
		}
		
		for(int i=limit-1; i>=0; i--){
			StepOperationTuple step = jumpingSteps.get(i);
			
			
			if(step.getNode().getOrder() == checkingStep.getNode().getOrder()){
				if(!step.getUserFeedback().getFeedbackType().equals(UserFeedback.UNCLEAR)){
					if(step.getUserFeedback().equals(checkingStep.getUserFeedback())){
						return true;
					}
				}
			}
		}
		
		return false;
	}


	private boolean isContainedInJump(TraceNode suspiciousNode, ArrayList<StepOperationTuple> jumpingSteps) {
		
		for(int i=jumpingSteps.size()-1; i>=0; i--){
			StepOperationTuple tuple = jumpingSteps.get(i);
			if(tuple.getNode().getOrder() == suspiciousNode.getOrder()){
				return true;
			}
		}
		
		return false;
	}


	@SuppressWarnings("unchecked")
	/**
	 * Apart from feedback, this method also back up the state for future re-trial.
	 * 
	 * @param observedFaultNode
	 * @param mutatedTrace
	 * @param pairList
	 * @param enableClear
	 * @param confusingStack
	 * @param jumpingSteps
	 * @param suspiciousNode
	 * @param isFirstTime
	 * @return
	 */
	private UserFeedback operateFeedback(TraceNode suspiciousNode,
			Trace mutatedTrace, PairList pairList, int maxUnclearFeedbackNum,
			Stack<StateWrapper> confusingStack,
			ArrayList<StepOperationTuple> jumpingSteps,
			boolean isFirstTime, HashSet<Attempt> failedAttempts) {
		
		UserInterestedVariables interestedVariables = Settings.interestedVariables.clone();
		
		UserFeedback feedbackType = user.feedback(suspiciousNode, mutatedTrace, pairList, 
				mutatedTrace.getCheckTime(), isFirstTime, maxUnclearFeedbackNum);
		
		int size = user.getOtherOptions().size();
		for(int i=size-1; i>=0; i--){
			CheckingState state = new CheckingState();
			state.recordCheckingState(suspiciousNode, recommender, mutatedTrace, 
					interestedVariables, Settings.wrongPathNodeOrder, Settings.potentialCorrectPatterns);
			
			ChosenVariableOption option = user.getOtherOptions().get(i);
			ArrayList<StepOperationTuple> clonedJumpingSteps = (ArrayList<StepOperationTuple>) jumpingSteps.clone();
			StateWrapper stateWrapper = new StateWrapper(state, option, clonedJumpingSteps);
			
			Attempt newAttempt = new Attempt(suspiciousNode.getOrder(), option);
			
			if(!failedAttempts.contains(newAttempt)){
				confusingStack.push(stateWrapper);				
			}
			
		}
		
		return feedbackType;
	}

	private Trial constructTrial(TraceNode rootCause, Trace mutatedTrace, Trace originalTrace,
			String testCaseName, String mutatedFile, boolean isBugFound, List<StepOperationTuple> jumpingSteps) {
		
		List<String> jumpStringSteps = new ArrayList<>();
		System.out.println("bug found: " + isBugFound);
		for(StepOperationTuple tuple: jumpingSteps){
			String correspondingStr = (tuple.getReferenceNode()==null)? "" : tuple.getReferenceNode().toString();
			
			String str = tuple.getNode().toString() + ": " + tuple.getUserFeedback() + " ... "
				+ correspondingStr + "\n";
			System.out.print(str);		
			jumpStringSteps.add(str);
		}
		System.out.println("Root Cause:" + rootCause);
		
		Trial trial = new Trial();
		trial.setTestCaseName(testCaseName);
		trial.setBugFound(isBugFound);
		trial.setMutatedLineNumber(rootCause.getLineNumber());
		trial.setJumpSteps(jumpStringSteps);
		trial.setTotalSteps(mutatedTrace.size());
		trial.setOriginalTotalSteps(originalTrace.size());
		trial.setMutatedFile(mutatedFile);
		trial.setResult(isBugFound? Trial.SUCESS : Trial.FAIL);
		return trial;
	}
	
	private void setCurrentNodeChecked(Trace trace, TraceNode currentNode) {
		int checkTime = trace.getCheckTime()+1;
		currentNode.setCheckTime(checkTime);
		trace.setCheckTime(checkTime);
	}
	
	private void updateVariableCheckTime(Trace trace, TraceNode currentNode) {
		for(VarValue var: currentNode.getReadVariables()){
			String varID = var.getVarID();
			if(Settings.interestedVariables.contains(varID)){
				Settings.interestedVariables.add(varID, trace.getCheckTime());
			}
		}
		
		for(VarValue var: currentNode.getWrittenVariables()){
			String varID = var.getVarID();
			if(Settings.interestedVariables.contains(varID)){
				Settings.interestedVariables.add(varID, trace.getCheckTime());
			}
		}
	}

	
	protected List<TraceNode> findAllDominatees(Trace mutationTrace, ClassLocation mutatedLocation){
		Map<Integer, TraceNode> allDominatees = new HashMap<>();
		
		for(TraceNode mutatedNode: mutationTrace.getExectionList()){
			if(mutatedNode.getClassCanonicalName().equals(mutatedLocation.getClassCanonicalName()) 
					&& mutatedNode.getLineNumber() == mutatedLocation.getLineNo()){
				
				if(allDominatees.get(mutatedNode.getOrder()) == null){
					Map<Integer, TraceNode> dominatees = mutatedNode.findAllDominatees();
					allDominatees.putAll(dominatees);
					allDominatees.put(mutatedNode.getOrder(), mutatedNode);
				}
				
			}
		}
		
		return new ArrayList<>(allDominatees.values());
	}
	
	private TraceNode findRootCause(String className, int lineNo, Trace mutatedTrace, PairList pairList) {
		for(TraceNode node: mutatedTrace.getExectionList()){
			if(node.getDeclaringCompilationUnitName().equals(className) && node.getLineNumber()==lineNo){
				TraceNodePair pair = pairList.findByAfterNode(node);
				
				if(pair != null){
					return pair.getAfterNode();
				}
				
			}
		}
		
		return null;
	}

	private TraceNode findSuspicioiusNode(TraceNode currentNode, Trace trace, UserFeedback feedbackType) {
		setCurrentNodeCheck(trace, currentNode);
		
		
		if(!feedbackType.equals(UserFeedback.UNCLEAR)){
			setCurrentNodeCheck(trace, currentNode);					
		}
		
		TraceNode suspiciousNode = recommender.recommendNode(trace, currentNode, feedbackType);
		return suspiciousNode;
		
//		TraceNode suspiciousNode = null;
//		
//		ConflictRuleChecker conflictRuleChecker = new ConflictRuleChecker();
//		TraceNode conflictNode = conflictRuleChecker.checkConflicts(trace, currentNode.getOrder());
//		
//		if(conflictNode == null){
//			suspiciousNode = recommender.recommendNode(trace, currentNode, feedbackType);
//		}
//		else{
//			suspiciousNode = conflictNode;
//		}
//		
//		return suspiciousNode;
	}
	
	private void setCurrentNodeCheck(Trace trace, TraceNode currentNode) {
		int checkTime = trace.getCheckTime()+1;
		currentNode.setCheckTime(checkTime);
		trace.setCheckTime(checkTime);
	}
}
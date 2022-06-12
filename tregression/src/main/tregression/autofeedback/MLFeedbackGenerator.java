package tregression.autofeedback;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.stepvectorizer.StepVectorizer;
import tregression.views.BuggyTraceView;
import tregression.views.CorrectTraceView;

public final class MLFeedbackGenerator extends FeedbackGenerator {
	
	/**
	 * It is used to communicate with the server.
	 */
	private ModelClient client;
	
	/**
	 * It stores all the previous trace nodes.
	 */
	private List<NodeFeedbackPair> correctnessRecords;
	
	/**
	 * It only stores the record of data incorrect and control incorrect
	 */
	private List<NodeFeedbackPair> incorrectnessRecords;

	/**
	 * If the certainty of the classification is within the range [50 - uncertaintyRange, 50 + uncertaintyRange]
	 * The the classification is considered to be uncertain.
	 */
	private final float uncertaintyRange = (float) 0.02;
	
	public MLFeedbackGenerator(Trace trace, AutoFeedbackMethods method) {
		super(trace, method);
		this.client = new ModelClient();
		this.correctnessRecords = new ArrayList<>();
		this.incorrectnessRecords = new ArrayList<>();
	}

	@Override
	public UserFeedback giveFeedback(TraceNode node) {
		
		// If there are no previous feedback, then it must be uncertain
		if (this.correctnessRecords.isEmpty()) {
			UserFeedback feedback = new UserFeedback(UserFeedback.UNCLEAR);
			return feedback;
		}
		
		UserFeedback finalFeedback = new UserFeedback();
		
		// First classify is the node correct or wrong by voting
		List<UserFeedback> votingFeedbacks = this.collectCorrectVotingFeedbacks(node);
		UserFeedback electedFeedback = this.getElectedFeedback(votingFeedbacks);
		
		if (electedFeedback.getFeedbackType() == UserFeedback.UNCLEAR) {
			return electedFeedback;
		} else if (electedFeedback.getFeedbackType() == UserFeedback.CORRECT) {
			finalFeedback = electedFeedback;
		} else {
			// Now the step is classified to be Incorrect
			// Decide is it Data Incorrect or Control Incorrect
			votingFeedbacks = this.collectIncorrectVotingFeedbacks(node);
			electedFeedback = this.getElectedFeedback(votingFeedbacks);
			if (electedFeedback.getFeedbackType() == UserFeedback.UNCLEAR) {
				return electedFeedback;
			} else {
				finalFeedback = electedFeedback;
			}
		}
		
		// If the elected feedback is data incorrect. Assign a random variable to be wrong
		// Note: It is possible that the model said a step without read variable is Data Incorrect.
		if (electedFeedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE) {
			List<VarValue> readVars = node.getReadVariables();
			readVars = this.removeVarsGenByJava(readVars);
			VarValue wrongVar = this.getRandVar(readVars, false);
			finalFeedback.setOption(new ChosenVariableOption(wrongVar, null));
		}
		
		this.addRecord(node, finalFeedback);
		return finalFeedback;
	}
	
	/**
	 * Get all the voting feedback for the given node to be Correct or Wrong
	 * @param node Target trace node
	 * @return List of voting feedbacks
	 */
	private List<UserFeedback> collectCorrectVotingFeedbacks(TraceNode node) {
		List<UserFeedback> votingFeedbacks = new ArrayList<>();
		StepVectorizer stepVectorizer = new StepVectorizer(this.trace);
		
		for (NodeFeedbackPair pair : this.correctnessRecords) {
			TraceNode ref_node = pair.getNode();
			UserFeedback candidateFeedback = new UserFeedback();
			
			String target_vec = stepVectorizer.vectorize(node.getOrder()).convertToCSV();
			String ref_vec = stepVectorizer.vectorize(ref_node.getOrder()).convertToCSV();
			
			float result = this.client.requestCorrectClassification(target_vec, ref_vec);
			if (result >= 0.5 + this.uncertaintyRange) {
				candidateFeedback.setFeedbackType(pair.getFeedback().getFeedbackType());
			} else {
				candidateFeedback.setFeedbackType(UserFeedback.UNCLEAR);
			}
			votingFeedbacks.add(candidateFeedback);
			System.out.println(result);
		}
		return votingFeedbacks;
	}
	
	/**
	 * Get all the voting feedback for the give node to be Data Incorrect or Control Incorrect
	 * @param node Target trace node
	 * @return List of voting feedbacks
	 */
	private List<UserFeedback> collectIncorrectVotingFeedbacks(TraceNode node) {
		List<UserFeedback> votingFeedbacks = new ArrayList<>();
		StepVectorizer stepVectorizer = new StepVectorizer(this.trace);
		
		for (NodeFeedbackPair pair : this.incorrectnessRecords) {
			TraceNode ref_node = pair.getNode();
			UserFeedback candidateFeedback = new UserFeedback();
			
			String target_vec = stepVectorizer.vectorize(node.getOrder()).convertToCSV();
			String ref_vec = stepVectorizer.vectorize(ref_node.getOrder()).convertToCSV();
			
			float result = this.client.requestIncorrectClassification(target_vec, ref_vec);
			if (result >= 0.5 + this.uncertaintyRange) {
				candidateFeedback.setFeedbackType(pair.getFeedback().getFeedbackType());
			} else {
				candidateFeedback.setFeedbackType(UserFeedback.UNCLEAR);
			}
			votingFeedbacks.add(candidateFeedback);
			System.out.println(result);
		}
		return votingFeedbacks;
	}
	/**
	 * From a list of feedback, get the majority feedback.
	 * Unclear feedback is ignored as long as there are at least one clear feedback
	 * @param votingFeedbacks List of candidate feedbacks
	 * @return final feedback (No wrong variable given even when Data Incorrect)
	 */
	private UserFeedback getElectedFeedback(final List<UserFeedback> votingFeedbacks) {
		int correctCount = 0;
		int DICount = 0;
		int CICount = 0;
		for (UserFeedback candidateFeedback : votingFeedbacks) {
			if (candidateFeedback.getFeedbackType() == UserFeedback.CORRECT) {
				correctCount++;
			} else if (candidateFeedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE) {
				DICount++;
			} else if (candidateFeedback.getFeedbackType() == UserFeedback.WRONG_PATH) {
				CICount++;
			}
		}
		
		UserFeedback electedFeedback = new UserFeedback();
		if (correctCount == 0 && DICount == 0 && CICount == 0) {
			electedFeedback.setFeedbackType(UserFeedback.UNCLEAR);
		} else if (correctCount == DICount && correctCount == CICount) {
			// If there is a tie, that mean the step has higher change to be wrong.
			// Whether is it Data Incorrect or Control Incorrect will be evaluated later
			electedFeedback.setFeedbackType(UserFeedback.WRONG_PATH); 
		} else if (correctCount > DICount && correctCount > CICount) {
			electedFeedback.setFeedbackType(UserFeedback.CORRECT);
			// If correctCount is 0, then it is incorrect classification
			// If there is a tie, then the result will be unclear
		} else if (correctCount == 0 && DICount == CICount) {
			electedFeedback.setFeedbackType(UserFeedback.UNCLEAR);
		} else if (DICount > CICount) {
			electedFeedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
		} else if (CICount >= DICount) {
			electedFeedback.setFeedbackType(UserFeedback.WRONG_PATH);
		}
		return electedFeedback;
	}
	
	/**
	 * Add a node feedback pair into record
	 * @param node Node in the pair
	 * @param feedback Feedback in the pair
	 */
	private void addRecord(TraceNode node, UserFeedback feedback) {
		NodeFeedbackPair pair = new NodeFeedbackPair(node, feedback);
		this.addRecord(pair);
	}

	/**
	 * Add a node feedback pair into record
	 * @param pair Node feedback pair
	 */
	private void addRecord(NodeFeedbackPair pair) {
		this.correctnessRecords.add(pair);
		if (pair.getFeedback().getFeedbackType() == UserFeedback.WRONG_PATH ||
			pair.getFeedback().getFeedbackType() == UserFeedback.WRONG_PATH) {
			this.incorrectnessRecords.add(pair);
		}
	}
	
	@Override
	public void notifyEnd() {
		this.client.endServer();
	}
	
	@Override
	public UserFeedback requestUserFeedback(TraceNode node, BuggyTraceView buggyView, CorrectTraceView correctView) {
		UserFeedback feedback = super.requestUserFeedback(node, buggyView, correctView);
		this.addRecord(node, feedback);
		return feedback;
	}
}
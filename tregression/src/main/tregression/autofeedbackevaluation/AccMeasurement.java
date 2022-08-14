package tregression.autofeedbackevaluation;

import java.util.Arrays;
import java.util.List;

import microbat.baseline.encoders.NodeFeedbackPair;
import microbat.recommendation.UserFeedback;

/**
 * AccMeasurement is used to store the evaluation result of debugging
 * @author David
 *
 */
public class AccMeasurement {
	
	public static final double NaN = -1;
	
	/**
	 * Target project name
	 */
	private String projectName;
	
	/**
	 * Target bug id
	 */
	private int bugID;
	
	/**
	 * Number of user feedback needed
	 */
	private int noOfFeedbackNeeded;
	
	/**
	 * Precision of Correct feedback
	 */
	private double precisionCorrect;
	
	/**
	 * Recall of Correct feedback
	 */
	private double recallCorrect;
	
	/**
	 * Precision of Data Incorrect feedback
	 */
	private double precisionDI;
	
	/**
	 * Recall of Data Incorrect feedback
	 */
	private double recallDI;
	
	/**
	 * Precision of Control Incorrect feedback
	 */
	private double precisionCI;
	/**
	 * Recall of Control Incorrect feedback
	 */
	private double recallCI;
	
	/**
	 * Accuracy for wrong variable prediction
	 */
	private double varAcc;
	/**
	 * Accuracy of feedback prediction, without considering the wrong variable prediction
	 */
	private double overallTypeAcc;
	/**
	 * Accuracy of feedback prediction 
	 */
	private double overallAcc;

	public AccMeasurement(String projectName, int bugID) {
		this.projectName = projectName;
		this.bugID = bugID;
		this.noOfFeedbackNeeded = 0;
		this.precisionCorrect = AccMeasurement.NaN;
		this.recallCorrect = AccMeasurement.NaN;
		this.precisionDI = AccMeasurement.NaN;
		this.recallDI = AccMeasurement.NaN;
		this.precisionCI = AccMeasurement.NaN;
		this.recallCI = AccMeasurement.NaN;
		this.varAcc = AccMeasurement.NaN;
		this.overallAcc = AccMeasurement.NaN;
		this.overallAcc = AccMeasurement.NaN;
	}
	
	public AccMeasurement(String projectName, int bugID,
						  int noOfFeedbackNeeded,
						  double precisionCorrect, double recallCorrect,
						  double precisionDI, double recallDI,
						  double precisionCI, double recallCI,
						  double varAcc,
						  double overallTypeAcc, double overallAcc) {
		this.projectName = projectName;
		this.bugID = bugID;
		this.noOfFeedbackNeeded = noOfFeedbackNeeded;
		this.precisionCorrect = precisionCorrect;
		this.recallCorrect = recallCorrect;
		this.precisionDI = precisionDI;
		this.recallDI = recallDI;
		this.precisionCI = precisionCI;
		this.recallCI = recallCI;
		this.varAcc = varAcc;
		this.overallAcc = overallTypeAcc;
		this.overallAcc = overallAcc;
	}
	
	public AccMeasurement(String projectName, int bugID, int noOfFeedbackNeeded, List<NodeFeedbackPair> predicted, List<NodeFeedbackPair> ref) {
		this.projectName = projectName;
		this.bugID = bugID;
		this.noOfFeedbackNeeded = noOfFeedbackNeeded;
		
		this.precisionCorrect = this.calPrecision(predicted, ref, UserFeedback.CORRECT);
		this.recallCorrect = this.calRecall(predicted, ref, UserFeedback.CORRECT);
		
		this.precisionDI = this.calPrecision(predicted, ref, UserFeedback.WRONG_VARIABLE_VALUE);
		this.recallDI = this.calRecall(predicted, ref, UserFeedback.WRONG_VARIABLE_VALUE);
		
		this.precisionCI = this.calPrecision(predicted, ref, UserFeedback.WRONG_PATH);
		this.recallCI = this.calRecall(predicted, ref, UserFeedback.WRONG_PATH);
		
		this.varAcc = this.calVarAcc(predicted, ref);
		
		this.overallTypeAcc = this.calOverallTypeAcc(predicted, ref);
		this.overallAcc = this.calOverallAcc(predicted, ref);
	}
	
	public static AccMeasurement parseAccMeasurement(String csvString) {
		List<String> tokens = Arrays.asList(csvString.split(","));
		
		String projectName = tokens.get(0);
		int bugID = Integer.parseInt(tokens.get(1));
		
		int noOfFeedbackNeeded = Integer.parseInt(tokens.get(2));
		
		double precisionCorrect = Double.parseDouble(tokens.get(3));
		double recallCorrect = Double.parseDouble(tokens.get(4));
		
		double precisionDI = Double.parseDouble(tokens.get(5));
		double recallDI = Double.parseDouble(tokens.get(6));
		
		double precisionCI = Double.parseDouble(tokens.get(7));
		double recallCI = Double.parseDouble(tokens.get(8));
		
		double varAcc = Double.parseDouble(tokens.get(9));
		
		double overallTypeAcc = Double.parseDouble(tokens.get(10));
		double overallAcc = Double.parseDouble(tokens.get(11));
		
		return new AccMeasurement(projectName, bugID, noOfFeedbackNeeded, 
				                  precisionCorrect, recallCorrect,
				                  precisionDI, recallDI,
				                  precisionCI, recallCI,
				                  varAcc,
				                  overallTypeAcc, overallAcc);
	}
	
	/**
	 * Calculate the precision of given target feedback type
	 * @param predictions List of feedback predicted
	 * @param refs List of ground truth feedback
	 * @param targetFeedbackType Target feedback type. Must be one of the value defined in UserFeedback
	 * @return Precision of target feedback type
	 */
	public double calPrecision(final List<NodeFeedbackPair> predictions, final List<NodeFeedbackPair> refs, final String targetFeedbackType) {
		int totalFeedbackCount = 0;
		int validFeedbackCount = 0;
		
		if (predictions.size() != refs.size()) {
			return AccMeasurement.NaN;
		}
		
		for (int i=0; i<predictions.size(); ++i) {
			NodeFeedbackPair prediction = predictions.get(i);
			NodeFeedbackPair ref = refs.get(i);
			
			if (prediction.getNode().getOrder() != ref.getNode().getOrder()) {
				this.printWrongMatchingError(prediction.getNode().getOrder(), ref.getNode().getOrder());
				continue;
			}

			if (prediction.getFeedback().getFeedbackType() == targetFeedbackType) {
				totalFeedbackCount++;
				if (ref.getFeedback().getFeedbackType() == targetFeedbackType) {
					validFeedbackCount++;
				}
			}
		}
		
		// Precision if not available when the target feedback type does not appear once
		if (totalFeedbackCount == 0) {
			return AccMeasurement.NaN;
		}
		
		return (double) validFeedbackCount / totalFeedbackCount; // Need to cast to double first to avoid rounding
	}
	
	/**
	 * Calculate the recall of given target feedback type
	 * @param predictions List of feedback predicted
	 * @param refs List of ground truth feedback
	 * @param targetFeedbackType Target feedback type. Must be one of the value defined in UserFeedback
	 * @return Recall of target feedback type
	 */
	public double calRecall(final List<NodeFeedbackPair> predictions, final List<NodeFeedbackPair> refs, final String targetFeedbackType) {
		int totalFeedbackCount = 0;
		int validFeedbackCount = 0;
		
		if (predictions.size() != refs.size()) {
			return AccMeasurement.NaN;
		}
		
		for (int i=0; i<predictions.size(); ++i) {
			NodeFeedbackPair prediction = predictions.get(i);
			NodeFeedbackPair ref = refs.get(i);
			
			if (prediction.getNode().getOrder() != ref.getNode().getOrder()) {
				this.printWrongMatchingError(prediction.getNode().getOrder(), ref.getNode().getOrder());
				continue;
			}
			
			if (prediction.getFeedback().getFeedbackType() == targetFeedbackType) {
				if (ref.getFeedback().getFeedbackType() == targetFeedbackType) {
					validFeedbackCount++;
				}
			}
			
			if (ref.getFeedback().getFeedbackType() == targetFeedbackType) {
				totalFeedbackCount++;
			}
		}
		
		// Recall is not available when target feedback type does not appear
		if (totalFeedbackCount == 0) {
			return AccMeasurement.NaN;
		}
		
		return (double) validFeedbackCount / totalFeedbackCount; // Need to cast to double first to avoid rounding
	}
	
	/**
	 * Calculate the accuracy of for wrong variable prediction
	 * @param predictions List of feedback predicted
	 * @param refs List of ground truth feedback
	 * @return Accuracy of wrong variable prediction 
	 */
	public double calVarAcc(final List<NodeFeedbackPair> predictions, final List<NodeFeedbackPair> refs) {
		
		if (predictions.size() != refs.size()) {
			return AccMeasurement.NaN;
		}
		
		int totalPredictionCount = 0;
		int validPredictionCount = 0;
		
		for (int i=0; i<predictions.size(); ++i) {
			NodeFeedbackPair prediction = predictions.get(i);
			NodeFeedbackPair ref = refs.get(i);
			
			if (prediction.getNode().getOrder() != ref.getNode().getOrder()) {
				this.printWrongMatchingError(prediction.getNode().getOrder(), ref.getNode().getOrder());
				continue;
			}
			
			UserFeedback predictedFeedback = prediction.getFeedback();
			UserFeedback refFeedback = ref.getFeedback();
			
			if (predictedFeedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE &&
				refFeedback.getFeedbackType() == UserFeedback.WRONG_VARIABLE_VALUE) {
				totalPredictionCount++;
				if(predictedFeedback.getOption().getReadVar() == refFeedback.getOption().getReadVar()) {
					validPredictionCount++;
				}
			}
			
		}
		
		if (totalPredictionCount == 0) {
			return AccMeasurement.NaN;
		}
		
		return (double) validPredictionCount / totalPredictionCount; // Cast to double first to avoid rounding
	}
	
	public double calOverallTypeAcc(final List<NodeFeedbackPair> predictions, final List<NodeFeedbackPair> refs) {
		if (predictions.size() != refs.size()) {
			return AccMeasurement.NaN;
		}
		
		int validFeedbackCount = 0;
		int totalFeedbackCount = predictions.size();
		if (totalFeedbackCount == 0) {
			return AccMeasurement.NaN;
		}
		
		for (int i=0; i<predictions.size(); ++i) {
			NodeFeedbackPair prediction = predictions.get(i);
			NodeFeedbackPair ref = refs.get(i);
			
			if (prediction.getFeedback().getFeedbackType() == ref.getFeedback().getFeedbackType()) {
				validFeedbackCount++;
			}
		}
		
		return (double) validFeedbackCount / totalFeedbackCount;
	}
	
	public double calOverallAcc(final List<NodeFeedbackPair> predictions, final List<NodeFeedbackPair> refs) {
		if (predictions.size() != refs.size()) {
			return AccMeasurement.NaN;
		}
		
		int validFeedbackCount = 0;
		int totalFeedbackCount = predictions.size();
		if (totalFeedbackCount == 0) {
			return AccMeasurement.NaN;
		}
		
		for (int i=0; i<predictions.size(); ++i) {
			NodeFeedbackPair prediction = predictions.get(i);
			NodeFeedbackPair ref = refs.get(i);
			
			if (prediction.getFeedback() == ref.getFeedback()) {
				validFeedbackCount++;
			}
		}
		
		return (double) validFeedbackCount / totalFeedbackCount;
	}
	
	public void setnoOfFeedbackNeeded(int noOfFeedbackNeeded) {
		this.noOfFeedbackNeeded = noOfFeedbackNeeded;
	}
	
	public void setPrecisionCorrect(double precisionCorrect) {
		this.precisionCorrect = precisionCorrect;
	}
	
	public void setRecallCorrect(double recallCorrect) {
		this.recallCorrect = recallCorrect;
	}
	
	public void setPrecisionDI(double precisionDI) {
		this.precisionDI = precisionDI;
	}
	
	public void setRecallDI(double recallDI) {
		this.recallDI = recallDI;
	}
	
	public void setPrecisionCI(double precisionCI) {
		this.precisionCI = precisionCI;
	}
	
	public void setRecallCI(double recallCI) {
		this.recallCI = recallCI;
	}
	
	public void setVarAcc(double varAcc) {
		this.varAcc = varAcc;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public void setBugID(int bugID) {
		this.bugID = bugID;
	}
	
	public void setOverallTypeAcc(double overallTypeAcc) {
		this.overallTypeAcc = overallTypeAcc;
	}
	
	public void setOverallAcc(double overallAcc) {
		this.overallAcc = overallAcc;
	}
	
	public int getnoOfFeedbackNeeded() {
		return this.noOfFeedbackNeeded;
	}
	
	public double getPrecisionCorrect() {
		return this.precisionCorrect;
	}
	
	public double getRecallCorrect() {
		return this.recallCorrect;
	}
	
	public double getPrecisionDI() {
		return this.precisionDI;
	}
	
	public double getRecallDI() {
		return this.recallDI;
	}
	
	public double getPrecisionCI() {
		return this.precisionCI;
	}
	
	public double getRecallCI() {
		return this.recallCI;
	}
	
	public double getVarAcc() {
		return this.varAcc;
	}

	public String getProjectName() {
		return this.projectName;
	}
	
	public int getBugID() {
		return this.bugID;
	}
	
	public double getOverallTypeAcc() {
		return this.overallTypeAcc;
	}
	
	public double getOverallAcc() {
		return this.overallAcc;
	}
	
	/**
	 * Export the measurements to csv format.
	 * Format: projectName,bugID,Number of feedback needed, 
	 * precision of Correct, recall of correct, 
	 * precision of Data Incorrect, recall of Data Incorrect, 
	 * precision of Control Incorrect, recall of Control Incorrect, wrong variable prediction accuracy.
	 * @return Measurement in csv format.
	 */
	public String toCSV() {
		return this.projectName + "," +
			   this.bugID + "," + 
			   this.noOfFeedbackNeeded + "," +
			   this.precisionCorrect + "," +
			   this.recallCorrect + "," + 
			   this.precisionDI + "," +
			   this.recallDI + "," +
			   this.precisionCI + "," +
			   this.recallCI + "," +
			   this.varAcc + "," + 
			   this.overallTypeAcc + "," +
			   this.overallAcc;
	}
	
	@Override
	public String toString() {
		return this.projectName + ":" + this.bugID + " " +
			   "no. of feedback needed: " + this.noOfFeedbackNeeded + 
			   " Correct precision: " + this.precisionCorrect +
			   " Correct recall: " + this.recallCorrect + 
			   " Data Incorrect precision: " + this.precisionDI +
			   " Data Incorrect recall: " + this.recallDI + 
			   " Control Incorrect precision: " + this.precisionCI +
			   " Control Incorrect recall: " + this.recallCI +
			   " Wrong Variable Prediction Accuracy: " + this.varAcc +
			   " Overall Type Accuracy: " + this.overallTypeAcc + 
			   " Overall Accuracy: " + this.overallAcc;
	}
	
	private void printWrongMatchingError(int predictOrder, int refOrder) {
		System.out.println("Trace Order Mismatch: " + this.projectName + ":" + this.bugID + ", " + predictOrder + "," + refOrder);
	}
}

package tregression.autofeedbackevaluation;

import java.util.List;

/**
 * AvgAccMeasurement is used to store the average measurement of a list of measurement
 * @author David
 *
 */
public class AvgAccMeasurement {
	/**
	 * Target project name
	 */
	private String projectName;
	
//	/**
//	 * Different in root cause step order between the debugging result and the ground truth
//	 */
//	private int rootCauseOrderError;
	
	/**
	 * Average number of user feedback needed
	 */
	private double noOfFeedbackNeeded;
	
	/**
	 * Average precision of Correct feedback
	 */
	private double precisionCorrect;
	
	/**
	 * Average recall of Correct feedback
	 */
	private double recallCorrect;
	
	/**
	 * Precision of Data Incorrect feedback
	 */
	private double precisionDI;
	
	/**
	 * Average recall of Data Incorrect feedback
	 */
	private double recallDI;
	
	/**
	 * Average precision of Control Incorrect feedback
	 */
	private double precisionCI;
	/**
	 * Average recall of Control Incorrect feedback
	 */
	private double recallCI;
	
	/**
	 * Average accuracy for wrong variable prediction
	 */
	private double varAcc;
	
	public AvgAccMeasurement(List<AccMeasurement> measurements) {
		
		if (measurements.isEmpty()) {
			this.projectName = "";
			this.noOfFeedbackNeeded = 0;
			this.precisionCorrect = 0;
			this.recallCorrect = 0;
			this.precisionDI = 0;
			this.recallDI = 0;
			this.precisionCI = 0;
			this.recallCI = 0;
			this.varAcc = 0;
		} else {
			
			int size = 0;
			
			int totalFeedbackCount = 0;
			double totalPrecisionCorrect = 0.0;
			double totalRecallCorrect = 0.0;
			double totalPrecisionDI = 0.0;
			double totalRecallDI = 0.0;
			double totalPrecisionCI = 0.0;
			double totalRecallCI = 0.0;
			double totalVarAcc = 0.0;
			
			// Average number of feedback needed
			for (AccMeasurement measurement : measurements) {
				totalFeedbackCount += measurement.getnoOfFeedbackNeeded();
				size++;
			}
			this.noOfFeedbackNeeded = (double) totalFeedbackCount / size;
			
			// Precision Correct
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getPrecisionCorrect() != AccMeasurement.NaN) {
					totalPrecisionCorrect += measurement.getPrecisionCorrect();
					size++;
				}
			}
			this.precisionCorrect = size == 0 ? AccMeasurement.NaN : totalPrecisionCorrect / size;
			
			// Recall Correct
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getRecallCorrect() != AccMeasurement.NaN) {
					totalRecallCorrect += measurement.getRecallCorrect();
					size++;
				}
			}
			this.recallCorrect = size == 0 ? AccMeasurement.NaN : totalRecallCorrect / size;
			
			// Precision Data Incorrect
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getPrecisionDI() != AccMeasurement.NaN) {
					totalPrecisionDI += measurement.getPrecisionDI();
					size++;
				}
			}
			this.precisionDI = size == 0 ? AccMeasurement.NaN : totalPrecisionDI / size;
			
			// Recall Data Incorrect
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getRecallDI() != AccMeasurement.NaN) {
					totalRecallDI += measurement.getRecallDI();
					size++;
				}
			}
			this.recallDI = size == 0 ? AccMeasurement.NaN : totalRecallDI / size;
			
			// Precision Control Incorrect
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getPrecisionCI() != AccMeasurement.NaN) {
					totalPrecisionCI += measurement.getPrecisionCI();
					size++;
				}
			}
			this.precisionCI = size == 0 ? AccMeasurement.NaN : totalPrecisionCI / size;
			
			// Recall Control Incorrect
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getRecallCI() != AccMeasurement.NaN) {
					totalRecallCI += measurement.getRecallCI();
					size++;
				}
			}
			this.recallCI = size == 0 ? AccMeasurement.NaN : totalRecallCI / size;
			
			// Wrong variable prediction accuracy
			size = 0;
			for (AccMeasurement measurement : measurements) {
				if (measurement.getVarAcc() != AccMeasurement.NaN) {
					totalVarAcc += measurement.getVarAcc();
					size++;
				}
			}
			this.varAcc = size == 0 ? AccMeasurement.NaN : totalVarAcc / size;
		}
	}
	
	public double getnoOfFeedbackNeeded() {
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
	
	@Override
	public String toString() {
		return this.projectName + ":" +
//			   "rootCauseOrderError: " + this.rootCauseOrderError +
			   " Avg. no. of feedback needed: " + this.noOfFeedbackNeeded + 
			   " Avg. Correct precision: " + this.precisionCorrect +
			   " Avg. Correct recall: " + this.recallCorrect + 
			   " Avg. Data Incorrect precision: " + this.precisionDI +
			   " Avg. Data Incorrect recall: " + this.recallDI + 
			   " Avg. Control Incorrect precision: " + this.precisionCI +
			   " Avg. Control Incorrect recall: " + this.recallCI +
			   " Avg. Wrong Variable Prediction Accuracy: " + this.varAcc;
	}
}

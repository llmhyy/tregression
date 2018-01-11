package tregression.io;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.sql.TraceRecorder;
import tregression.empiricalstudy.Defects4jProjectConfig;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.MendingRecord;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;

public class RegressionRecorder extends TraceRecorder {
	
	/**
	 * The mending information can be retrieved through trial.
	 * 
	 * @param trial
	 * @param buggyTrace
	 * @param correctTrace
	 * @param diffMatcher
	 * @param pairList
	 * @param config 
	 * @param realcauseNode
	 */
	public void record(EmpiricalTrial trial, Trace buggyTrace, Trace correctTrace, 
			DiffMatcher diffMatcher, PairList pairList, Defects4jProjectConfig config) throws SQLException {
		List<MendingRecord> mendingRecords = trial.getRootCauseFinder().getMendingRecordList();
		Connection conn = null;
		List<AutoCloseable> closables = new ArrayList<AutoCloseable>();
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			String[] tc = trial.getTestcase().split("#");
			int buggyTraceId = insertTrace(buggyTrace, config.projectName, null, tc[0], tc[1], conn, closables);
			int correctTraceId = insertTrace(correctTrace, config.projectName, null, tc[0], tc[1], conn, closables);
			int regressionId = insertRegression(config, trial, buggyTraceId, correctTraceId, conn, closables);
			insertMendingRecord(regressionId, mendingRecords, conn, closables);
			insertRegressionMatch(regressionId, pairList, conn, closables);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollback(conn);
			throw e;
		} finally {
			closeDb(conn, closables);
		}
		System.currentTimeMillis();
	}

	private void insertRegressionMatch(int regressionId, PairList pairList, Connection conn, List<AutoCloseable> closables)
			throws SQLException {
		String sql = "INSERT INTO regressionMatch (regression_id, buggy_step, correct_step) VALUES (?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		closables.add(ps);
		for (TraceNodePair nodePair : pairList.getPairList()) {
			int idx = 1;
			ps.setInt(idx++, regressionId);
			ps.setInt(idx++, nodePair.getBeforeNode().getOrder());
			ps.setInt(idx++, nodePair.getAfterNode().getOrder());
			ps.addBatch();
		}
		ps.executeBatch();
	}

	private void insertMendingRecord(int regressionId, List<MendingRecord> mendingRecords, Connection conn,
			List<AutoCloseable> closables) throws SQLException {
		String sql = "INSERT INTO mendingRecord (regression_id, mending_type, mending_start,"
				+ " mending_correspondence, mending_return, variable)"
				+ " VALUES (?,?,?,?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		closables.add(ps);
		for (MendingRecord mendingRecord : mendingRecords) {
			int idx = 1;
			ps.setInt(idx++, regressionId);
			ps.setInt(idx++, mendingRecord.getType());
			ps.setInt(idx++, mendingRecord.getStartOrder());
			ps.setInt(idx++, mendingRecord.getCorrespondingStepOnReference());
			ps.setInt(idx++, mendingRecord.getReturningPoint());
			if (mendingRecord.getType() == MendingRecord.CONTROL) {
				ps.setString(idx, null);
			} else {
				ps.setString(idx++, generateXmlContent(Arrays.asList(mendingRecord.getVarValue())));
			}
			ps.addBatch();
		}
		ps.executeBatch();
	}

	private int insertRegression(Defects4jProjectConfig config, EmpiricalTrial trial, int buggyTraceId,
			int correctTraceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps;
		String sql = "INSERT INTO regression (project_name, project_version, bug_id, buggy_trace, "
				+ "correct_trace, root_cause_step, is_overskip, over_skip_number) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		closables.add(ps);
		int idx = 1;
		ps.setString(idx++, config.projectName);
		ps.setString(idx++, null);
		ps.setString(idx++, String.valueOf(config.bugID));
		ps.setInt(idx++, buggyTraceId);
		ps.setInt(idx++, correctTraceId);
		
		int order = -1;
		if(trial.getRootcauseNode()!=null) {
			order = trial.getRootcauseNode().getOrder();
		}
		
		ps.setInt(idx++, order);
		ps.setInt(idx++, trial.getOverskipLength() == 0 ? 0 : 1);
		ps.setInt(idx++, trial.getOverskipLength());
		ps.execute();
		int regressionId = getFirstGeneratedIntCol(ps);
		return regressionId;
	}

}

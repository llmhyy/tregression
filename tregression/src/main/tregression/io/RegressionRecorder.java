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
		List<Statement> stmts = new ArrayList<Statement>();
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			String[] tc = trial.getTestcase().split("#");
			int buggyTraceId = insertTrace(buggyTrace, config.projectName, null, tc[0], tc[1], conn, stmts);
			int correctTraceId = insertTrace(correctTrace, config.projectName, null, tc[0], tc[1], conn, stmts);
			int regressionId = insertRegression(config, trial, buggyTraceId, correctTraceId, conn, stmts);
			insertMendingRecord(regressionId, mendingRecords, conn, stmts);
			insertRegressionMatch(regressionId, pairList, conn, stmts);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollback(conn);
			throw e;
		} finally {
			closeDb(conn, stmts, null);
		}
		System.currentTimeMillis();
	}

	private void insertRegressionMatch(int regressionId, PairList pairList, Connection conn, List<Statement> stmts)
			throws SQLException {
		String sql = "INSERT INTO regressionMatch (regression_id, buggy_step, correct_step) VALUES (?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		for (TraceNodePair nodePair : pairList.getPairList()) {
			int idx = 1;
			ps.setInt(idx++, regressionId);
			ps.setInt(idx++, nodePair.getBeforeNode().getOrder());
			ps.setInt(idx++, nodePair.getAfterNode().getOrder());
			ps.addBatch();
		}
		ps.executeBatch();
		stmts.add(ps);
	}

	private void insertMendingRecord(int regressionId, List<MendingRecord> mendingRecords, Connection conn,
			List<Statement> stmts) throws SQLException {
		String sql = "INSERT INTO mendingRecord (regression_id, mending_type, mending_start,"
				+ " mending_correspondence, mending_return, variable)"
				+ " VALUES (?,?,?,?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
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
		stmts.add(ps);
	}

	private int insertRegression(Defects4jProjectConfig config, EmpiricalTrial trial, int buggyTraceId,
			int correctTraceId, Connection conn, List<Statement> stmts) throws SQLException {
		PreparedStatement ps;
		String sql = "INSERT INTO regression (project_name, project_version, bug_id, buggy_trace, "
				+ "correct_trace, root_cause_step, is_overskip, over_skip_number) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		int idx = 1;
		ps.setString(idx++, config.projectName);
		ps.setString(idx++, null);
		ps.setString(idx++, String.valueOf(config.bugID));
		ps.setInt(idx++, buggyTraceId);
		ps.setInt(idx++, correctTraceId);
		ps.setInt(idx++, trial.getRootcauseNode().getOrder());
		ps.setInt(idx++, trial.getOverskipLength() == 0 ? 0 : 1);
		ps.setInt(idx++, trial.getOverskipLength());
		stmts.add(ps);
		ps.execute();
		int regressionId = getFirstGeneratedIntCol(ps);
		return regressionId;
	}

}

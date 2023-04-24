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
import sav.common.core.SavRtException;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class RegressionRecorder extends TraceRecorder {
	
	/**
	 * The mending information can be retrieved through trial.
	 */
	public void record(EmpiricalTrial trial, Trace buggyTrace, Trace correctTrace, PairList pairList,
			String projectName, String bugId) throws SQLException {
		List<DeadEndRecord> deadEndRecords = trial.getDeadEndRecordList();
		Connection conn = null;
		List<AutoCloseable> closables = new ArrayList<AutoCloseable>();
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			String[] tc = trial.getTestcase().split("#");
			if (tc.length != 2) {
				tc = trial.getTestcase().split("::");
			} 
			if (tc.length != 2) {
				throw new SavRtException(String.format("Testcase-Unknown format: %s", trial.getTestcase()));
			}
			int buggyTraceId = insertTrace(buggyTrace, projectName, null, tc[0], tc[1], conn, closables);
			int correctTraceId = insertTrace(correctTrace, projectName, null, tc[0], tc[1], conn, closables);
			int regressionId = insertRegression(projectName, bugId, trial, buggyTraceId, correctTraceId, conn, closables);
			insertMendingRecord(regressionId, deadEndRecords, conn, closables);
			insertRegressionMatch(regressionId, pairList, conn, closables);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollback(conn);
			throw e;
		} finally {
			closeDb(conn, closables);
		}
		System.out.println("RecordDB finished!");
	}

	protected void insertRegressionMatch(int regressionId, PairList pairList, Connection conn, List<AutoCloseable> closables)
			throws SQLException {
		String sql = "INSERT INTO RegressionMatch (regression_id, buggy_step, correct_step) VALUES (?,?,?)";
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

	protected void insertMendingRecord(int regressionId, List<DeadEndRecord> deadEndRecords, Connection conn,
			List<AutoCloseable> closables) throws SQLException {
		String sql = "INSERT INTO DeadEndRecord (regression_id, record_type, occur_order,"
				+ " dead_end_order, break_step_order, variable)"
				+ " VALUES (?,?,?,?,?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		closables.add(ps);
		for (DeadEndRecord deadEndRecord : deadEndRecords) {
			int idx = 1;
			ps.setInt(idx++, regressionId);
			ps.setInt(idx++, deadEndRecord.getType());
			ps.setInt(idx++, deadEndRecord.getOccurOrder());
			ps.setInt(idx++, deadEndRecord.getDeadEndOrder());
			ps.setInt(idx++, deadEndRecord.getBreakStepOrder());
			if (deadEndRecord.getType() == DeadEndRecord.CONTROL) {
				ps.setString(idx, null);
			} else {
				ps.setString(idx++, generateXmlContent(Arrays.asList(deadEndRecord.getVarValue())));
			}
			ps.addBatch();
		}
		ps.executeBatch();
	}

	protected int insertRegression(String projectName, String bugId, EmpiricalTrial trial, int buggyTraceId,
			int correctTraceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps;
		String sql = "INSERT INTO Regression (project_name, project_version, bug_id, buggy_trace, "
				+ "correct_trace, root_cause_step, is_overskip, over_skip_number) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		closables.add(ps);
		int idx = 1;
		ps.setString(idx++, projectName);
		ps.setString(idx++, null);
		ps.setString(idx++, bugId);
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

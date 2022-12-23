package tregression.io;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.sql.MysqlTraceRetriever;
import tregression.empiricalstudy.Regression;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class RegressionRetriever extends MysqlTraceRetriever {
	
	public Regression retriveRegression(String projectName, String bugID) throws SQLException{
		Connection conn = null;
		List<AutoCloseable> closables = new ArrayList<>();
		try {
			conn = getConnection();
			Object[] rs = loadRegression(projectName, bugID, conn, closables);
			int idx = 0;
			int regressionId = (int) rs[idx++];
			int buggyTraceId = (int) rs[idx++];
			int correctTraceId = (int) rs[idx++];
			Regression regression = retrieveRegressionInfo(regressionId, buggyTraceId, correctTraceId, conn, closables);
			System.out.println("Retrieve done!");
			return regression;
		} finally {
			closeDb(conn, closables);
		}
	}

	protected Regression retrieveRegressionInfo(int regressionId, int buggyTraceId, int correctTraceId, Connection conn,
			List<AutoCloseable> closables) throws SQLException {
		Trace buggyTrace = loadTrace(buggyTraceId, conn, closables);
		Trace correctTrace = loadTrace(correctTraceId, conn, closables);
		List<TraceNodePair> pairList = loadRegressionMatch(buggyTrace, correctTrace, regressionId, conn, closables);
		Regression regression = new Regression(buggyTrace, correctTrace, new PairList(pairList));
		Object[] traceInfo = loadTraceInfo(buggyTraceId, conn, closables);
		regression.setTestCase((String)traceInfo[0], (String)traceInfo[1]);
//		buggyTrace.setMultiThread((boolean) traceInfo[2]);
//		correctTrace.setMultiThread((boolean) traceInfo[2]);
		return regression;
	}
	
	private List<TraceNodePair> loadRegressionMatch(Trace buggyTrace, Trace correctTrace, int regressionId,
			Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT rm.buggy_step, rm.correct_step FROM RegressionMatch rm WHERE rm.regression_id=?");
		ps.setInt(1, regressionId);
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		List<TraceNodePair> result = new ArrayList<>(countNumberOfRows(rs));
		while (rs.next()) {
			int idx = 1;;
			TraceNode beforeNode = buggyTrace.getTraceNode(rs.getInt(idx++));
			TraceNode afterNode = correctTrace.getTraceNode(rs.getInt(idx));
			TraceNodePair pair = new TraceNodePair(beforeNode, afterNode);
			result.add(pair);
		}
		ps.close();
		rs.close();
		return result;
	}

	/**
	 * return Object[]: regression_id, buggy_trace id, correct_trace id
	 */
	protected Object[] loadRegression(String projectName, String bugId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT * FROM Regression WHERE regression_id="
							+ "(SELECT MAX(regression_id) FROM Regression WHERE project_name=? AND bug_id=?)");
		int idx = 1;
		ps.setString(idx++, projectName);
		ps.setString(idx++, bugId);
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		Object[] result = new Object[3];
		if (countNumberOfRows(rs) == 0) {
			throw new SQLException(
					String.format("No record of Regression found for project %s with bugId %s", projectName, bugId));
		}
		if (rs.next()) {
			int rIdx = 0;
			result[rIdx++] = rs.getInt("regression_id"); // regression_id
			result[rIdx++] = rs.getInt("buggy_trace"); // buggy_trace
			result[rIdx++] = rs.getInt("correct_trace"); // correct_trace
		} else {
			throw new SQLException(
					String.format("No regression record is found for project % with bugId %s", projectName, bugId));
		}
		return result;
	}
	
	private Object[] loadTraceInfo(int traceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT t.launch_class, t.launch_method, t.is_multithread FROM Trace t WHERE t.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		Object[] result = new Object[3];
		if (rs.next()) {
			int idx = 0;
			result[idx++] = rs.getString(idx);
			result[idx++] = rs.getString(idx);
			result[idx++] = rs.getBoolean(idx);
		} else {
			throw new SQLException("Cannot load trace with traceId = " + traceId);
		}
		return result;
	}
	
}

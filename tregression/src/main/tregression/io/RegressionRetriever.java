package tregression.io;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.handler.xml.VarValueXmlReader;
import microbat.model.BreakPoint;
import microbat.model.trace.StepVariableRelationEntry;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.sql.DbService;
import microbat.sql.TraceRecorder;
import sav.common.core.utils.StringUtils;
import tregression.empiricalstudy.Regression;
import tregression.model.PairList;
import tregression.model.TraceNodePair;

public class RegressionRetriever extends DbService {
	
	
	public Regression retriveRegression(String projectName, String bugID) throws SQLException{
		Connection conn = null;
		try {
			conn = getConnection();
			Object[] rs = loadRegression(projectName, bugID, conn);
			int idx = 0;
			int regressionId = (int) rs[idx++];
			int buggyTraceId = (int) rs[idx++];
			Trace buggyTrace = loadTrace(buggyTraceId, conn);
			Trace correctTrace = loadTrace((int) rs[idx++], conn);
			List<TraceNodePair> pairList = loadRegressionMatch(buggyTrace, correctTrace, regressionId, conn);
			Regression regression = new Regression(buggyTrace, correctTrace, new PairList(pairList));
			Object[] tc = loadTraceInfo(buggyTraceId, conn);
			regression.setTestCase((String)tc[0], (String)tc[1]);
			System.out.println("Retrieve done!");
			return regression;
		} finally {
			closeDb(conn, null, null);
		}
	}
	
	private List<TraceNodePair> loadRegressionMatch(Trace buggyTrace, Trace correctTrace, int regressionId,
			Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT rm.buggy_step, rm.correct_step FROM regressionMatch rm WHERE rm.regression_id=?");
		ps.setInt(1, regressionId);
		ResultSet rs = ps.executeQuery();
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
	private Object[] loadRegression(String projectName, String bugId, Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT * FROM regression WHERE project_name=? AND bug_id=?");
		int idx = 1;
		ps.setString(idx++, projectName);
		ps.setString(idx++, bugId);
		ResultSet rs = ps.executeQuery();
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
		rs.close();
		ps.close();
		return result;
	}
	
	private Object[] loadTraceInfo(int traceId, Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
				"SELECT t.launch_class, t.launch_method FROM trace t WHERE t.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		Object[] result = new Object[2];
		if (rs.next()) {
			result[0] = rs.getString(1);
			result[1] = rs.getString(2);
		} else {
			throw new SQLException("Cannot load trace with traceId = " + traceId);
		}
		return result;
	}
	
	private Trace loadTrace(int traceId, Connection conn) throws SQLException {
		Trace trace = new Trace(null); 
		// load step
		List<TraceNode> steps = loadSteps(traceId, conn);
		trace.setExectionList(steps);
		// load stepVar
		List<Object[]> rows = loadStepVariableRelation(traceId, conn);
		Map<String, StepVariableRelationEntry> stepVariableTable = trace.getStepVariableTable();
		for (Object[] row : rows) {
			int stepOrder = (int) row[0];
			String varId = (String) row[1];
			int rw = (int) row[2];
			StepVariableRelationEntry entry = stepVariableTable.get(varId);
			if (entry == null) {
				entry = new StepVariableRelationEntry(varId);
				stepVariableTable.put(varId, entry);
			}
			if (rw == TraceRecorder.WRITE) {
				entry.addProducer(steps.get(stepOrder - 1));
			} else if (rw == TraceRecorder.READ) {
				entry.addConsumer(steps.get(stepOrder - 1));
			} else {
				throw new SQLException("Tabel StepVariableRelationEntry: Invalid RW value!");
			}
		}
		return trace;
	}
	
	/**
	 * return list of relation info [step_order, var_id, RW]
	 */
	private List<Object[]> loadStepVariableRelation(int traceId, Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT r.step_order, r.var_id, r.RW FROM stepVariableRelation r WHERE r.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		List<Object[]> result = new ArrayList<>();
		while (rs.next()) {
			int idx = 1;
			Object[] row = new Object[]{
					rs.getInt(idx++),
					rs.getString(idx++),
					rs.getInt(idx++)
			};
			result.add(row);
		}
		ps.close();
		rs.close();
		return result;
	}

	private List<TraceNode> loadSteps(int traceId, Connection conn) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT s.* FROM step s WHERE s.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		int total = countNumberOfRows(rs);
		List<TraceNode> allSteps = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			allSteps.add(new TraceNode(null, null, i));
		}
		Map<Integer, TraceNode> locationIdMap = new HashMap<>();
		while (rs.next()) {
			// step order
			int order = rs.getInt("step_order");
			if (order > total) {
				throw new SQLException("Detect invalid step order in result set!");
			}
			TraceNode step = allSteps.get(order - 1);
			step.setOrder(order);
			// control_dominator
			step.setControlDominator(getRelNode(allSteps, rs, "control_dominator"));
			// step_in
			step.setStepInNext(getRelNode(allSteps, rs, "step_in"));
			// step_over
			step.setStepOverNext(getRelNode(allSteps, rs, "step_over"));
			// invocation_parent
			step.setInvocationParent(getRelNode(allSteps, rs, "invocation_parent"));
			// loop_parent
			step.setLoopParent(getRelNode(allSteps, rs, "loop_parent"));
			// location_id
			locationIdMap.put(rs.getInt("location_id"), step);
			// read_vars
			step.setReadVariables(toVarValue(rs.getString("read_vars")));
			// written_vars
			step.setWrittenVariables(toVarValue(rs.getString("written_vars")));
		}
		rs.close();
		ps.close();
		loadLocations(locationIdMap, conn);
		return allSteps;
	}
	
	private void loadLocations(Map<Integer, TraceNode> locationIdMap, Connection conn) throws SQLException {
		String matchList = StringUtils.join(locationIdMap.keySet(), ",");
		PreparedStatement ps = conn.prepareStatement(
						String.format("SELECT loc.* FROM location loc WHERE loc.location_id in (%s)", matchList));
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			int locId = rs.getInt("location_id");
			TraceNode node = locationIdMap.get(locId);
			String className = rs.getString("class_name");
			int lineNo = rs.getInt("line_number");
			BreakPoint bkp = new BreakPoint(className, className, lineNo);
			bkp.setConditional(rs.getBoolean("is_conditional"));
			bkp.setReturnStatement(rs.getBoolean("is_return"));
			node.setBreakPoint(bkp);
		}
		ps.close();
		rs.close();
	}

	protected List<VarValue> toVarValue(String xmlContent) {
		return VarValueXmlReader.read(xmlContent);
	}
	
	private TraceNode getRelNode(List<TraceNode> allSteps, ResultSet rs, String colName) throws SQLException {
		int relNodeOrder = rs.getInt(colName);
		if (!rs.wasNull()) {
			if (relNodeOrder > allSteps.size()) {
				System.err.println(String.format("index out of bound: size=%d, idx=%d", allSteps.size(), relNodeOrder));
				return null;
			}
			return allSteps.get(relNodeOrder - 1);
		}
		return null;
	}
}

package tregression.io;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.handler.xml.VarValueXmlReader;
import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import microbat.model.ControlScope;
import microbat.model.SourceScope;
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
		List<AutoCloseable> closables = new ArrayList<>();
		try {
			conn = getConnection();
			Object[] rs = loadRegression(projectName, bugID, conn, closables);
			int idx = 0;
			int regressionId = (int) rs[idx++];
			int buggyTraceId = (int) rs[idx++];
			Trace buggyTrace = loadTrace(buggyTraceId, conn, closables);
			Trace correctTrace = loadTrace((int) rs[idx++], conn, closables);
			List<TraceNodePair> pairList = loadRegressionMatch(buggyTrace, correctTrace, regressionId, conn, closables);
			Regression regression = new Regression(buggyTrace, correctTrace, new PairList(pairList));
			Object[] traceInfo = loadTraceInfo(buggyTraceId, conn, closables);
			regression.setTestCase((String)traceInfo[0], (String)traceInfo[1]);
			buggyTrace.setMultiThread((boolean) traceInfo[2]);
			correctTrace.setMultiThread((boolean) traceInfo[2]);
			System.out.println("Retrieve done!");
			return regression;
		} finally {
			closeDb(conn, closables);
		}
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
	private Object[] loadRegression(String projectName, String bugId, Connection conn, List<AutoCloseable> closables) throws SQLException {
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
	
	private Trace loadTrace(int traceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		Trace trace = new Trace(null); 
		// load step
		List<TraceNode> steps = loadSteps(traceId, conn, closables);
		trace.setExectionList(steps);
		// load stepVar
		List<Object[]> rows = loadStepVariableRelation(traceId, conn, closables);
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
	private List<Object[]> loadStepVariableRelation(int traceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT r.step_order, r.var_id, r.RW FROM StepVariableRelation r WHERE r.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
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

	private List<TraceNode> loadSteps(int traceId, Connection conn, List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT s.* FROM Step s WHERE s.trace_id=?");
		ps.setInt(1, traceId);
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
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
			TraceNode controlDominator = getRelNode(allSteps, rs, "control_dominator");
			step.setControlDominator(controlDominator);
			if (controlDominator != null) {
				controlDominator.addControlDominatee(step);
			}
			// step_in
			TraceNode stepIn = getRelNode(allSteps, rs, "step_in");
			step.setStepInNext(stepIn);
			if (stepIn != null) {
				stepIn.setStepInPrevious(step);
			}
			// step_over
			TraceNode stepOver = getRelNode(allSteps, rs, "step_over");
			step.setStepOverNext(stepOver);
			if (stepOver != null) {
				stepOver.setStepOverPrevious(step);
			}
			// invocation_parent
			TraceNode invocationParent = getRelNode(allSteps, rs, "invocation_parent");
			step.setInvocationParent(invocationParent);
			if (invocationParent != null) {
				invocationParent.addInvocationChild(step);
			}
			// loop_parent
			TraceNode loopParent = getRelNode(allSteps, rs, "loop_parent");
			step.setLoopParent(loopParent);
			if (loopParent != null) {
				loopParent.addLoopChild(step);
			}
			// location_id
			locationIdMap.put(rs.getInt("location_id"), step);
			try {
				// read_vars
				step.setReadVariables(toVarValue(rs.getString("read_vars")));
				// written_vars
				step.setWrittenVariables(toVarValue(rs.getString("written_vars")));
			} catch (Exception e) {
				System.out.println(String.format("Xml error at step: [trace_id, order] = [%d, %d]", traceId, order));
				throw e;
			}
		}
		rs.close();
		ps.close();
		loadLocations(locationIdMap, conn, closables);
		return allSteps;
	}
	
	private void loadLocations(Map<Integer, TraceNode> locIdStepMap, Connection conn, List<AutoCloseable> closables) throws SQLException {
		Set<Integer> locationSet = locIdStepMap.keySet();
		String matchList = StringUtils.join(locationSet, ",");
		/* control scope */
		Map<Integer, ControlScope> controlScopeMap = loadControlScopes(locationSet, matchList, conn, closables);
		/* loop scope */
		Map<Integer, SourceScope> loopScopeMap = loadLoopScope(locationSet, matchList, conn, closables);
		/* location */
		PreparedStatement ps = conn.prepareStatement(String.format(
				"SELECT location_id,class_name,line_number,is_conditional,is_return FROM Location WHERE location_id IN (%s)", 
					matchList));
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		while(rs.next()) {
			int locId = rs.getInt("location_id");
			TraceNode node = locIdStepMap.get(locId);
			String className = rs.getString("class_name");
			int lineNo = rs.getInt("line_number");
			BreakPoint bkp = new BreakPoint(className, className, lineNo);
			bkp.setConditional(rs.getBoolean("is_conditional"));
			bkp.setReturnStatement(rs.getBoolean("is_return"));
			bkp.setControlScope(controlScopeMap.get(locId));
			bkp.setLoopScope(loopScopeMap.get(locId));
			node.setBreakPoint(bkp);
		}
		ps.close();
		rs.close();
	}

	private Map<Integer, ControlScope> loadControlScopes(Set<Integer> locationSet, String matchList, Connection conn,
			List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(String.format(
				"SELECT location_id, class_name, line_number, is_loop FROM ControlScope WHERE location_id IN (%s)",
					matchList));
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		Map<Integer, ControlScope> map = new HashMap<>();
		for (int locId : locationSet) {
			map.put(locId, new ControlScope());
		}
		while(rs.next()) {
			ControlScope scope = map.get(rs.getInt("location_id"));
			String className = rs.getString("class_name");
			int lineNo = rs.getInt("line_number");
			scope.addLocation(new ClassLocation(className, null, lineNo));
			scope.setLoop(rs.getBoolean("is_loop"));
		}
		return map;
	}
	
	private Map<Integer, SourceScope> loadLoopScope(Set<Integer> locationSet, String matchList, Connection conn,
			List<AutoCloseable> closables) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(String.format(
				"SELECT location_id, class_name, start_line, end_line FROM LoopScope WHERE location_id IN (%s)",
					matchList));
		ResultSet rs = ps.executeQuery();
		closables.add(ps);
		closables.add(rs);
		Map<Integer, SourceScope> map = new HashMap<>();
		while(rs.next()) {
			SourceScope scope = new SourceScope();
			scope.setClassName(rs.getString("class_name"));
			scope.setStartLine(rs.getInt("start_line"));
			scope.setEndLine(rs.getInt("end_line"));
			map.put(rs.getInt("location_id"), scope);
		}
		return map;
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

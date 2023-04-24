package tregression.empiricalstudy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class CausalityGraph {
	private List<CausalityNode> observedFaults = new ArrayList<>();
	private List<CausalityNode> roots = new ArrayList<>();
	private Map<CausalityNode, CausalityNode> nodes = new HashMap<>();

	public CausalityNode findOrCreate(TraceNode node, boolean isOnBefore){
		CausalityNode n = new CausalityNode(node, isOnBefore);
		if(nodes.keySet().contains(n)){
			return nodes.get(n);
		}
		else{
			nodes.put(n, n);
			return n;
		}
	}
	
	class VisitedResult{
		CausalityNode node;
		VarValue value;
		
		public VisitedResult(CausalityNode node, VarValue value) {
			super();
			this.node = node;
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			VisitedResult other = (VisitedResult) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (node == null) {
				if (other.node != null)
					return false;
			} else if (!node.equals(other.node))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		private CausalityGraph getOuterType() {
			return CausalityGraph.this;
		}
		
		
	}
	
	private Map<CausalityNode, VarValue> guidance = new HashMap<>(); 
	public void generateSimulationGuidance(){
		if(this.roots.isEmpty()){
			return;
		}
		
		CausalityNode rootOnBuggyTrace = null;
		for(CausalityNode root: roots){
			if(root.isOnBefore()){
				rootOnBuggyTrace = root;
				break;
			}
		}
		
		for(CausalityNode root: roots){
			if(rootOnBuggyTrace!=null && !root.isOnBefore()){
				continue;
			}
			
			guidance.put(root, null);
			Set<VisitedResult> visitedSet = new HashSet<>();
			visitedSet.add(new VisitedResult(root, null));
			traverse(root, guidance, visitedSet);			
		}
		
	}

	private void traverse(CausalityNode node, Map<CausalityNode, VarValue> guidance, Set<VisitedResult> visitedSet) {
		for(CausalityNode result: node.getResults().keySet()){
			VarValue value = node.getResults().get(result);
			if(value!=null){
				guidance.put(result, value);
			}
			
			VisitedResult vr = new VisitedResult(result, value);
			if(!visitedSet.contains(vr)){
				visitedSet.add(vr);
				traverse(result, guidance, visitedSet);					
			}
		}
	}

	public List<CausalityNode> getObservedFaults() {
		return observedFaults;
	}

	public void setObservedFaults(List<CausalityNode> observedFaults) {
		this.observedFaults = observedFaults;
	}

	public Map<CausalityNode, CausalityNode> getNodes() {
		return nodes;
	}

	public void setNodes(Map<CausalityNode, CausalityNode> nodes) {
		this.nodes = nodes;
	}

	public Map<CausalityNode, VarValue> getGuidance() {
		return guidance;
	}

	public void setGuidance(Map<CausalityNode, VarValue> guidance) {
		this.guidance = guidance;
	}

	public List<CausalityNode> getRoots() {
		return roots;
	}

	public void setRoots(List<CausalityNode> roots) {
		this.roots = roots;
	}

	public void addRoot(CausalityNode root){
		if(!this.roots.contains(root)){
			this.roots.add(root);
		}
	}
}

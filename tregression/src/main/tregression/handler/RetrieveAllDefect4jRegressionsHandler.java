/**
 * 
 */
package tregression.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import tregression.empiricalstudy.ReadEmpiricalTrial;
import tregression.empiricalstudy.TrialReader;

/**
 * @author LLT
 *
 */
public class RetrieveAllDefect4jRegressionsHandler extends RegressionRetrieveHandler  {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Retrieve all regressions") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					retrieveAll(monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	
	private void retrieveAll(IProgressMonitor monitor) {
		int skippedNum = 0;
		int endNum = 500;
		
		String[] projects = {"Chart", "Closure", "Lang", "Math", "Mockito", "Time"};
		int[] bugNum = {26, 133, 65, 106, 38, 27};
		
		String fileName = "defects4j0.old.xlsx";
		Map<ReadEmpiricalTrial, ReadEmpiricalTrial> map = new HashMap<>();
		try {
			map = new TrialReader().readXLSX(fileName);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	
		int count = 0;
		for(int i=0; i<projects.length; i++) {
			for(int j=1; j<=bugNum[i]; j++) {
				count++;
				if(count <= skippedNum || count > endNum) {
					continue;
				}
				
				
				
				if(!map.isEmpty()){
					ReadEmpiricalTrial tmp = new ReadEmpiricalTrial();
					tmp.setProject(projects[i]);
					tmp.setBugID(String.valueOf(j));
					
					ReadEmpiricalTrial t = map.get(tmp);
					if(t==null){
						System.err.println(projects[i]+"-"+j+" is missing.");
						continue;
					}
					
					String deadEndType = t.getDeadEndType();
					if(deadEndType==null || 
							!(deadEndType.equals("control") /*|| deadEndType.equals("data")*/)){
						continue;
					}
					
//					String exception = t.getException();
//					if(exception==null || !exception.contains("over long")){
//						continue;
//					}
				}
						
				try {
					
					System.out.println("***working on the " + j + "th bug of " + projects[i] + " project.");
					
					retrieveRegression(projects[i], String.valueOf(j));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}

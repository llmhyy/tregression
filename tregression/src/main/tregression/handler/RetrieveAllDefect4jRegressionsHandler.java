/**
 * 
 */
package tregression.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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
	
		int count = 0;
		for(int i=0; i<projects.length; i++) {
			for(int j=1; j<=bugNum[i]; j++) {
				count++;
				if(count <= skippedNum || count > endNum) {
					continue;
				}
				try {
					retrieveRegression(projects[i], String.valueOf(j));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}

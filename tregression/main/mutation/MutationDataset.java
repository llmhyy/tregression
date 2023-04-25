package mutation;

import java.io.IOException;

import jmutation.dataset.BugDataset;
import jmutation.dataset.BugDataset.BugData;
import jmutation.dataset.execution.Request;
import jmutation.dataset.execution.handler.TraceCollectionHandler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class MutationDataset {
    public static void main(String[] args) throws IOException {
        String repoPath = "E:\\chenghin";
        String projName = "math_70";
        int traceCollectionTimeoutSeconds = 60;
        
        List<Integer> failingBugIds = new ArrayList<>();
        final int startIdx = 1;
        final int endIdx = 2;
        BugDataset bugdataset = new BugDataset(repoPath + "\\" + projName);
        for (int i = startIdx; i < endIdx; i++) {
        	System.out.println(i);
            new TraceCollectionHandler(repoPath, projName, i, traceCollectionTimeoutSeconds,0, 0).handle(new Request(true));
            
            try {
            	BugData data = bugdataset.getData(i);
            	System.out.println(data);
            	System.out.println(data.getRootCauseNode());
            } catch (Exception e) {
            	failingBugIds.add(i);
            }
        }
        
        System.out.println("The following ids fail to execute");
        System.out.println(failingBugIds);
    }
}

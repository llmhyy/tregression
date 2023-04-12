package mutation;

import java.io.IOException;

import dataset.BugDataset;
import dataset.BugDataset.BugData;
import dataset.execution.Request;
import dataset.execution.handler.TraceCollectionHandler;

import java.util.ArrayList;
import java.util.List;

public class MutationDataset {
    public static void main(String[] args) throws IOException {
        String repoPath = "E:\\chenghin";
        String projName = "math_70";
        int traceCollectionTimeoutSeconds = 60;
        
        List<Integer> failingBugIds = new ArrayList<>();
        final int startIdx = 8;
        final int endIdx = 100;
        BugDataset bugdataset = new BugDataset(repoPath + "\\" + projName);
        for (int i = startIdx; i < endIdx; i++) {
        	System.out.println(i);
            new TraceCollectionHandler(repoPath, projName, i, traceCollectionTimeoutSeconds,0, 0).handle(new Request(true));
            
            try {
            	BugData data = bugdataset.getData(i);
            	System.out.println(data);
            } catch (Exception e) {
            	failingBugIds.add(i);
            }
        }
        
        System.out.println("The following ids fail to execute");
        System.out.println(failingBugIds);
    }
}

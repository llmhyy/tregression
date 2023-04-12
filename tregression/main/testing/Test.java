package testing;

import java.io.IOException;

import dataset.BugDataset;
import dataset.BugDataset.BugData;
import dataset.execution.Request;
import dataset.execution.handler.TraceCollectionHandler;

public class Test{

    public static void main(String[] args) throws IOException {
        String repoPath = "E:\\chenghin";
        String projName = "math_70";
        int traceCollectionTimeoutSeconds = 60;
        	
        final int startIdx = 6;
        final int endIdx = 7;
        BugDataset bugdataset = new BugDataset(repoPath + "\\" + projName);
        for (int i = startIdx; i < endIdx; i++) {
        	System.out.println(i);
            new TraceCollectionHandler(repoPath, projName, i, traceCollectionTimeoutSeconds,0, 0).handle(new Request(true));
            BugData data = bugdataset.getData(i);
            System.out.println(data);
        }
    }
}
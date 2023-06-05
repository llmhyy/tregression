package tregression.separatesnapshots.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DiffParser {
    public static String ADDED_OR_REMOVED_FILE_NAME = "/dev/null";

    public List<FilePairWithDiff> parseDiff(List<String> diffContent, String sourceFolderName) {
        List<FilePairWithDiff> fileDiffList = new ArrayList<>();
        FilePairWithDiff fileDiff = null;

        for (String line : diffContent) {
            if (line.startsWith("diff")) {
                if (fileDiff != null) {
                    fileDiffList.add(fileDiff);
                }
                fileDiff = new FilePairWithDiff();
                fileDiff.setSourceFolderName(sourceFolderName);
            } else if (line.startsWith("---")) {
                fileDiff.setSourceFile(getFilePath(true, line));
            } else if (line.startsWith("+++")) {
                fileDiff.setTargetFile(getFilePath(false, line));
            } else if (line.startsWith("@@")) {
                String chunkInfo = line.substring(line.indexOf("@@") + 3, line.lastIndexOf("@@") - 1);

                String startLineInSource = chunkInfo.substring(chunkInfo.indexOf("-") + 1, chunkInfo.indexOf(","));
                int startLineInS = Integer.valueOf(startLineInSource);

                String lengthInSource = chunkInfo.substring(chunkInfo.indexOf(",") + 1, chunkInfo.indexOf(" "));
                int lengthInS = Integer.valueOf(lengthInSource);

                String startLineInTarget = chunkInfo.substring(chunkInfo.indexOf(" ") + 2, chunkInfo.lastIndexOf(","));
                int startLinInT = Integer.valueOf(startLineInTarget);

                String lengthInTarget = chunkInfo.substring(chunkInfo.lastIndexOf(",") + 1, chunkInfo.length());
                int lengthInT = Integer.valueOf(lengthInTarget);

                DiffChunk chunk = new DiffChunk(startLineInS, lengthInS, startLinInT, lengthInT);
                fileDiff.getChunks().add(chunk);
            } else if (line.startsWith(" ") || line.startsWith("+") || line.startsWith("-")) {
                int size = fileDiff.getChunks().size();
                DiffChunk chunk = fileDiff.getChunks().get(size - 1);

                int index = chunk.getChangeList().size();
                int type = changeType(line.toCharArray()[0]);

                LineChange change = new LineChange(index, type, line);
                chunk.getChangeList().add(change);
            }
        }

        return fileDiffList;
    }

    private int changeType(char s) {
        if (s == ' ') {
            return LineChange.UNCHANGE;
        } else if (s == '+') {
            return LineChange.ADD;
        } else if (s == '-') {
            return LineChange.REMOVE;
        }

        return -1;
    }

    private String getFilePath(boolean isSource, String line) {
        String diffSuffix = "";
        if (isSource) {
            diffSuffix = "a/";
        } else {
            diffSuffix = "b/";
        }
        String resultFilePath;
        if (line.contains(ADDED_OR_REMOVED_FILE_NAME)) {
            resultFilePath = ADDED_OR_REMOVED_FILE_NAME;
        } else {
            String osName = System.getProperty("os.name");
            line = line.trim();
            int idxOfJava = line.indexOf(".java");
            int endOfString = line.length();
            if (idxOfJava != -1) {
                endOfString = idxOfJava + 5;
            }
            if (osName.contains("Win")) {
                resultFilePath = line.substring(line.indexOf(diffSuffix) + 2, endOfString);
            } else {
                resultFilePath = line.substring(line.indexOf(diffSuffix) + 1, endOfString);
            }
            resultFilePath = resultFilePath.replace("/", File.separator);
            resultFilePath = resultFilePath.replace("\\\\", File.separator);
        }
        return resultFilePath;
    }
}

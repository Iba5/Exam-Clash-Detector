package src.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the outcome of a batch import.
 * succeeded = rows that were inserted into DB.
 * failed    = rows that broke, each carrying the REASON it failed.
 */
public class ImportResult {

    private final List<String[]> succeeded = new ArrayList<>();
    private final List<FailedRow> failed   = new ArrayList<>();

    public void addSuccess(String[] row) {
        succeeded.add(row);
    }

    public void addFailure(String[] row, String reason) {
        failed.add(new FailedRow(row, reason));
    }

    public List<String[]> getSucceeded() { return succeeded; }
    public List<FailedRow> getFailed()   { return failed; }

    public int successCount() { return succeeded.size(); }
    public int failCount()    { return failed.size(); }
    public int totalCount()   { return successCount() + failCount(); }

    public String summary() {
        return successCount() + " imported, " + failCount() + " failed (out of " + totalCount() + ")";
    }

    /**
     * A row that failed import, together with the human-readable reason.
     */
    public static class FailedRow {

        private final String[] row;
        private final String reason;

        public FailedRow(String[] row, String reason) {
            this.row = row;
            this.reason = reason;
        }

        public String[] getRow()   { return row; }
        public String getReason()  { return reason; }
    }
}
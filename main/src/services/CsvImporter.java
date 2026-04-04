package src.services;

import src.models.*;
import src.services.ImportResult;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CsvImporter — parses CSV files into model lists.
 *
 * Rules:
 *   - First row is ALWAYS a header (skipped)
 *   - Rows that can't be parsed go into ImportResult.failed with reason
 *   - Uses only java.io — no external libraries
 *   - Handles quoted fields with commas inside them
 */
public class CsvImporter {

    // ── FILE VALIDATION ──────────────────────────────────────────────

    /**
     * Validates the file before parsing.
     * Throws IllegalArgumentException with a user-friendly message.
     */
    public static void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("No file selected.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + file.getName());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Selected item is not a file.");
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("File is empty: " + file.getName());
        }

        String name = file.getName().toLowerCase();
        if (!name.endsWith(".csv")) {
            // Detect common wrong formats and give specific messages
            if (name.endsWith(".pdf")) {
                throw new IllegalArgumentException(
                    "Cannot import a PDF file. Please provide a CSV file.\n" +
                    "CSV files can be created from Excel: File → Save As → CSV.");
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                throw new IllegalArgumentException(
                    "Cannot import an Excel file directly. Please save it as CSV first.\n" +
                    "In Excel: File → Save As → select \"CSV (Comma delimited)\".");
            } else if (name.endsWith(".json")) {
                throw new IllegalArgumentException(
                    "Cannot import a JSON file. Please provide a CSV file.");
            } else if (name.endsWith(".txt")) {
                throw new IllegalArgumentException(
                    "Cannot import a .txt file. Please rename it to .csv if it contains comma-separated data,\n" +
                    "or re-export from your source as CSV.");
            } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
                throw new IllegalArgumentException(
                    "Cannot import a Word document. Please provide a CSV file.");
            } else {
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "unknown";
                throw new IllegalArgumentException(
                    "Unsupported file type: " + ext + "\n" +
                    "Please provide a .csv file with comma-separated values.");
            }
        }
    }

    // ── STUDENTS ────────────────��────────────────────────────────────
    // Expected: roll_number, name, department, semester

    public static ParsedData<Students> parseStudents(File file) {
        validateFile(file);
        ParsedData<Students> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows (only a header or empty).");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2; // +2 because: 0-indexed + header row

            if (cols.length < 4) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 4 columns (roll_number, name, department, semester), got " + cols.length));
                continue;
            }

            String roll = cols[0].trim();
            String name = cols[1].trim();
            String dept = cols[2].trim();
            String semStr = cols[3].trim();

            if (roll.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Roll number is empty"));
                continue;
            }
            if (name.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Name is empty"));
                continue;
            }
            if (dept.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Department is empty"));
                continue;
            }

            int semester;
            try {
                semester = Integer.parseInt(semStr);
                if (semester <= 0 || semester > 8) {
                    data.failed.add(new ImportResult.FailedRow(cols,
                        "Row " + lineNum + ": Semester must be between 1 and 8, got " + semester));
                    continue;
                }
            } catch (NumberFormatException e) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Semester is not a valid number: \"" + semStr + "\""));
                continue;
            }

            Students s = new Students(roll, name, dept, semester);
            data.items.add(s);
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── COURSES ──────────────────────────────────────────────────────
    // Expected: course_code, course_name, credits

    public static ParsedData<Courses> parseCourses(File file) {
        validateFile(file);
        ParsedData<Courses> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows.");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2;

            if (cols.length < 3) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 3 columns (course_code, course_name, credits), got " + cols.length));
                continue;
            }

            String code = cols[0].trim();
            String name = cols[1].trim();
            String credStr = cols[2].trim();

            if (code.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Course code is empty"));
                continue;
            }
            if (name.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Course name is empty"));
                continue;
            }

            int credits;
            try {
                credits = Integer.parseInt(credStr);
                if (credits <= 0 || credits > 10) {
                    data.failed.add(new ImportResult.FailedRow(cols,
                        "Row " + lineNum + ": Credits must be between 1 and 10, got " + credits));
                    continue;
                }
            } catch (NumberFormatException e) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Credits is not a valid number: \"" + credStr + "\""));
                continue;
            }

            data.items.add(new Courses(code, name, credits));
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── HALLS ────────────────────────────────────────────────────────
    // Expected: hall_name, seating_capacity

    public static ParsedData<Hall> parseHalls(File file) {
        validateFile(file);
        ParsedData<Hall> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows.");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2;

            if (cols.length < 2) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 2 columns (hall_name, seating_capacity), got " + cols.length));
                continue;
            }

            String name = cols[0].trim();
            String capStr = cols[1].trim();

            if (name.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Hall name is empty"));
                continue;
            }

            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) {
                    data.failed.add(new ImportResult.FailedRow(cols,
                        "Row " + lineNum + ": Capacity must be greater than 0, got " + capacity));
                    continue;
                }
            } catch (NumberFormatException e) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Capacity is not a valid number: \"" + capStr + "\""));
                continue;
            }

            Hall h = new Hall();
            h.setHallName(name);
            h.setSeatingCapacity(capacity);
            data.items.add(h);
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── INVIGILATORS ─────────────────────────────────────────────────
    // Expected: name, department, email

    public static ParsedData<Invigilator> parseInvigilators(File file) {
        validateFile(file);
        ParsedData<Invigilator> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows.");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2;

            if (cols.length < 3) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 3 columns (name, department, email), got " + cols.length));
                continue;
            }

            String name = cols[0].trim();
            String dept = cols[1].trim();
            String email = cols[2].trim();

            if (name.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Name is empty"));
                continue;
            }
            if (dept.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Department is empty"));
                continue;
            }

            data.items.add(new Invigilator(0, name, dept, email));
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── EXAMS ────────────────────────────────────────────────────────
    // Expected: course_code, exam_date, session

    public static ParsedData<Exam> parseExams(File file) {
        validateFile(file);
        ParsedData<Exam> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows.");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2;

            if (cols.length < 3) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 3 columns (course_code, exam_date, session), got " + cols.length));
                continue;
            }

            String code = cols[0].trim();
            String dateStr = cols[1].trim();
            String session = cols[2].trim().toUpperCase();

            if (code.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Course code is empty"));
                continue;
            }

            LocalDate examDate;
            try {
                examDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Invalid date format: \"" + dateStr + "\". Use YYYY-MM-DD"));
                continue;
            }

            if (!session.equals("FN") && !session.equals("AN")) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Session must be FN or AN, got \"" + session + "\""));
                continue;
            }

            Exam e = new Exam();
            e.setExamId(0);
            e.setCourseCode(code);
            e.setExamDate(examDate);
            e.setSession(session);
            data.items.add(e);
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── COURSE OFFERINGS ─────────────────────────────────────────────
    // Expected: course_code, department, semester

    public static ParsedData<CourseOffering> parseCourseOfferings(File file) {
        validateFile(file);
        ParsedData<CourseOffering> data = new ParsedData<>();
        List<String[]> rawRows = readCsv(file);

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows.");
        }

        for (int i = 0; i < rawRows.size(); i++) {
            String[] cols = rawRows.get(i);
            int lineNum = i + 2;

            if (cols.length < 3) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Expected 3 columns (course_code, department, semester), got " + cols.length));
                continue;
            }

            String code   = cols[0].trim();
            String dept   = cols[1].trim();
            String semStr = cols[2].trim();

            if (code.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Course code is empty"));
                continue;
            }
            if (dept.isEmpty()) {
                data.failed.add(new ImportResult.FailedRow(cols, "Row " + lineNum + ": Department is empty"));
                continue;
            }

            int semester;
            try {
                semester = Integer.parseInt(semStr);
                if (semester <= 0 || semester > 8) {
                    data.failed.add(new ImportResult.FailedRow(cols,
                        "Row " + lineNum + ": Semester must be between 1 and 8, got " + semester));
                    continue;
                }
            } catch (NumberFormatException e) {
                data.failed.add(new ImportResult.FailedRow(cols,
                    "Row " + lineNum + ": Semester is not a valid number: \"" + semStr + "\""));
                continue;
            }

            CourseOffering co = new CourseOffering();
            co.setCourseCode(code);
            co.setDepartment(dept);
            co.setSemester(semester);
            data.items.add(co);
            data.rawRows.add(cols);
        }

        return data;
    }

    // ── PARSED DATA CONTAINER ────────────────────────────────────────

    /**
     * Holds parsed items + their raw row data + any parse-time failures.
     * The service layer then processes items and may add more failures.
     */
    public static class ParsedData<T> {
        public final List<T> items                    = new ArrayList<>();
        public final List<String[]> rawRows           = new ArrayList<>();
        public final List<ImportResult.FailedRow> failed = new ArrayList<>();
    }

    // ── CSV READER ───────────────────────────────────────────────────

    /**
     * Reads a CSV file, skips the first header row, returns data rows.
     * Handles quoted fields (fields containing commas wrapped in double quotes).
     */
    private static List<String[]> readCsv(File file) {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                line = line.trim();
                if (line.isEmpty()) continue; // skip blank lines
                rows.add(parseCsvLine(line));
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Cannot read file: " + file.getName() + "\n" + e.getMessage());
        }

        return rows;
    }

    /**
     * Parses a single CSV line respecting quoted fields.
     * "Hall A, Main Campus",200  →  ["Hall A, Main Campus", "200"]
     */
    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
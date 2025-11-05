import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Table {
    private File file;

    public Table(String path) {
        this.file = new File(path);
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating table file: " + e.getMessage());
        }
    }

    // Create new table with given columns
    public void create(List<String> columns) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(String.join(",", columns));
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error creating table: " + e.getMessage());
        }
    }

    // Insert record
    public void insert(List<String> values) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(String.join(",", values));
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error inserting row: " + e.getMessage());
        }
    }

    // Select all records
    public void selectAll() {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines)
                System.out.println(line);
        } catch (IOException e) {
            System.out.println("Error reading table: " + e.getMessage());
        }
    }

    // Select where column=value
    public void selectWhere(String column, String value) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) return;

            String[] headers = lines.get(0).split(",");
            int index = Arrays.asList(headers).indexOf(column);
            if (index == -1) {
                System.out.println("Column not found: " + column);
                return;
            }

            System.out.println(String.join(",", headers));
            for (int i = 1; i < lines.size(); i++) {
                String[] vals = lines.get(i).split(",");
                if (vals[index].equals(value))
                    System.out.println(lines.get(i));
            }

        } catch (IOException e) {
            System.out.println("Error reading table: " + e.getMessage());
        }
    }

    // Delete all records except header
    public void deleteAll() {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (!lines.isEmpty()) {
                String header = lines.get(0);
                Files.write(file.toPath(), List.of(header));
            }
        } catch (IOException e) {
            System.out.println("Error deleting data: " + e.getMessage());
        }
    }

    // ✅ Update specific records
    public void update(String setCol, String setVal, String whereCol, String whereVal) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) return;

            String[] headers = lines.get(0).split(",");
            int setIndex = Arrays.asList(headers).indexOf(setCol);
            int whereIndex = Arrays.asList(headers).indexOf(whereCol);

            if (setIndex == -1 || whereIndex == -1) {
                System.out.println("Invalid column in UPDATE.");
                return;
            }

            for (int i = 1; i < lines.size(); i++) {
                String[] vals = lines.get(i).split(",");
                if (vals[whereIndex].equals(whereVal)) {
                    vals[setIndex] = setVal;
                    lines.set(i, String.join(",", vals));
                }
            }

            Files.write(file.toPath(), lines);
            System.out.println("Updated records where " + whereCol + "=" + whereVal);

        } catch (IOException e) {
            System.out.println("Error updating: " + e.getMessage());
        }
    }
}

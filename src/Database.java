import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Database {
    private final String root = "data";
    private String currentDatabase;
    private final ReentrantLock dbLock = new ReentrantLock(true);
    private final File commitLog = new File("commit.log");

    public Database() {
        File rootDir = new File(root);
        if (!rootDir.exists()) rootDir.mkdir();
        recoverUnfinishedTransactions();
    }

    // ---------------- DATABASE MANAGEMENT ----------------
    public String createDatabase(String name) {
        File dbFolder = new File(root, name);
        if (dbFolder.exists()) return "Database already exists.";
        if (dbFolder.mkdir()) return "Database created: " + name;
        return "Error: Could not create database.";
    }

    public String useDatabase(String name) {
        File dbFolder = new File(root, name);
        if (dbFolder.exists()) {
            currentDatabase = name;
            return "Using database: " + name;
        }
        return "Database not found.";
    }

    public String dropDatabase(String name) {
        File dbFolder = new File(root, name);
        if (!dbFolder.exists()) return "Database not found.";
        for (File f : Objects.requireNonNull(dbFolder.listFiles())) f.delete();
        dbFolder.delete();
        if (name.equals(currentDatabase)) currentDatabase = null;
        return "Database deleted: " + name;
    }

    // ---------------- TABLE MANAGEMENT ----------------
    public String createTable(String table, String[] columns) {
        ensureDBSelected();
        File tableFile = getTableFile(table);
        if (tableFile.exists()) return "Table already exists.";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tableFile))) {
            bw.write(String.join(",", columns));
            return "Table created: " + table;
        } catch (IOException e) {
            return "Error creating table: " + e.getMessage();
        }
    }

    public String showTables() {
        ensureDBSelected();
        File folder = new File(root, currentDatabase);
        String[] tables = folder.list((dir, name) -> name.endsWith(".txt"));
        if (tables == null || tables.length == 0)
            return "No tables found.";
        return "Tables:\n" + String.join("\n", Arrays.stream(tables)
                .map(t -> t.replace(".txt", ""))
                .toList());
    }

    public String dropTable(String table) {
        ensureDBSelected();
        File tableFile = getTableFile(table);
        if (tableFile.exists() && tableFile.delete())
            return "Table deleted: " + table;
        return "Table not found.";
    }

    // ---------------- CRUD ----------------

    public String insert(String table, String[] values) {
        ensureDBSelected();
        dbLock.lock();
        try {
            File tableFile = getTableFile(table);
            if (!tableFile.exists()) throw new IOException("Table not found");
            File tempFile = new File(tableFile + ".tmp");

            try (BufferedReader br = new BufferedReader(new FileReader(tableFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {

                String header = br.readLine();
                if (header == null) throw new IOException("Corrupted table");
                bw.write(header + "\n");
                String[] cols = header.split(",");
                if (values.length != cols.length)
                    throw new IOException("Value count mismatch with columns");

                String line;
                while ((line = br.readLine()) != null)
                    bw.write(line + "\n");
                bw.write(String.join(",", values) + "\n");
            }
            commitTransaction(tempFile, tableFile);
            return "Row inserted successfully.";
        } catch (IOException e) {
            rollbackTransaction();
            return "Insert failed: " + e.getMessage();
        } finally {
            dbLock.unlock();
        }
    }

    public String selectAll(String table) {
        ensureDBSelected();
        File tableFile = getTableFile(table);
        if (!tableFile.exists()) return "Table not found: " + table;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(tableFile))) {
            sb.append("---- ").append(table).append(" ----\n");
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append("\n");
        } catch (IOException e) {
            sb.append("Error reading: ").append(e.getMessage());
        }
        return sb.toString();
    }

    public String selectWhere(String table, String column, String value) {
        ensureDBSelected();
        File tableFile = getTableFile(table);
        if (!tableFile.exists()) return "Table not found: " + table;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(tableFile))) {
            String header = br.readLine();
            String[] cols = header.split(",");
            int index = getColumnIndex(cols, column);
            sb.append("---- ").append(table)
                    .append(" WHERE ").append(column).append("=").append(value).append(" ----\n");

            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                if (vals.length > index && vals[index].equals(value))
                    sb.append(line).append("\n");
            }
        } catch (IOException e) {
            sb.append("Error: ").append(e.getMessage());
        }
        return sb.toString();
    }

    public String update(String table, String column, String value, String condCol, String condVal) {
        ensureDBSelected();
        dbLock.lock();
        try {
            File tableFile = getTableFile(table);
            if (!tableFile.exists()) throw new IOException("Table not found");
            File tempFile = new File(tableFile + ".tmp");

            try (BufferedReader br = new BufferedReader(new FileReader(tableFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {

                String header = br.readLine();
                String[] cols = header.split(",");
                int colIndex = getColumnIndex(cols, column);
                int condIndex = getColumnIndex(cols, condCol);
                bw.write(header + "\n");

                String line;
                while ((line = br.readLine()) != null) {
                    String[] vals = line.split(",");
                    if (vals[condIndex].equals(condVal))
                        vals[colIndex] = value;
                    bw.write(String.join(",", vals) + "\n");
                }
            }
            commitTransaction(tempFile, tableFile);
            return "Update successful.";
        } catch (IOException e) {
            rollbackTransaction();
            return "Update failed: " + e.getMessage();
        } finally {
            dbLock.unlock();
        }
    }

    public String deleteAll(String table) {
        ensureDBSelected();
        dbLock.lock();
        try {
            File tableFile = getTableFile(table);
            if (!tableFile.exists()) throw new IOException("Table not found");
            File tempFile = new File(tableFile + ".tmp");
            try (BufferedReader br = new BufferedReader(new FileReader(tableFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
                String header = br.readLine();
                if (header != null) bw.write(header + "\n");
            }
            commitTransaction(tempFile, tableFile);
            return "All rows deleted.";
        } catch (IOException e) {
            rollbackTransaction();
            return "Delete failed: " + e.getMessage();
        } finally {
            dbLock.unlock();
        }
    }

    // ---------------- TRANSACTION SYSTEM ----------------
    private void commitTransaction(File temp, File original) throws IOException {
        try (FileWriter log = new FileWriter(commitLog, true)) {
            log.write("COMMIT " + original.getPath() + " " + System.currentTimeMillis() + "\n");
        }
        Files.move(temp.toPath(), original.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void rollbackTransaction() {
        System.out.println("Transaction rolled back — no data loss.");
    }

    private void recoverUnfinishedTransactions() {
        System.out.println("Checking for unfinished transactions...");
        File rootDir = new File(root);
        if (!rootDir.exists()) return;
        for (File dir : Objects.requireNonNull(rootDir.listFiles())) {
            if (dir.isDirectory()) {
                for (File f : Objects.requireNonNull(dir.listFiles()))
                    if (f.getName().endsWith(".tmp")) {
                        System.out.println("Rolling back: " + f.getName());
                        f.delete();
                    }
            }
        }
    }

    // ---------------- UTILITIES ----------------
    private void ensureDBSelected() {
        if (currentDatabase == null)
            throw new IllegalStateException("No database selected.");
    }

    private File getTableFile(String table) {
        return new File(root + "/" + currentDatabase, table + ".txt");
    }

    private int getColumnIndex(String[] cols, String col) {
        for (int i = 0; i < cols.length; i++)
            if (cols[i].equalsIgnoreCase(col)) return i;
        throw new IllegalArgumentException("Column not found: " + col);
    }

    public String getTablesList() {
        ensureDBSelected();
        File folder = new File(root, currentDatabase);
        String[] tables = folder.list((dir, name) -> name.endsWith(".txt"));
        if (tables == null || tables.length == 0)
            return "No tables found.";
        return String.join("\n", Arrays.stream(tables)
                .map(t -> t.replace(".txt", ""))
                .toList());
    }

    public String getDatabasesList() {
        File folder = new File(root);
        String[] dbs = folder.list((dir, name) -> new File(dir, name).isDirectory());
        if (dbs == null || dbs.length == 0)
            return "No databases found.";
        return String.join("\n", dbs);
    }

    // Expose current database for UI/status purposes
    public String getCurrentDatabase() {
        return currentDatabase;
    }
}

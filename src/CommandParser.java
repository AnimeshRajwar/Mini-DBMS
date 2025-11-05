import java.util.*;
import java.util.regex.*;

public class CommandParser {
    private final Database db;

    public CommandParser(Database db) {
        this.db = db;
    }

    public String execute(String command) {
        command = command.trim();
        if (command.isEmpty()) return "Empty command.";

        // Case-insensitive matching but preserve original case for values
        String lower = command.toLowerCase();

        try {
            // CREATE DATABASE
            if (lower.startsWith("create database")) {
                String dbName = command.split("\\s+")[2].replace(";", "");
                return db.createDatabase(dbName);
            }

            // USE DATABASE
            else if (lower.startsWith("use")) {
                String dbName = command.split("\\s+")[1].replace(";", "");
                return db.useDatabase(dbName);
            }

            // CREATE TABLE table(col1, col2, col3)
            else if (lower.startsWith("create table")) {
                Pattern p = Pattern.compile("create table (\\w+) \\((.+)\\);?", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(command);
                if (m.find()) {
                    String table = m.group(1);
                    String[] cols = Arrays.stream(m.group(2).split(","))
                            .map(String::trim)
                            .toArray(String[]::new);
                    return db.createTable(table, cols);
                }
                return "Invalid CREATE TABLE syntax.";
            }

            // INSERT INTO table VALUES(...)
            else if (lower.startsWith("insert into")) {
                Pattern p = Pattern.compile("insert into (\\w+) values\\s*\\((.+)\\);?", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(command);
                if (m.find()) {
                    String table = m.group(1);
                    String valuesPart = m.group(2).trim();

                    // ✅ Split on commas that are NOT inside quotes
                    List<String> vals = new ArrayList<>();
                    Matcher valueMatcher = Pattern.compile("('[^']*'|[^,]+)").matcher(valuesPart);
                    while (valueMatcher.find()) {
                        String val = valueMatcher.group(1).trim();
                        // remove quotes if present
                        if (val.startsWith("'") && val.endsWith("'"))
                            val = val.substring(1, val.length() - 1);
                        vals.add(val);
                    }

                    return db.insert(table, vals.toArray(new String[0]));
                }
                return "Invalid INSERT syntax.";
            }

            // SELECT * FROM table [WHERE col=value]
            else if (lower.startsWith("select")) {
                Pattern p = Pattern.compile(
                        "select \\* from (\\w+)( where (\\w+)=([\\w']+))?;?",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(command);
                if (m.find()) {
                    String table = m.group(1);
                    if (m.group(3) != null) {
                        String col = m.group(3);
                        String val = m.group(4).replace("'", "");
                        return db.selectWhere(table, col, val);
                    } else {
                        return db.selectAll(table);
                    }
                }
                return "Invalid SELECT syntax.";
            }

            // UPDATE table SET col=value WHERE col=value
            else if (lower.startsWith("update")) {
                Pattern p = Pattern.compile(
                        "update (\\w+) set (\\w+)=([\\w']+) where (\\w+)=([\\w']+);?",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(command);
                if (m.find()) {
                    String table = m.group(1);
                    String setCol = m.group(2);
                    String setVal = m.group(3).replace("'", "");
                    String whereCol = m.group(4);
                    String whereVal = m.group(5).replace("'", "");
                    return db.update(table, setCol, setVal, whereCol, whereVal);
                }
                return "Invalid UPDATE syntax.";
            }

            // DELETE FROM table
            else if (lower.startsWith("delete from")) {
                String[] parts = command.split("\\s+");
                if (parts.length >= 3) {
                    String table = parts[2].replace(";", "");
                    return db.deleteAll(table);
                }
                return "Invalid DELETE syntax.";
            }

            // DROP TABLE
            else if (lower.startsWith("drop table")) {
                String table = command.split("\\s+")[2].replace(";", "");
                return db.dropTable(table);
            }

            // DROP DATABASE
            else if (lower.startsWith("drop database")) {
                String dbName = command.split("\\s+")[2].replace(";", "");
                return db.dropDatabase(dbName);
            }

            // SHOW TABLES
            else if (lower.startsWith("show tables")) {
                return db.showTables();
            }

            // SHOW DATABASES
            else if (lower.startsWith("show databases")) {
                return db.getDatabasesList();
            }

            else {
                return "Unknown command: " + command;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

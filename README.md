```markdown
# 🗄️ Mini DBMS (Java)

A minimal, file-backed **Database Management System** implemented in **pure Java**, featuring a **simple SQL-like parser**, **text-based storage**, and a **Swing GUI**.  
This project is intended as a **learning/demo project** — not a production-grade database.

---

## 📁 Project Structure

```

Mini-DBMS/
│
├── src/
│   ├── DBMSGUI.java         # Swing GUI: input, output, menus, status bar, command history
│   ├── CommandParser.java   # Parses SQL-like commands and dispatches to Database
│   ├── Database.java        # Core DB engine: storage, CRUD, commit/rollback
│   └── Table.java           # Utility helper for per-table operations (alternate API)
│
├── data/                    # Root data folder (each database is a subfolder)
│   └── demo/
│       └── students.txt     # Example table file
│
├── commit.log               # Log of commits for recovery
└── README.md

````

---

## 🚀 Features

✅ File-based data storage using plain text files  
✅ SQL-like commands for **CREATE**, **INSERT**, **SELECT**, **UPDATE**, **DELETE**  
✅ Multiple databases supported (`CREATE DATABASE`, `USE`, `SHOW DATABASES`)  
✅ Transaction-like file commit and recovery mechanism  
✅ Swing GUI with command history and hotkeys  
✅ Commit logging and automatic cleanup of unfinished transactions  

---

## 💡 Supported SQL-like Commands

| Command | Example |
|----------|----------|
| `CREATE DATABASE <name>;` | `CREATE DATABASE demo;` |
| `DROP DATABASE <name>;` | `DROP DATABASE demo;` |
| `USE <name>;` | `USE demo;` |
| `SHOW DATABASES;` | Lists all database folders under `data/` |
| `CREATE TABLE <name> (<col1>, <col2>, ...);` | `CREATE TABLE students (id, name, age);` |
| `DROP TABLE <name>;` | `DROP TABLE students;` |
| `SHOW TABLES;` | Lists all tables in the current database |
| `INSERT INTO <table> VALUES('<v1>', '<v2>', ...);` | `INSERT INTO students VALUES('1', 'Bruce Wayne', '25');` |
| `SELECT * FROM <table> [WHERE <col>=<value>];` | `SELECT * FROM students WHERE name='Bruce Wayne';` |
| `UPDATE <table> SET <col>=<value> WHERE <col>=<value>;` | `UPDATE students SET age='26' WHERE id='1';` |
| `DELETE FROM <table>;` | Deletes all rows but keeps header |

> ⚙️ Commands are **case-insensitive**, and simple single-quoted string values are supported.

---

## 🖥️ GUI Overview

The GUI (`DBMSGUI.java`) provides:
- 🧾 **Command input area** and **output console**
- 🧭 **Menu bar** with Database and Table operations
- 💬 **Status bar** showing the active database
- ⌨️ **Hotkeys**:
  - `Ctrl + Enter` — Execute command  
  - `Ctrl + ↑` / `Ctrl + ↓` — Navigate command history  
- 🪟 Input dialogs for quick database/table creation

> If your desktop environment hides the menu bar, resize or switch focus to reveal it.

---

## ⚙️ Storage Model & Transaction Logic

- Tables stored under `data/<database>/<table>.txt`
- First line = column headers, following lines = rows (comma-separated)
- Write operations (INSERT, UPDATE, DELETE):
  1. Acquire global write lock
  2. Write updates to `.tmp` file
  3. Log commit entry to `commit.log`
  4. Replace original file atomically

**Recovery:** On startup, unfinished `.tmp` files are removed automatically.

---

## 🧑‍💻 Example Session

Run from terminal:

```bash
# Compile
javac -d out src/*.java

# Launch GUI
java -cp out DBMSGUI
````

Then in the GUI:

```sql
CREATE DATABASE demo;
USE demo;
CREATE TABLE students (id, name, age);
INSERT INTO students VALUES('1', 'Bruce Wayne', '25');
INSERT INTO students VALUES('2', 'Diana Prince', '1000');
SELECT * FROM students;
UPDATE students SET age='26' WHERE id='1';
DELETE FROM students;
SHOW TABLES;
SHOW DATABASES;
```

---

## 🧰 Implementation Notes

* `Database.java` handles:

  * File operations
  * Temporary file commits
  * Recovery on startup
  * Concurrency control with a `ReentrantLock`
* `CommandParser.java` handles:

  * Regex-based SQL-like parsing
  * Command dispatch to Database
* `DBMSGUI.java`:

  * Integrates parser + database
  * Displays output in Swing text area



# 🗄️ Mini DBMS (Java)

A simple **file-based Database Management System** built entirely in **Java**.  
It supports basic **SQL-like commands**, **CRUD operations**, and a **Swing GUI** for easy interaction.  
Designed as a **learning project** to understand how databases work internally.

---

## 🚀 Features

- File-based data storage using plain text files  
- SQL-like commands: `CREATE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`  
- Multiple databases (`CREATE DATABASE`, `USE`, `SHOW DATABASES`)  
- Transaction-like commit and recovery system  
- Swing GUI with command history and hotkeys  
- Simple logging via `commit.log`

---

## 🖥️ GUI Overview

The Swing-based GUI provides:
- Command input and output display  
- Menu bar for Database/Table actions  
- Status bar showing current database  
- Hotkeys:  
  - `Ctrl + Enter` → Execute command  
  - `Ctrl + ↑ / ↓` → Navigate command history  

---

## ⚙️ How It Works

- Each database is stored under `data/<database>/`  
- Tables are `.txt` files (first row = headers, remaining = data)  
- Write operations are performed via `.tmp` files for safety  
- `commit.log` records changes  
- Unfinished transactions are cleaned up on startup  

---

## 🧑‍💻 Example Usage

```bash
# Compile
javac -d out src/*.java

# Run the GUI
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
SHOW TABLES;
```

---

## 📘 Notes

* All data is stored as plain text — easy to inspect and debug.
* No indexing or transaction rollback beyond basic safety.
* Ideal for students and beginners exploring DBMS internals.

```

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class DBMSGUI extends JFrame {
    private JTextArea commandArea;
    private JTextArea outputArea;
    private JButton executeButton;
    private Database db;
    private CommandParser parser;
    private JLabel statusLabel;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    public DBMSGUI() {
        db = new Database();
        parser = new CommandParser(db);

        setTitle("Mini DBMS (Java)");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

    JLabel title = new JLabel("Mini DBMS", JLabel.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 26));
    add(title, BorderLayout.NORTH);

    // Menu bar
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    JMenuItem exitItem = new JMenuItem("Exit");
    exitItem.addActionListener(e -> System.exit(0));
    fileMenu.add(exitItem);

    JMenu dbMenu = new JMenu("Database");
    JMenuItem createDb = new JMenuItem("Create Database");
    createDb.addActionListener(e -> createDatabaseDialog());
    JMenuItem useDb = new JMenuItem("Use Database");
    useDb.addActionListener(e -> useDatabaseDialog());
    JMenuItem dropDb = new JMenuItem("Drop Database");
    dropDb.addActionListener(e -> dropDatabaseDialog());
    JMenuItem showDbs = new JMenuItem("Show Databases");
    showDbs.addActionListener(e -> appendOutput(parser.execute("SHOW DATABASES;")));
    dbMenu.add(createDb);
    dbMenu.add(useDb);
    dbMenu.add(dropDb);
    dbMenu.addSeparator();
    dbMenu.add(showDbs);

    JMenu tableMenu = new JMenu("Table");
    JMenuItem createTable = new JMenuItem("Create Table");
    createTable.addActionListener(e -> createTableDialog());
    JMenuItem dropTable = new JMenuItem("Drop Table");
    dropTable.addActionListener(e -> dropTableDialog());
    JMenuItem showTables = new JMenuItem("Show Tables");
    showTables.addActionListener(e -> appendOutput(parser.execute("SHOW TABLES;")));
    tableMenu.add(createTable);
    tableMenu.add(dropTable);
    tableMenu.addSeparator();
    tableMenu.add(showTables);

    JMenu actionsMenu = new JMenu("Actions");
    JMenuItem clearOut = new JMenuItem("Clear Output");
    clearOut.addActionListener(e -> outputArea.setText(""));
    JMenuItem execCmd = new JMenuItem("Execute Command");
    execCmd.addActionListener(e -> executeCommand());
    actionsMenu.add(execCmd);
    actionsMenu.add(clearOut);

    menuBar.add(fileMenu);
    menuBar.add(dbMenu);
    menuBar.add(tableMenu);
    menuBar.add(actionsMenu);
    setJMenuBar(menuBar);

        commandArea = new JTextArea(5, 50);
        commandArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        commandArea.setBorder(BorderFactory.createTitledBorder("Enter Command"));

        // Key bindings: Ctrl+Enter executes, Ctrl+Up/Down for history
        InputMap im = commandArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = commandArea.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "executeCmd");
        am.put("executeCmd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCommand();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "historyUp");
        am.put("historyUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (history.isEmpty()) return;
                if (historyIndex == -1) historyIndex = history.size();
                historyIndex = Math.max(0, historyIndex - 1);
                commandArea.setText(history.get(historyIndex));
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "historyDown");
        am.put("historyDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (history.isEmpty()) return;
                if (historyIndex == -1) historyIndex = -1;
                historyIndex = Math.min(history.size() - 1, historyIndex + 1);
                if (historyIndex >= 0 && historyIndex < history.size())
                    commandArea.setText(history.get(historyIndex));
            }
        });

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createTitledBorder("Output"));

        executeButton = new JButton("Execute Command");
        executeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        executeButton.addActionListener(e -> executeCommand());

    JButton clearButton = new JButton("Clear Output");
    clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    clearButton.addActionListener(e -> outputArea.setText(""));

        JPanel center = new JPanel(new GridLayout(2, 1, 10, 10));
        center.add(new JScrollPane(commandArea));
        center.add(new JScrollPane(outputArea));

    add(center, BorderLayout.CENTER);

    // bottom panel with execute/clear buttons and status
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonsPanel.add(clearButton);
    buttonsPanel.add(executeButton);

    statusLabel = new JLabel();
    updateStatus();

    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(buttonsPanel, BorderLayout.NORTH);
    JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
    statusWrap.add(statusLabel);
    southPanel.add(statusWrap, BorderLayout.SOUTH);

    add(southPanel, BorderLayout.SOUTH);

        // Remove old loadAll/saveAll
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Database GUI closed.");
            }
        });

        setVisible(true);
    }

    private void executeCommand() {
        String command = commandArea.getText().trim();
        if (command.isEmpty()) return;
        try {
            String result = parser.execute(command);
            appendOutput(">> " + command + "\n" + result + "\n");
            // add to history
            history.add(command);
            historyIndex = -1;
            updateStatus();
        } catch (Exception ex) {
            appendOutput("Error: " + ex.getMessage() + "\n");
        }
        commandArea.setText("");
    }

    private void appendOutput(String text) {
        outputArea.append(text + "\n");
    }

    private void updateStatus() {
        try {
            String cur = db.getCurrentDatabase();
            statusLabel.setText("Current DB: " + (cur == null ? "<none>" : cur));
        } catch (Exception e) {
            statusLabel.setText("Current DB: <unknown>");
        }
    }

    // Dialog helpers for menu actions
    private void createDatabaseDialog() {
        String name = JOptionPane.showInputDialog(this, "Database name:");
        if (name != null && !name.isBlank()) {
            appendOutput(parser.execute("CREATE DATABASE " + name + ";"));
            updateStatus();
        }
    }

    private void useDatabaseDialog() {
        String name = JOptionPane.showInputDialog(this, "Use database:");
        if (name != null && !name.isBlank()) {
            appendOutput(parser.execute("USE " + name + ";"));
            updateStatus();
        }
    }

    private void dropDatabaseDialog() {
        String name = JOptionPane.showInputDialog(this, "Drop database:");
        if (name != null && !name.isBlank()) {
            appendOutput(parser.execute("DROP DATABASE " + name + ";"));
            updateStatus();
        }
    }

    private void createTableDialog() {
        String name = JOptionPane.showInputDialog(this, "Table name:");
        if (name == null || name.isBlank()) return;
        String cols = JOptionPane.showInputDialog(this, "Columns (comma separated):");
        if (cols == null || cols.isBlank()) return;
        appendOutput(parser.execute("CREATE TABLE " + name + " (" + cols + ");"));
    }

    private void dropTableDialog() {
        String name = JOptionPane.showInputDialog(this, "Drop table:");
        if (name != null && !name.isBlank()) {
            appendOutput(parser.execute("DROP TABLE " + name + ";"));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DBMSGUI::new);
    }
}

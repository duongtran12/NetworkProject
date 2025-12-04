package client;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

public class ClientGUI extends JFrame {
    private JTextField ipField, portField, userField, passField, commandField;
    private JButton connectButton, sendButton;
    private JTextArea logArea, clientFilesArea, serverFilesArea;
    private Client client;

    private JComboBox<String> cmdTemplateBox;  

    private static final Color PRIMARY_COLOR = new Color(52, 152, 219); 
    private static final Color SECONDARY_COLOR = new Color(46, 204, 113); 
    private static final Color BACKGROUND_COLOR = new Color(236, 240, 241); 
    private static final Color TEXT_COLOR = new Color(44, 62, 80); 
    private static final Color BUTTON_COLOR = new Color(243, 156, 18); 
    private static final Font MONOSPACE_BOLD_FONT = new Font("Consolas", Font.BOLD, 14);

    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TEXT_FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font MONOSPACE_FONT = new Font("Consolas", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private static final Color LOG_DEFAULT_TEXT_COLOR = Color.WHITE;

    public ClientGUI(Client client) {
        this.client = client;
        setTitle("FTP Client");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        getContentPane().setBackground(BACKGROUND_COLOR);
        getContentPane().setLayout(new BorderLayout(5, 5));
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(Color.WHITE); 
        
        ipField = new JTextField("172.20.10.2", 10);
        portField = new JTextField("12345", 5);
        userField = new JTextField("", 10);
        passField = new JTextField("", 10);
        
        ipField.setFont(TEXT_FIELD_FONT);
        portField.setFont(TEXT_FIELD_FONT);
        userField.setFont(TEXT_FIELD_FONT);
        passField.setFont(TEXT_FIELD_FONT);
        
        connectButton = new JButton("Connect");
        connectButton.setFont(BUTTON_FONT);
        connectButton.setBackground(PRIMARY_COLOR);
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        
        JLabel ipLabel = new JLabel("IP:"); ipLabel.setFont(LABEL_FONT); ipLabel.setForeground(TEXT_COLOR);
        JLabel portLabel = new JLabel("Port:"); portLabel.setFont(LABEL_FONT); portLabel.setForeground(TEXT_COLOR);
        JLabel userLabel = new JLabel("User:"); userLabel.setFont(LABEL_FONT); userLabel.setForeground(TEXT_COLOR);
        JLabel passLabel = new JLabel("Pass:"); passLabel.setFont(LABEL_FONT); passLabel.setForeground(TEXT_COLOR);
        
        topPanel.add(ipLabel); topPanel.add(ipField);
        topPanel.add(portLabel); topPanel.add(portField);
        topPanel.add(userLabel); topPanel.add(userField);
        topPanel.add(passLabel); topPanel.add(passField);
        topPanel.add(connectButton);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        clientFilesArea = new JTextArea();
        clientFilesArea.setEditable(false);
        clientFilesArea.setFont(MONOSPACE_FONT);
        JScrollPane clientScroll = new JScrollPane(clientFilesArea);
        clientScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            "Client Files", TitledBorder.LEFT, TitledBorder.TOP, LABEL_FONT.deriveFont(Font.BOLD, 13), TEXT_COLOR));

        serverFilesArea = new JTextArea();
        serverFilesArea.setEditable(false);
        serverFilesArea.setFont(MONOSPACE_FONT);
        JScrollPane serverScroll = new JScrollPane(serverFilesArea);
        serverScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            "Server Files", TitledBorder.LEFT, TitledBorder.TOP, LABEL_FONT.deriveFont(Font.BOLD, 13), TEXT_COLOR));

        JPanel clientPanel = new JPanel(new BorderLayout());
        clientPanel.add(clientScroll, BorderLayout.CENTER);
        clientPanel.setBackground(BACKGROUND_COLOR); 

        JPanel serverPanel = new JPanel(new BorderLayout());
        serverPanel.add(serverScroll, BorderLayout.CENTER);
        serverPanel.setBackground(BACKGROUND_COLOR); 

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientPanel, serverPanel);
        splitPane.setDividerLocation(450);
        splitPane.setBackground(BACKGROUND_COLOR); 
        getContentPane().add(splitPane, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(MONOSPACE_BOLD_FONT);
        logArea.setBackground(new Color(128, 128, 128)); 
        logArea.setForeground(LOG_DEFAULT_TEXT_COLOR); 
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            "Log", TitledBorder.LEFT, TitledBorder.TOP, LABEL_FONT.deriveFont(Font.BOLD, 13), TEXT_COLOR));

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        bottomPanel.setBackground(BACKGROUND_COLOR); 

        // 🔥 COMBOBOX LIST COMMAND
        cmdTemplateBox = new JComboBox<>(new String[]{
            "LIST",
            "PWD",
            "CWD ..",
            "CWD <folder>",
            "MKD <folder>",
            "DELE <file>",
            "RMD <folder>",
            //tạo file rỗng trên server
            "CREA <file>",
            "WRITE <file> <nội dung>",
            "READ <file>",
            "RETR <file>",
            "STOR <file>",
            "APPE <file>",
            "RNFR <file>",
            "RNTO <newFile>",
         // 🔽 LỆNH LOCAL PHÍA CLIENT
            "CLT_PWD",
            "CLT_CWD ..",
            "CLT_CWD <folder>",
            "CLT_MKD <folder>",
            "CLT_CREAFILE <file>",
            "CLT_WRITE <file> <nội dung>",
            "CLT_READ <file>",
            "CLT_DELE <file>",
            "CLT_RMD <folder>",
            "SYST",
            "STAT",
            "HELP",
            "QUIT"
        });
        cmdTemplateBox.setFont(TEXT_FIELD_FONT);

        JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
        commandField = new JTextField();
        commandField.setFont(TEXT_FIELD_FONT);
        
        sendButton = new JButton("Send");
        sendButton.setFont(BUTTON_FONT);
        sendButton.setBackground(PRIMARY_COLOR);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        // ADD COMBOBOX + TEXTFIELD + BUTTON
        commandPanel.add(cmdTemplateBox, BorderLayout.WEST);
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);
        commandPanel.setBackground(BACKGROUND_COLOR);

        bottomPanel.add(commandPanel, BorderLayout.SOUTH);
        bottomPanel.setPreferredSize(new Dimension(0, 200));
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendCommand());
        commandField.addActionListener(e -> sendCommand());

        cmdTemplateBox.addActionListener(e -> {
            String selected = (String) cmdTemplateBox.getSelectedItem();
            if (selected != null) commandField.setText(selected);
        });

        setVisible(true);
    }

    private void connectToServer() {
        String ip = ipField.getText().trim();
        int port = 0; 
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
             return;
        }
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        client.setServerInfo(ip, port, user, pass);
        client.connectToServer();
    }

    private void sendCommand() {
        String cmd = commandField.getText().trim();
        if (!cmd.isEmpty()) {
            client.sendCommand(cmd);
            commandField.setText("");
        }
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.toUpperCase().contains("ERROR") || message.toUpperCase().contains("FAIL")) {
                 logArea.setForeground(new Color(231, 76, 60));
            } else if (message.startsWith("2") || message.startsWith("3")) {
                logArea.setForeground(SECONDARY_COLOR.darker()); 
            } else {
                 logArea.setForeground(LOG_DEFAULT_TEXT_COLOR); 
            }
            
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

//    public void updateClientFiles(String path) {
//        SwingUtilities.invokeLater(() -> {
//            File dir = new File(path);
//            if (!dir.exists()) {
//                clientFilesArea.setText("Thư mục không tồn tại");
//                return;
//            }
//            File[] files = dir.listFiles();
//            StringBuilder sb = new StringBuilder();
//            if (files != null) {
//                for (File f : files) {
//                    sb.append(f.getName());
//                    if (f.isDirectory()) sb.append("/");
//                    sb.append("\n");
//                }
//            }
//            clientFilesArea.setText(sb.toString());
//        });
//    }

    public void updateClientFiles(String path) {
        SwingUtilities.invokeLater(() -> {
            File dir = new File(path);
            if (!dir.exists()) {
                clientFilesArea.setText("Thư mục không tồn tại");
                return;
            }

            StringBuilder sb = new StringBuilder();

            // tên root: chỉ hiện tên thư mục user (vd: cam/)
            sb.append(dir.getName()).append("/\n");

            File[] children = dir.listFiles();
            if (children != null && children.length > 0) {
                // thư mục trước, file sau; sort theo tên
                Arrays.sort(children, Comparator
                        .comparing(File::isFile)     // dir=false, file=true → dir trước
                        .thenComparing(File::getName, String::compareToIgnoreCase));

                for (int i = 0; i < children.length; i++) {
                    boolean isLast = (i == children.length - 1);
                    buildClientTree(children[i], "", isLast, sb);
                }
            }

            clientFilesArea.setText(sb.toString());
        });
    }

    // vẽ cây đẹp cho panel Client Files
    private void buildClientTree(File f, String prefix, boolean isLast, StringBuilder sb) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(f.getName());
        if (f.isDirectory()) sb.append("/");
        sb.append("\n");

        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null || children.length == 0) return;

            Arrays.sort(children, Comparator
                    .comparing(File::isFile)
                    .thenComparing(File::getName, String::compareToIgnoreCase));

            String newPrefix = prefix + (isLast ? "    " : "│   ");
            for (int i = 0; i < children.length; i++) {
                boolean childLast = (i == children.length - 1);
                buildClientTree(children[i], newPrefix, childLast, sb);
            }
        }
    }

    
    public void updateServerFiles(String filesList) {
        SwingUtilities.invokeLater(() -> serverFilesArea.setText(filesList));
    }
}

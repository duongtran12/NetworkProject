package client;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ClientGUI extends JFrame {
    private JTextArea logArea;
    private JTextField commandField;
    private JButton uploadBtn, downloadBtn, listBtn, sendBtn, quitBtn;
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private Client client;

    public ClientGUI(Client client) {
        this.client = client;
        setTitle("Client GUI - FTP & Chat");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout(10, 10));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
        commandField = new JTextField();
        sendBtn = new JButton("Gửi");
        sendBtn.addActionListener(e -> sendCommandFromField());
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendBtn, BorderLayout.EAST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        uploadBtn = new JButton("Upload");
        downloadBtn = new JButton("Download");
        listBtn = new JButton("List Files");
        quitBtn = new JButton("Quit");

        actionPanel.add(uploadBtn);
        actionPanel.add(downloadBtn);
        actionPanel.add(listBtn);
        actionPanel.add(quitBtn);

        uploadBtn.addActionListener(e -> chooseAndUpload());
        downloadBtn.addActionListener(e -> downloadSelectedFile());
        listBtn.addActionListener(e -> client.sendCommand("LIST"));
        quitBtn.addActionListener(e -> client.sendCommand("Q"));

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setFont(new Font("Arial", Font.PLAIN, 13));
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setPreferredSize(new Dimension(200, 0));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, scrollPane, listScroll);
        splitPane.setDividerLocation(380);

        add(splitPane, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);
        add(actionPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private void sendCommandFromField() {
        String command = commandField.getText().trim();
        if (!command.isEmpty()) {
            client.sendCommand(command);
            commandField.setText("");
        }
    }

    private void chooseAndUpload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./client_files"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            client.sendCommand("UPLOAD " + file.getName());
        }
    }

    private void downloadSelectedFile() {
        String selected = fileList.getSelectedValue();
        if (selected != null) {
            client.sendCommand("DOWNLOAD " + selected);
        } else {
            appendLog("Chưa chọn file để tải xuống!");
        }
    }

    public void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());

        if (message.startsWith("Server:") && message.contains(",")) {
            updateFileList(message.replace("Server:", "").trim());
        }
    }

    private void updateFileList(String files) {
        listModel.clear();
        if (!files.equals("Không có file")) {
            for (String f : files.split(",")) {
                listModel.addElement(f.trim());
            }
        }
    }
}

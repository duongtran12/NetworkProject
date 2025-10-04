package client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JFrame {
    private JTextArea logArea;
    private JTextField commandField;
    private JButton sendButton;
    private JLabel statusLabel;
    private Client client;

    public ClientGUI(Client client) {
        this.client = client;
        setTitle("FTP Chat Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ==== Header ====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(45, 52, 54));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("FTP Chat Client");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        statusLabel = new JLabel("● Connected to server");
        statusLabel.setForeground(new Color(0, 255, 0));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        // ==== Log area ====
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // ==== Input area ====
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        commandField = new JTextField();
        commandField.setFont(new Font("Consolas", Font.PLAIN, 14));

        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(39, 174, 96));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setFocusPainted(false);

        // Action
        commandField.addActionListener(e -> sendCommandFromField());
        sendButton.addActionListener(e -> sendCommandFromField());

        inputPanel.add(commandField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // ==== Layout tổng ====
        setLayout(new BorderLayout(5, 5));
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void sendCommandFromField() {
        String command = commandField.getText().trim();
        if (!command.isEmpty()) {
            client.sendCommand(command);
            commandField.setText("");
        }
    }

    public void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

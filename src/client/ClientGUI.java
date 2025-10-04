package client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JFrame {
    private final JTextArea logArea;
    private final JTextField commandField;
    private final JButton sendButton;
    private final JLabel statusLabel;
    private final Client client;
    

    private String lastCommand = "";
    

    public ClientGUI(Client client) {
        this.client = client;
        setTitle("FTP Chat Client");
        setSize(700, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(45, 52, 54));
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JLabel titleLabel = new JLabel("FTP Chat Client");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusLabel = new JLabel("● Connected to server");
        statusLabel.setForeground(new Color(0, 200, 0));
        header.add(titleLabel, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        // log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(new Color(0, 255, 100));
        JScrollPane scroll = new JScrollPane(logArea);

        // input
        JPanel input = new JPanel(new BorderLayout(6, 6));
        commandField = new JTextField();
        sendButton = new JButton("Send");
        input.add(commandField, BorderLayout.CENTER);
        input.add(sendButton, BorderLayout.EAST);

        commandField.addActionListener(e -> sendCommandFromField());
        sendButton.addActionListener(e -> sendCommandFromField());

        setLayout(new BorderLayout(6, 6));
        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void sendCommandFromField() {
        String cmd = commandField.getText().trim();
        if (!cmd.isEmpty()) {
            setLastCommand(cmd);
            client.sendCommand(cmd);
            commandField.setText("");
        }
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void setLastCommand(String cmd) {
        this.lastCommand = cmd;
    }

    public String getLastCommand() {
        return lastCommand == null ? "" : lastCommand;
    }
}

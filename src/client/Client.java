package client;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket controlSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private ClientGUI gui;

    private String lastCommand = "";
    private String currentUser = "anonymous"; 

    public Client() {
        gui = new ClientGUI(this);
        connectToServer();
    }

    private void connectToServer() {
        try {
            controlSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new DataInputStream(controlSocket.getInputStream());
            out = new DataOutputStream(controlSocket.getOutputStream());
            gui.appendLog("✅ Connected to server");

            new Thread(() -> {
                try {
                    while (true) {
                        String response = in.readUTF();
                        handleServerResponse(response);
                    }
                } catch (IOException e) {
                    gui.appendLog("❌ Disconnected: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            gui.appendLog("Lỗi kết nối đến Server: " + e.getMessage());
        }
    }

    private void handleServerResponse(String response) {
        gui.appendLog("Server: " + response);

        try {
            if (response.startsWith("331")) {
                String[] cmdParts = lastCommand.split(" ", 2);
                if (cmdParts.length > 1) {
                    currentUser = cmdParts[1].trim();
                    File userDir = new File("client_files/" + currentUser);
                    if (!userDir.exists()) userDir.mkdirs();
                    gui.appendLog("📁 Đã tạo thư mục riêng cho client: " + currentUser);
                }
            }

            if (response.startsWith("150 Opening data connection on port")) {
                int port = extractPort(response);
                if (lastCommand.startsWith("LIST")) {
                    handleList(port);
                } else if (lastCommand.startsWith("STOR")) {
                    handleStor(port, lastCommand);
                } else if (lastCommand.startsWith("RETR")) {
                    handleRetr(port, lastCommand);
                }
            }
        } catch (Exception e) {
            gui.appendLog("⚠️ Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    private void handleList(int port) throws IOException {
        try (Socket dataSocket = new Socket(SERVER_ADDRESS, port);
             DataInputStream din = new DataInputStream(dataSocket.getInputStream())) {
            String list = din.readUTF();
            gui.appendLog("📂 Danh sách file:\n" + list);
        }
        gui.appendLog("✅ Transfer complete.");
    }

    private void handleStor(int port, String command) throws IOException {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            gui.appendLog("⚠️ Thiếu tên file để upload!");
            return;
        }
        String filename = parts[1].trim();
        File file = new File("client_files/" + currentUser + "/" + filename);
        if (!file.exists()) {
            gui.appendLog("⚠️ File không tồn tại: " + file.getPath());
            return;
        }

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        try (Socket dataSocket = new Socket(SERVER_ADDRESS, port);
             DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = fis.read(buffer)) != -1) {
                dout.write(buffer, 0, bytes);
            }
            dout.flush();
        }

        gui.appendLog("✅ Uploaded: " + filename);
    }

    private void handleRetr(int port, String command) throws IOException {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            gui.appendLog("⚠️ Thiếu tên file để tải về!");
            return;
        }
        String filename = parts[1].trim();
        File dest = new File("client_files/" + currentUser + "/" + filename);
        dest.getParentFile().mkdirs();

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        try (Socket dataSocket = new Socket(SERVER_ADDRESS, port);
             DataInputStream din = new DataInputStream(dataSocket.getInputStream());
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = din.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }
            fos.flush();
        }

        gui.appendLog("✅ Downloaded: " + filename + " vào thư mục " + currentUser);
    }

    private int extractPort(String response) {
        String[] tokens = response.split(" ");
        return Integer.parseInt(tokens[tokens.length - 1]);
    }

    public void sendCommand(String command) {
        try {
            lastCommand = command;
            out.writeUTF(command);
            out.flush();

            if (command.equalsIgnoreCase("QUIT") || command.equalsIgnoreCase("Q")) {
                controlSocket.close();
                System.exit(0);
            }
        } catch (IOException e) {
            gui.appendLog("Lỗi gửi lệnh: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}

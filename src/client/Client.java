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

    public Client() {
        gui = new ClientGUI(this);
        connectToServer();
    }

    private void connectToServer() {
        try {
            controlSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new DataInputStream(controlSocket.getInputStream());
            out = new DataOutputStream(controlSocket.getOutputStream());

            String welcome = in.readUTF();
            gui.appendLog("Server: " + welcome);

        } catch (IOException e) {
            gui.appendLog("Lỗi kết nối server: " + e.getMessage());
        }
    }

    public void sendCommand(String command) {
        try {
            out.writeUTF(command);
            out.flush();

            String response = in.readUTF();
            gui.appendLog("Server: " + response);

            if (command.startsWith("LIST")) {
                handleList(response);
            } else if (command.startsWith("STOR")) {
                String[] parts = command.split(" ", 2);
                if (parts.length > 1) handleUpload(response, parts[1]);
            } else if (command.startsWith("RETR")) {
                String[] parts = command.split(" ", 2);
                if (parts.length > 1) handleDownload(response, parts[1]);
            } else if (command.startsWith("QUIT")) {
                controlSocket.close();
                System.exit(0);
            }

        } catch (IOException e) {
            gui.appendLog("Lỗi gửi lệnh: " + e.getMessage());
        }
    }

    private void handleList(String response) {
        try {
            if (!response.startsWith("150")) return;

            int port = extractPort(response);
            Socket dataSocket = new Socket(SERVER_ADDRESS, port);
            DataInputStream din = new DataInputStream(dataSocket.getInputStream());

            String list = din.readUTF();
            gui.appendLog("Danh sách file: " + list);

            dataSocket.close();

            String finalResp = in.readUTF();
            gui.appendLog("Server: " + finalResp);

        } catch (IOException e) {
            gui.appendLog("Lỗi LIST: " + e.getMessage());
        }
    }

    private void handleUpload(String response, String filename) {
        try {
            if (!response.startsWith("150")) return;

            int port = extractPort(response);
            Socket dataSocket = new Socket(SERVER_ADDRESS, port);
            DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());

            File file = new File("client_files/" + filename);
            if (!file.exists()) {
                gui.appendLog("File không tồn tại: " + filename);
                dataSocket.close();
                return;
            }

            FileManager.sendFile(dout, file.getPath());
            dataSocket.close();

            String finalResp = in.readUTF();
            gui.appendLog("Server: " + finalResp);

        } catch (IOException e) {
            gui.appendLog("Lỗi UPLOAD: " + e.getMessage());
        }
    }

    private void handleDownload(String response, String filename) {
        try {
            if (!response.startsWith("150")) return;

            int port = extractPort(response);
            Socket dataSocket = new Socket(SERVER_ADDRESS, port);
            DataInputStream din = new DataInputStream(dataSocket.getInputStream());

            FileManager.receiveFile(din, "client_files/" + filename);
            dataSocket.close();

            String finalResp = in.readUTF();
            gui.appendLog("Server: " + finalResp);

        } catch (IOException e) {
            gui.appendLog("Lỗi DOWNLOAD: " + e.getMessage());
        }
    }

    private int extractPort(String response) {
        String[] parts = response.split(" ");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    public static void main(String[] args) {
        new Client();
    }
}

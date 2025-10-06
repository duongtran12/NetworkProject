package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final ServerGUI gui;

    public ClientHandler(Socket socket, ServerGUI gui) {
        this.socket = socket;
        this.gui = gui;

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF("220 FTP Server Ready");
            out.flush();

        } catch (IOException e) {
            gui.appendLog("Lỗi khởi tạo stream: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            gui.appendLog("Stream chưa sẵn sàng, kết nối bị đóng.");
            try { socket.close(); } catch (IOException ignored) {}
            return;
        }

        try {
            while (true) {
                String command = in.readUTF();
                gui.appendLog("Client gửi: " + command);

                String response = CommandHandler.handleCommand(command, socket, in, out, gui);

                if (response != null && !response.isEmpty()) {
                    synchronized (out) {
                        out.writeUTF(response);
                        out.flush();
                    }
                }

                if (response != null && response.startsWith("221")) {
                    gui.appendLog("Client ngắt kết nối.");
                    try { socket.close(); } catch (IOException ignored) {}
                    break;
                }
            }
        } catch (IOException e) {
            gui.appendLog("Client ngắt kết nối: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void sendMessage(String message) {
        try {
            synchronized (out) {
                out.writeUTF(message);
                out.flush();
            }
        } catch (IOException e) {
            gui.appendLog("Lỗi gửi message đến client: " + e.getMessage());
        }
    }
}

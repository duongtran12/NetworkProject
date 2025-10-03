package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ServerGUI gui;
    private CommandHandler handler; 

    public ClientHandler(Socket socket, ServerGUI gui) {
        this.socket = socket;
        this.gui = gui;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            handler = new CommandHandler(in, out, gui);

            out.writeUTF("220 FTP Server Ready");
            out.flush();
        } catch (IOException e) {
            gui.appendLog("Lỗi khởi tạo stream: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = in.readUTF();
                gui.appendLog("Client gửi: " + command);

                String response = handler.handleCommand(command, socket);

                synchronized (out) {
                    out.writeUTF(response);
                    out.flush();
                }

                if (response.startsWith("221")) {
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
            gui.appendLog("Lỗi gửi message: " + e.getMessage());
        }
    }
}

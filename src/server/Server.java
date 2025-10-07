package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static ServerGUI gui;

    public static void main(String[] args) {
        gui = new ServerGUI();
        gui.appendLog("Server đang khởi động trên port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            gui.appendLog("Server khởi động thành công!");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                gui.appendLog("Client mới kết nối: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, gui);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            gui.appendLog("Lỗi server: " + e.getMessage());
        }
    }
    
    public static List<ClientHandler> getClientHandlers() {
        return clients;
    }

}
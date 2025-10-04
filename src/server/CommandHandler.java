package server;

import java.io.*;
import java.net.*;

public class CommandHandler {
    private static final String ROOT_DIR = "server_files/";
    private static boolean isLoggedIn = false;
    private static String currentDir = ROOT_DIR;

    public static String handleCommand(String command, Socket socket, DataInputStream in, DataOutputStream out, ServerGUI gui) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String arg = (parts.length > 1) ? parts[1].trim() : null;

        try {
            switch (cmd) {
                case "S":
                    if (arg != null) return "Server nhận: " + arg;
                    return "Lỗi: Thiếu message.";

                case "R":
                    return "Server: Hello from server!";

                case "Q":
                    return "Goodbye! Kết nối đóng.";

                case "USER":
                    if (arg == null) return "501 Missing username.";
                    return "331 User name okay, need password.";

                case "PASS":
                    isLoggedIn = true;
                    currentDir = ROOT_DIR;
                    gui.appendLog("Người dùng đã đăng nhập.");
                    return "230 User logged in, proceed.";

                case "QUIT":
                    return "221 Goodbye.";

                case "PWD":
                    if (!isLoggedIn) return "530 Not logged in.";
                    return "257 \"" + currentDir + "\" is current directory.";

                case "CWD":
                    if (!isLoggedIn) return "530 Not logged in.";
                    if (arg == null) return "501 Missing directory.";
                    File newDir = new File(currentDir, arg);
                    if (newDir.exists() && newDir.isDirectory()) {
                        currentDir = newDir.getPath();
                        return "250 Directory changed to " + currentDir;
                    }
                    return "550 Directory not found.";

                case "LIST":
                    if (!isLoggedIn) return "530 Not logged in.";
                    openDataConnection(socket, out, gui, "LIST", null);
                    return "150 Opening data connection.";

                case "STOR":
                    if (!isLoggedIn) return "530 Not logged in.";
                    if (arg == null) return "501 Missing filename.";
                    openDataConnection(socket, out, gui, "STOR", arg);
                    return "150 Opening data connection.";
                    
                case "MKD":
                    if (!isLoggedIn) return "530 Not logged in.";
                    if (arg == null) return "501 Missing directory name.";
                    File newFolder = new File(currentDir + "/" + arg);
                    if (newFolder.exists()) {
                        return "550 Directory already exists.";
                    }
                    if (newFolder.mkdirs()) {
                        return "257 \"" + newFolder.getPath() + "\" directory created.";
                    } else {
                        return "550 Failed to create directory.";
                }
    

                case "RETR":
                    if (!isLoggedIn) return "530 Not logged in.";
                    if (arg == null) return "501 Missing filename.";
                    openDataConnection(socket, out, gui, "RETR", arg);
                    return "150 Opening data connection.";

                default:
                    return "502 Command not implemented.";
            }

        } catch (Exception e) {
            return "500 Error: " + e.getMessage();
        }
    }

    private static void openDataConnection(Socket controlSocket, DataOutputStream out, ServerGUI gui,
                                           String mode, String filename) throws IOException {
        ServerSocket dataServer = new ServerSocket(0);
        int port = dataServer.getLocalPort();

        out.writeUTF("150 Opening data connection on port " + port);
        out.flush();

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        new Thread(() -> {
            try (Socket dataSocket = dataServer.accept()) {

                if ("LIST".equals(mode)) {
                    DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
                    String list = FileManager.listFiles(currentDir);
                    dout.writeUTF(list);
                    dout.flush();
                    gui.appendLog("Đã gửi danh sách file.");

                } else if ("STOR".equals(mode)) {
                    DataInputStream din = new DataInputStream(dataSocket.getInputStream());
                    File dest = new File(currentDir + "/" + filename);
                    FileManager.receiveFile(din, dest.getPath());
                    gui.appendLog("Upload thành công: " + dest.getPath());

                } else if ("RETR".equals(mode)) {
                    DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
                    File src = new File(currentDir + "/" + filename);
                    FileManager.sendFile(dout, src.getPath());
                    gui.appendLog("Download thành công: " + src.getPath());
                }

                out.writeUTF("226 Transfer complete");
                out.flush();

            } catch (IOException e) {
                gui.appendLog("⚠️ Lỗi data connection: " + e.getMessage());
                try {
                    out.writeUTF("425 Can't open data connection.");
                    out.flush();
                } catch (IOException ignored) {}
            } finally {
                try { dataServer.close(); } catch (IOException ignored) {}
            }
        }).start();
    }
}

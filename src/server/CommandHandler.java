package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CommandHandler {
    private String currentDir;
    private boolean loggedIn = false;
    private String renameFrom = null; 
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ServerGUI gui;

    public CommandHandler(DataInputStream in, DataOutputStream out, ServerGUI gui) {
        this.in = in;
        this.out = out;
        this.gui = gui;
        this.currentDir = "server_files/"; 
        File root = new File(currentDir);
        if (!root.exists()) root.mkdirs();
    }

    public String handleCommand(String command, Socket controlSocket) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toUpperCase();

        try {
            switch (cmd) {
                case "S":
                    if (parts.length > 1) return "200 Server nhận: " + parts[1];
                    return "501 Thiếu message";

                case "R":
                    return "200 Server: Hello from server!";

                case "Q":
                    return "221 Goodbye.";

                case "USER":
                    return "331 User name okay, need password.";
                case "PASS":
                    loggedIn = true;
                    return "230 User logged in, proceed.";

                case "PWD":
                    if (!loggedIn) return "530 Not logged in.";
                    return "257 \"" + currentDir + "\" is current directory";

                case "CWD":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File newDir = new File(currentDir, parts[1]);
                    if (newDir.exists() && newDir.isDirectory()) {
                        currentDir = newDir.getCanonicalPath() + File.separator;
                        return "250 Directory changed to " + currentDir;
                    } else {
                        return "550 Directory not found.";
                    }

                case "LIST":
                    if (!loggedIn) return "530 Not logged in.";
                    return openDataConnection(controlSocket, "LIST", null);

                case "STOR":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    return openDataConnection(controlSocket, "STOR", parts[1]);

                case "RETR":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    return openDataConnection(controlSocket, "RETR", parts[1]);

                case "DELE":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File delFile = new File(currentDir, parts[1]);
                    if (delFile.exists() && delFile.isFile()) {
                        if (delFile.delete()) {
                            return "250 File deleted successfully.";
                        } else {
                            return "450 File delete failed.";
                        }
                    } else {
                        return "550 File not found.";
                    }

                case "MKD":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File newFolder = new File(currentDir, parts[1]);
                    if (newFolder.exists()) return "550 Directory already exists.";
                    if (newFolder.mkdir()) {
                        return "257 \"" + newFolder.getPath() + "\" directory created.";
                    } else {
                        return "550 Create directory failed.";
                    }

                case "RMD":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File rmDir = new File(currentDir, parts[1]);
                    if (rmDir.exists() && rmDir.isDirectory()) {
                        if (rmDir.delete()) {
                            return "250 Directory removed.";
                        } else {
                            return "550 Remove directory failed.";
                        }
                    } else {
                        return "550 Directory not found.";
                    }

                case "RNFR":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File fromFile = new File(currentDir, parts[1]);
                    if (fromFile.exists()) {
                        renameFrom = fromFile.getAbsolutePath();
                        return "350 Ready for RNTO.";
                    } else {
                        return "550 File not found.";
                    }

                case "RNTO":
                    if (!loggedIn) return "530 Not logged in.";
                    if (renameFrom == null) return "503 Bad sequence of commands.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    File toFile = new File(currentDir, parts[1]);
                    File fromF = new File(renameFrom);
                    if (fromF.renameTo(toFile)) {
                        renameFrom = null;
                        return "250 File renamed successfully.";
                    } else {
                        renameFrom = null;
                        return "550 Rename failed.";
                    }

                case "TYPE":
                    if (!loggedIn) return "530 Not logged in.";
                    if (parts.length < 2) return "501 Syntax error in parameters.";
                    String type = parts[1].toUpperCase();
                    if (type.equals("I")) return "200 Type set to I (Binary).";
                    if (type.equals("A")) return "200 Type set to A (ASCII).";
                    return "504 Type not supported.";

                case "NOOP":
                    return "200 OK";

                case "QUIT":
                    return "221 Goodbye.";

                default:
                    return "502 Command not implemented.";
            }
        } catch (Exception e) {
            return "500 Error: " + e.getMessage();
        }
    }

    private String openDataConnection(Socket controlSocket, String mode, String filename) throws IOException {
        ServerSocket dataServer = new ServerSocket(0);
        int port = dataServer.getLocalPort();

        new Thread(() -> {
            try (Socket dataSocket = dataServer.accept()) {
                if ("LIST".equals(mode)) {
                    try (DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream())) {
                        String list = FileManager.listFiles(currentDir);
                        dout.writeUTF(list);
                        dout.flush();
                    }
                } else if ("STOR".equals(mode)) {
                    try (DataInputStream din = new DataInputStream(dataSocket.getInputStream())) {
                        FileManager.receiveFile(din, currentDir + filename);
                        gui.appendLog("Upload thành công: " + filename);
                    }
                } else if ("RETR".equals(mode)) {
                    try (DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream())) {
                        FileManager.sendFile(dout, currentDir + filename);
                        gui.appendLog("Download thành công: " + filename);
                    }
                }

                synchronized (out) {
                    out.writeUTF("226 Transfer complete");
                    out.flush();
                }
            } catch (IOException e) {
                gui.appendLog("Lỗi data connection: " + e.getMessage());
                try {
                    synchronized (out) {
                        out.writeUTF("425 Can't open data connection.");
                        out.flush();
                    }
                } catch (IOException ignored) {}
            } finally {
                try { dataServer.close(); } catch (IOException ignored) {}
            }
        }).start();

        return "150 Opening data connection on port " + port;
    }
}

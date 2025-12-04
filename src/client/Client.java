package client;

import java.io.*;
import java.net.Socket;
import java.util.Locale;

public class Client {
    private String serverAddress = "localhost";
    private int serverPort = 12345;
    private String username = "anonymous";
    private String password = "";

    private Socket controlSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private ClientGUI gui;

    private String lastCommand = "";

    // Thư mục hiện tại phía client (trong client_files/<username>)
    private String clientCurrentDir = "client_files/" + username;

    public Client() {
        gui = new ClientGUI(this);
    }

    public void setServerInfo(String ip, int port, String user, String pass) {
        this.serverAddress = ip;
        this.serverPort = port;
        this.username = user;
        this.password = pass;

        // Cập nhật lại thư mục client hiện tại theo user mới
        this.clientCurrentDir = "client_files/" + this.username;
    }

    public void connectToServer() {
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }

            controlSocket = new Socket(serverAddress, serverPort);
            in = new DataInputStream(controlSocket.getInputStream());
            out = new DataOutputStream(controlSocket.getOutputStream());
            gui.appendLog("Connected to server: " + serverAddress + ":" + serverPort);

            sendCommand("USER " + username);
            sendCommand("PASS " + password);

            new Thread(() -> {
                try {
                    while (true) {
                        String response = in.readUTF();
                        handleServerResponse(response);
                    }
                } catch (IOException e) {
                    gui.appendLog("Disconnected: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            gui.appendLog("Lỗi kết nối: " + e.getMessage());
        }

    }

    private void handleServerResponse(String response) {
        gui.appendLog("Server: " + response);

        try {
            if (response.startsWith("331")) {
                String userDir = "client_files/" + username;
                new File(userDir).mkdirs();
                clientCurrentDir = userDir;
                gui.updateClientFiles(clientCurrentDir);
            }

            if (response.startsWith("230")) {
                String userDir = "client_files/" + username;
                new File(userDir).mkdirs();
                clientCurrentDir = userDir;
                gui.updateClientFiles(clientCurrentDir);
            }

            if (response.startsWith("150 Opening data connection on port")) {
                int port = extractPort(response);
                if (lastCommand.startsWith("LIST")) {
                    handleList(port);
                } else if (lastCommand.startsWith("STOR") || lastCommand.startsWith("APPE")) {
                    handleStor(port, lastCommand);
                } else if (lastCommand.startsWith("RETR")) {
                    handleRetr(port, lastCommand);
                }
            }

//            if (response.startsWith("226")) {
//                gui.updateClientFiles(clientCurrentDir);
//            }
            autoRefreshServerFiles(response);

        } catch (Exception e) {
            gui.appendLog("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    private void handleList(int port) throws IOException {
        try (Socket dataSocket = new Socket(serverAddress, port);
             DataInputStream din = new DataInputStream(dataSocket.getInputStream())) {
            String list = din.readUTF();
            gui.updateServerFiles(list);
        }
        gui.appendLog("Transfer complete.");
    }

    private void handleStor(int port, String command) throws IOException {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            gui.appendLog("Thiếu tên file để upload!");
            return;
        }
        String filename = parts[1].trim();
        File file = new File(clientCurrentDir + "/" + filename);
        if (!file.exists()) {
            gui.appendLog("File không tồn tại: " + file.getPath());
            return;
        }

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        try (Socket dataSocket = new Socket(serverAddress, port);
             DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = fis.read(buffer)) != -1) {
                dout.write(buffer, 0, bytes);
            }
            dout.flush();
        }

        gui.appendLog("Uploaded: " + filename);
        gui.updateClientFiles(clientCurrentDir);
    }

    private void handleRetr(int port, String command) throws IOException {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            gui.appendLog("Thiếu tên file để tải về!");
            return;
        }
        String filename = parts[1].trim();
        File dest = new File(clientCurrentDir + "/" + filename);
        dest.getParentFile().mkdirs();

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        try (Socket dataSocket = new Socket(serverAddress, port);
             DataInputStream din = new DataInputStream(dataSocket.getInputStream());
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = din.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }
            fos.flush();
        }

        gui.appendLog("Downloaded: " + filename + " vào thư mục client " + clientCurrentDir);
        gui.updateClientFiles(clientCurrentDir);
    }

    private int extractPort(String response) {
        String[] tokens = response.split(" ");
        return Integer.parseInt(tokens[tokens.length - 1]);
    }
    
    /**
     * Tự động gửi lệnh LIST để cập nhật ô "Server Files"
     * sau các lệnh có thể làm thay đổi file/thư mục trên server.
     */
    private void autoRefreshServerFiles(String response) {
        String upperLast = lastCommand.toUpperCase(Locale.ROOT);

        // 1) Sau các lệnh có data (LIST, STOR, APPE, RETR...) -> 226
        if (response.startsWith("226")) {
            // Nếu lệnh trước KHÔNG PHẢI LIST thì mới auto LIST,
            // để tránh vòng lặp vô hạn.
            if (!upperLast.startsWith("LIST")) {
                sendCommand("LIST");
            }
            return;
        }

        // 2) Các lệnh thay đổi cấu trúc file/folder: MKD, DELE, RNTO, CREA, WRITE, RMD
        boolean fsChangingCmd =
                upperLast.startsWith("MKD")   ||  // tạo thư mục
                upperLast.startsWith("DELE")  ||  // xóa file
                upperLast.startsWith("RNTO")  ||  // đổi tên xong
                upperLast.startsWith("CREA")  ||  // tạo file rỗng
                upperLast.startsWith("WRITE") ||  // ghi file
                upperLast.startsWith("RMD");     // 🔥 xóa thư mục

        if (fsChangingCmd && (response.startsWith("250") || response.startsWith("257"))) {
            sendCommand("LIST");
            return;
        }

        // 3) Riêng CWD: đổi thư mục thành công (250) -> LIST luôn thư mục mới
        if (upperLast.startsWith("CWD") && response.startsWith("250")) {
            sendCommand("LIST");
        }
        
        // 4) PWD cũng có thể LIST để đồng bộ view (tùy bạn)
        // server thường trả 257 "<path>" is current directory.
        if (upperLast.startsWith("PWD") && response.startsWith("257")) {
            sendCommand("LIST");
        }
    }

    /** Xây chuỗi đường dẫn hiển thị dạng client_files/cam/... */
    private String buildClientDisplayPath(File rootDir, File currentDirFile) throws IOException {
        File canonicalRoot = rootDir.getCanonicalFile();      // client_files/<user>
        File canonicalCurrent = currentDirFile.getCanonicalFile();

        String rootPath = canonicalRoot.getPath();
        String curPath  = canonicalCurrent.getPath();

        if (curPath.startsWith(rootPath)) {
            // phần phía sau client_files/<user>
            String rel = curPath.substring(rootPath.length());
            if (rel.startsWith(File.separator)) {
                rel = rel.substring(1);
            }
            if (rel.isEmpty()) {
                return ("client_files/" + username).replace("\\", "/");
            } else {
                return ("client_files/" + username + "/" + rel).replace("\\", "/");
            }
        } else {
            // fallback: in full nếu vì lý do gì đó không nằm trong root
            return curPath.replace("\\", "/");
        }
    }

    /** Xóa đệ quy file/thư mục phía client */
    private boolean deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }
        return f.delete();
    }

    /**
     * Xử lý các lệnh LOCAL phía client (không gửi lên server).
     * Trả về true nếu ĐÃ xử lý local.
     */
    private boolean handleLocalCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.isEmpty()) return false;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        // root phía client: client_files/<username>
        String rootDirPath = "client_files/" + username;
        File rootDir = new File(rootDirPath);
        if (!rootDir.exists()) rootDir.mkdirs();

        // nếu clientCurrentDir chưa set thì cho bằng root
        if (clientCurrentDir == null) {
            clientCurrentDir = rootDirPath;
        }

        File currentDirFile = new File(clientCurrentDir);

        try {
            // ===== CLT_PWD: in thư mục hiện tại phía client =====
            if (upper.equals("CLT_PWD")) {
                String displayPath = buildClientDisplayPath(rootDir, currentDirFile);
                gui.appendLog("Client PWD: " + displayPath);
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

            // ===== CLT_CWD <path>: đổi thư mục phía client =====
            if (upper.startsWith("CLT_CWD")) {
                String arg = "";
                if (trimmed.length() > "CLT_CWD".length()) {
                    arg = trimmed.substring("CLT_CWD".length()).trim();
                }

                if (arg.isEmpty()) {
                    gui.appendLog("Thiếu tên thư mục (client). Cú pháp: CLT_CWD <folder> hoặc CLT_CWD ..");
                    return true;
                }

                File target;
                if ("..".equals(arg)) {
                    target = currentDirFile.getParentFile();
                    if (target == null) target = currentDirFile;
                } else {
                    target = new File(currentDirFile, arg);
                }

                File canonicalRoot = rootDir.getCanonicalFile();
                File canonicalTarget = target.getCanonicalFile();

                // Không cho phép đi ra ngoài root client_files/<username>
                if (!canonicalTarget.getPath().startsWith(canonicalRoot.getPath())) {
                    gui.appendLog("Không thể CWD ra ngoài thư mục gốc client: " + canonicalRoot.getPath());
                    return true;
                }

                if (!canonicalTarget.exists() || !canonicalTarget.isDirectory()) {
                    gui.appendLog("Thư mục client không tồn tại: " + canonicalTarget.getPath());
                    return true;
                }

                clientCurrentDir = canonicalTarget.getPath();
                String displayPath = buildClientDisplayPath(rootDir, new File(clientCurrentDir));
                gui.appendLog("Client CWD tới: " + displayPath);
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

            // ===== CLT_MKD <folder>: tạo thư mục phía client =====
            if (upper.startsWith("CLT_MKD ")) {
                String folderName = trimmed.substring("CLT_MKD".length()).trim();
                if (folderName.isEmpty()) {
                    gui.appendLog("Thiếu tên thư mục (client). Cú pháp: CLT_MKD <folder>");
                    return true;
                }
                File dir = new File(currentDirFile, folderName);
                if (dir.exists()) {
                    gui.appendLog("Thư mục client đã tồn tại: " + dir.getPath());
                } else if (dir.mkdirs()) {
                    gui.appendLog("Đã tạo thư mục client: " + dir.getPath());
                } else {
                    gui.appendLog("Tạo thư mục client thất bại: " + dir.getPath());
                }
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

            // ===== CLT_CREAFILE <file>: tạo file rỗng phía client =====
            if (upper.startsWith("CLT_CREAFILE ")) {
                String fileName = trimmed.substring("CLT_CREAFILE".length()).trim();
                if (fileName.isEmpty()) {
                    gui.appendLog("Thiếu tên file (client). Cú pháp: CLT_CREAFILE <file>");
                    return true;
                }
                File f = new File(currentDirFile, fileName);
                if (f.exists()) {
                    gui.appendLog("File client đã tồn tại: " + f.getPath());
                } else if (f.createNewFile()) {
                    gui.appendLog("Đã tạo file client: " + f.getPath());
                } else {
                    gui.appendLog("Tạo file client thất bại: " + f.getPath());
                }
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

            // ===== CLT_WRITE <file> <nội dung>: ghi nội dung vào file client =====
            if (upper.startsWith("CLT_WRITE ")) {
                String[] parts = trimmed.split(" ", 3);
                if (parts.length < 3) {
                    gui.appendLog("Cú pháp: CLT_WRITE <file> <nội dung>");
                    return true;
                }
                String fileName = parts[1];
                String content = parts[2];

                File f = new File(currentDirFile, fileName);
                // nếu chưa có file thì tạo mới
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    if (!f.createNewFile()) {
                        gui.appendLog("Không thể tạo file client: " + f.getPath());
                        return true;
                    }
                }

                try (FileWriter fw = new FileWriter(f, false);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(content);
                }

                gui.appendLog("Đã ghi nội dung vào file client: " + f.getPath());
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }
            
         // ===== CLT_READ <file>: xem nội dung file phía client =====
            if (upper.startsWith("CLT_READ ")) {
                String fileName = trimmed.substring("CLT_READ".length()).trim();
                if (fileName.isEmpty()) {
                    gui.appendLog("Cú pháp: CLT_READ <file>");
                    return true;
                }

                File f = new File(currentDirFile, fileName);
                if (!f.exists() || !f.isFile()) {
                    gui.appendLog("File client không tồn tại hoặc không phải file: " + f.getPath());
                    return true;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                if (sb.length() == 0) {
                    gui.appendLog("Nội dung file client " + fileName + " (rỗng).");
                } else {
                    gui.appendLog("Nội dung file client " + fileName + ":\n" + sb.toString());
                }
                return true;
            }

            // ===== CLT_DELE <file>: xóa file phía client =====
            if (upper.startsWith("CLT_DELE ")) {
                String fileName = trimmed.substring("CLT_DELE".length()).trim();
                if (fileName.isEmpty()) {
                    gui.appendLog("Thiếu tên file (client). Cú pháp: CLT_DELE <file>");
                    return true;
                }
                File f = new File(currentDirFile, fileName);
                if (!f.exists() || !f.isFile()) {
                    gui.appendLog("File client không tồn tại hoặc không phải file: " + f.getPath());
                } else if (f.delete()) {
                    gui.appendLog("Đã xóa file client: " + f.getPath());
                } else {
                    gui.appendLog("Xóa file client thất bại: " + f.getPath());
                }
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

            // ===== CLT_RMD <folder>: xóa thư mục phía client (đệ quy) =====
            if (upper.startsWith("CLT_RMD ")) {
                String folderName = trimmed.substring("CLT_RMD".length()).trim();
                if (folderName.isEmpty()) {
                    gui.appendLog("Thiếu tên thư mục (client). Cú pháp: CLT_RMD <folder>");
                    return true;
                }
                File dir = new File(currentDirFile, folderName);
                if (!dir.exists() || !dir.isDirectory()) {
                    gui.appendLog("Thư mục client không tồn tại hoặc không phải thư mục: " + dir.getPath());
                } else if (deleteRecursively(dir)) {
                    gui.appendLog("Đã xóa thư mục client (kèm nội dung): " + dir.getPath());
                } else {
                    gui.appendLog("Xóa thư mục client thất bại: " + dir.getPath());
                }
                gui.updateClientFiles(clientCurrentDir);
                return true;
            }

        } catch (IOException e) {
            gui.appendLog("Lỗi thao tác file client: " + e.getMessage());
            return true; // đã xử lý local, không gửi lên server nữa
        }

        // Không phải lệnh client-side
        return false;
    }

    public void sendCommand(String command) {
        try {
            // Nếu là lệnh local phía client thì xử lý tại đây và KHÔNG gửi lên server
            if (handleLocalCommand(command)) {
                return;
            }

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

    public String getUsername() {
        return username;
    }
}

package server;

import java.io.*;
import java.net.*;

public class CommandHandler {
    private static final String ROOT_DIR = "server_files/";
    private static String currentDir = ROOT_DIR;

    private static ThreadLocal<Boolean> isLoggedIn = ThreadLocal.withInitial(() -> false);
    private static ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static ThreadLocal<File> pendingRename = new ThreadLocal<>();
    private static ThreadLocal<Long> restartOffset = ThreadLocal.withInitial(() -> 0L);
    private static ThreadLocal<Thread> currentDataThread = new ThreadLocal<>();

    public static String handleCommand(String command, Socket socket,
                                       DataInputStream in, DataOutputStream out,
                                       ServerGUI gui) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String arg = (parts.length > 1) ? parts[1].trim() : null;

        try {
            switch (cmd) {
                case "QUIT":
                    isLoggedIn.set(false);
                    currentUser.remove();
                    return "221 Tạm biệt.";

                case "USER":
                    if (arg == null) return "501 Thiếu tên người dùng.";
                    currentUser.set(arg);
                    return "331 Tên người dùng hợp lệ, yêu cầu mật khẩu.";

                case "PASS":
                    if (currentUser.get() == null) return "503 Thứ tự lệnh không hợp lệ.";
                    if (arg == null) return "501 Thiếu mật khẩu.";
                    boolean valid = UserDAO.checkLogin(currentUser.get(), arg);
                    if (valid) {
                        isLoggedIn.set(true);
                        currentDir = ROOT_DIR + currentUser.get() + "/";
                        File userDir = new File(currentDir);
                        if (!userDir.exists()) {
                            userDir.mkdirs();
                        }
                        gui.appendLog("Người dùng '" + currentUser.get()
                                      + "' đã đăng nhập. Thư mục: " + currentDir);
                        return "230 Đăng nhập thành công.";
                    } else {
                        gui.appendLog("Đăng nhập thất bại cho user: " + currentUser.get());
                        currentUser.remove();
                        return "530 Sai tên đăng nhập hoặc mật khẩu.";
                    }

                case "PWD":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    return "257 \"" + currentDir + "\" là thư mục hiện tại.";

                case "CWD":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên thư mục.";

                    File newDirLogic = new File(currentDir, arg);
                    String canonicalPath = newDirLogic.getCanonicalPath();

                    if (!isPathAllowed(canonicalPath)) {
                        return "550 Từ chối truy cập: Không thể truy cập ra ngoài thư mục gốc của server.";
                    }

                    if (newDirLogic.exists() && newDirLogic.isDirectory()) {
                        String logicalPath = newDirLogic.getPath().replace("\\", "/");
                        logicalPath = logicalPath.replaceAll("/[^/]+/\\.\\.$", "/");
                        logicalPath = logicalPath.replaceAll("/\\.$", "");
                        if (!logicalPath.endsWith("/")) logicalPath += "/";
                        currentDir = logicalPath;
                        return "250 Đã chuyển sang thư mục " + currentDir;
                    }

                    return "550 Không tìm thấy thư mục.";

                case "LIST":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    openDataConnection(socket, out, gui, "LIST", null);
                    return "150 Đang chuẩn bị mở kết nối dữ liệu.";

                case "STOR":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";
                    openDataConnection(socket, out, gui, "STOR", arg);
                    return "150 Đang chuẩn bị mở kết nối dữ liệu.";

                case "APPE":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";
                    openDataConnection(socket, out, gui, "APPE", arg);
                    return "150 Đang chuẩn bị mở kết nối dữ liệu.";

                case "RETR":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";
                    openDataConnection(socket, out, gui, "RETR", arg);
                    return "150 Đang chuẩn bị mở kết nối dữ liệu.";

                case "MKD":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên thư mục.";
                    File newFolder = new File(currentDir + "/" + arg);
                    if (newFolder.exists()) return "550 Thư mục đã tồn tại.";
                    if (newFolder.mkdirs())
                        return "257 \"" + newFolder.getPath() + "\" đã được tạo.";
                    else
                        return "550 Tạo thư mục thất bại.";

                case "DELE":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";
                    File fileToDelete = new File(currentDir + "/" + arg);
                    if (fileToDelete.exists() && fileToDelete.isFile()) {
                        if (fileToDelete.delete()) {
                            gui.appendLog("Đã xóa file: " + fileToDelete.getName());
                            return "250 Xóa tập tin thành công.";
                        } else return "450 Xóa tập tin thất bại.";
                    } else return "550 Không tìm thấy tập tin.";

                case "SYST":
                    return "215 Máy chủ FTP chạy trên " + System.getProperty("os.name");

                case "TYPE":
                    if (arg == null) return "501 Thiếu tham số TYPE.";
                    return "200 Đã đặt kiểu truyền dữ liệu là " + arg.toUpperCase();

                case "MODE":
                    if (arg == null) return "501 Thiếu tham số MODE.";
                    return "200 Đã đặt chế độ truyền là " + arg.toUpperCase();

                case "STRU":
                    if (arg == null) return "501 Thiếu tham số STRU.";
                    return "200 Đã đặt cấu trúc truyền là " + arg.toUpperCase();

                case "RNFR":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";
                    File oldFile = new File(currentDir + "/" + arg);
                    if (!oldFile.exists()) return "550 Không tìm thấy tập tin.";
                    pendingRename.set(oldFile);
                    return "350 Tập tin tồn tại, hãy gửi tên mới.";

                case "RNTO":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (pendingRename.get() == null) return "503 Thứ tự lệnh không hợp lệ.";
                    if (arg == null) return "501 Thiếu tên tập tin mới.";
                    File newFile = new File(currentDir + "/" + arg);
                    if (pendingRename.get().renameTo(newFile)) {
                        pendingRename.remove();
                        return "250 Đổi tên tập tin thành công.";
                    } else return "450 Đổi tên tập tin thất bại.";

                case "ABOR":
                    if (currentDataThread.get() != null) {
                        currentDataThread.get().interrupt();
                        return "226 Đã hủy phiên truyền dữ liệu.";
                    } else return "225 Không có phiên truyền dữ liệu nào đang diễn ra.";

                case "NOOP":
                    return "200 OK";

                case "REST":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu vị trí bắt đầu (offset).";
                    try {
                        restartOffset.set(Long.parseLong(arg));
                        return "350 Đã chấp nhận vị trí bắt đầu (" + arg + ").";
                    } catch (NumberFormatException e) {
                        return "501 Giá trị số không hợp lệ.";
                    }
                    
                case "CREA":
                    if (!isLoggedIn.get()) return "530 Not logged in.";
                    if (arg == null) return "501 Missing filename.";

                    File newEmptyFile = new File(currentDir + "/" + arg);

                    if (newEmptyFile.exists()) {
                        if (newEmptyFile.isDirectory()) {
                            return "550 Name already used for a directory.";
                        } else {
                            return "550 File already exists.";
                        }
                    }

                    try {
                        if (newEmptyFile.createNewFile()) {
                            gui.appendLog("Đã tạo tập tin rỗng: " + newEmptyFile.getPath());
                            return "257 \"" + newEmptyFile.getPath() + "\" file created.";
                        } else {
                            return "550 Failed to create file.";
                        }
                    } catch (IOException e) {
                        gui.appendLog("Lỗi tạo file: " + e.getMessage());
                        return "550 Error while creating file.";
                    }

                case "WRITE":
                    if (!isLoggedIn.get()) return "530 Not logged in.";
                    if (arg == null) return "501 Missing filename or content.";

                    // arg đang chứa toàn bộ "<filename> <content...>"
                    String[] writeParts = arg.split(" ", 2);
                    if (writeParts.length < 2) {
                        return "501 Missing content.";
                    }

                    String writeFileName = writeParts[0].trim();
                    String writeContent  = writeParts[1]; // phần còn lại, có thể chứa khoảng trắng

                    if (writeFileName.isEmpty()) {
                        return "501 Missing filename.";
                    }

                    File writeFile = new File(currentDir + "/" + writeFileName);
                    File parentDir = writeFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    try (FileWriter fw = new FileWriter(writeFile, false);
                         BufferedWriter bw = new BufferedWriter(fw)) {

                        bw.write(writeContent);

                        gui.appendLog("Đã tạo/ghi nội dung file trên server: " + writeFile.getPath());
                        return "257 \"" + writeFile.getPath() + "\" file created with content.";

                    } catch (IOException e) {
                        gui.appendLog("Lỗi ghi nội dung file: " + e.getMessage());
                        return "550 Error while writing file.";
                    }
                    
                case "READ":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên tập tin.";

                    File readFile = new File(currentDir + "/" + arg);
                    if (!readFile.exists() || !readFile.isFile()) {
                        return "550 Tập tin không tồn tại hoặc không phải file.";
                    }

                    try (BufferedReader br = new BufferedReader(new FileReader(readFile))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }

                        if (sb.length() == 0) {
                            return "212 Tập tin rỗng.";
                        }

                        // trả về nhiều dòng, log bên client sẽ hiện đầy đủ
                        return "212 Nội dung tập tin " + readFile.getName() + ":\n" + sb.toString().trim();

                    } catch (IOException e) {
                        gui.appendLog("Lỗi đọc file: " + e.getMessage());
                        return "550 Lỗi khi đọc tập tin.";
                    }
                   
                case "RMD":
                    if (!isLoggedIn.get()) return "530 Chưa đăng nhập.";
                    if (arg == null) return "501 Thiếu tên thư mục.";

                    // Xác định thư mục cần xóa dựa trên currentDir + arg
                    File dirToDelete = new File(currentDir, arg);

                    try {
                        String dirCanonicalPath = dirToDelete.getCanonicalPath();

                        // Không cho phép xóa ngoài ROOT_DIR
                        if (!isPathAllowed(dirCanonicalPath)) {
                            return "550 Không được phép xóa ngoài thư mục gốc server.";
                        }

                        if (!dirToDelete.exists() || !dirToDelete.isDirectory()) {
                            return "550 Thư mục không tồn tại hoặc không phải thư mục.";
                        }

                        if (deleteDirectoryRecursively(dirToDelete)) {
                            gui.appendLog("Đã xóa thư mục (kèm nội dung) trên server: " + dirToDelete.getPath());
                            return "250 Thư mục đã được xóa thành công.";
                        } else {
                            return "450 Xóa thư mục thất bại.";
                        }

                    } catch (IOException e) {
                        gui.appendLog("Lỗi khi xóa thư mục: " + e.getMessage());
                        return "550 Lỗi khi xóa thư mục.";
                    }


                case "HELP":
                    return
                        "214- Danh sách lệnh FTP được hỗ trợ\n" +
                        "-----------------------------------------\n" +
                        " LỆNH XÁC THỰC:\n" +
                        "   USER   - Gửi tên người dùng\n" +
                        "   PASS   - Gửi mật khẩu\n" +
                        "   QUIT   - Thoát khỏi máy chủ\n" +
                        "\n" +
                        " LỆNH THƯ MỤC:\n" +
                        "   LIST   - Liệt kê file/thư mục\n" +
                        "   PWD    - Hiển thị thư mục hiện tại\n" +
                        "   CWD    - Đổi thư mục làm việc\n" +
                        "   MKD    - Tạo thư mục mới\n" +
                        "   DELE   - Xóa một tập tin\n" +
                        "	RMD    - Xóa một thư mục\n" +
                        "\n" +
                        " LỆNH TRUYỀN TẬP TIN:\n" +
                        "   STOR   - Upload tập tin lên server\n" +
                        "   RETR   - Download tập tin từ server\n" +
                        "   APPE   - Ghi nối thêm vào tập tin\n" +
                        "\n" +
                        " LỆNH TẬP TIN KHÁC:\n" +
                        "   CREA   - Tạo tập tin rỗng trên server\n" +
                        "   WRITE  - Tạo/ghi nội dung tập tin trên server\n" +
                        "   READ   - Xem nội dung tập tin văn bản trên server\n" +
                        "\n" +
                        " LỆNH ĐỔI TÊN TẬP TIN:\n" +
                        "   RNFR   - Chọn tập tin cần đổi tên\n" +
                        "   RNTO   - Đặt tên mới cho tập tin\n" +
                        "\n" +
                        " LỆNH HỆ THỐNG / TRẠNG THÁI:\n" +
                        "   SYST   - Thông tin hệ thống server\n" +
                        "   STAT   - Trạng thái máy chủ\n" +
                        "   HELP   - Hiển thị danh sách trợ giúp\n" +
                        "-----------------------------------------\n" +
                        "214 Kết thúc HELP";

                case "STAT":
                    return "211 Server đang chạy. Thư mục hiện tại: " + currentDir;

                default:
                    return "502 Lệnh chưa được hỗ trợ.";
            }

        } catch (Exception e) {
            return "500 Lỗi hệ thống: " + e.getMessage();
        }
    }

    private static boolean isPathAllowed(String path) {
        try {
            File serverRoot = new File(ROOT_DIR).getCanonicalFile();
            File target = new File(path).getCanonicalFile();
            return target.getPath().startsWith(serverRoot.getPath());
        } catch (IOException e) {
            return false;
        }
    }

    private static void openDataConnection(Socket controlSocket, DataOutputStream out,
                                           ServerGUI gui, String mode, String filename)
            throws IOException {
        ServerSocket dataServer = new ServerSocket(0);
        int port = dataServer.getLocalPort();

        // Giữ nguyên prefix tiếng Anh để client nhận port, thêm tiếng Việt phía sau
        out.writeUTF("150 Opening data connection on port " + port);
        out.flush();

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        Thread t = new Thread(() -> {
            currentDataThread.set(Thread.currentThread());
            try (Socket dataSocket = dataServer.accept()) {
                if ("LIST".equals(mode)) {
                    DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
                    String list = FileManager.listFiles(currentDir);
                    dout.writeUTF(list);
                    dout.flush();
                    gui.appendLog("Đã gửi danh sách file cho client.");

                } else if ("STOR".equals(mode) || "APPE".equals(mode)) {
                    boolean append = mode.equals("APPE");
                    DataInputStream din = new DataInputStream(dataSocket.getInputStream());
                    File dest = new File(currentDir + "/" + filename);
                    FileManager.receiveFile(din, dest.getPath(), append);
                    gui.appendLog((append ? "Append" : "Upload") + " thành công: " + dest.getPath());

                } else if ("RETR".equals(mode)) {
                    File src = new File(currentDir + "/" + filename);
                    if (!src.exists()) return;
                    try (DataOutputStream dout = new DataOutputStream(dataSocket.getOutputStream());
                         RandomAccessFile raf = new RandomAccessFile(src, "r")) {
                        raf.seek(restartOffset.get());
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = raf.read(buffer)) != -1) {
                            dout.write(buffer, 0, bytesRead);
                        }
                        dout.flush();
                        gui.appendLog("Downloaded: " + src.getPath());
                    }
                    restartOffset.set(0L);
                }

                out.writeUTF("226 Truyền dữ liệu hoàn tất.");
                out.flush();

            } catch (IOException e) {
                gui.appendLog("Lỗi data connection: " + e.getMessage());
                try {
                    out.writeUTF("425 Không thể mở kết nối dữ liệu.");
                    out.flush();
                } catch (IOException ignored) {}
            } finally {
                try { dataServer.close(); } catch (IOException ignored) {}
                currentDataThread.remove();
            }
        });
        t.start();
    }

 // Xóa thư mục đệ quy trên server (thư mục + toàn bộ nội dung)
    private static boolean deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) return true;

        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteDirectoryRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
    
    // Cho ClientHandler dùng để log tên người dùng
    public static String getCurrentUser() {
        return currentUser.get();
    }
}

package server;

import java.io.*;

public class FileManager {
    public static String listFiles(String dir) {
        File folder = new File(dir);
        if (!folder.exists()) folder.mkdirs();
        StringBuilder sb = new StringBuilder();
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return "Không có file";
        boolean first = true;
        for (File f : files) {
            if (f.isFile()) {
                if (!first) sb.append(", ");
                sb.append(f.getName());
                first = false;
            }
        }
        return sb.toString();
    }

    public static void sendFile(DataOutputStream out, String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) throw new IOException("File không tồn tại: " + path);

        long size = file.length();
        out.writeLong(size); 
        out.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    public static void receiveFile(DataInputStream in, String path) throws IOException {
        long size = in.readLong(); 
        try (FileOutputStream fos = new FileOutputStream(path)) {
            byte[] buffer = new byte[4096];
            long remaining = size;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) throw new EOFException("EOF bất ngờ khi nhận file");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();
        }
    }
}

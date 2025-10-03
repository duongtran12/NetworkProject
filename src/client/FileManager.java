package client;

import java.io.*;

public class FileManager {
    public static void sendFile(DataOutputStream out, String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("Không tìm thấy file: " + path);
        }

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
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) throw new EOFException("EOF bất ngờ khi nhận file");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();
        }
    }
}

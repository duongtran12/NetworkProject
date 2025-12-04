package server;

import java.io.*;
import java.util.*;

public class FileManager {

//    public static String listFiles(String directoryPath) {
//        File dir = new File(directoryPath);
//        if (!dir.exists()) return "Thư mục không tồn tại.";
//        File[] files = dir.listFiles();
//        if (files == null || files.length == 0) return "Thư mục trống.";
//        StringBuilder sb = new StringBuilder();
//        for (File f : files) {
//            sb.append(f.getName());
//            if (f.isDirectory()) sb.append("/"); 
//            sb.append("\n");
//        }
//        return sb.toString();
//    }
	
	public static String listFiles(String directoryPath) {
	    File dir = new File(directoryPath);
	    if (!dir.exists()) return "Thư mục không tồn tại.";
	    File[] files = dir.listFiles();
	    if (files == null || files.length == 0) return "Thư mục trống.";

	    StringBuilder sb = new StringBuilder();

	    // root: chỉ hiện tên thư mục hiện tại (vd: cam/)
	    sb.append(dir.getName()).append("/\n");

	    Arrays.sort(files, Comparator
	            .comparing(File::isFile)
	            .thenComparing(File::getName, String::compareToIgnoreCase));

	    for (int i = 0; i < files.length; i++) {
	        boolean isLast = (i == files.length - 1);
	        buildServerTree(files[i], "", isLast, sb);
	    }

	    return sb.toString();
	}

	private static void buildServerTree(File f, String prefix, boolean isLast, StringBuilder sb) {
	    sb.append(prefix);
	    sb.append(isLast ? "└── " : "├── ");
	    sb.append(f.getName());
	    if (f.isDirectory()) sb.append("/");
	    sb.append("\n");

	    if (f.isDirectory()) {
	        File[] children = f.listFiles();
	        if (children == null || children.length == 0) return;

	        Arrays.sort(children, Comparator
	                .comparing(File::isFile)
	                .thenComparing(File::getName, String::compareToIgnoreCase));

	        String newPrefix = prefix + (isLast ? "    " : "│   ");
	        for (int i = 0; i < children.length; i++) {
	            boolean childLast = (i == children.length - 1);
	            buildServerTree(children[i], newPrefix, childLast, sb);
	        }
	    }
	}

    public static void sendFile(DataOutputStream out, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return;
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        fis.close();
        out.flush();
    }

    public static void receiveFile(DataInputStream in, String destPath, boolean append) throws IOException {
        File file = new File(destPath);
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        FileOutputStream fos = new FileOutputStream(file, append);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();
    }

}
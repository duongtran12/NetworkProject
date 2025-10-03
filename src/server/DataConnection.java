package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class DataConnection {
    private ServerSocket dataServer;
    private int port;

    public DataConnection() throws IOException {
        dataServer = new ServerSocket(0); 
        port = dataServer.getLocalPort();
    }

    public int getPort() {
        return port;
    }

    public Socket accept() throws IOException {
        return dataServer.accept();
    }

    public void close() throws IOException {
        dataServer.close();
    }
}

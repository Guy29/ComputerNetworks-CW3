package tcptftp;

import java.io.*;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.Arrays;

import static tcptftp.Peer.RequestType.*;

public class Peer extends Thread {
    public enum RequestType {RRQ, WRQ}

    Socket socket;
    RequestType requestType;
    String filename;
    InputStream in;
    OutputStream out;

    public Peer(Socket socket, RequestType requestType, String filename) {
        this.socket = socket;
        this.requestType = requestType;
        this.filename = filename;
        try {
            in = (requestType == RRQ) ? new FileInputStream(filename) : socket.getInputStream();
            out = (requestType == RRQ) ? socket.getOutputStream() : new FileOutputStream(filename);
        } catch (IOException e) {
            System.out.println("Server encountered I/O error, terminated.");
            System.out.println(e);
            System.out.println(e.getCause());
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        if(in==null || out==null){return;}
        try {
            byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
            out.close();
            System.out.println("Transfer of file \"" + filename + "\" complete.");
        } catch (IOException e) {
            System.out.println("Peer encountered I/O error, terminated.");
        }
    }
}

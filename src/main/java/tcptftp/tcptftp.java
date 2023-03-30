package tcptftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static tcptftp.Peer.RequestType;
import static tcptftp.Peer.RequestType.*;

public class tcptftp {

    static byte[] requestBuffer = new byte[128];
    static RequestType requestType;

    public static void usage() {
        System.out.println("Usage:\n");
        System.out.println("(Server)         tcptftp -s");
        System.out.println("(Read  Client)   tcptftp -cr [hostname/IP] [remote file name] [local file name]");
        System.out.println("(Write Client)   tcptftp -cw [hostname/IP] [remote file name] [local file name]");
    }

    public static void main(String[] args) {


        if (args.length == 1 && args[0].equals("-s")) {
            System.out.println("Daemon started");
            try (ServerSocket daemon = new ServerSocket(69)) {
                while (true) {
                    Socket tempSocket = daemon.accept();
                    InputStream in = tempSocket.getInputStream();
                    in.read(requestBuffer);
                    requestType = requestBuffer[1] == 1 ? RRQ : requestBuffer[1] == 2 ? WRQ : null;
                    if (requestType == null) {
                        System.out.println("Daemon received unknown request type, ignoring.");
                        continue;
                    }
                    String s = new String(requestBuffer, 2, requestBuffer.length - 2);
                    String requestedFilename = s.substring(0, s.indexOf('\0'));
                    System.out.println("Daemon received "+requestType+" request for file "+requestedFilename+". Starting server.");
                    (new Peer(tempSocket, requestType, requestedFilename)).start();
                }

            } catch (IOException e) {
                System.out.println("Encountered an I/O exception, terminated.");
            }

        }


        else if (args.length == 4) {
            try {
                requestType = args[0].equals("-cr") ? RRQ : args[0].equals("-cw") ? WRQ : null;
                if (requestType == null) {
                    usage();
                    return;
                }
                String address = args[1];
                String requestedFilename = args[2];
                String localFilename = args[3];
                Socket remote = new Socket(address, 69);
                OutputStream out = remote.getOutputStream();
                int requestLength = 2 + requestedFilename.length();
                requestBuffer = new byte[requestLength];
                requestBuffer[1] = (byte)(requestType==RRQ?1:2);
                System.arraycopy(requestedFilename.getBytes(), 0, requestBuffer, 2, requestedFilename.length());
                out.write(requestBuffer);
                (new Peer(remote, requestType==RRQ?WRQ:RRQ, localFilename)).start();

            } catch (IOException e) {
                System.out.println("Write client encountered an I/O error, terminated.");
            }

        } else {
            usage();
        }
    }
}
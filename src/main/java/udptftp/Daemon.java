package udptftp;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import static udptftp.TFTPPacket.Opcode.*;

public class Daemon {

    DatagramSocket socket;
    byte[] recvBuf = new byte[516];
    DatagramPacket packet = new DatagramPacket(recvBuf, 516);
    public void start(){
        System.out.println("Daemon started");
        try {
            socket = new DatagramSocket(69);
        } catch (SocketException e) {
            System.out.println("Could not bind local port 69");
            return;
        }

        while(true){

            try {
                socket.receive(packet);

                System.out.println(packet);
                System.out.println(packet.getAddress());
                System.out.println(packet.getPort());
                System.out.println(packet.getLength());
                System.out.println(Arrays.toString(packet.getData()));

                try {
                    TFTPPacket received = TFTPPacket.parse(packet);
                    System.out.println(received.opcode);
                    System.out.println(received.mode);
                    System.out.println(received.filename);
                    System.out.println(received.errorCode);
                    System.out.println(received.errorMessage);

                    DatagramSocket tempSocket = new DatagramSocket();

                    switch(received.opcode){
                        case RRQ:
                            System.out.println("Daemon received read request for file: " + received.filename);
                            System.out.println("Packet passed to read server: "+packet);
                            System.out.println("Packet details: "+packet.getAddress()+", "+packet.getPort());
                            Sender readServer = new Sender(tempSocket, packet.getAddress(), packet.getPort(), received.filename);
                            readServer.start();
                            break;
                        case WRQ:
                            System.out.println("Daemon received write request for file: " + received.filename);
                            System.out.println("Packet passed to write server: "+packet);
                            System.out.println("Packet details: "+packet.getAddress()+", "+packet.getPort());
                            TFTPPacket ack0 = new TFTPPacket(ACK);
                            ack0.blockNum = 0;
                            tempSocket.send(ack0.toDatagram(packet.getAddress(),packet.getPort()));
                            Recipient writeServer = new Recipient(tempSocket, packet.getAddress(), received.filename);
                            writeServer.senderPort = packet.getPort();
                            writeServer.start();
                            break;
                        default:
                            tempSocket.close();
                    }
                } catch (TFTPPacket.TFTPParseException e){
                    System.out.println("Server could not parse received packet as a TFTP packet.");
                }
            } catch (java.io.IOException e){
                System.out.println("Server could not establish connection because of error with received packet");
                System.out.println(e.toString());
                System.out.println(e.getMessage());
                System.out.println(e.getCause());
                continue;
            }

        }

        //socket.close();
    }
}

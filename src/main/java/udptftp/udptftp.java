package udptftp;

import java.io.IOException;
import java.net.*;

import static udptftp.TFTPPacket.Opcode.*;

public class udptftp {

    public static void usage(){
        System.out.println("Usage:\n");
        System.out.println("(Server)         udptftp -s");
        System.out.println("(Read  Client)   udptftp -cr [hostname/IP] [remote file name] [local file name]");
        System.out.println("(Write Client)   udptftp -cw [hostname/IP] [remote file name] [local file name]");
    }
    public static void main(String[] args) {


        if(args.length==1 && args[0].equals("-s")){
            Daemon daemon = new Daemon();
            daemon.start();
            return;
        }


        else if(args.length==4 && args[0].equals("-cr")){
            InetAddress address;
            try {
                address = InetAddress.getByName(args[1]);
            } catch (UnknownHostException e) {
                System.out.println("Host name incorrect");
                return;
            }

            String requestedFilename = args[2];
            String localFilename = args[3];

            try {
                DatagramSocket clientSocket = new DatagramSocket();

                TFTPPacket readRequest = new TFTPPacket(RRQ);
                readRequest.filename = requestedFilename;
                clientSocket.send(readRequest.toDatagram(address,69));

                Recipient client = new Recipient(clientSocket, address, localFilename);
                client.start();
                return;

            } catch (SocketException e) {
                System.out.println("Read client could not reserve socket, terminated");
                return;
            } catch (IOException e){
                System.out.println("Read client encountered an I/O error, terminated");
            }
        }


        else if(args.length==4 && args[0].equals("-cw")){
            InetAddress address;
            try {
                address = InetAddress.getByName(args[1]);
            } catch (UnknownHostException e) {
                System.out.println("Host name incorrect");
                return;
            }

            String requestedFilename = args[2];
            String localFilename = args[3];

            try{
                DatagramSocket clientSocket = new DatagramSocket();

                TFTPPacket writeRequest = new TFTPPacket(WRQ);
                writeRequest.filename = requestedFilename;
                clientSocket.send(writeRequest.toDatagram(address, 69));
                System.out.println("1");

                byte[] buffer = new byte[516];
                DatagramPacket packet = new DatagramPacket(buffer, 516);
                clientSocket.receive(packet);

                System.out.println("good so far");

                TFTPPacket received = TFTPPacket.parse(packet);
                if(received.opcode!=ACK){
                    System.out.println("Write client received non-ACK packet while waiting for ACK 0, terminated");
                    return;
                }
                if(received.blockNum!=0){
                    System.out.println("Write client received ACK packet with wrong block number, terminated");
                    return;
                }
                System.out.println(received.blockNum);

                int serverPort = packet.getPort();

                System.out.println(serverPort);
                Sender client = new Sender(clientSocket, address, serverPort, localFilename);
                client.start();

                return;

            } catch (SocketException e) {
                System.out.println("Write client could not reserve socket, terminated");
            } catch (IOException e){
                System.out.println("Write client encountered an I/O error, terminated");
            } catch (TFTPPacket.TFTPParseException e) {
                System.out.println("Write client received non-TFTP packet while waiting for ACK 0, termianted");
            }
        }
        usage();
    }
}
package udptftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.SQLOutput;
import java.util.Arrays;

import static udptftp.TFTPPacket.Opcode.*;

public class Sender extends Thread{

    private DatagramSocket socket;
    private InetAddress recipientAddress;
    private int recipientPort;
    private FileInputStream localFile;

    public Sender(DatagramSocket socket, InetAddress recipientAddress, int recipientPort, String localFilename){
        this.socket = socket;
        this.recipientAddress = recipientAddress;
        this.recipientPort = recipientPort;
        try{this.localFile = new FileInputStream(localFilename);}
        catch (FileNotFoundException e){
            System.out.println("Sender instance could not get access to file \""+localFilename+"\", terminated.");
        }
    }

    public void run(){
        if (localFile==null) { return; }
        System.out.println("Sender instance starting...");
        byte[] fileBuffer = new byte[512];
        byte[] buffer = new byte[516];
        DatagramPacket message = new DatagramPacket(buffer, 516);
        try{
            int curBlockNum = 1;
            boolean done = false;
            while(!done){
                int toSendLength = localFile.read(fileBuffer);
                if (toSendLength<512){done=true; fileBuffer = Arrays.copyOf(fileBuffer, toSendLength);}
                TFTPPacket toSend = new TFTPPacket(DATA);
                toSend.blockNum = curBlockNum;
                toSend.data = fileBuffer;
                socket.send(toSend.toDatagram(recipientAddress, recipientPort));
                socket.receive(message);
                TFTPPacket received = TFTPPacket.parse(message);
                if(received.opcode != ACK){
                    System.out.println("Sender instance received non-ACK TFTP packet while waiting for ACK, terminated.");
                    return;
                }
                if(received.blockNum != curBlockNum){
                    System.out.println("Sender instance received ACK packet with block number " +
                            received.blockNum + ", expected: " + curBlockNum+", terminated.");
                    return;
                }
                curBlockNum++;
            }
        } catch (IOException e){
            System.out.println("Sender instance encountered an I/O error, terminated.");
        } catch (TFTPPacket.TFTPParseException e){
            System.out.println("Sender instance received non-TFTP packet while waiting for ACK, terminated.");
        }
        socket.close();
    }

}

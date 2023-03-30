package udptftp;

import javax.xml.crypto.Data;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.SQLOutput;
import java.util.Arrays;

import static udptftp.TFTPPacket.Opcode.*;

public class Recipient extends Thread{

    DatagramSocket socket;
    InetAddress senderAddress;
    int senderPort;
    FileOutputStream localFile;
    public Recipient(DatagramSocket socket, InetAddress senderAddress, String localFilename){
        this.socket = socket;
        this.senderAddress = senderAddress;
        try{this.localFile = new FileOutputStream(localFilename);}
        catch(FileNotFoundException e){
            System.out.println("Recipient instance could not get access to file \""+localFilename+"\", terminated.");
        }
    }

    public void run(){
        if(localFile==null){ return; }
        System.out.println("Recipient instance starting...");
        byte[] buffer = new byte[516];
        DatagramPacket message = new DatagramPacket(buffer, 516);
        try{
            int curBlockNum = 1;
            boolean done = false;
            while(!done){
                System.out.println("Recipient instance expecting block "+curBlockNum);
                socket.receive(message);
                System.out.println("Recipient instance received packet");
                TFTPPacket received = TFTPPacket.parse(message);
                if(received.opcode != DATA){
                    System.out.println("Recipient instance received non-DATA TFTP packet while waiting for DATA, terminated.");
                    return;
                }
                if(received.blockNum != curBlockNum){
                    System.out.println("Recipient instance received DATA packet with block number" +
                            received.blockNum + ", expected: " + curBlockNum+", terminated.");
                    return;
                }
                System.out.println("Packet type and block number correct");
                if(received.blockNum==1){senderPort=message.getPort();}
                if(received.data.length<512){done=true;}
                System.out.println("Packet payload length: "+received.data.length);
                localFile.write(received.data);  // TODO
                TFTPPacket toSend = new TFTPPacket(ACK);
                toSend.blockNum = curBlockNum;
                socket.send(toSend.toDatagram(senderAddress, senderPort));
                curBlockNum++;
            }
        } catch (IOException e){
            System.out.println("Recipient instance encountered an I/O error, terminated.");
        } catch (TFTPPacket.TFTPParseException e){
            System.out.println("Recipient instance received non-TFTP packet while waiting for DATA, terminated.");
        }
        socket.close();
    }
}

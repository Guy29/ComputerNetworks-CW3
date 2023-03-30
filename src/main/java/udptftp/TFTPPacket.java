package udptftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class TFTPPacket {


    public enum Opcode{
    RRQ, WRQ, DATA, ACK, ERROR
    }


    public final Opcode opcode;
    public final String mode = "octet";
    public String filename = "";
    public int blockNum = 0;
    public byte[] data = new byte[0];
    public int errorCode = 0;
    public String errorMessage = "";


    public TFTPPacket(Opcode opcode){
        this.opcode = opcode;
    }

    public byte[] toBytes(){
        byte[] out;
        int length;
        int nextPos = 1;
        byte[] fnameBytes = filename.getBytes();
        byte[] modeBytes = mode.getBytes();
        byte[] errorBytes = errorMessage.getBytes();
        switch(opcode){
            case RRQ:
            case WRQ:
                length = modeBytes.length + fnameBytes.length + 4;
                out = new byte[length];
                out[nextPos++] = (byte)(opcode==Opcode.RRQ?1:2);
                System.arraycopy(fnameBytes, 0, out, nextPos, fnameBytes.length);
                nextPos += filename.length() + 1;
                System.arraycopy(modeBytes, 0, out, nextPos, modeBytes.length);
                return out;
            case DATA:
                length = data.length + 4;
                out = new byte[length];
                out[nextPos++] = 3;
                out[nextPos++] = (byte)(blockNum/256);
                out[nextPos++] = (byte)(blockNum%256);
                System.arraycopy(data, 0, out, nextPos, data.length);
                return out;
            case ACK:
                out = new byte[4];
                out[nextPos++] = 4;
                out[nextPos++] = (byte)(blockNum/256);
                out[nextPos++] = (byte)(blockNum%256);
                return out;
            case ERROR:
                length = errorMessage.length() + 5;
                out = new byte[length];
                out[nextPos++] = 5;
                out[nextPos++] = (byte)(errorCode/256);
                out[nextPos++] = (byte)(errorCode%256);
                System.arraycopy(errorBytes, 0, out, nextPos, errorBytes.length);
                return out;
            default: return null;
        }
    }

    public DatagramPacket toDatagram(InetAddress address, int port){
//        System.out.println("toDatagram address: "+address);
//        System.out.println("toDatagram port: "+port);
        byte[] byteRepresentation = toBytes();
        DatagramPacket response = new DatagramPacket(byteRepresentation, byteRepresentation.length);
        response.setAddress(address);
        response.setPort(port);
        return response;
    }

    public static TFTPPacket parse(DatagramPacket packet) throws TFTPParseException{
        byte[] bytes = packet.getData();
        int packetLength = packet.getLength();
        if (bytes[1]<1 || bytes[1]>5){
            throw new TFTPParseException("Unparseable TFTP packet encountered. Opcode: "+bytes[1]);
        }
        Opcode op = bytes[1]==1?Opcode.RRQ:bytes[1]==2?Opcode.WRQ:
                    bytes[1]==3?Opcode.DATA:bytes[1]==4?Opcode.ACK: Opcode.ERROR;

        TFTPPacket out = new TFTPPacket(op);
        int nextPos;
        String s;
        System.out.println("New packet being parsed, Opcode: "+op+", length: " + packetLength);
        switch(op){
            case RRQ:
            case WRQ:
                s = new String(bytes, 2, bytes.length-2);
                nextPos = s.indexOf('\0');
                out.filename = s.substring(0, nextPos++);
                break;
            case DATA:
                out.data = Arrays.copyOfRange(bytes, 4, packetLength);
            case ACK:
                out.blockNum = (int)bytes[2]*256 + (int)bytes[3];
                break;
            case ERROR:
                out.errorCode = (int)bytes[2]*256 + (int)bytes[3];
                s = new String(bytes, 4, bytes.length-4);
                nextPos = s.indexOf('\0');
                out.errorMessage = s.substring(0, nextPos++);
                break;
            default: return null;
        }
        return out;
    }

    public static class TFTPParseException extends Exception {
        public TFTPParseException(String s) {
            super(s);
        }
    }

}
package com.sourceservermanager.rcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.sourceservermanager.rcon.exception.BadRcon;
import com.sourceservermanager.rcon.exception.ResponseEmpty;

/**
 * Rcon is a simple Java library for issuing RCON commands to game servers.
 * <p/>
 * This has currently only been used with HalfLife based servers.
 * <p/>
 * Example:
 * <p/>
 * response = Rcon.send(27778, "127.0.0.1", 27015, rconPassword, "log on");
 * <p/>
 * PiTaGoRas - 21/12/2004<br>
 * Now also supports responses divided into multiple packets, bad rcon password
 * detection and other minor fixes/improvements.
 * <p/>
 * @author DeadEd
 * @version 1.1
 */
public abstract class Rcon {

    private static final int RESPONSE_TIMEOUT = 2000;
    private static final int MULTIPLE_PACKETS_TIMEOUT = 300;
    
    static Socket rconSocket;
    static InputStream in = null;
    static OutputStream out = null;

    /**
     * Send the RCON request.  Sends the command to the game server.  A port
     * (localPort must be opened to send the command through.
     *
     * @param localPort The port on the local machine where the RCON request can be made from.
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @return The reponse text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    public static String send(int localPort, String ipStr, int port, String password, String command)
            throws SocketTimeoutException, BadRcon, ResponseEmpty {

        RconPacket[] requested = sendRequest(localPort, ipStr, port, password, command);

        String response = assemblePacket(requested);

        if (response.matches("Bad rcon_password.\n")) {
            throw new BadRcon();
        }
        if (response.length() == 0) {
            throw new ResponseEmpty();
        }

        return response;
    }

    private static DatagramPacket getDatagramPacket(String request, InetAddress inet, int port) {
        byte first = -1;
        byte last = 0;
        byte[] buffer = request.getBytes();
        byte[] commandBytes = new byte[buffer.length + 5];
        commandBytes[0] = first;
        commandBytes[1] = first;
        commandBytes[2] = first;
        commandBytes[3] = first;
        for (int i = 0; i < buffer.length; i++) {
            commandBytes[i + 4] = buffer[i];
        }
        commandBytes[buffer.length + 4] = last;

        return new DatagramPacket(commandBytes, commandBytes.length, inet, port);
    }

    private static RconPacket[] sendRequest(int localPort, String ipStr, int port, String password,
                                            String command) throws SocketTimeoutException {

        DatagramSocket socket = null;
        RconPacket[] resp = new RconPacket[128];

        try {
            socket = new DatagramSocket(localPort);
            int packetSize = 1400;

            InetAddress address = InetAddress.getByName(ipStr);
            //InetAddress address = InetAddress.getByName(getLocalIpAddress());
            byte[] ip = address.getAddress();
            InetAddress inet = InetAddress.getByAddress(ip);
            String msg = "challenge rcon\n";
            
            DatagramPacket out = getDatagramPacket(msg, inet, port);
            socket.send(out);

            // get the challenge
            byte[] data = new byte[packetSize];
            DatagramPacket inPacket = new DatagramPacket(data, packetSize);

            socket.setSoTimeout(RESPONSE_TIMEOUT);
            socket.receive(inPacket);

            // compose the final command and send to the server
            String challenge = parseResponse(inPacket.getData());
            String challengeNumber = challenge.substring(challenge.indexOf("rcon") + 5).trim();
            String commandStr = "rcon " + challengeNumber + " \"" + password + "\" " + command;
            DatagramPacket out2 = getDatagramPacket(commandStr, inet, port);
            socket.send(out2);

            // get the response
            byte[] data2 = new byte[packetSize];
            DatagramPacket inPacket2 = new DatagramPacket(data2, packetSize);
            socket.setSoTimeout(RESPONSE_TIMEOUT);
            socket.receive(inPacket2);

            resp[0] = new RconPacket(inPacket2);
            try {
            // Wait for a possible multiple packets response
                socket.setSoTimeout(MULTIPLE_PACKETS_TIMEOUT);
                int i = 1;
                while (true) {
                    socket.receive(inPacket2);
                    resp[i++] = new RconPacket(inPacket2);
                }
            } catch (SocketTimeoutException sex) {
                // Server didn't send more packets
            }

        } catch (SocketTimeoutException sex) {
            throw sex;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return resp;
    }

    private static String parseResponse(byte[] buf) {
        String retVal = "";

        if (buf[0] != -1 || buf[1] != -1 || buf[2] != -1 || buf[3] != -1) {
            retVal = "ERROR";
        } else {
            int off = 5;
            StringBuffer challenge = new StringBuffer(20);
            while (buf[off] != 0) {
                challenge.append((char) (buf[off++] & 255));
            }
            retVal = challenge.toString();
        }

        return retVal;
    }

    private static String assemblePacket(RconPacket[] respPacket) {

        String resp = "";

        // TODO: inspect the headers to decide the correct order

        for (int i = 0; i < respPacket.length; i++) {
            if (respPacket[i] != null) {
                resp = resp.concat(respPacket[i].data);
            }
        }
        return resp;
    }
}

class RconPacket {

    /**
     * ASCII representation of the full packet received (header included)
     */
    public String ascii = "";

    /**
     * The data included in the packet, header removed
     */
    public String data = "";

    /**
     * The full packet received (header included) in bytes
     */
    public byte[] bytes = new byte[1400];

    /**
     * Length of the packet
     */
    public int length = 0;

    /**
     * Represents a rcon response packet from the game server. A response may be split
     * into multiple packets, so an array of RconPackets should be used.
     *
     * @param packet One DatagramPacket returned by the server
     */
    public RconPacket(DatagramPacket packet) {

        this.ascii = new String(packet.getData(), 0, packet.getLength());
        this.bytes = ascii.getBytes();
        this.length = packet.getLength();

        // Now we remove the headers from the packet to have just the text
        if (bytes[0] == -2) {
            // this response comes divided into two packets
            if (bytes[13] == 108) {
                this.data = new String(packet.getData(), 14, packet.getLength() - 16);
            } else {
                this.data = new String(packet.getData(), 11, packet.getLength() - 13);
            }
        } else {
            // Single packet
            this.data = new String(packet.getData(), 5, packet.getLength() - 7);
        }
    }
}
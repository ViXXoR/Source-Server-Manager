package com.sourceservermanager.rcon;

import com.sourceservermanager.rcon.exception.BadRcon;
import com.sourceservermanager.rcon.exception.ResponseEmpty;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

/**
 * User: oscahie (aka PiTaGoRaS)<br/>
 * Date: 03-jan-2005<br/>
 * Time: 19:11:40<br/>
 * version: 0.4<br/>
 * Rcon library for Source Engine based games<br/> 
 */
public class SourceRcon {

    final static int SERVERDATA_EXECCOMMAND = 2;
    final static int SERVERDATA_AUTH = 3;
    final static int SERVERDATA_RESPONSE_VALUE = 0;
    final static int SERVERDATA_AUTH_RESPONSE = 2;

    final static int RESPONSE_TIMEOUT = 2000;
    final static int MULTIPLE_PACKETS_TIMEOUT = 300;

    static Socket rconSocket = null;
    static InputStream in = null;
    static OutputStream out = null;


    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @return The response text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    public static String send(String ipStr, int port, String password, String command) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        return send(ipStr, port, password, command, 0);
    }
    
    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @param localPort The port of the local machine to use for sending out the RCON request.
     * @return The response text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     */
    public static String send(String ipStr, int port, String password, String command, int localPort) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        String response = "";

        try {

            rconSocket = new Socket();

            //InetAddress addr = InetAddress.getLocalHost();
            // getLocalIpAddress() will get IP regardless of WiFi or 3G
            InetAddress addr = InetAddress.getByName(getLocalIpAddress());
            byte[] ipAddr = addr.getAddress();
            InetAddress inetLocal = InetAddress.getByAddress(ipAddr);
            
            rconSocket.bind(new InetSocketAddress(inetLocal, localPort));
            rconSocket.connect(new InetSocketAddress(ipStr, port), 1000);

            out = rconSocket.getOutputStream();
            in = rconSocket.getInputStream();

            rconSocket.setSoTimeout(RESPONSE_TIMEOUT);

            if (rcon_auth(password)) {
                // We are now authed
                ByteBuffer[] resp = sendCommand(command);
                // Close socket handlers, we don't need them more
                out.close(); in.close(); rconSocket.close();
                if (resp != null) {
                    response = assemblePackets(resp);
                    if (response.length() == 0) {
                        throw new ResponseEmpty();
                    }
                }
            }
            else {
                throw new BadRcon();
            }
        } catch (SocketTimeoutException timeout) {
            throw timeout;
        } catch (UnknownHostException e) {
            //System.err.println("UnknownHostException: " + e.getCause());
            return "UnknownHostException: " + e.getCause();
        } catch (IOException e) {
            //System.err.println("Couldn't get I/O for the connection: "+ e.getCause());
            //Toast.makeText(SourceRcon.this, stringShow, Toast.LENGTH_LONG).show();
        	return "Couldn't get I/O for the connection: "+ e.getCause();
        }

        return response;
    }
    
    // Get IP regardless of WiFi or 3G
    public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        //Log.e(LOG_TAG, ex.toString());
	    	//Toast.makeText(sourceServerManager.this, ex.toString(), Toast.LENGTH_LONG).show();
	    }
	    return null;
	}


    private static ByteBuffer[] sendCommand(String command) throws SocketTimeoutException {

        byte[] request = contructPacket(2, SERVERDATA_EXECCOMMAND, command);

        ByteBuffer[] resp = new ByteBuffer[128];
        int i = 0;
        try {
            out.write(request);
            resp[i] = receivePacket();  // First and maybe the unique response packet
            try {
                // We don't know how many packets will return in response, so we'll
                // read() the socket until TimeoutException occurs.
                rconSocket.setSoTimeout(MULTIPLE_PACKETS_TIMEOUT);
                while (true) {
                    resp[++i] = receivePacket();
                }
            } catch (SocketTimeoutException e) {
                // No more packets in the response, go on
                return resp;
            }

        } catch (SocketTimeoutException timeout) {
            // Timeout while connecting to the server
            throw timeout;
        } catch (Exception e2) {
            System.err.println("I/O error on socket\n");
        }
        return null;
    }


    private static byte[] contructPacket(int id, int cmdtype, String s1) {

        ByteBuffer p = ByteBuffer.allocate(s1.length() + 16);
        p.order(ByteOrder.LITTLE_ENDIAN);

        // length of the packet
        p.putInt(s1.length() + 12);
        // request id
        p.putInt(id);
        // type of command
        p.putInt(cmdtype);
        // the command itself
        p.put(s1.getBytes());
        // two null bytes at the end
        p.put((byte) 0x00);
        p.put((byte) 0x00);
        // null string2 (see Source protocol)
        p.put((byte) 0x00);
        p.put((byte) 0x00);

        return p.array();
    }

    private static ByteBuffer receivePacket() throws Exception {

        ByteBuffer p = ByteBuffer.allocate(4120);
        p.order(ByteOrder.LITTLE_ENDIAN);

        byte[] length = new byte[4];

        if (in.read(length, 0, 4) == 4) {
            // Now we've the length of the packet, let's go read the bytes
            p.put(length);
            int i = 0;
            while (i < p.getInt(0)) {
                p.put((byte) in.read());
                i++;
            }
            return p;
        }
        else {
            return null;
        }
    }


    private static String assemblePackets(ByteBuffer[] packets) {
    // Return the text from all the response packets together

        String response = "";

        for (int i = 0; i < packets.length; i++) {
            if (packets[i] != null) {
                response = response.concat(new String(packets[i].array(), 12, packets[i].position()-14));
            }
        }
        return response;
    }


    private static boolean rcon_auth(String rcon_password) throws SocketTimeoutException {

        byte[] authRequest = contructPacket(1337, SERVERDATA_AUTH, rcon_password);

        ByteBuffer response = ByteBuffer.allocate(64);
        try {
            out.write(authRequest);
            response = receivePacket(); // junk response packet
            response = receivePacket();

            // Lets see if the received request_id is leet enough ;)
            if ((response.getInt(4) == 1337) && (response.getInt(8) == SERVERDATA_AUTH_RESPONSE)) {
                return true;
            }
        } catch (SocketTimeoutException timeout) {
            throw timeout;
        } catch (Exception e) {
            System.err.println("I/O error on socket\n");
        }

        return false;
    }

}

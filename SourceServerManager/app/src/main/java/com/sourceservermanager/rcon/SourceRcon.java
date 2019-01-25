package com.sourceservermanager.rcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

/*import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;*/

import com.sourceservermanager.rcon.exception.BadRcon;
import com.sourceservermanager.rcon.exception.ResponseEmpty;

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
    
    static Socket listenerSocket = null;
    static InputStream listenerIn = null;
    static OutputStream listenerOut = null;


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
    public static String send(String ipStr, int port, String password, String command, String timeout) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        return send(ipStr, port, password, command, 0, timeout);
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
    public static String send(String ipStr, int port, String password, String command, int localPort, String rconTimeout)
    	throws SocketTimeoutException, BadRcon, ResponseEmpty {
        String response = "";

        try {
            rconSocket = new Socket();

            // getLocalIpAddress() will get IP regardless of WiFi or 3G
            InetAddress addr = InetAddress.getByName(getLocalIpAddress());
            byte[] ipAddr = addr.getAddress();
            InetAddress inetLocal = InetAddress.getByAddress(ipAddr);
            
            //rconSocket.setReuseAddress(true);
            rconSocket.bind(new InetSocketAddress(inetLocal, localPort));
            rconSocket.connect(new InetSocketAddress(ipStr, port), Integer.parseInt(rconTimeout)*1000);

            out = rconSocket.getOutputStream();
            in = rconSocket.getInputStream();
            
            rconSocket.setSoTimeout(Integer.parseInt(rconTimeout)*1000);

            if (rcon_auth(password)) {
            	
                // We are now authed
                ByteBuffer[] resp = sendCommand(command);
                // Close socket handlers, we don't need them more
                out.close(); in.close(); rconSocket.close();
                if (resp != null) {
                    response = assemblePackets(resp);
                    if (response.length() == 0) {
                        //throw new ResponseEmpty();
                    }
                }
                /*
            	byte[] request = contructPacket(2, SERVERDATA_EXECCOMMAND, command);
            	out.write(request);
            	
            	
            	while (true)
		        {
            		ByteBuffer[] resp = getData();
		        	response = assemblePackets(resp);
		        	if (response.length() > 0)
		        	{
		        		System.out.println(response);
		        		//Toast.makeText(currContext, response, Toast.LENGTH_SHORT).show();
		        	}
	            }
	            */
            }
            else {
                throw new BadRcon();
            }
        } catch (SocketTimeoutException timeout) {
            throw timeout;
        } catch (UnknownHostException e) {
            return "UnknownHostException: " + e.getCause();
        } catch (IOException e) {
        	return "Couldn't get I/O for the connection: "+ e.getCause();
        } catch (Exception e) {
			//e.printStackTrace();
		}

        return response;
    }
    
    public static String rconListener(String ipStr, int port, String password, String[] filterList)
		throws Exception
	{
    	DatagramSocket socket = null;
    	/*Socket socket = null;
    	DataOutputStream dataOutputStream = null;
    	DataInputStream dataInputStream = null;*/
    	try {
    		
    		/*
    		socket = new Socket(ipStr, port);
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream.writeUTF("hello");
			*/
			
    		
    		/*
    		InetAddress serverAddr = InetAddress.getByName(ipStr);
            DatagramChannel channel = DatagramChannel.open();
            socket = channel.socket();

            //socket = new DatagramSocket();
            socket.setReuseAddress(true);

            //InetSocketAddress ia = new InetSocketAddress("localhost", SERVERPORT);
            InetSocketAddress sa = new InetSocketAddress(8080);
            socket.bind(sa);
            DatagramPacket holepunh = new DatagramPacket(new byte[]{0,1,2,3},4, serverAddr, port);
            socket.send(holepunh);

            // create a buffer to copy packet contents into
            byte[] buf = new byte[1500];
            // create a packet to receive

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            //Log.d("UDP", "***Waiting on packet!");
            socket.setSoTimeout(10000);
            // wait to receive the packet
            socket.receive(packet);
            */
    		
            /*
             * START MEGA IGNORE
             */
            
            
            // Retrieve the ServerName
            //InetAddress serverAddr = InetAddress.getByName(ipStr);

            //Log.d("UDP", "S: Connecting...");
            // Create new UDP-Socket
            //DatagramSocket socket = new DatagramSocket(port, serverAddr);
            socket = new DatagramSocket(port);

            // By magic we know, how much data will be waiting for us
            byte[] message = new byte[1500];
            // Prepare a UDP-Packet that can
            // contain the data we want to receive 
            DatagramPacket packet = new DatagramPacket(message, message.length);
            //Log.d("UDP", "S: Receiving...");

            // Receive the UDP-Packet
            // Wait 10 seconds before timing out
            socket.setSoTimeout(10000);
            socket.receive(packet);
            
            
            /*
             * END MEGA IGNORE
             */
            
            //Log.d("UDP", "S: Length: " + packet.getLength());
            //Log.d("UDP", "S: Offset: " + packet.getOffset());
            String tempResp = new String(packet.getData());
			//String tempResp = dataInputStream.readLine();
            //tempResp = stripGarbage(tempResp);
            //tempResp = tempResp.substring(0, packet.getLength());
            // Remove timestamp
            tempResp = tempResp.substring(tempResp.indexOf(":")+8);
            
            //Log.d("UDP", "S: Received: '" + tempResp + "'");
            //Log.d("UDP", "S: Done.");
            socket.close();
            
            if(filterList != null) {
            	for(String filter : filterList) {
            		int sayIndex = tempResp.indexOf(filter);
	            	if(sayIndex > 0) {
	            		String userName = tempResp.substring(1, tempResp.indexOf("<"));
	            		String msg = tempResp.substring(tempResp.indexOf("\"", sayIndex+1)+1, tempResp.lastIndexOf("\""));
	            		
	            		if(filter.contains("say_team")) {
	            			return userName + "<T>: " + msg;
	            		} else {
	            			return userName + ": " + msg;
	            		}
	            	}
            	}
            } else {
            	return tempResp;
            }
	    } catch (Exception e) {
	    	if(socket != null) {
	    		socket.close();
	    	}
            //Log.e("UDP", "S: Error", e);
	    }
		return "";
    }
    
    public static String stripGarbage(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= ' ' && ch <= '?') || 
            	(ch >= 'A' && ch <= 'Z') || 
                (ch >= 'a' && ch <= 'z') ||
                (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            }
        }
        String cleanString = sb.toString();
        // Exclude timestamp
        // ex: 19:59:56: rcon from 127.0.0.1 ...
        cleanString = cleanString.substring(cleanString.indexOf(":")+8);
        
        return cleanString;
    }
    
    // Get IP regardless of WiFi or 3G
    public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        // Just going to return null anyways
	    }
	    return null;
	}
    
    public static String getExternalIpAddress() {
    	String extIP = "";
        try {
            /*HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://api.externalip.net/ip");
            // HttpGet httpget = new HttpGet("http://whatismyip.com.au/");
            // HttpGet httpget = new HttpGet("http://www.whatismyip.org/");
            //HttpResponse response;

            //response = httpclient.execute(httpget);

            HttpResponse execute = httpclient.execute(httpget);
            InputStream content = execute.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(
                    new InputStreamReader(content));
            String s = "";
            while ((s = buffer.readLine()) != null) {
            	extIP += s;
            }*/
            //Log.i("externalip",response.getStatusLine().toString());

        }
        catch (Exception e)
        {
        	extIP = "error: " + e.getMessage();
        }

        //Log.d("SSM", "EXT IP:" + extIP);
        return extIP;
    }


    private static ByteBuffer[] sendCommand(String command) throws SocketTimeoutException {

        byte[] request = contructPacket(2, SERVERDATA_EXECCOMMAND, command);

        ByteBuffer[] resp = new ByteBuffer[128];
        int i = 0;
        try {
            out.write(request);
            resp[i] = receivePacket(in);  // First and maybe the unique response packet
            try {
                // We don't know how many packets will return in response, so we'll
                // read() the socket until TimeoutException occurs.
                rconSocket.setSoTimeout(MULTIPLE_PACKETS_TIMEOUT);
                while (true) {
                    resp[++i] = receivePacket(in);
                }
            } catch (SocketTimeoutException e) {
                // No more packets in the response, go on
                return resp;
            }

        } catch (SocketTimeoutException timeout) {
            // Timeout while connecting to the server
            throw timeout;
        } catch (Exception e2) {
            //System.err.println("I/O error on socket\n");
        }
        return null;
    }
    
    /*
    private static ByteBuffer[] getData() throws SocketTimeoutException {
        ByteBuffer[] resp = new ByteBuffer[128];
        int i = 0;
        try {
            resp[i] = receivePacket(in);  // First and maybe the unique response packet
            try {
                // We don't know how many packets will return in response, so we'll
                // read() the socket until TimeoutException occurs.
                rconSocket.setSoTimeout(MULTIPLE_PACKETS_TIMEOUT);
                while (true) {
                    resp[++i] = receivePacket(in);
                }
            } catch (SocketTimeoutException e) {
                // No more packets in the response, go on
                return resp;
            }

        } catch (SocketTimeoutException timeout) {
            // Timeout while connecting to the server
            //throw timeout;
        	return resp;
        } catch (Exception e2) {
            //System.err.println("I/O error on socket\n");
        }
        return null;
    }
     */

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

    private static ByteBuffer receivePacket(InputStream inStream) throws Exception {

        ByteBuffer p = ByteBuffer.allocate(4120);
        p.order(ByteOrder.LITTLE_ENDIAN);

        byte[] length = new byte[4];

        if (inStream.read(length, 0, 4) == 4) {
            // Now we've the length of the packet, let's go read the bytes
            p.put(length);
            int i = 0;
            while (i < p.getInt(0)) {
                p.put((byte) inStream.read());
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

        if(packets != null) {
	        for (int i = 0; i < packets.length; i++) {
	            if (packets[i] != null) {
	            	//String resp = new String(packets[i].array(), 12, packets[i].position()-14);
	            	//if (resp != null)
	            	//{
	            		response = response.concat(new String(packets[i].array(), 12, packets[i].position()-14));
	            	//} else
	            	//{
	            		//response = response.concat("NULL");
	            	//}
	            }
	        }
        }
        return response;
    }


    private static boolean rcon_auth(String rcon_password) throws SocketTimeoutException {

        byte[] authRequest = contructPacket(1337, SERVERDATA_AUTH, rcon_password);

        ByteBuffer response = ByteBuffer.allocate(64);
        try {
            out.write(authRequest);
            response = receivePacket(in); // junk response packet
            response = receivePacket(in);

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
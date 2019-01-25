package com.sourceservermanager;

/**
 * Created by Matthew on 2/15/2016.
 */
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {

    private String serverMessage;
    //public static final String SERVERIP = "104.236.78.109"; // IHTOAYA-Mini
    public static final String SERVERIP = "159.203.9.183"; // Donatello
    public static final int SERVERPORT = 27100;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private Socket mSocket = null;

    PrintWriter out;
    BufferedReader in;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void stopClient(){
        mRun = false;
        try {
            mSocket.close();
            mSocket = null;
        } catch (Exception e) {
            Log.e("TCP", "Stopping: Error", e);
        }
    }

    public void run(String messageOnConnect) {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            mSocket = new Socket(serverAddr, SERVERPORT);
            mSocket.setSoTimeout(10000);

            try {

                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);

                // Send connect message
                sendMessage(messageOnConnect);

                Log.e("TCP Client", "C: Sent.");

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    try {
                        serverMessage = in.readLine();
                    } catch (Exception e) {
                        Log.e("TCP", "readLine: Error", e);
                    }

                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(serverMessage);
                    }
                    serverMessage = null;
                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");

            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                if(mSocket != null) {
                    mSocket.close();
                }
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}

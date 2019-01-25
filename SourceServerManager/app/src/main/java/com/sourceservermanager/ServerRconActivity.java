package com.sourceservermanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sourceservermanager.rcon.*;
import com.sourceservermanager.rcon.exception.BadRcon;
import com.sourceservermanager.rcon.exception.ResponseEmpty;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class ServerRconActivity extends AppCompatActivity {

    private static String LOG_TAG = "ServerRconActivity";
    //public static String LOG_IP = "104.236.78.109"; // IHTOAYA-Mini
    public static  String LOG_IP = "159.203.9.183"; // Donatello
    private static String LOG_PORT = "12020";
    private static String SSM_CMD = "::SSMCMD::";

    public String nickname;
    public String hostname;
    public int port;
    public String password;
    public String serverResponse;
    final Handler mHandler = new Handler();
    final Handler scrollHandler = new Handler();

    // For Real-time logging
    private boolean CHAT_MODE_ACTIVE = false;
    private boolean LOG_MODE_ACTIVE = false;
    private TCPClient mTcpClient;
    private TextView mChatBanner;
    public PowerManager.WakeLock WAKELOCK = null;

    // Create runnable for scrolling to bottom on scrollview
    final Runnable scrollBottom = new Runnable() {
        public void run() {
            final ScrollView rconRepsonseScroll = (ScrollView) findViewById(R.id.rconResponseScroll);
            // Force scroll to scroll to the bottom
            rconRepsonseScroll.fullScroll(ScrollView.FOCUS_DOWN);
        }
    };

    // Create runnable for posting server response from thread
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            if(serverResponse != null) {
                final TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);

                rconRepsonseText.append(serverResponse);
                // Force scroll to scroll to the bottom
                scrollHandler.postDelayed(scrollBottom, 10);

                Log.i(LOG_TAG, serverResponse);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_rcon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mChatBanner = (TextView) findViewById(R.id.chatEnabledBanner);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e ) {
            Log.i(LOG_TAG, "ActionBar: Error " + e);
        }

        // Handle passed ServerDataObject
        ServerDataObject sdo = (ServerDataObject)getIntent().getParcelableExtra(ServerListActivity.PAR_KEY);

        Log.i(LOG_TAG, sdo.getNickname() + "/" + sdo.getHost() + "/" + sdo.getPort() + "/" + sdo.getPassword());

        setTitle(sdo.getNickname());

        hostname = sdo.getHost();
        port = Integer.parseInt(sdo.getPort());
        password = sdo.getPassword();

        //final EditText rconCommandText = (EditText) findViewById(R.id.rconCommand);

        //sendRconRequest("status");
        //String[] tempCmd = { rconCommandText.getText().toString() };
        //threadRconRequest(false, tempCmd);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rcon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_chat) {
            if(CHAT_MODE_ACTIVE) {
                // Disable chat mode
                disableLogMode();

                CHAT_MODE_ACTIVE = false;
            } else {
                if(LOG_MODE_ACTIVE == false) {
                    // Enable chat mode
                    enableLogMode();
                } else {
                    LOG_MODE_ACTIVE = false;
                }

                CHAT_MODE_ACTIVE = true;
            }
            return true;
        } else if (id == R.id.action_clear_log) {
            TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);
            rconRepsonseText.setText("");
            // Force scroll to scroll to the bottom
            scrollHandler.postDelayed(scrollBottom, 10);

            return true;
        } else if (id == R.id.action_log) {
            if(LOG_MODE_ACTIVE) {
                // Disable chat mode
                disableLogMode();

                LOG_MODE_ACTIVE = false;
            } else {
                if(CHAT_MODE_ACTIVE == false) {
                    // Enable chat mode
                    enableLogMode();
                } else {
                    CHAT_MODE_ACTIVE = false;
                }

                LOG_MODE_ACTIVE = true;
            }

            return true;
        }else if (id == R.id.action_settings) {
            Intent intent = new Intent(ServerRconActivity.this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void disableLogMode() {
        // Disable chat mode
        if(mTcpClient != null) {
            mTcpClient.stopClient();
            mTcpClient = null;
        }

        releaseWakeLock();

        mChatBanner.setVisibility(View.GONE);

        // Send commands to server to run
        String[] tempCmds = { "logaddress_del "+LOG_IP+":"+LOG_PORT, "log off" };
        threadRconRequest(false, tempCmds);
    }

    private void enableLogMode() {
        // Real-time logging connection
        new connectTask().execute("");

        // Get a wakelock so we don't disconnect from the socket
        releaseWakeLock();
        acquireWakeLock();

        mChatBanner.setVisibility(View.VISIBLE);

        // Send commands to server to run
        String[] tempCmds = { "logaddress_add "+LOG_IP+":"+LOG_PORT, "log off", "log on" };
        threadRconRequest(false, tempCmds);
    }

    @Override
    protected void onDestroy() {
        if(CHAT_MODE_ACTIVE || LOG_MODE_ACTIVE) {
            // Disable chat mode before we exit
            disableLogMode(); // NOTE: This also calls releaseWakeLock()
        }

        super.onDestroy();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        WAKELOCK = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSM Logging Lock");
        WAKELOCK.acquire();
    }

    private void releaseWakeLock() {
        // Release the wakelock, if it's held
        Log.d(LOG_TAG, "Releasing Wakelock");
        if(WAKELOCK != null) {
            if(WAKELOCK.isHeld()) {
                WAKELOCK.release();
            }
        }
    }

    public void sendButtonClicked(View view) {
        final EditText rconCommandText = (EditText) findViewById(R.id.rconCommand);

        String[] tempCmd = { rconCommandText.getText().toString() };
        threadRconRequest(false, tempCmd);

        rconCommandText.setText("");
    }

    public void sayButtonClicked(View view) {
        final EditText rconCommandText = (EditText) findViewById(R.id.rconCommand);

        String[] tempCmd = { rconCommandText.getText().toString() };
        threadRconRequest(true, tempCmd);

        rconCommandText.setText("");
    }

    public void sendRconRequest(String command) {
        // GO HERE
        // SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        try {
            // Check IP/port
            if (hostname.length() <= 0) {
                serverResponse = getString(R.string.noIP);
            } else if (port == -1) {
                serverResponse = getString(R.string.noPort);
            } else {
                // Call Source (Half Life 2 & others) rcon without local port
                serverResponse = SourceRcon.send(hostname, port,
                        password, command,
                        settings.getString("pref_key_rcon_timeout", "5"));
                //"5");
            }

        } catch (ResponseEmpty e) {
            serverResponse = getString(R.string.emptyRcon);
        } catch (BadRcon e) {
            // Wrong RCON password
            serverResponse = getString(R.string.badRcon);
        } catch (IOException e) {
            // The socket timed out on HL2 style, try HL1! (inefficient, I know,
            // but I don't want to add anything to server prefs now)
            try {
                // Call HL1 rcon with local port 0
                serverResponse = Rcon.send(0, hostname, port,
                        password, command,
                        settings.getString("pref_key_rcon_timeout", "5"));
                //"5");
            } catch (ResponseEmpty e2) {
                serverResponse = getString(R.string.emptyRcon);
            } catch (SocketTimeoutException e2) {
                serverResponse = getString(R.string.socketTimeout);
            } catch (BadRcon e2) {
                // Wrong RCON password
                serverResponse = getString(R.string.badRcon);
            } catch (Exception e2) {
                // Something else happened...
                serverResponse = getString(R.string.failedRcon);
            }
        } catch (Exception e) {
            // Something else happened...
            serverResponse = getString(R.string.failedRcon);
        }
    }

    protected boolean threadRconRequest(final boolean isSay,
                                        final String[] commands) {
        // final EditText rconCommandText = (EditText)
        // findViewById(R.id.rconCommand);

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            public void run() {
                boolean isLogCommand = false;
                if (isSay) {
                    for (String command : commands) {
                        sendRconRequest("say " + command);
                    }
                } else {
                    for (String command : commands) {
                        if(command != null && command.length() >= 3) {
                            if (command.substring(0, 3) == "log") {
                                isLogCommand = true;
                            }
                            sendRconRequest(command);
                        }
                    }
                }

                // We won't send response to our textview since we're going to
                // see it with our log listener
                // This should resolve the duplicate message issue
                if(CHAT_MODE_ACTIVE) {
                    if (!isSay && !isLogCommand) {
                        if(commands[0].toLowerCase() == "status") {
                            // Don't touch response
                        } else {
                            // Clean up any message while logging is on, as they are quite verbose
                            String[] lines = serverResponse.split("\n");
                            serverResponse = "";
                            for (String line : lines) {
                                if(!line.startsWith("L")) {
                                    serverResponse = serverResponse + line + "\n";
                                }
                                /*int index = line.indexOf(":");
                                if(index >= 0) {
                                    String temp = line.substring(line.indexOf(":") + 1);
                                    if (!temp.startsWith("rcon from")) {
                                        serverResponse = serverResponse + temp + "\n";
                                    }
                                } else {
                                    serverResponse = serverResponse + line + "\n";
                                }*/
                            }
                        }
                        //serverResponse = serverResponse.substring(serverResponse.indexOf(":")+8);
                        mHandler.post(mUpdateResults);
                    }
                } else {
                    mHandler.post(mUpdateResults);
                }
            }
        };
        t.start();
        return true;
    }

    public class connectTask extends AsyncTask<String,String,TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            // Sends the message to the server with the host we want to get logs for
            mTcpClient.run(hostname+":"+port);

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            String tempResp = values[0];

            // Intercept any SSM messages
            int ssmCmdIndex = tempResp.indexOf(SSM_CMD);
            if(ssmCmdIndex >= 0) {
                // It's a SSMCMD, check what the 2-character command is
                int startIndex = ssmCmdIndex+SSM_CMD.length();
                String ssmCmd = tempResp.substring(startIndex, startIndex+2);

                boolean check = ssmCmd.equalsIgnoreCase("UD");
                if(ssmCmd.equalsIgnoreCase("UD")) {
                    serverResponse = getString(R.string.remote_disconnect_warning);
                    mHandler.post(mUpdateResults);

                    disableLogMode();
                }
            } else {

                // Filter server log for say and say_team messages
                tempResp = tempResp.substring(tempResp.indexOf(":") + 8);

                //Log.d("UDP", "S: Received: '" + tempResp + "'");
                //Log.d("UDP", "S: Done.");
                String[] filterList = new String[]{
                        "\" say \"",
                        "\" say_team \""
                };

                /*if(CHAT_MODE_ACTIVE) {
                    // Enable filtering for chat mode
                    filterList =
                }*/

                if (CHAT_MODE_ACTIVE) {
                    for (String filter : filterList) {
                        int sayIndex = tempResp.indexOf(filter);
                        if (sayIndex > 0) {
                            String userName = tempResp.substring(1, tempResp.indexOf("<"));
                            String msg = tempResp.substring(tempResp.indexOf("\"", sayIndex + 1) + 1, tempResp.lastIndexOf("\""));

                            if (filter.contains("say_team")) {
                                serverResponse = userName + "<T>: " + msg + "\n";
                            } else {
                                serverResponse = userName + ": " + msg + "\n";
                            }

                            mHandler.post(mUpdateResults);
                        }
                    }
                } else {
                    serverResponse = tempResp;
                    mHandler.post(mUpdateResults);
                }
            }

            //serverResponse = values[0];

            //in the arrayList we add the messaged received from server
            //arrayList.add(values[0]);
            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            //mAdapter.notifyDataSetChanged();
        }
    }
}

package com.sourceservermanager;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class ServerListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public static final String PREFS_NAME = "sourceServerManagerPrefs";
    private static String LOG_TAG = "ServerListActivity";
    private static int INTERNET_PERMISSION_REQUEST = 100;

    public final static String PAR_KEY = "com.sourceservermanager.object.par";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_server_list);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ServerListActivity.this, AddServerActivity.class);
                startActivity(intent);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //mAdapter = new MyRecyclerViewAdapter(getDataSet());
        //mRecyclerView.setAdapter(mAdapter);

        registerForContextMenu(mRecyclerView);

        // Import servers from older versions
        importOldServers();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Inflate Menu from xml resource
        MenuInflater menuInflater = ServerListActivity.this.getMenuInflater();
        menuInflater.inflate(R.menu.server_list_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Toast.makeText(ServerListActivity.this, " User selected something ", Toast.LENGTH_LONG).show();

        // TODO: Determine how to get info to store proper ID
        ContextMenuRecyclerView.RecyclerContextMenuInfo info = (ContextMenuRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.context_menu_edit:
                //Log.i(LOG_TAG, " Clicked on Item " + position);

                // Get SDO item to send to next activity
                ServerDataObject sdo = getSingleSDO(info.id);

                if (sdo != null) {
                    Intent intent = new Intent(ServerListActivity.this, AddServerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(PAR_KEY, sdo);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }

                break;
            case R.id.context_menu_delete:
                ServerReaderDbHelper mDbHelper = new ServerReaderDbHelper(this.getApplication());
                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                // Define 'where' part of query.
                String selection = ServerReaderContract.ServerEntry._ID + " = ?";
                // Specify arguments in placeholder order.
                String[] selectionArgs = {String.valueOf(info.id)};
                // Issue SQL statement.
                db.delete(ServerReaderContract.ServerEntry.TABLE_NAME, selection, selectionArgs);

                // Ensure the list updates
                mAdapter = new MyRecyclerViewAdapter(getDataSet());
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.invalidate();
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(ServerListActivity.this, SettingsActivity.class);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_add_server) {
            Intent intent = new Intent(ServerListActivity.this, AddServerActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        doPermissionsCheck();

        mAdapter = new MyRecyclerViewAdapter(getDataSet());
        mRecyclerView.setAdapter(mAdapter);

        ((MyRecyclerViewAdapter) mAdapter).setOnItemClickListener(new MyRecyclerViewAdapter
                .MyClickListener() {
            @Override
            public void onItemClick(int position, View v, ServerDataObject sdo) {
                Log.i(LOG_TAG, " Clicked on Item " + position);

                Intent intent = new Intent(ServerListActivity.this, ServerRconActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(PAR_KEY, sdo);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        /*((MyRecyclerViewAdapter) mAdapter).setOnItemLongClickListener(new MyRecyclerViewAdapter
                .MyClickListener() {
            @Override
            public void onItemClick(int position, View v, ServerDataObject sdo) {
                Log.i(LOG_TAG, " LONG Clicked on Item " + position);

                Intent intent = new Intent(ServerListActivity.this, ServerRconActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(PAR_KEY, sdo);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });*/
    }

    private void doPermissionsCheck() {
        //INTERNET
        //ACCESS_NETWORK_STATE
        //ACCESS_WIFI_STATE
        //WAKE_LOCK

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(ServerListActivity.this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ServerListActivity.this,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ServerListActivity.this,
                        Manifest.permission.ACCESS_WIFI_STATE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ServerListActivity.this,
                        Manifest.permission.WAKE_LOCK)
                        != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(ServerListActivity.this,
                    Manifest.permission.INTERNET)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {*/

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(ServerListActivity.this,
                    new String[]{Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK},
                    INTERNET_PERMISSION_REQUEST);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            //}
        }
    }

    private ArrayList<ServerDataObject> getDataSet() {
        ArrayList results = new ArrayList<>(); //ServerDataObject

        ServerReaderDbHelper mDbHelper = new ServerReaderDbHelper(getContext());

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ServerReaderContract.ServerEntry._ID,
                ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME,
                ServerReaderContract.ServerEntry.COLUMN_NAME_HOST,
                ServerReaderContract.ServerEntry.COLUMN_NAME_PORT,
                ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME + " ASC";

        Cursor c = db.query(
                ServerReaderContract.ServerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        while (c.moveToNext()) {
            long itemId = c.getLong(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry._ID));
            String itemNickname = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME));
            String itemHost = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_HOST));
            String itemPort = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_PORT));
            String itemPassword = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD));

            // Create Data Object
            ServerDataObject sdo = new ServerDataObject(itemId, itemNickname, itemHost, itemPort, itemPassword);

            results.add(sdo);
        }
        c.close();

        return results;
    }

    private ServerDataObject getSingleSDO(long id) {

        ServerReaderDbHelper mDbHelper = new ServerReaderDbHelper(getContext());

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ServerReaderContract.ServerEntry._ID,
                ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME,
                ServerReaderContract.ServerEntry.COLUMN_NAME_HOST,
                ServerReaderContract.ServerEntry.COLUMN_NAME_PORT,
                ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME + " ASC";

        String selection = ServerReaderContract.ServerEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor c = db.query(
                ServerReaderContract.ServerEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        ServerDataObject sdo = null;
        if (c.moveToFirst()) {
            long itemId = c.getLong(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry._ID));
            String itemNickname = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME));
            String itemHost = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_HOST));
            String itemPort = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_PORT));
            String itemPassword = c.getString(c.getColumnIndexOrThrow(ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD));

            // Create Data Object
            sdo = new ServerDataObject(itemId, itemNickname, itemHost, itemPort, itemPassword);
        }
        c.close();

        return sdo;
    }

    private void importOldServers() {
        Log.d(LOG_TAG, " Starting Import");
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                MODE_PRIVATE);

        int serverCount = settings.getInt("serverCount", 0);
        int newServerCount = settings.getInt("serverCount", 0);

        if (serverCount > 0) {
            Log.d(LOG_TAG, " Found " + serverCount + " server to import");

            ServerReaderDbHelper mDbHelper = new ServerReaderDbHelper(this.getApplication());
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Generate list of servers
            for (int i = 0; i < serverCount; i++) {
                String nickname = settings.getString("serverName" + i, "NoName");
                String address = settings.getString("serverIP" + i, "192.168.1.1");
                int port = settings.getInt("serverPort" + i, 27015);
                String password = settings.getString("rconPass" + i, "");

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME, nickname);
                values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_HOST, address);
                values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_PORT, port);
                values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD, password);

                long newRowId;
                newRowId = db.insert(
                        ServerReaderContract.ServerEntry.TABLE_NAME,
                        null,
                        values);

                if (newRowId != -1) {
                    Log.d(LOG_TAG, " Imported server: " + nickname + " ( " + address + ":" + port + ")");
                    newServerCount--;
                }
            }
            if (newServerCount != serverCount) {
                settings.edit().putInt("serverCount", newServerCount).apply();
            }
        }

        // Once complete, remove all shared preferences
        //settings.edit().clear().commit();
    }

    public Context getContext() {
        return this.getApplication();
    }
}

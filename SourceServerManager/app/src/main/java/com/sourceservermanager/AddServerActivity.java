package com.sourceservermanager;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AddServerActivity extends AppCompatActivity {

    private ServerDataObject PASSED_SDO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // User input
        final EditText nicknameEditText = (EditText) findViewById(R.id.nickname);
        final EditText addressEditText = (EditText) findViewById(R.id.address);
        final EditText portEditText = (EditText) findViewById(R.id.port);
        EditText passwordEditText = (EditText) findViewById(R.id.password);

        // Register imeAction stuff
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.action_add_server || actionId == 1001) {
                    addServer(v);
                }
                return false;
            }
        });

        // Register onclick for "Add Server" button
        Button addServerButton = (Button) findViewById(R.id.add_server_button);
        addServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addServer(view);
            }
        });

        // Handle POTENTIALLY passed ServerDataObject
        PASSED_SDO = (ServerDataObject)getIntent().getParcelableExtra(ServerListActivity.PAR_KEY);
        if(PASSED_SDO != null) {
            setTitle(R.string.title_edit_server);

            nicknameEditText.setText(PASSED_SDO.getNickname());
            addressEditText.setText(PASSED_SDO.getHost());
            portEditText.setText(PASSED_SDO.getPort());
            passwordEditText.setText(PASSED_SDO.getPassword());

            // Update button text
            addServerButton.setText(R.string.action_save_changes);
            passwordEditText.setImeActionLabel(getString(R.string.action_save_changes_short), R.id.action_add_server);
        }
    }

    private void addServer(View view) {
        // User input
        EditText nicknameEditText = (EditText) findViewById(R.id.nickname);
        EditText addressEditText = (EditText) findViewById(R.id.address);
        EditText portEditText = (EditText) findViewById(R.id.port);
        EditText passwordEditText = (EditText) findViewById(R.id.password);

        String nickname = nicknameEditText.getText().toString();
        String address = addressEditText.getText().toString();
        String port = portEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        //Snackbar.make(view, nickname + "/" + address + "/" + port + "/" + password, Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show();

        ServerReaderDbHelper mDbHelper = new ServerReaderDbHelper(this.getApplication());
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_NICKNAME, nickname);
        values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_HOST, address);
        values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_PORT, port);
        values.put(ServerReaderContract.ServerEntry.COLUMN_NAME_PASSWORD, password);

        // Check to see if we are updating or inserting
        if(PASSED_SDO == null) {
            // INSERTING
            // Insert the new row, returning the primary key value of the new row
            long newRowId;
            newRowId = db.insert(
                    ServerReaderContract.ServerEntry.TABLE_NAME,
                    null,
                    values);

            if(newRowId != -1) {
                finish();
            }
        } else {
            // UPDATING
            // Which row to update, based on the ID
            String selection = ServerReaderContract.ServerEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(PASSED_SDO.getID()) };

            int count = db.update(
                    ServerReaderContract.ServerEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            if(count > 0) {
                finish();
            }
        }
    }

}

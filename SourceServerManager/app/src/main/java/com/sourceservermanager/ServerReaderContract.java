package com.sourceservermanager;

import android.provider.BaseColumns;

/**
 * Created by Matthew on 1/27/2016.
 */

public final class ServerReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public ServerReaderContract() {}

    /* Inner class that defines the table contents */
    public static abstract class ServerEntry implements BaseColumns {
        public static final String TABLE_NAME = "servers";
        public static final String COLUMN_NAME_NICKNAME = "nickname";
        public static final String COLUMN_NAME_HOST = "host";
        public static final String COLUMN_NAME_PORT = "port";
        public static final String COLUMN_NAME_PASSWORD = "password";
    }
}

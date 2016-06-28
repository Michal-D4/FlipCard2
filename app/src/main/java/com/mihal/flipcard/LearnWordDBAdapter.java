package com.mihal.flipcard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by Davidovich_M on 2016-06-28.
 */
class LearnWordDBAdapter {
    private static final String FILES_TABLE = "files";
    private static final String FILE_ID = "_id";
    private static final String FILE_NAME = "file_name";
    private static final String DESCRIPTION = "description";
    private static final String WORD_NUMBER = "word_number";
    private static final String CHOSEN = "chosen";
    private static final String LANGUAGE = "language";

    private static final String WORDS_TABLE = "words_to_learn";
    private static final String WORD_ID = FILE_ID;
    private static final String NATIVE = "word";
    private static final String FOREIGN = "translation";
    private static final String TRANSCRIPTION = "transcription";
    private static final String IS_LEARNED = "is_learned";
    private static final String WORD_FILE_ID = "file_id";

    private static final String PERSIST_TABLE = "persist";
    private static final String ROW_ID = "rid";
    private static final String PATH = "path";
    private static final String TABS = "tabs";
    private static final String OTHER = "other";
    private static final String SHOW_LEARNED = "show_learned";
    private static final String PREVIEW_MODE = "preview_mode";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    LearnWordDBAdapter(Context mCtx) {
        this.mCtx = mCtx;
    }

    public void open() throws SQLiteException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }

    public MyPersist getMyPersist() {
        MyPersist mp = new MyPersist();
        Cursor cc = mDb.rawQuery("select * from " + PERSIST_TABLE + ";", null);
//            Log.i(TAG_4, "getMyPersist moveToFirst = " + cc.moveToFirst());
        if (cc.moveToFirst()) {
            mp.setPath(cc.getString(cc.getColumnIndex("path")));
            mp.setTabs_used(cc.getInt(cc.getColumnIndex("tabs")) == 1);
            mp.setOther(cc.getString(cc.getColumnIndex("other")));
            mp.setPreview(cc.getInt(cc.getColumnIndex("preview_mode")) == 1);
            mp.setShow_learned(cc.getInt(cc.getColumnIndex("show_learned")) == 1);
        }
        cc.close();
        return mp;
    }

    public void setMyPersist(MyPersist mp) {
        ContentValues val = new ContentValues();
        val.put(PATH, mp.getPath());
        val.put(TABS, mp.isTabs_used());
        val.put(OTHER, mp.getOther());
        val.put(SHOW_LEARNED, mp.isShow_learned());
        val.put(PREVIEW_MODE, mp.isPreview());
        mDb.update(PERSIST_TABLE, val, ROW_ID + "= 1", null);
    }

    public void setPath(String path) {
        ContentValues val = new ContentValues();
        val.put(PATH, path);
        mDb.update(PERSIST_TABLE, val, ROW_ID + "= 1", null);
    }

    public void deleteFile(int fileId) {
        mDb.delete(WORDS_TABLE, WORD_FILE_ID + "=" + String.valueOf(fileId), null);
        mDb.delete(FILES_TABLE, FILE_ID + "=" + String.valueOf(fileId), null);
    }

    public void clearSelection() {
        ContentValues val = new ContentValues();
        val.put(CHOSEN, false);
        mDb.update(FILES_TABLE, val, CHOSEN + "= 1", null);
    }

    void setWordNumber(int fileId, int wn) {
        ContentValues val = new ContentValues();
        val.put(WORD_NUMBER, wn);
        mDb.update(FILES_TABLE, val, FILE_ID + "=" + String.valueOf(fileId), null);
    }

    public void setSelection(int fileId, boolean isSelected) {
        ContentValues val = new ContentValues();
        val.put(CHOSEN, isSelected);
        mDb.update(FILES_TABLE, val, FILE_ID + "=" + String.valueOf(fileId), null);
    }

    // params = {fileName, description, language}
    public int addFile(String[] params) {
//            Log.i(TAG_4, "addFile >> params=" + params[0] + "; " + params[1] + "; " + params[2]);
        ContentValues val = new ContentValues();
        val.put(FILE_NAME, params[0]);
        val.put(DESCRIPTION, params[1]);
        val.put(LANGUAGE, params[2]);
        return (int) mDb.insert(FILES_TABLE, null, val);
    }

    public void addWords(ArrayList<dictEntry> words) {
        ContentValues val;
        int wordId;
        for (dictEntry e : words) {
            val = new ContentValues();
            val.put(NATIVE, e.getRuStr());
            val.put(FOREIGN, e.getEnStr());
            val.put(TRANSCRIPTION, e.getTranscriptStr());
            val.put(IS_LEARNED, (e.isLearned() ? 1 : 0));
            val.put(WORD_FILE_ID, e.getFile_id());
            wordId = (int) mDb.insert(WORDS_TABLE, null, val);
            e.setWord_id(wordId);
        }
    }

    public Cursor fetchFiles() {
        return mDb.rawQuery("select * from " + FILES_TABLE, null);
    }

    public Cursor fetchWords(int fileID) {
        return mDb.rawQuery("select * from " + WORDS_TABLE
                + " where " + WORD_FILE_ID + "="
                + String.valueOf(fileID) + ";", null);
    }

    public String getWordGroupName(int fileID) {
        String res = "";
        Cursor cc = mDb.rawQuery("select " + DESCRIPTION + " from " + FILES_TABLE
                + " where " + FILE_ID + "="
                + String.valueOf(fileID) + ";", null);
        if (cc.moveToFirst()) res = cc.getString(cc.getColumnIndex(DESCRIPTION));
        cc.close();
        return res;
    }

    public String getFileName(int fileId) {
        String res = "";
        Cursor cc = mDb.rawQuery("select " + FILE_NAME + " from " + FILES_TABLE
                + " where " + FILE_ID + "="
                + String.valueOf(fileId) + ";", null);
        if (cc.moveToFirst()) res = cc.getString(cc.getColumnIndex(FILE_NAME));
        cc.close();
        return res;
    }

    public String getLanguage(int fileId) {
        String res = "";
        Cursor cc = mDb.rawQuery("select " + LANGUAGE + " from " + FILES_TABLE
                + " where " + FILE_ID + "="
                + String.valueOf(fileId) + ";", null);
        if (cc.moveToFirst()) {
            res = cc.getString(cc.getColumnIndex(LANGUAGE));
//                Log.i(TAG_4, "getLanguage moveToFirst res = " + res);
        }
        cc.close();
        return res;
    }

    public Cursor fetchWords(boolean showLearned) {
        Cursor mCursor;
        StringBuilder sCursor = new StringBuilder("select ww._id _id, ww.word word,"
                + " ww.translation translation,"
                + " ww.transcription transcription,"
                + " ww.is_learned is_learned,"
                + " ww.file_id file_id from " + WORDS_TABLE
                + " as ww inner join " + FILES_TABLE
                + " as ff on ww.file_id = ff._id where ff.chosen = 1");
        if (showLearned) {
            // all words including already learned
            sCursor.append(";");
        } else {
            // only words that is not learned
            sCursor.append(" and ww.is_learned = 0 ;");
        }
        mCursor = mDb.rawQuery(sCursor.toString(), null);
        return mCursor;
    }

    public void setLearned(int wordID) {
        ContentValues val = new ContentValues();
        val.put(IS_LEARNED, 1);
        mDb.update(WORDS_TABLE, val, WORD_ID + "=" + String.valueOf(wordID), null);
    }

    public void updateWord(dictEntry de) {
        ContentValues val = new ContentValues();
        val.put(NATIVE, de.getRuStr());
        val.put(FOREIGN, de.getEnStr());
        val.put(TRANSCRIPTION, de.getTranscriptStr());
        mDb.update(WORDS_TABLE, val, WORD_ID + "=" + String.valueOf(de.getWord_id()), null);
    }

/*
    public void dropDB() {
        mDbHelper.onUpgrade(mDb, mDb.getVersion(), mDb.getVersion() + 1);
    }
*/


    private class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "flip_card_db";
        private static final int DATABASE_VERSION = 1;

        private static final String CREATE_FILES_TABLE = "create table " + FILES_TABLE + " ("
                + FILE_ID + " integer primary key autoincrement, "
                + FILE_NAME + " text not null, "
                + DESCRIPTION + " text, "
                + WORD_NUMBER + " integer, "
                + LANGUAGE + " text, "
                + CHOSEN + " integer ); ";

        private static final String CREATE_WORDS_TABLE = "create table " + WORDS_TABLE + " ("
                + WORD_ID + " integer primary key autoincrement, "
                + NATIVE + " text not null, "
                + FOREIGN + " text not null, "
                + TRANSCRIPTION + " text, "
                + IS_LEARNED + " integer, "
                + WORD_FILE_ID + " integer, foreign key("
                + WORD_FILE_ID + ") references "
                + FILES_TABLE + "(" + FILE_ID + ")); ";

        private static final String CREATE_PERSIST_TABLE = "create table " + PERSIST_TABLE
                + "(" + ROW_ID + " integer primary key, "
                + PATH + " text not null, "
                + TABS + " integer, "
                + OTHER + " text not null, "
                + SHOW_LEARNED + " integer, "
                + PREVIEW_MODE + " integer); ";

        private DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // will not upgrade
            db.execSQL("DROP TABLE IF EXISTS " + WORDS_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + FILES_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + PERSIST_TABLE);
            // recreate the tables
            onCreate(db);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_FILES_TABLE);
            db.execSQL(CREATE_WORDS_TABLE);
            db.execSQL(CREATE_PERSIST_TABLE);

            ContentValues val = new ContentValues();
            val.put(ROW_ID, 1);
            val.put(PATH, "");
            val.put(TABS, 1);
            val.put(OTHER, ";");
            db.insert(PERSIST_TABLE, null, val);
        }
    }
}

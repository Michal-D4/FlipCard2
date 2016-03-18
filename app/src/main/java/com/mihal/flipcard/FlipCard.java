package com.mihal.flipcard;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;


public class FlipCard extends Activity implements DataExchange {

    @Override
    protected void onPause() {
        super.onPause();
        if (TTS != null) TTS.stop();
    }

    @Override
    protected void onDestroy() {
        DBAdapter.close();
        TTS.shutdown();
        super.onDestroy();
    }

    private Menu menu;
    private ProgressBar pbRead;
    private ArrayList<dictEntry> Words;
    private LinearLayout main;
    private TextView descr;
    private TextView counts;
    private TextView preview;
    private TextView tvWord;
    private TextView tvTranscript;
    private GestureDetector detector;
    private int flipsCounter = 0;
    static private final String FLIPS_COUNTER = "FLIPS_COUNTER";
    private int prevFileId;
    private SparseArray<wnItem> wordsNumber;
    static private final String WORD_NUMBER = "WORD_NUMBER";
    private int currWord;
    static private final String CURR_WORD = "CURR_WORD";
    static private final String CURR_ITEM = "CURR_ITEM";
    private boolean isSettingNow = false;
    private MyPersist myPersist;
    private final int PICK_FILE_REQUEST = 1;
    private final int SELECT_WORDS_REQUEST = 2;
    private final int CHECK_TTS = 3;
    private MyTTS TTS;
    private boolean isTTSavailable = true;
    private int TTSmessageNo;
    private boolean isReadingNow = false;
    static private final String IS_SETTING_NOW = "IS_SETTING_NOW";

    static private final int FILE_ERROR_READING = -1;
    static private final int FILE_ERROR_OPENING = -2;
    static LearnWordDBAdapter DBAdapter;
    static final String TAG_0 = "Gesture";
    static final String TAG_1 = "logTTS";
    static final String TAG_2 = "lifeCycle";
    static final String TAG_4 = "language";

    private boolean isMarking = false;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.i(TAG_2, "onPostCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG_2, "onStart");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i(TAG_2, "onAttachedToWindow");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG_0, "onResume");
        Log.i(TAG_0, "(menu != null) =" + (menu != null) +
                " (detector != null) =" + (detector != null));
        GestureDetector.SimpleOnGestureListener FGL = new FlipGestureDetector();
        detector = new GestureDetector(this, FGL);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.i(TAG_2, "onPostResume");
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        Log.i(TAG_2, "onContentChanged");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG_2, "onConfigurationChanged");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        currWord = savedInstanceState.getInt(CURR_WORD);
        flipsCounter = savedInstanceState.getInt(FLIPS_COUNTER);
        isSettingNow = savedInstanceState.getBoolean(IS_SETTING_NOW);
        wordsNumber = savedInstanceState.getSparseParcelableArray(WORD_NUMBER);

        if (isSettingNow) return;
        if (Words.size() == 0) return;
        dictEntry tmp = Words.get(currWord);
        if (myPersist.isPreview()) {
            preview.setText(tmp.getRu());
            tvWord.setText(tmp.getEn());
            tvTranscript.setText(tmp.getTranscript());
        } else {
            if (flipsCounter % 2 == 0) {
                tvWord.setText(tmp.getRu());
            } else {
                tvWord.setText(tmp.getEn());
                tvTranscript.setText(tmp.getTranscript());
            }
        }
        /*
        int newId = tmp.getFile_id();
        wnItem wi = wordsNumber.get(newId);
        counts.setText(wi.toString());
        */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURR_WORD, currWord);
        outState.putInt(FLIPS_COUNTER, flipsCounter);
        outState.putBoolean(IS_SETTING_NOW, isSettingNow);

        outState.putSparseParcelableArray(WORD_NUMBER, wordsNumber);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG_2, "onCreate start");
        setContentView(R.layout.activity_flip_card);

        prevFileId = 0;

        descr = (TextView) findViewById(R.id.tb_descr);
        counts = (TextView) findViewById(R.id.tb_count);

        preview = (TextView) findViewById(R.id.preview);
        tvWord = (TextView) findViewById(R.id.tvWord);
        tvTranscript = (TextView) findViewById(R.id.tvTrscr);

        setCallback();  // set CustomSelectionActionModeCallback to tvTranscript

        pbRead = (ProgressBar) findViewById(R.id.pb_read);


        DBAdapter = new LearnWordDBAdapter(this);

        try {
            DBAdapter.open();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        Words = new ArrayList<>();
        wordsNumber = new SparseArray<>();

        myPersist = null;
        addOnCreate();
        checkForTTS();
        Log.i(TAG_2, "onCreate end");
    }

    private void checkForTTS() {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, CHECK_TTS);
    }

    private void setCallback() {
        ActionMode.Callback cb = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                Log.i(TAG_0, "onCreateActionMode");
                isMarking = true;
                getMenuInflater().inflate(R.menu.menu_mark_selection, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                Log.i(TAG_0, "onPrepareActionMode");
                menu.removeItem(android.R.id.selectAll);
                menu.removeItem(android.R.id.copy);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                Log.i(TAG_0, "onActionItemClicked");
                switch (menuItem.getItemId()) {
                    case R.id.do_mark:
                        final int start = tvTranscript.getSelectionStart();
                        final int end = tvTranscript.getSelectionEnd();
                        markSelected(start, end);
                        actionMode.finish();
                        break;
                    case R.id.cancel_mark:
                        tvTranscript.clearFocus();
                        actionMode.finish();
                        break;
                    default:
                        break;
                }
                isMarking = false;
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                Log.i(TAG_0, "onDestroyActionMode");
                isMarking = false;
            }
        };
        tvTranscript.setCustomSelectionActionModeCallback(cb);
    }

    private void addOnCreate() {
        main = (LinearLayout) findViewById(R.id.ID);

        LinearLayout llWords = (LinearLayout) findViewById(R.id.linear_layout);
        View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.i(TAG_0, "onTouch - (detector == null)=" + (detector == null));
                detector.onTouchEvent(motionEvent);
                return false;
            }
        };
        llWords.setOnTouchListener(listener);
    }

    private void markSelected(int start, int end) {
        CharSequence ss = tvTranscript.getText();
        StringBuilder sb = new StringBuilder();
        sb.append(ss.subSequence(0, start)).append('{').
                append(ss.subSequence(start, end)).append('}').
                append(ss.subSequence(end, ss.length()));
        Words.get(currWord).setTranscript(sb.toString());
        DBAdapter.setWords(Words.get(currWord));
        tvTranscript.clearFocus();
        tvTranscript.setText(Words.get(currWord).getTranscript());
    }

    private void getPersistence(MyPersist myPersist) {
        if (this.myPersist == null) {
            this.myPersist = myPersist;
            SetWords();
            changePreviewMode();
        } else {
            MyPersist tmp = this.myPersist;
            this.myPersist = myPersist;
            if (this.myPersist.isShow_learned() != tmp.isShow_learned() ||
                    (this.myPersist.isPreview() != tmp.isPreview())) {
                SetWords();
                changePreviewMode();
            }
        }
    }

    void changePreviewMode() {
        if (myPersist.isPreview()) {
            preview.setVisibility(View.VISIBLE);
            fillAllTextViews();
        } else {
            currWord = 0;
            preview.setVisibility(View.GONE);
            tvTranscript.setText("");
            if (Words.isEmpty()) {
                prevFileId = 0;
                descr.setText("");
                counts.setText("");
                tvWord.setText(getString(R.string.nothing_selected));
            } else {
                tvWord.setText(Words.get(currWord).getRu());
            }
        }
    }

    void SetWords() {
        Words.clear();
        wordsNumber.clear();
        currWord = 0;
        int fId = 0;
        int nn = 0;
        Cursor wordCursor = DBAdapter.fetchWords(myPersist.isShow_learned());
        if (wordCursor.moveToFirst()) {
            do { // extract data from cursor and put it into Words ArrayList
                int id = wordCursor.getInt(wordCursor.getColumnIndex("file_id"));
                if (id != fId) {
                    wordsNumber.append(fId, new wnItem(nn));
                    fId = id;
                    nn = 0;
                }
                Words.add(new dictEntry(wordCursor.getInt(wordCursor.getColumnIndex("_id")),
                        wordCursor.getString(wordCursor.getColumnIndex("word")),
                        wordCursor.getString(wordCursor.getColumnIndex("translation")),
                        wordCursor.getString(wordCursor.getColumnIndex("transcription")),
                        (wordCursor.getInt(wordCursor.getColumnIndex("is_learned")) == 1), fId));
                nn++;
            } while (wordCursor.moveToNext());
            if (!Words.isEmpty()) {
                wordsNumber.append(fId, new wnItem(nn));
                showStatusBar(0);
                if (myPersist.isPreview()) {
                    fillAllTextViews();
                } else {
                    tvWord.setText(Words.get(currWord).getRu());
                }
            } else {
                prevFileId = 0;
            }
        } else {
            fillAllTextViews();
        }
    }

    private void fillAllTextViews() {
        if (Words.isEmpty()) {
            prevFileId = 0;
            preview.setText("");
            descr.setText("");
            counts.setText("");
            tvWord.setText(getString(R.string.nothing_selected));
            tvTranscript.setText("");
        } else {
            dictEntry tmp = Words.get(currWord);
            preview.setText(tmp.getRu());
            tvWord.setText(tmp.getEn());
            tvTranscript.setText(tmp.getTranscript());
        }
    }

    private void showStatusBar(int incr) {
        if (Words.isEmpty()) return;
        int newId = Words.get(currWord).getFile_id();
        wnItem curItem = wordsNumber.get(newId);
        if (prevFileId != newId) {
            descr.setText(String.format("%s/%s",
                    DBAdapter.getFileName(newId), DBAdapter.getWordGroupName(newId)));
            String newLang = DBAdapter.getLanguage(newId);
            Log.i(TAG_1, "showStatusBar  newLang =" + newLang);
            changeTTSLocale(newLang);
            prevFileId = newId;
        } else {
            int curNum = curItem.getCur();
            curNum += incr;
            curItem.setCur(curNum);
        }
        counts.setText(curItem.toString());
    }

    private void changeTTSLocale(String newLang) {
        Log.i(TAG_1, "changeTTSLocale newLang=" + newLang +
                " TTS is null =" + (TTS == null));
        if (TTS == null) return;
        if (newLang == null || newLang.length() == 0) return;

        Locale locale = new Locale(newLang);
        Log.i(TAG_1, "TTS.getCurrentLanguage=" + TTS.getCurrentLanguage() +
                " new one=" + locale.getDisplayLanguage());
        if (TTS.getCurrentLanguage().equals(locale.getDisplayLanguage())) return;

        TTSmessageNo = TTS.setLanguage(locale);
        if (isTTSavailable != (TTSmessageNo >= TextToSpeech.LANG_AVAILABLE)) {
            isTTSavailable = true;
            changeTTSMenuIcon();
        } else {
            isTTSavailable = false;
            showMessage();
            changeTTSMenuIcon();
        }
    }

    private void flip_card() {
        if (!Words.isEmpty()) {
            if (myPersist.isPreview()) {
                if (currWord > 0) {
                    prevFileId = Words.get(currWord).getFile_id();
                    currWord--;
                    fillAllTextViews();
                    showStatusBar(-1);
                } else {
                    Toast.makeText(this, R.string.first_word, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (flipsCounter % 2 == 1) {
                    tvWord.setText(Words.get(currWord).getRu());
                    tvTranscript.setText("");
                } else {
                    tvWord.setText(Words.get(currWord).getEn());
                    tvTranscript.setText(Words.get(currWord).getTranscript());
                }
                flipsCounter++;
            }
        }
    }

    private void NextWord() {
        if (myPersist.isPreview()) {
            currWord++;
            if (currWord < Words.size()) {
                fillAllTextViews();
                showStatusBar(1);
            } else {
                currWord--;
                Toast.makeText(this, R.string.last_word, Toast.LENGTH_SHORT).show();
            }
        } else {
            tvTranscript.setText("");
            if (!Words.isEmpty()) {
                prevFileId = Words.get(currWord).file_id;
                dictEntry w = Words.remove(currWord);
                if (flipsCounter > 0) {
                    flipsCounter = 0;
                    int iPos = 5 + (int) (Math.random() * 15.0d);
                    if (Words.size() > iPos)
                        Words.add(iPos, w);
                    else
                        Words.add(w);
                } else {
                    // change status learned
                    if (!w.isLearned()) DBAdapter.setLearned(w.getWord_id());
                    showStatusBar(1);
                }
                if (!Words.isEmpty()) {
                    tvWord.setText(Words.get(currWord).getRu());
                }
            } else {
                prevFileId = 0;
                tvWord.setText("");
                tvTranscript.setText("");
                Toast.makeText(this, R.string.all_words, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG_2, "onActivityResult start");
        if (isReadingNow) {
            Toast.makeText(this, R.string.is_reading, Toast.LENGTH_SHORT).show();
        } else {
            switch (requestCode) {
                case PICK_FILE_REQUEST:
                    if (data != null) {
                        String[] path = new String[1];
                        path[0] = data.getStringExtra("path");
                        (new ReadFile()).execute(path);
                    }
                    break;
                case SELECT_WORDS_REQUEST:
                    if (resultCode == RESULT_OK) {
                        SetWords();
                        Toast.makeText(this, String.format(getString(R.string.sel_words),
                                Words.size()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CHECK_TTS:
                    if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                        TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status == TextToSpeech.SUCCESS) {
                                    getPersistence(DBAdapter.getMyPersist());
                                    Log.i(TAG_1, "checkForTTS -> onInit " + (TTS == null));
                                    //changeTTSLocale();  // TODO 2016-03-18 why
                                } else if (status == TextToSpeech.ERROR) {
                                    TTS_failed(2);
                                }
                            }
                        };
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            TTS = new MyTTSbefore21(this, listener);
                        } else {
                            Log.i(TAG_2, "TTS - Build.VERSION_CODES.LOLLIPOP");
                            TTS = new MyTTS21(this, listener);
                        }
                        isTTSavailable = true;
                    } else {
                        TTS_failed(3);
                    }
            }
        }
        Log.i(TAG_2, "onActivityResult end");
    }

    private void TTS_failed(int errID) {
        isTTSavailable = false;
        showMessage();
        changeTTSMenuIcon();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_flip_card, menu);
        Log.i(TAG_2, "onCreateOptionsMenu");
        this.menu = menu;
/*
        if (!isTTSavailable) {
            changeTTSMenuIcon();
        }
*/
        return true;
    }

    private void changeTTSMenuIcon() {
        Drawable ico;
        int id = isTTSavailable ? R.drawable.ic_action_volume_on :
                R.drawable.ic_action_volume_muted;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ico = getResources().getDrawable(id);
        } else {
            ico = getResources().getDrawable(id, null);
        }
        menu.findItem(R.id.action_tts).setIcon(ico);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (!isSettingNow) {  // to not invoke menu second time
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                isSettingNow = true;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    getFragmentManager().beginTransaction()
                            .addToBackStack(this.getLocalClassName())
                            .replace(android.R.id.content, new PersistFragment())
                            .commit();
                } else {
                    Log.i(TAG_2, "MyPersist23");
                    getFragmentManager().beginTransaction()
                            .addToBackStack(this.getLocalClassName())
                            .replace(android.R.id.content, new MyPersist23())
                            .commit();
                }
                return true;
            }
            if (id == R.id.action_open) {
                Intent pickFile = new Intent(this, PickFile.class);
                pickFile.putExtra("read-only", true);
                pickFile.putExtra("path", myPersist.getPath());
                startActivityForResult(pickFile, PICK_FILE_REQUEST);
                return true;
            }
            if (id == R.id.action_words) {
                Intent setWords = new Intent(this, WordSetting.class);
                setWords.putExtra("sep", myPersist.getDelimiter());
                setWords.putExtra("path", myPersist.getPath());
                startActivityForResult(setWords, SELECT_WORDS_REQUEST);
                return true;
            }
            if (id == R.id.action_tts) {
                if (isTTSavailable) {
                    TTS.mySpeak(tvWord.getText(), TextToSpeech.QUEUE_FLUSH);
                } else {
                    showMessage();
                }
            }
            if (id == R.id.action_info) {
                Toast.makeText(getApplicationContext(), "About will be shown here",
                        Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMessage() {
        String[] messages = getResources().getStringArray(R.array.tts_messages);
        Toast.makeText(this, messages[TTSmessageNo + 2], Toast.LENGTH_SHORT).show();
    }

    @Override
    public void receivePersist(MyPersist myPersist) {
        DBAdapter.setMyPersist(myPersist);
        getPersistence(myPersist);  // to change visibility if necessary
        isSettingNow = false;
    }

    @Override
    public MyPersist sendPersist() {
        return DBAdapter.getMyPersist();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return detector.onTouchEvent(ev);
    }

    private class ReadFile extends AsyncTask<String, Void, Integer> {
        private final ArrayList<dictEntry> mWords;
        private final MyPersist persist;

        @Override
        protected void onPreExecute() {
            pbRead.setVisibility(ProgressBar.VISIBLE);
            isReadingNow = true;
        }

        private ReadFile() {
            this.mWords = Words;
            this.persist = myPersist;
        }

        @Override
        protected Integer doInBackground(String[] params) {
            return readFile(params[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {

            isReadingNow = false;
            pbRead.setVisibility(ProgressBar.INVISIBLE);

            Context context = getApplicationContext();

            DBAdapter.setPath(persist.getPath());
            if (!Words.isEmpty()) {
                showStatusBar(0);
                if (myPersist.isPreview()) {
                    fillAllTextViews();
                } else {
                    prevFileId = 0;
                    descr.setText("");
                    counts.setText("");
                    tvWord.setText(Words.get(currWord).getRu());
                }
            }
            if (result == FILE_ERROR_READING)
                Toast.makeText(context, R.string.error_reading, Toast.LENGTH_LONG).show();
            else if (result == FILE_ERROR_OPENING)
                Toast.makeText(context, R.string.can_t_read, Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(context,
                        String.format(getString(R.string.words_no), result),
                        Toast.LENGTH_LONG).show();
                currWord = 0;
            }
        }

        private void checkNumOfWords(int count, int fileID) {
            if (count == 0) {
                DBAdapter.deleteFile(fileID);
            } else {
                DBAdapter.setWordNumber(fileID, count);
                wordsNumber.append(fileID, new wnItem(count));
            }
        }

        private int readFile(String path) {
            Log.i(TAG_4, "readFile - " + path);
            InputStreamReader is;
            try {
                is = new InputStreamReader(new FileInputStream(path), "UTF8");
            } catch (IOException e) {
                e.printStackTrace();
                return FILE_ERROR_OPENING;
            }

            String[] params = new String[3];
            params[0] = path.substring(path.lastIndexOf(File.separator) + 1);
            BufferedReader in;
            String[] ww;
            String commentSign = getString(R.string.comment_sign);
            DBAdapter.clearSelection();
            mWords.clear();
            wordsNumber.clear();
            int fileID = 0;
            try {
                in = new BufferedReader(is);
                String word;
                int numOfWords = 0;
                int numLines = 0;
                boolean toCheck = true;
                String sDelimiter;
                // Skip BOM
                word = in.readLine();
                if (word != null) {
                    sDelimiter = "\uFEFF";
                    Log.i(TAG_4, "Starts with BOM? " + sDelimiter + " " + sDelimiter.length());
                    if (word.startsWith(sDelimiter)) {
                        Log.i(TAG_4, "Starts with BOM");
                        word = word.substring(sDelimiter.length());
                    }
                }
                sDelimiter = persist.getDelimiter();
                while (word != null) {
                    Log.i(TAG_4, word);
                    numLines++;
                    if (word.startsWith(commentSign)) {
                        if (fileID > 0) {
                            checkNumOfWords(numOfWords, fileID);
                        }
                        numOfWords = 0;
                        int iL = word.indexOf("L:", commentSign.length());
                        Log.i(TAG_4, "if language found? " + iL);
                        int ii;
                        if (iL >= 0) {
                            ii = word.indexOf(" ", iL);
                            params[2] = word.substring(iL + 2, ii);
                        } else {
                            ii = commentSign.length();
                            params[2] = "";
                        }
                        params[1] = word.substring(ii).trim();
                        fileID = DBAdapter.addFile(params);
                        if (fileID > 0) DBAdapter.setSelection(fileID, true);
                    } else {
                        if (fileID == 0) {
                            params[1] = "";
                            params[2] = "";
                            fileID = DBAdapter.addFile(params);
                            DBAdapter.setSelection(fileID, true);
                        }
                        ww = word.split(sDelimiter);
                        if (ww.length > 1) {
                            mWords.add(new dictEntry(ww, fileID));
                            numOfWords++;
                        }
                    }
                    if (toCheck) {
                        if (mWords.size() > 0) toCheck = false;
                        else if (numLines > 10) {
                            in.close();
                            return FILE_ERROR_READING;
                        }
                    }
                    word = in.readLine();
                }
                if (fileID > 0) {
                    checkNumOfWords(numOfWords, fileID);
                }
                in.close();
                DBAdapter.addWords(mWords);
                DBAdapter.setSelection(fileID, true);
            } catch (IOException e) {
                e.printStackTrace();
                return FILE_ERROR_READING;
            }
            persist.setPath(path.substring(0, path.lastIndexOf(File.separator)));
            return mWords.size();
        }
    }

    static class LearnWordDBAdapter {
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
            Log.i(TAG_4, "getMyPersist moveToFirst = " + cc.moveToFirst());
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
            Log.i(TAG_4, "params=" + params[0] + "; " + params[1] + "; " + params[2]);
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
                Log.i(TAG_4, "getLanguage moveToFirst res = " + res);
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

        public void setWords(dictEntry de) {
            ContentValues val = new ContentValues();
            val.put(NATIVE, de.getRuStr());
            val.put(FOREIGN, de.getEnStr());
            val.put(TRANSCRIPTION, de.getTranscriptStr());
            mDb.update(WORDS_TABLE, val, WORD_ID + "=" + String.valueOf(de.word_id), null);
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

    static class wnItem implements Parcelable {
        private int cur;
        private int tot;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(cur);
            dest.writeInt(tot);
        }

        public static final Creator<wnItem> CREATOR =
                new Creator<wnItem>() {
                    @Override
                    public wnItem createFromParcel(Parcel source) {
                        return new wnItem(source);
                    }

                    @Override
                    public wnItem[] newArray(int size) {
                        return new wnItem[0];
                    }
                };

        public wnItem() {
        }

        public wnItem(Parcel in) {
            readFromParcel(in);
        }

        public void readFromParcel(Parcel in) {
            cur = in.readInt();
            tot = in.readInt();
        }

        public wnItem(int n2) {
            cur = 1;
            tot = n2;
        }

        @Override
        public String toString() {
            return String.format("%d/%d", cur, tot);
        }

        public int getCur() {
            return cur;
        }

        public void setCur(int cur) {
            if ((cur <= tot) && (cur > 0)) this.cur = cur;
        }
    }

    static class dictEntry {
        private int word_id;
        private final String Ru;
        private final String En;
        private String Transcript;
        private final int file_id;
        private final boolean Learned;

        // used when read from file
        public dictEntry(String[] ww, int fileId) {
            word_id = 0;
            file_id = fileId;
            Ru = ww[0];
            En = ww[1];
            if (ww.length == 3) Transcript = ww[2];
            else Transcript = "";
            Learned = false;
        }

        // used when read from DB
        public dictEntry(int wid, String ru, String en, String tr, boolean learned, int fid) {
            word_id = wid;
            Ru = ru;
            En = en;
            Transcript = tr;
            Learned = learned;
            file_id = fid;
        }

        public void setWord_id(int word_id) {
            this.word_id = word_id;
        }

        public boolean isLearned() {
            return Learned;
        }

        public int getWord_id() {
            return word_id;
        }

        public int getFile_id() {
            return file_id;
        }

        private CharSequence applySpans(String s) {
            class pair {
                int a;
                int b;

                public pair(int a, int b) {
                    this.a = a;
                    this.b = b;
                }
            }
            SparseArray<pair> ind = new SparseArray<>();
            SpannableStringBuilder ss = new SpannableStringBuilder();
            int beg;
            int end = -1;
            int i = 0;
            while ((beg = s.indexOf('{', end)) > -1) {
                ss.append(s.subSequence(end + 1, beg));
                if ((end = s.indexOf('}', beg)) == -1) {
                    end = beg;
                    break;
                }
                int i1 = ss.length();
                ss.append(s.subSequence(beg + 1, end));
                int i2 = ss.length();
                ind.append(i++, new pair(i1, i2));
            }
            ss.append(s.subSequence(end + 1, s.length()));

            for (int j = 0; j < ind.size(); j++) {
                pair be = ind.get(j);
                ss.setSpan(new RelativeSizeSpan(1.5f), be.a, be.b,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(Color.RED), be.a, be.b,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
            return ss;
        }

        public CharSequence getRu() {
            return applySpans(Ru);
        }

        public CharSequence getEn() {
            return applySpans(En);
        }

        public CharSequence getTranscript() {
            return applySpans(Transcript);
        }

        public void setTranscript(String transcript) {
            Transcript = transcript;
        }

        public String getRuStr() {
            return Ru;
        }

        public String getEnStr() {
            return En;
        }

        public String getTranscriptStr() {
            return Transcript;
        }
    }

    private class FlipGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int MIN_DISTANCE = 50;
        private static final int VELOCITY_THRESHOLD = 60;
        private static final int TAP_NEXT = 1;
        private static final int TAP_PREV_FLIP = 2;
        private final int halfScrWidth;
        private int midY;

        public FlipGestureDetector() {
            int[] loc = new int[2];
            main.getLocationOnScreen(loc);
            halfScrWidth = main.getWidth() / 2;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i(TAG_0, "onSingleTapUp - isMarking=" + isMarking);
            if (isMarking) return false;
            switch (tapZone(e)) {
                case TAP_NEXT:
                    Log.i(TAG_0, "onSingleTapUp - TAP_NEXT");
                    NextWord();
                    return true;
                case TAP_PREV_FLIP:
                    Log.i(TAG_0, "onSingleTapUp - TAP_PREV_FLIP");
                    flip_card();
                    return true;
                default:
                    Log.i(TAG_0, "onSingleTapUp - NO_ACTION");
                    return false;
            }
        }

        private boolean isWorkingZone(int Y) {
            Log.i(TAG_0, "isWorkingZone Y=" + Y);
            int TOP_OF_TAP_ZONE;
            int BOTTOM_OF_TAP_ZONE;
            int[] loc = new int[2];
            main.getLocationOnScreen(loc);
            int h = main.getHeight();
            TOP_OF_TAP_ZONE = loc[1];
            BOTTOM_OF_TAP_ZONE = TOP_OF_TAP_ZONE + h;
            Log.i(TAG_0, "loc[1]=" + loc[1] + " height=" + h +
                    " TOP_OF_TAP_ZONE=" + TOP_OF_TAP_ZONE +
                    " BOTTOM_OF_TAP_ZONE=" + BOTTOM_OF_TAP_ZONE +
                    " halfScrWidth=" + halfScrWidth);

            if (tvTranscript.getVisibility() == View.VISIBLE &&
                    !("".equals(tvTranscript.getText().toString()))) {
                int viewHeight = tvTranscript.getHeight();

                int[] locS = new int[2];
                tvTranscript.getLocationOnScreen(locS);
                midY = (TOP_OF_TAP_ZONE + BOTTOM_OF_TAP_ZONE) / 2;

                int TOP_TRANSCRIPTION_FIELD;
                int BOTTOM_TRANSCRIPTION_FIELD;
                TOP_TRANSCRIPTION_FIELD = locS[1];
                BOTTOM_TRANSCRIPTION_FIELD = TOP_TRANSCRIPTION_FIELD + viewHeight;
                Log.i(TAG_0, "TOP_TRANSCRIPTION_FIELD=" + TOP_TRANSCRIPTION_FIELD +
                        " BOTTOM_TRANSCRIPTION_FIELD=" + BOTTOM_TRANSCRIPTION_FIELD);

                if ((Y > TOP_OF_TAP_ZONE && Y < BOTTOM_OF_TAP_ZONE) &&
                        (Y < TOP_TRANSCRIPTION_FIELD || Y > BOTTOM_TRANSCRIPTION_FIELD)) {
                    Log.i(TAG_0, "return true");
                    return true;
                }
            } else if (Y > TOP_OF_TAP_ZONE && Y < BOTTOM_OF_TAP_ZONE) {
                Log.i(TAG_0, "return true");
                return true;
            }

            Log.i(TAG_0, "return false");
            return false;
        }

        private int tapZone(MotionEvent e) {
            int rez = 0;

            int Y = (int) e.getY();
            if (isWorkingZone(Y)) {
                int X = (int) e.getX();
                Log.i(TAG_0, "tapZone X,Y = " + X + "," + Y);
                if ((X < halfScrWidth && Y > midY) ||
                        (X > halfScrWidth && Y < midY)) {
                    rez = TAP_NEXT;
                } else {
                    rez = TAP_PREV_FLIP;
                }
            }
            Log.i(TAG_0, "tapZone rez = " + rez);
            return rez;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!isMarking && (Math.abs(e1.getY() - e2.getY()) < MIN_DISTANCE)) {
                if (e1.getX() - e2.getX() > MIN_DISTANCE && Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                    NextWord();
                    return true;
                } else {
                    if (e2.getX() - e1.getX() > MIN_DISTANCE && Math.abs(velocityX) >
                            VELOCITY_THRESHOLD) {
                        flip_card();
                        return true;
                    }
                }
            }
            return false;
        }
    }
}


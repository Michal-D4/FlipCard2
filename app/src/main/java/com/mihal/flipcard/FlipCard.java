package com.mihal.flipcard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
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
    protected void onDestroy() {
//        Log.i(TAG_1, "onDestroy");
        DBAdapter.close();
        //проблема. Нужно освободить ресурсы : TTS.shutdown()
        // но при повороте экрана не хочется!!!
//        if (TTS != null) TTS.shutdown();
        super.onDestroy();
    }

    static private final String WORDS = "WORDS";
    static private final String FLIPS_COUNTER = "FLIPS_COUNTER";
    static private final String PREV_FILE_ID = "PREV_FILE_ID";
    static private final String WORD_NUMBER = "WORD_NUMBER";
    static private final String CURR_WORD = "CURR_WORD";
    static private final String IS_TTS_AVAILABLE = "IS_TTS_AVAILABLE";
    static private final String IS_SETTING_NOW = "IS_SETTING_NOW";
    private final int PICK_FILE_REQUEST = 1;
    private final int SELECT_WORDS_REQUEST = 2;
    private final int CHECK_TTS_REQUEST = 3;

    private ImageView imSpeak;
    private View vScroll;
    private TextView descr;
    private TextView counts;
    private TextView preview;
    private TextView tvWord;
    private TextView tvTranscript;

    static LearnWordDBAdapter DBAdapter;
    private GestureDetector detector;
    private MyPersist myPersist;
    private ArrayList<dictEntry> Words;   // TODO - move to separate class
    private SparseArray<wnItem> wordsNumber;
    private static MyTTS TTS = null;

    private int flipsCounter = 0;
    private int prevFileId = 0;
    private int currWord = 0;
    private int TTSmessageNo;
    private boolean isSettingNow = false;
    private boolean isTTSavailable = true;
    private boolean isReadingNow = false;
    private boolean isMarking = false;

//    static final String TAG_0 = "Gesture";
//    static final String TAG_1 = "logTTS";
//    static final String TAG_2 = "lifeCycle";
    static final String TAG_4 = "language";


    @Override
    protected void onResume() {
        super.onResume();
        showStatusBar(currWord);
        GestureDetector.SimpleOnGestureListener flipGestureDetector = new FlipGestureDetector();
        detector = new GestureDetector(this, flipGestureDetector);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        currWord = savedInstanceState.getInt(CURR_WORD);
        flipsCounter = savedInstanceState.getInt(FLIPS_COUNTER);
        isSettingNow = savedInstanceState.getBoolean(IS_SETTING_NOW);
        wordsNumber = savedInstanceState.getSparseParcelableArray(WORD_NUMBER);
        prevFileId = savedInstanceState.getInt(PREV_FILE_ID);
        Words = savedInstanceState.getParcelableArrayList(WORDS);
        isTTSavailable = savedInstanceState.getBoolean(IS_TTS_AVAILABLE);

        if (isSettingNow) return;
        if (Words.size() == 0) return;   // TODO Words & myPersist are already initialized ? onCreate already done ?
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURR_WORD, currWord);
        outState.putInt(FLIPS_COUNTER, flipsCounter);
        outState.putBoolean(IS_SETTING_NOW, isSettingNow);
        outState.putInt(PREV_FILE_ID, prevFileId);
        outState.putBoolean(IS_TTS_AVAILABLE, isTTSavailable);

        outState.putSparseParcelableArray(WORD_NUMBER, wordsNumber);

        outState.putParcelableArrayList(WORDS, Words);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_flip_card);

        imSpeak = (ImageView) findViewById(R.id.btn_speak);
        imSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TTS initialization in this onCreate
//                if (TTS == null) {
//                    checkForTTS();
//                } else {
                    SpeakThis();
//                }
            }
        });

        findViewById(R.id.mark_learned).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBAdapter.setLearned(Words.get(currWord).getWord_id());
                NextWord();
            }
        });

        descr = (TextView) findViewById(R.id.tb_descr);
        counts = (TextView) findViewById(R.id.tb_count);

        preview = (TextView) findViewById(R.id.preview);
        tvWord = (TextView) findViewById(R.id.tvWord);
        tvTranscript = (TextView) findViewById(R.id.tvTrscr);

        setCallback();  // set CustomSelectionActionModeCallback to tvTranscript - popup menu

        DBAdapter = new LearnWordDBAdapter(this);

        try {
            DBAdapter.open();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        Words = new ArrayList<>();
        wordsNumber = new SparseArray<>();

        myPersist = DBAdapter.getMyPersist();

        if (savedInstanceState == null) SetWords();

        vScroll = findViewById(R.id.MAIN);
        checkForTTS();
    }

    private void checkForTTS() {
//        Log.i(TAG_1, "checkForTTS " + System.currentTimeMillis());
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, CHECK_TTS_REQUEST);
    }

    private void setCallback() {
        ActionMode.Callback cb = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                isMarking = true;
                getMenuInflater().inflate(R.menu.menu_mark_selection, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                menu.removeItem(android.R.id.selectAll);
                menu.removeItem(android.R.id.copy);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
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
                isMarking = false;
            }
        };
        tvTranscript.setCustomSelectionActionModeCallback(cb);
    }

    private void markSelected(int start, int end) {
        CharSequence ss = tvTranscript.getText();
        StringBuilder sb = new StringBuilder();
        sb.append(ss.subSequence(0, start)).append('{').
                append(ss.subSequence(start, end)).append('}').
                append(ss.subSequence(end, ss.length()));
        Words.get(currWord).setTranscript(sb.toString());
        DBAdapter.updateWord(Words.get(currWord));
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
                if (myPersist.isPreview()) {
                    fillAllTextViews();
                } else {
                    tvWord.setText(Words.get(currWord).getRu());
                }
            }
        } else {
            fillAllTextViews();
        }
    }

    private void fillAllTextViews() {
        if (Words.isEmpty()) {
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

    private void showStatusBar(int savedCurrWord) {
        if (Words.isEmpty()) return;
        int newId = Words.get(currWord).getFile_id();
        wnItem curItem = wordsNumber.get(newId);
        if ((prevFileId != newId) || "".equals(descr.getText())) {
            String newLang = DBAdapter.getLanguage(newId);
            descr.setText(String.format("%s | %s/%s", newLang,
                    DBAdapter.getFileName(newId), DBAdapter.getWordGroupName(newId)));
            if (TTS != null) changeTTSLocale(newLang);
            prevFileId = newId;
        } else {
            if (currWord != savedCurrWord) {
                int curNum = curItem.getCur();
                curNum += (currWord - savedCurrWord);
                curItem.setCur(curNum);
            }
        }
        counts.setText(curItem.toString());
    }

    private void changeTTSLocale(String newLang) {
        Log.i(TAG_4, "changeTTSLocale newLang=" + newLang);
        if (TTS == null) return;
        if (newLang == null || newLang.length() == 0) return;

        Locale locale = new Locale(newLang);
        Log.i(TAG_4, "TTS Current Language=" + TTS.getCurrentLanguage() +
                " <---> new Language=" + locale.getDisplayLanguage());
        if (TTS.getCurrentLanguage().equals(locale.getDisplayLanguage())) return;

        TTSmessageNo = TTS.setLanguage(locale);
        Log.i(TAG_4, " setLanguage ret.code = " + TTSmessageNo) ;
        // if availability of TTS changed ?
        if (isTTSavailable != (TTSmessageNo >= TextToSpeech.LANG_AVAILABLE)) {
            isTTSavailable = !isTTSavailable;
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
                    showStatusBar(currWord + 1);
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
        int savedCurrWord = currWord;
        if (myPersist.isPreview()) {
            prevFileId = Words.get(currWord).getFile_id();
            currWord++;
            if (currWord < Words.size()) {
                fillAllTextViews();
            } else {
                currWord--;
                Toast.makeText(this, R.string.last_word, Toast.LENGTH_SHORT).show();
            }
        } else {
            tvTranscript.setText("");
            if (!Words.isEmpty()) {
                dictEntry w = Words.remove(currWord);
                if (flipsCounter > 0) {
                    flipsCounter = 0;
                    moveItem(w);
                } else {
                    savedCurrWord = -1;   // to change counter in Status bar.
                    // change status learned
                    if (!w.isLearned()) DBAdapter.setLearned(w.getWord_id());
                }
                if (!Words.isEmpty()) {
//                    Log.i(TAG_0, "NextWord=" + Words.get(currWord).getRu().toString());
                    tvWord.setText(Words.get(currWord).getRu());
                }
            } else {
                prevFileId = 0;
                tvWord.setText("");
                tvTranscript.setText("");
                Toast.makeText(this, R.string.all_words, Toast.LENGTH_SHORT).show();
            }
        }
        showStatusBar(savedCurrWord);
    }

    private void moveItem(dictEntry w) {
        int pos = 5 + (int) (Math.random() * 15.0d);
        wnItem wi = wordsNumber.get(w.getFile_id());

//        Log.i(TAG_0, String.format("moveItem pos=%d / %s", pos, wi.toString()));
//        Log.i(TAG_0, w.getRu().toString());
        int lPos = wi.getTot() - wi.getCur();
        if (pos > lPos) pos = lPos;
        Words.add(pos, w);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                case CHECK_TTS_REQUEST:
//                    Log.i(TAG_1, "onActivityResult " + System.currentTimeMillis());
                    if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                        TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status == TextToSpeech.SUCCESS) {
                                    changeTTSLocale(DBAdapter.getLanguage(prevFileId));
                                } else {
                                    TTS_failed(1);
                                }
                            }
                        };
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            TTS = new MyTTSbefore21(getApplicationContext(), listener);
                        } else {
                            TTS = new MyTTS21(getApplicationContext(), listener);
                        }
                        isTTSavailable = true;
                    } else {
                        TTS_failed(2);
                    }
            }
        }
    }

    private void TTS_failed(int errID) {
        isTTSavailable = false;
        String[] messages = getResources().getStringArray(R.array.tts_messages2);
        Toast.makeText(this, messages[errID - 1], Toast.LENGTH_SHORT).show();
        changeTTSMenuIcon();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_flip_card, menu);
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
        imSpeak.setImageDrawable(ico);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (!isSettingNow) {  // to not invoke menu second time
            int id = item.getItemId();

            if (id == R.id.action_settings) {
                isSettingNow = true;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    getFragmentManager().beginTransaction()
                            .addToBackStack(this.getLocalClassName())
                            .replace(android.R.id.content, new PersistFragment())
                            .commit();
                } else {
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void SpeakThis() {
        if (isTTSavailable) {
            TTS.mySpeak(tvWord.getText(), TextToSpeech.QUEUE_ADD);
        } else {
            showMessage();
        }
    }

    private void showMessage() {
        String[] messages = getResources().getStringArray(R.array.tts_messages);
        Toast.makeText(this, messages[TTSmessageNo + 2], Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setPersist(MyPersist myPersist) {       // used from PersistFragment
        DBAdapter.setMyPersist(myPersist);
        getPersistence(myPersist);  // to change visibility if necessary
        showStatusBar(currWord);
        isSettingNow = false;
    }

    @Override
    public MyPersist getPersist() {
        return DBAdapter.getMyPersist();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return detector.onTouchEvent(ev);
    }

    private class ReadFile extends AsyncTask<String, Void, Integer> {
        static private final int FILE_ERROR_READING = -1;
        static private final int FILE_ERROR_OPENING = -2;
        private final ArrayList<dictEntry> mWords;
        private final MyPersist persist;
        private String lang = "";
        private String fileName;
        private ProgressBar pbRead;

        @Override
        protected void onPreExecute() {
            pbRead.setVisibility(ProgressBar.VISIBLE);
            isReadingNow = true;
        }

        private ReadFile() {
            this.mWords = Words;
            this.persist = myPersist;
            pbRead = (ProgressBar) findViewById(R.id.pb_read);
        }

        @Override
        protected Integer doInBackground(String... params) {
            return readFile(params[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {

            isReadingNow = false;
            pbRead.setVisibility(ProgressBar.INVISIBLE);
            Context context = getApplicationContext();

            if (result == FILE_ERROR_READING)
                Toast.makeText(context, R.string.error_reading, Toast.LENGTH_LONG).show();
            else if (result == FILE_ERROR_OPENING)
                Toast.makeText(context, R.string.can_t_read, Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(context,
                        String.format(getString(R.string.words_no), result),
                        Toast.LENGTH_LONG).show();
                currWord = 0;

                DBAdapter.setPath(persist.getPath());
                if (!Words.isEmpty()) {
                    showStatusBar(currWord);
                    if (myPersist.isPreview()) {
                        fillAllTextViews();
                    } else {
                        prevFileId = 0;
                        descr.setText("");
                        counts.setText("");
                        tvWord.setText(Words.get(currWord).getRu());
                    }
                }
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

            fileName = path.substring(path.lastIndexOf(File.separator) + 1);
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
                    if (word.startsWith(sDelimiter)) {
                        word = word.substring(sDelimiter.length());
                    }
                }
                sDelimiter = persist.getDelimiter();
                while (word != null) {
                    numLines++;
                    ww = word.split(sDelimiter);
                    if (ww[0].startsWith(commentSign)) {
                        if (fileID > 0) {
                            checkNumOfWords(numOfWords, fileID);
                        }
                        numOfWords = 0;

                        ww[0] = fileName;
                        fileID = processHeaderRecord(ww);

                        if (fileID > 0) DBAdapter.setSelection(fileID, true);
                    } else {   // нет комментария - сохраняем только имя файла params[0] и
                               // получаем fileID - без него никак !!!
                        if (fileID == 0) {
                            fileID = DBAdapter.addFile(new String[] {fileName, "", ""});

                            DBAdapter.setSelection(fileID, true);
                        }

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

        private int processHeaderRecord(String[] ww) {
            String[] params = new String[3];
            params[0] = fileName;
            params[2] = lang;
            switch (ww.length) {
                case 2:
                    params[1] = ww[1];
                    break;
                case 3:
                    params[1] = ww[2];
                    lang = params[2] = ww[1].trim();
            }
            Log.i(TAG_4, " language  " + params[2]);
            return DBAdapter.addFile(params);
        }
    }

    private class FlipGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int MIN_DISTANCE = 50;
        private static final int VELOCITY_THRESHOLD = 60;
        private static final int TAP_NEXT = 1;
        private static final int TAP_PREV_FLIP = 2;
        private boolean isFirstGesture;
        private int TOP_OF_TAP_ZONE;
        private int BOTTOM_OF_TAP_ZONE;
        private int halfScrWidth;
        private int midY;

        FlipGestureDetector() {
            isFirstGesture = true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (isMarking) return false;
            switch (tapZone(e)) {
                case TAP_NEXT:
                    NextWord();
                    return true;
                case TAP_PREV_FLIP:
                    flip_card();
                    return true;
                default:
                    return false;
            }
        }

        private boolean isWorkingZone(int Y) {
            int[] loc = new int[2];
            if (isFirstGesture) {
                vScroll.getLocationOnScreen(loc);
                halfScrWidth = vScroll.getWidth() / 2;
                TOP_OF_TAP_ZONE = loc[1];
                imSpeak.getLocationOnScreen(loc);
                BOTTOM_OF_TAP_ZONE = loc[1];
                isFirstGesture = false;
//                Log.i(TAG_0,
//                        " TOP_OF_TAP_ZONE=" + TOP_OF_TAP_ZONE +
//                                " BOTTOM_OF_TAP_ZONE=" + BOTTOM_OF_TAP_ZONE +
//                                " halfScrWidth=" + halfScrWidth);
            }


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

                if ((Y > TOP_OF_TAP_ZONE && Y < BOTTOM_OF_TAP_ZONE) &&
                        (Y < TOP_TRANSCRIPTION_FIELD || Y > BOTTOM_TRANSCRIPTION_FIELD)) {
                    return true;
                }
            } else if (Y > TOP_OF_TAP_ZONE && Y < BOTTOM_OF_TAP_ZONE) {
                return true;
            }
            return false;
        }

        private int tapZone(MotionEvent e) {
            int rez = 0;

            int Y = (int) e.getY();
            if (isWorkingZone(Y)) {
                int X = (int) e.getX();
//                Log.i(TAG_0, "tapZone X,Y = " + X + "," + Y);
                if ((X < halfScrWidth && Y > midY) ||
                        (X > halfScrWidth && Y < midY)) {
                    rez = TAP_NEXT;
                } else {
                    rez = TAP_PREV_FLIP;
                }
            }
            return rez;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int distY = (int) Math.abs(e1.getY() - e2.getY());
            int distX = (int) (e1.getX() - e2.getX());
            if (!isMarking) {
                if ((distX > MIN_DISTANCE) && (distX > distY)
                        && Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                    NextWord();
                    return true;
                } else {
                    distX = -distX;
                    if ((distX > MIN_DISTANCE) && (distX > distY)
                            && Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                        flip_card();
                        return true;
                    }
                }
            }
            return false;
        }
    }
}


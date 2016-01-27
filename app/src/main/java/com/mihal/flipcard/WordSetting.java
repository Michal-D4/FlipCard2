package com.mihal.flipcard;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class WordSetting extends Activity implements View.OnClickListener {
    private static int state;
    private static int iCheckedCount;
    private static ImageView imCheckAll;
    private ArrayList<arrayItem> fList;
    private ArrayList<Integer> selectedForDeletion;
    private WordArrayAdapter wa;
    private boolean isDeleted = false;
    private String sep;
    private String path;
    private boolean isSavingNow = false;
    private boolean isDeletingNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sep = getIntent().getStringExtra("sep");
        path = getIntent().getStringExtra("path");
        if (path.equals("")) path = Environment.getExternalStorageDirectory().getPath();

        setContentView(R.layout.activity_word_setting);

        createArray();

        ListView lvSelectWords = (ListView) findViewById(R.id.list);
        TextView tvEmpty = (TextView) findViewById(R.id.empty);
        lvSelectWords.setEmptyView(tvEmpty);

        myActionBarSetting();

        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);

        wa = new WordArrayAdapter(this, fList);
        lvSelectWords.setAdapter(wa);
        for (arrayItem i : fList) {
            if (i.eState) {
                lvSelectWords.setSelection(fList.indexOf(i));
                break;
            }
        }

    }

    private void myActionBarSetting() {
        ActionBar mActionBar = getActionBar();
        try {
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        View mCustomView = LayoutInflater.from(this).inflate(R.layout.word_setting_action_bar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_word_setting);
        mTitleTextView.setText(getString(R.string.title_activity_word_setting));

        imCheckAll = (ImageView) mCustomView.findViewById(R.id.selectAll);
        imCheckAll.setOnClickListener(this);

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
        setState();
        setIcon();
    }

    private void setState() {
        if (iCheckedCount > 0) {
            if (iCheckedCount < fList.size()) {
                state = ThreeStateView.PARTIALLY_CHECKED_STATE;
            } else {
                state = ThreeStateView.CHECKED_STATE;
            }
        } else {
            state = ThreeStateView.UNCHECKED_STATE;
        }
    }

    private static void setIcon() {
        switch (state) {
            case ThreeStateView.CHECKED_STATE:
                imCheckAll.setImageResource(R.drawable.btn_check_on);
                break;
            case ThreeStateView.UNCHECKED_STATE:
                imCheckAll.setImageResource(R.drawable.btn_check_off);
                break;
            case ThreeStateView.PARTIALLY_CHECKED_STATE:
                imCheckAll.setImageResource(R.drawable.btn_half);
        }
    }

    // Toggle check status of all listItems when click checkBox on ActionBar
    private void setChecking() {
        for (arrayItem f : fList) {
            f.eState = (state == ThreeStateView.CHECKED_STATE);
        }
        wa.notifyDataSetChanged();
    }

    private void createArray() {
        Cursor cSelectWords = FlipCard.DBAdapter.fetchFiles();
        fList = new ArrayList<>();
        arrayItem ai;
        iCheckedCount = 0;
        if (cSelectWords.moveToFirst()) {
            do {
                ai = new arrayItem();
                ai.id = cSelectWords.getInt(cSelectWords.getColumnIndex("_id"));
                ai.comment = cSelectWords.getString(cSelectWords.getColumnIndex("description"))
                        + " ("
                        + cSelectWords.getInt(cSelectWords.getColumnIndex("word_number"))
                        + ")";
                ai.iState = (cSelectWords.getInt(cSelectWords.getColumnIndex("chosen")) == 1);
                ai.eState = ai.iState;
                if (ai.eState) iCheckedCount++;
                fList.add(ai);
            } while (cSelectWords.moveToNext());
        }
        cSelectWords.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = data.getStringExtra("path");
            FlipCard.DBAdapter.setPath(path.substring(0, path.lastIndexOf(File.separator)));
            (new SaveToFile()).execute(path);
        }
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean> {
        ArrayList<Integer> curSel = null;

        @Override
        protected void onPreExecute() {
            curSel = new ArrayList<>();
            for (arrayItem i : fList) {
                if (i.eState) curSel.add(i.id);
            }
            isSavingNow = true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            isSavingNow = false;
            if (isDeletingNow) {
                (new DeleteSelected()).execute();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            saveSelection(params[0]);
            return true;
        }

        // Write words to external file
        private void saveSelection(String path_to_file) {
            FileOutputStream os;
            try {
                os = new FileOutputStream(path_to_file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            String ls = System.getProperty("line.separator");
            String commentPrefix = getString(R.string.comment_sign);
            for (int i : curSel) {
                WriteString(os, commentPrefix + FlipCard.DBAdapter.getWordGroupName(i) + ls);
                Cursor cc = FlipCard.DBAdapter.fetchWords(i);
                if (cc.moveToFirst()) {
                    do {
                        WriteString(os, cc.getString(cc.getColumnIndex("word")) + sep
                                + cc.getString(cc.getColumnIndex("translation")) + sep
                                + cc.getString(cc.getColumnIndex("transcription")) + ls);
                    } while (cc.moveToNext());
                }
            }
        }

        private void WriteString(FileOutputStream os, String s) {
            try {
                os.write(s.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private ArrayList<Integer> preDeletion() {
        ArrayList<Integer> curSel = new ArrayList<>();
        int i = 0;
        while (i < fList.size()) {
            if (fList.get(i).eState) {
                curSel.add(fList.get(i).id);
                fList.remove(i);
            } else i++;
        }
        iCheckedCount = 0;
        imCheckAll.setImageResource(R.drawable.btn_check_off);
        wa.notifyDataSetChanged();
        isDeletingNow = true;
        isDeleted = true;
        return curSel;
    }

    private class DeleteSelected extends AsyncTask<Void, Void, Boolean> {
        ArrayList<Integer> curSel = null;

        @Override
        protected void onPreExecute() {
            if (isDeletingNow) {
                curSel = selectedForDeletion;
            } else curSel = preDeletion();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            deleteSelected();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            isDeletingNow = false;
            if (fList.isEmpty()) {
                Toast.makeText(getApplicationContext(), getString(R.string.del_everything),
                        Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }

        private void deleteSelected() {
            for (int i : curSel) {
                FlipCard.DBAdapter.deleteFile(i);
            }
        }
    }

    //store in DB what is chosen for studying
    private void storeSelection() {
        for (arrayItem i : fList) {
            if (i.eState != i.iState) FlipCard.DBAdapter.setSelection(i.id, i.eState);
        }
    }

    @Override
    public void onClick(View v) {
        final int NEW_FILE_REQUEST = 3;
        // get id of the button that invoked the listener
        switch (v.getId()) {
            case R.id.selectAll:
                if (state == ThreeStateView.PARTIALLY_CHECKED_STATE)
                    state = ThreeStateView.UNCHECKED_STATE;
                else state = (state + 1) % 2;  // toggle CHECKED/UNCHECKED
                if (state == ThreeStateView.CHECKED_STATE) {
                    iCheckedCount = fList.size();
                } else {
                    iCheckedCount = 0;
                }
                setIcon();
                setChecking();
                break;
            case R.id.ok:
                if (!isDeletingNow) {
                    storeSelection();
                    if (path != null) {
                        Intent intent = new Intent();
                        intent.putExtra("path", path);
                        setResult(RESULT_OK, intent);
                    } else setResult(RESULT_OK);
                    finish();
                }
                break;
            case R.id.cancel:
                onBackPressed();
                break;
            case R.id.save:
                if (!isDeletingNow) {
                    Intent pickFile = new Intent(this, PickFile.class);
                    pickFile.putExtra("read-only", false);
                    pickFile.putExtra("path", path);
                    startActivityForResult(pickFile, NEW_FILE_REQUEST);
                }
                break;
            case R.id.delete:
                AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
                adb2.setMessage(getString(R.string.alert_delete));
                adb2.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isSavingNow) {
                            selectedForDeletion = preDeletion();
                        }
                        else {
                            (new DeleteSelected()).execute();
                        }
                    }
                });
                adb2.setNegativeButton(android.R.string.cancel, null);
                adb2.create().show();
        }
    }

    @Override
    public void onBackPressed() {
        if (isDeleted)
            setResult(RESULT_OK);
        else
            setResult(RESULT_CANCELED);
        finish();
    }

    static class WordArrayAdapter extends BaseAdapter {

        private final ArrayList<arrayItem> fList;
        private final LayoutInflater inflater;

        public WordArrayAdapter(Context context, ArrayList<arrayItem> ar) {
            inflater = LayoutInflater.from(context);
            fList = ar;
        }

        @Override
        public int getCount() {
            return fList.size();
        }

        @Override
        public Object getItem(int position) {
            return fList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return fList.get(position).id;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            arrayItem item = (arrayItem) getItem(position);
            final RowView holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.word_setting_list_item, null);
                holder = new RowView();
                holder.f_description = (TextView) convertView.findViewById(R.id.description);
                holder.f_description.setText(item.comment);
                holder.isSelect = (CheckBox) convertView.findViewById(R.id.selectedFile);
                holder.pos = position;
                convertView.setTag(holder);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        arrayItem item = (arrayItem) getItem(holder.pos);
                        item.eState = !item.eState;    // toggle eState
                        if (item.eState) WordSetting.iCheckedCount++;
                        else WordSetting.iCheckedCount--;
                        if (WordSetting.iCheckedCount < getCount()) {
                            if (WordSetting.iCheckedCount > 0)
                                WordSetting.state = ThreeStateView.PARTIALLY_CHECKED_STATE;
                            else
                                WordSetting.state = ThreeStateView.UNCHECKED_STATE;
                        } else
                            WordSetting.state = ThreeStateView.CHECKED_STATE;
                        WordSetting.setIcon();
                        holder.isSelect.setChecked(item.eState);
                    }
                });
            } else {
                holder = (RowView) convertView.getTag();
                holder.f_description.setText(item.comment);
                holder.pos = position;
            }

            holder.isSelect.setChecked(item.eState);
            return convertView;
        }

        class RowView {
            TextView f_description;
            CheckBox isSelect;
            int pos;
        }

    }

    static final class ThreeStateView {
        final static int UNCHECKED_STATE = 0;
        final static int CHECKED_STATE = 1;
        final static int PARTIALLY_CHECKED_STATE = 2;
    }

    class arrayItem {
        int id;
        String comment;
        boolean iState;          // initial state: checked/unchecked
        boolean eState;          //   final state: checked/unchecked
    }
}

package com.mihal.flipcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;


public class PickFile extends Activity {

    private FileListAdapter adapter;
    private ArrayList<FileAtr> files;
    private String curDir;
    private boolean isRoot = false;
    private Toast tt;
    private long lastPress;
    private boolean readOnly;
    private EditText et;
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readOnly = getIntent().getBooleanExtra("read-only", true);
        curDir = getIntent().getStringExtra("path");
        if (curDir == null || curDir.equals(""))
            curDir = Environment.getExternalStorageDirectory().getPath();

        setContentView(R.layout.activity_pick_file);
        ListView fl = (ListView) findViewById(R.id.fileList);
        fl.setEmptyView(findViewById(R.id.empty_folder));

        et = (EditText) findViewById(R.id.editText);
        et.setFocusable(!readOnly);

        btn = (Button) findViewById(R.id.done);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et.getText().length() > 0) {
                    if (readOnly) onFileClick(curDir + File.separator + et.getText().toString());
                    else clickButton();
                }
            }
        });

        fl.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileAtr o = (FileAtr) adapter.getItem(position);
                if (o.isFolder()) {
                    et.setText("");
                    curDir = curDir + File.separator + o.getName();
                    isRoot = false;
                    Fill();
                } else et.setText(o.getName());
            }
        });

        files = new ArrayList<>();
        adapter = new FileListAdapter(this, files);
        fl.setAdapter(adapter);

        Fill();
    }

    private void clickButton() {
        String fn = et.getText().toString();
        final File newFile = new File(curDir + File.separator + fn);
        if (newFile.exists()) {
            AlertDialog.Builder adb = new AlertDialog.Builder(et.getContext());
            adb.setMessage(getString(R.string.q_overwrite));
            adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onFileClick(newFile.getAbsolutePath());
                }
            });
            adb.setNegativeButton(android.R.string.cancel, null);
            adb.create().show();
        } else {
            File tmp = new File(newFile.getParent());
            tmp.mkdir();
            onFileClick(newFile.getAbsolutePath());
        }
    }

    private void Fill() {
        files.clear();
        File dir = new File(curDir);
        File[] dirs = dir.listFiles();    //returns null for if canRead = false
        if (dirs == null) {
            TextView empty = (TextView) findViewById(R.id.empty_folder);
            empty.setText(getString(R.string.can_t_read) + curDir);
        } else if (dirs.length > 0) {
            this.setTitle(dir.getName());
            FillArray(dirs);
        } else {
            TextView empty = (TextView) findViewById(R.id.empty_folder);
            empty.setText(getString(R.string.empty_folder));
        }
        adapter.notifyDataSetChanged();
    }

    private void FillArray(File[] dirs) {
        ArrayList<FileAtr> fls = new ArrayList<>();
        try {
            for (File ff : dirs) {
                if (ff.isDirectory())
                    files.add(new FileAtr(ff.getName(), true));
                else {
                    fls.add(new FileAtr(ff.getName(), false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(files);
        Collections.sort(fls);
        files.addAll(fls);
    }

    private void onFileClick(String path) {
        Intent intent = new Intent();
        intent.putExtra("path", path);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isRoot) {
            if (System.currentTimeMillis() - lastPress < 3000) {
                tt.cancel();
                setResult(RESULT_CANCELED);
                finish();
            } else {
                tt = Toast.makeText(this, getString(R.string.press_to_exit), Toast.LENGTH_SHORT);
                tt.show();
                lastPress = System.currentTimeMillis();
            }
            return;
        }

        et.setText("");
        int i = curDir.lastIndexOf(File.separator);
        if (i >= 0) {
            if (i == 0) {
                i = 1;
                isRoot = true;
            } else isRoot = false;
            curDir = curDir.substring(0, i);
            Fill();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_pick_file, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
    static class FileListAdapter extends BaseAdapter {

        private final ArrayList<FileAtr> listData;
        private final LayoutInflater inflater;

        public FileListAdapter(Context context, ArrayList<FileAtr> fl) {
            inflater = LayoutInflater.from(context);
            listData = fl;
        }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.pick_file_item, null);
                holder = (TextView) convertView;
                convertView.setTag(holder);
            } else {
                holder = (TextView) convertView.getTag();
            }

            if (listData.get(position).isFolder()) {
                holder.setCompoundDrawablesWithIntrinsicBounds(R.drawable.folder_icon, 0, 0, 0);
            } else {
                holder.setCompoundDrawablesWithIntrinsicBounds(R.drawable.file_icon, 0, 0, 0);
            }
            holder.setText(listData.get(position).getName());

            return convertView;
        }

    }

    static class FileAtr implements Comparable<FileAtr> {
        private final String name;
        private final boolean isFolder;

        public boolean isFolder() {
            return isFolder;
        }

        FileAtr(String name, boolean isFolder) {
            this.name = name;
            this.isFolder = isFolder;
        }

        public String getName() {
            return name;
        }

        @Override
        public int compareTo(FileAtr fa) {
            if (this.name != null)
                return this.name.toLowerCase(Locale.getDefault()).
                        compareTo(fa.getName().toLowerCase(Locale.getDefault()));
            else
                throw new IllegalArgumentException();
        }
    }
}

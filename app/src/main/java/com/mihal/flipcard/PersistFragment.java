package com.mihal.flipcard;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class PersistFragment extends Fragment  {
    private ViewGroup container;
    private MyPersist mp;
    private EditText etPath;
    private RadioButton rbTabs;
    private TextView tvOther;
    private CheckBox cbLearned;
    private CheckBox cbPreview;
    public DataExchange mCallback;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {

            getView().setBackgroundColor(Color.WHITE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        mp = mCallback.sendPersist();
        rbTabs = (RadioButton) container.findViewById(R.id.rb_tabs);
        rbTabs.setChecked(mp.isTabs_used());
        ((RadioButton) container.findViewById(R.id.rb_other)).setChecked(!mp.isTabs_used());
        tvOther = ((TextView) container.findViewById(R.id.tv_other));
        if (mp.isTabs_used()) tvOther.setVisibility(View.INVISIBLE);
        else tvOther.setVisibility(View.VISIBLE);
        tvOther.setText(mp.getOther());
        cbLearned = (CheckBox) container.findViewById(R.id.cb_learned);
        cbLearned.setChecked(mp.isShow_learned());
        cbPreview = (CheckBox) container.findViewById(R.id.cb_preview);
        cbPreview.setChecked(mp.isPreview());

        RadioGroup rg = (RadioGroup) container.findViewById(R.id.radioGroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_other) {
                    tvOther.setVisibility(View.VISIBLE);
                } else {
                    tvOther.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        this.container = container;
        return inflater.inflate(R.layout.persist_fragment, container, false);
    }

/* Added in API 23
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (DataExchange) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement PersistExchange");
        }
    }
*/

/* Deprecated in API 23 */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (DataExchange) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PersistExchange");
        }
    }


    @Override
    public void onDestroy() {
        mp.setTabs_used(rbTabs.isChecked());
        mp.setOther(tvOther.getText().toString());
        mp.setShow_learned(cbLearned.isChecked());
        mp.setPreview(cbPreview.isChecked());

        mCallback.receivePersist(mp);

        super.onDestroyView();
    }
}


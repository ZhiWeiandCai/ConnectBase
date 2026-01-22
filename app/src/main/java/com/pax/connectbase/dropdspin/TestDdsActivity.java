package com.pax.connectbase.dropdspin;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pax.connectbase.APP;
import com.pax.connectbase.R;
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener;
import com.skydoves.powerspinner.PowerSpinnerView;

public class TestDdsActivity extends AppCompatActivity {
    PowerSpinnerView powerSpinnerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dds);

        initView();
    }

    private void initView() {
        powerSpinnerView = findViewById(R.id.power_spinner_v);
        powerSpinnerView.setOnSpinnerItemSelectedListener(new OnSpinnerItemSelectedListener<String>() {
            @Override
            public void onItemSelected(int i, @Nullable String s, int i1, String t1) {
                Toast.makeText(APP.context, "item=" + i1 + " s=" + t1, Toast.LENGTH_LONG).show();
            }
        });
    }
}
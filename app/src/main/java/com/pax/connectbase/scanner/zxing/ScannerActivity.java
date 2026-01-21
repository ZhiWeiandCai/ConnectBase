package com.pax.connectbase.scanner.zxing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.pax.connectbase.R;
import com.pax.connectbase.util.LogUtils;

import java.util.List;


public class ScannerActivity extends AppCompatActivity {

    public static final String CLOSE_SCANNER_INTENT_ACTION = "android.intent.action.CloseZxingScanner";
    private String TAG ="ZxingScannerActivity";
    private CaptureManager capture;
    private DecoratedBarcodeView bv_barcode;

    private BroadcastReceiver mDestroyActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CLOSE_SCANNER_INTENT_ACTION)) {
                LogUtils.d(TAG, "receiver unregistered");
                ScannerActivity.this.finish();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);
        registerDestroyActivityReceiver();
        TextView tvTitle=findViewById(R.id.header_title);
        tvTitle.setText(R.string.title_scanner);
        ImageView imageView=findViewById(R.id.header_back);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bv_barcode = findViewById(R.id.bv_barcode);
        capture = new CaptureManager(this, bv_barcode);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        bv_barcode.setStatusText((String) getText(R.string.scan_text));
//        capture.decode();
        bv_barcode.decodeSingle(barcodeCallback);
    }


    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
        unregisterDestroyActivityReceiver();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        sendCancelBroadcast();
        return bv_barcode.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }


    private BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            LogUtils.d("ScannerActivity","barcodeResult");
            if (result != null){
                sendSuccessBroadcast(result.getText());
            }else {
                sendScanErrorBroadcast();
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            LogUtils.d("ScannerActivity","possibleResultPoints");
        }
    };

    private void registerDestroyActivityReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CLOSE_SCANNER_INTENT_ACTION);
        registerReceiver(mDestroyActivityReceiver, intentFilter);
    }

    private void unregisterDestroyActivityReceiver(){
        unregisterReceiver(mDestroyActivityReceiver);
    }

    private void sendSuccessBroadcast(String qrCodeStr) {
        Intent intent = new Intent(ZxingScanner.SCAN_INTENT_ACTION);
        intent.putExtra(ZxingScanner.FLAGS, ZxingScanner.SUCCESS_FLAG);
        intent.putExtra(ZxingScanner.QR_CODE_STR, qrCodeStr);
        sendBroadcast(intent);
    }

    private void sendCancelBroadcast() {
        Intent intent = new Intent(ZxingScanner.SCAN_INTENT_ACTION);
        intent.putExtra(ZxingScanner.FLAGS, ZxingScanner.CANCEL_FLAG);
        sendBroadcast(intent);
    }

    public void sendScanErrorBroadcast() {
        Intent intent = new Intent(ZxingScanner.SCAN_INTENT_ACTION);
        intent.putExtra(ZxingScanner.FLAGS, ZxingScanner.ERROR_FLAG);
        sendBroadcast(intent);
    }



}

package com.pax.connectbase.scanner.zxing;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.zxing.integration.android.IntentIntegrator;
import com.pax.connectbase.util.LogUtils;


public class ZxingScanner {
    public static final String SCAN_INTENT_ACTION = "android.intent.action.OpenZxingScanner";
    public static final String FLAGS = "FLAGS";
    public static final String QR_CODE_STR = "QR_CODE_STR";
    public static final int SUCCESS_FLAG = 0;
    public static final int CANCEL_FLAG = 1;
    public static final int ERROR_FLAG = 2;

    private Context context;
    private ZxingPortListener listener;

    private boolean isBroadcastRegistered = false;
    private String qrCodeStr;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCAN_INTENT_ACTION)) {
                int flag = intent.getIntExtra(FLAGS, CANCEL_FLAG);
                switch (flag) {
                    case SUCCESS_FLAG:
                        qrCodeStr = intent.getStringExtra(QR_CODE_STR);
                        LogUtils.i("SerialPortScanner", "qrCode: " + qrCodeStr);
                        if (listener != null) {
                            listener.onReadSuccess(qrCodeStr);
                        }
                        break;
                    case CANCEL_FLAG:
                        if (listener != null) {
                            listener.onCancel();
                        }
                        break;
                    case ERROR_FLAG:
                    default:
                        if (listener != null) {
                            listener.onReadError();
                        }
                        break;
                }
            }
        }
    };

    public ZxingScanner(Context context) {
        this.context = context;
    }


    public void start(ZxingPortListener listener) {
        this.listener = listener;
    }

    public void open() {

        new IntentIntegrator((Activity) context).setCaptureActivity(ScannerActivity.class).initiateScan();

        registerOpenBroadcastReceiver();
    }

    public void close() {
        unRegisterMyBroadcastReceiver();

        Intent intent = new Intent(ScannerActivity.CLOSE_SCANNER_INTENT_ACTION);
        context.sendBroadcast(intent);
    }

    private void registerOpenBroadcastReceiver() {
        if (!isBroadcastRegistered) {
            isBroadcastRegistered = !isBroadcastRegistered;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SCAN_INTENT_ACTION);
            context.registerReceiver(receiver, intentFilter);
        }
    }

    private void unRegisterMyBroadcastReceiver() {
        if (isBroadcastRegistered) {
            isBroadcastRegistered = !isBroadcastRegistered;
            context.unregisterReceiver(receiver);
        }
    }

    public interface ZxingPortListener {
        void onReadSuccess(String result);

        void onReadError();

        void onCancel();
    }
}

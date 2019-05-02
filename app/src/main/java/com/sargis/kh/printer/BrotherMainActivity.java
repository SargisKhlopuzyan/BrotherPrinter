package com.sargis.kh.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.brother.ptouch.sdk.Printer;
import com.sargis.kh.printer.databinding.ActivityBrotherMainBinding;

public class BrotherMainActivity extends AppCompatActivity implements PrinterContract.View {

    ActivityBrotherMainBinding binding;
    PrintingPresenter printingPresenter;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_brother_main);

        printingPresenter = new PrintingPresenter(this, this);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        binding.setOnPrintClick(v -> {
            printingPresenter.startPrinting();
        });

        binding.setOnFindPrinterModelAndLabelTypeClick(v -> {
            printingPresenter.findPrinterModel();
        });
    }


    public boolean hasUSBPermission(Printer myPrinter2, int requestCode) {

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            showToast("usbManager == null", true);
            return false;
        }

        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
        if (usbDevice == null) {
            showToast("usbDevice == null", true);
            return false;
        }

        if (!usbManager.hasPermission(usbDevice)) {

            showToast("** if (!usbManager.hasPermission(usbDevice)) { **", true);
            Intent intent = new Intent(PrintingPresenter.ACTION_USB_PERMISSION);
            intent.putExtra(PrintingPresenter.REQUEST_CODE_USB_PERMISSION, requestCode);

            PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
            usbManager.requestPermission(usbDevice, permissionIntent);

            registerReceiver(mUsbReceiver, new IntentFilter(PrintingPresenter.ACTION_USB_PERMISSION));
            return false;
        }
        showToast("return *****************", true);
        return true;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast(" ********* BroadcastReceiver -> onReceive", true);

            String action = intent.getAction();
            if (PrintingPresenter.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        switch (intent.getIntExtra(PrintingPresenter.REQUEST_CODE_USB_PERMISSION, 0)) {
                            case PrintingPresenter.REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE:
                                printingPresenter.findPrinterModel();

                                break;
                            case PrintingPresenter.REQUEST_CODE_PRINT:
                                printingPresenter.setupAndPrintWithoutAskingPermission();
                                break;
                        }

                    } else {
                        //TODO
//                        if (myPrinter2 == null) {
//                            myPrinter2 = new Printer();
//                        }
//
//                        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//                        if (usbManager == null) {
//                            return;
//                        }
//
//                        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
//                        if (usbDevice == null) {
//                            return;
//                        }
//
//                        PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
//                        usbManager.requestPermission(usbDevice, permissionIntent);
//
//                        usbManager.requestPermission(usbDevice, permissionIntent);
                    }
                }
            }
        }
    };

    @Override
    public void printingStatusUpdated(String status) {

    }

    @Override
    public void printingSucceed() {

    }

    @Override
    public void printingError(String errorMessage) {

    }

    private void showToast(String text, boolean showToast) {
//        runOnUiThread(() -> {
        Log.e("LOG_TAG", "*** " + text);
//            binding.textViewErrorCode.setText(text);
//            if (showToast) Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
//        });
    }


    public static boolean hasPermissions (Context context, String...permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


}
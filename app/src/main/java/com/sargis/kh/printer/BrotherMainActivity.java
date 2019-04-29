package com.sargis.kh.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.google.zxing.WriterException;
import com.sargis.kh.printer.databinding.ActivityBrotherMainBinding;

import java.util.ArrayList;
import java.util.List;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class BrotherMainActivity extends AppCompatActivity {

    ActivityBrotherMainBinding binding;

    public static Printer myPrinter2;
    public static Bitmap imageToPrint;
    protected PrinterStatus printResult2;
    protected PrinterInfo myPrinterInfo2;

    int selectedPosition = 0;

    //************************************//
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //************************************//

    //
    String inputValue;
    Bitmap bitmap;
    QRGEncoder qrgEncoder;

    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_brother_main);

        //************************************//

        CustomArrayAdapter adapter = new CustomArrayAdapter(this, getLabels());
        binding.spinner.setAdapter(adapter);
        binding.spinner.setSelection(adapter.getCount() - 2);
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = getLabels().get(position).ordinal();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        setQRCodeImageView();
        //************************************//

        binding.setOnPrintClick(v -> {
            print();
        });
    }

    public void print() {

        if (myPrinter2 == null)
            myPrinter2 = new Printer();

        //************************************//
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            showToast("usbManager == null", true);
            return;
        }

        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
        if (usbDevice == null) {
            showToast("usbDevice == null", true);
            return;
        }

        if (!usbManager.hasPermission(usbDevice)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(usbDevice, permissionIntent);
            registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
            return;
        }

        Log.e("LOGGGG", "print()");

        setupAndPrint();
        //************************************//
    }

    private void setQRCodeImageView() {
        inputValue = "Sargis Khlopuzyan, Android Developer";
        if (inputValue.length() > 0) {
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int smallerDimension = width < height ? width : height;
            smallerDimension = smallerDimension * 4 / 4; // 3 / 4

            qrgEncoder = new QRGEncoder(
                    inputValue, null,
                    QRGContents.Type.TEXT,
                    smallerDimension);
            try {
                bitmap = qrgEncoder.encodeAsBitmap();
                binding.layoutTicket.imageViewQr.setImageBitmap(bitmap);
            } catch (WriterException e) {
                showToast("WriterException: " + e.toString(), true);
            }
        } else {
            showToast("Required", true);
        }
    }

    private void setupAndPrint() {
        myPrinterInfo2 = new PrinterInfo();
        myPrinterInfo2 = myPrinter2.getPrinterInfo();
        myPrinterInfo2.printerModel = PrinterInfo.Model.QL_800;
        myPrinterInfo2.port = PrinterInfo.Port.USB;
        myPrinterInfo2.paperSize = PrinterInfo.PaperSize.CUSTOM;
        myPrinterInfo2.orientation = PrinterInfo.Orientation.PORTRAIT; //LANDSCAPE;
        myPrinterInfo2.valign = PrinterInfo.VAlign.MIDDLE;
        myPrinterInfo2.align = PrinterInfo.Align.CENTER;
        myPrinterInfo2.halftone = PrinterInfo.Halftone.THRESHOLD; //
        myPrinterInfo2.printMode = PrinterInfo.PrintMode.FIT_TO_PAPER; //ORIGINAL;
        myPrinterInfo2.numberOfCopies = 1;
        myPrinterInfo2.labelNameIndex = selectedPosition; // myPrinterInfo2.labelNameIndex = LabelInfo.QL700.W62RB.ordinal();
        myPrinterInfo2.isAutoCut = false; // true
        myPrinterInfo2.isCutAtEnd = false;
        myPrinterInfo2.isHalfCut = false;
        myPrinterInfo2.isSpecialTape = false;
        myPrinter2.setPrinterInfo(myPrinterInfo2);

        FrameLayout view = findViewById(R.id.frame_layout_ticket);
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        imageToPrint = view.getDrawingCache();
        print2();
    }

    private void print2() {
        PrinterThread printerThread = new PrinterThread();
        printerThread.start();
    }

    protected class PrinterThread extends Thread {

        @Override
        public void run() {
            printResult2 = new PrinterStatus();
            myPrinter2.startCommunication();
            printResult2 = myPrinter2.printImage(imageToPrint);

            if (printResult2.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {

                runOnUiThread(() -> {
                    binding.textViewErrorCode.setText("errorCode: " + printResult2.errorCode.name());
                        showToast("errorCode -> Alert Message : " + printResult2.errorCode.name(), true);
                });

            }

            myPrinter2.endCommunication();
        }
    }

    private void showToast(String text, boolean showToast) {
        runOnUiThread(() -> {
            Log.e("LOG_TAG", "Toast: " + text);
            binding.textViewErrorCode.setText(text);
            if (showToast) Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        });
    }

    //************************************//

    private List<LabelInfo.QL700> getLabels() {
        List<LabelInfo.QL700> list = new ArrayList<>();
        list.add(LabelInfo.QL700.W62H100);
        list.add(LabelInfo.QL700.W62);
        list.add(LabelInfo.QL700.W12);
        list.add(LabelInfo.QL700.W17H54);
        list.add(LabelInfo.QL700.W17H87);
        list.add(LabelInfo.QL700.W23H23);
        list.add(LabelInfo.QL700.W29);
        list.add(LabelInfo.QL700.W29H42);
        list.add(LabelInfo.QL700.W29H90);
        list.add(LabelInfo.QL700.W38);
        list.add(LabelInfo.QL700.W38H90);
        list.add(LabelInfo.QL700.W39H48);
        list.add(LabelInfo.QL700.W50);
        list.add(LabelInfo.QL700.W52H29);
        list.add(LabelInfo.QL700.W54);
        list.add(LabelInfo.QL700.W54H29);
        list.add(LabelInfo.QL700.W60H86);
        list.add(LabelInfo.QL700.W62H29);
        list.add(LabelInfo.QL700.W62RB);
        list.add(LabelInfo.QL700.UNSUPPORT);
        return list;
    }

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast("BroadcastReceiver -> onReceive", true);
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        showToast("USB permission granted", false);
                        setupAndPrint();
                    }
                    else {
                        if (myPrinter2 == null)
                            myPrinter2 = new Printer();

                        //************************************//
                        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                        if (usbManager == null) {
                            showToast("usbManager == null", true);
                            return;
                        }

                        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
                        if (usbDevice == null) {
                            showToast("usbDevice == null", true);
                            return;
                        }

                        PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(usbDevice, permissionIntent);

                        if (!usbManager.hasPermission(usbDevice)) {
                            usbManager.requestPermission(usbDevice, permissionIntent);
                        }
                        showToast("USB permission rejected", false);
                    }
                }
            }
        }
    };

    //************************************//

}
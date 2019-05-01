package com.sargis.kh.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.google.zxing.WriterException;
import com.sargis.kh.printer.databinding.ActivityBrotherMainBinding;

import java.util.Arrays;

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

    private static final String REQUEST_CODE_USB_PERMISSION = "REQUEST_CODE_USB_PERMISSION";
    private static final int REQUEST_CODE_PRINT = 1;
    private static final int REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE = 2;
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

        CustomArrayAdapter adapter = new CustomArrayAdapter(this, Arrays.asList(getQL700LabelList()));
        binding.spinner.setAdapter(adapter);
        binding.spinner.setSelection(adapter.getCount() - 3);
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = Arrays.asList(getQL700LabelList()).get(position).ordinal();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        setQRCodeImageView();
        imageToPrint = getBitmapFromResource();

        //************************************//

        binding.setOnPrintClick(v -> {
            printButtonClicked();
        });

        binding.setOnFindPrinterModelAndLabelTypeClick(v -> {
            findPrinterModel();
        });
    }

    public void printButtonClicked() {
        if (myPrinter2 == null) {
            myPrinter2 = new Printer();
        }

        if (hasUSBPermission(REQUEST_CODE_PRINT)) {
            setupAndPrintWithoutAskingPermission();
        }
    }

    private boolean hasUSBPermission(int request_code) {
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
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.putExtra(REQUEST_CODE_USB_PERMISSION, request_code);

            PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
            usbManager.requestPermission(usbDevice, permissionIntent);
            registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
            return false;
        }
        showToast("return *****************", true);

        return true;
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
            smallerDimension = smallerDimension * 3 / 4; // 3 / 4

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


    private Bitmap getBitmapFromResource() {
        Drawable drawable = getResources().getDrawable(R.drawable.ic_brother);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;

//        binding.frameLayoutTicket.setDrawingCacheEnabled(true);
//        binding.frameLayoutTicket.buildDrawingCache();
////        imageToPrint = binding.frameLayoutTicket.getDrawingCache();
//        return  binding.frameLayoutTicket.getDrawingCache();
    }

    protected class PrinterThread extends Thread {

        @Override
        public void run() {
            printResult2 = new PrinterStatus();
            boolean startCommunication = myPrinter2.startCommunication();
            showToast("run() : 1 startCommunication: " + startCommunication, true);
            Bitmap bitmap1 = imageToPrint.copy(imageToPrint.getConfig(), true);
            printResult2 = myPrinter2.printImage(bitmap1);
            showToast("run() : 2", true);

            if (printResult2.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
//                runOnUiThread
                showToast("errorCode -> Alert Message : " + printResult2.errorCode.name(), true);
            }
            boolean endCommunication = myPrinter2.endCommunication();
            showToast("run() : 3 : endCommunication: " + endCommunication, true);
        }
    }

    protected class FindPrinterModelAndLabelTypeThread extends Thread {

        @Override
        public void run() {

            PrinterInfo.Model printerModel;
            LabelInfo.QL700 labelType;

            if (myPrinter2 == null)
                myPrinter2 = new Printer();

            myPrinterInfo2 = new PrinterInfo();
            myPrinterInfo2 = myPrinter2.getPrinterInfo();
            myPrinterInfo2.port = PrinterInfo.Port.USB;

            for (PrinterInfo.Model model: getPrinterModelList()) {

                myPrinterInfo2.printerModel =  model;

                for (LabelInfo.QL700 ql700: getQL700LabelList()) {
                    myPrinterInfo2.labelNameIndex = ql700.ordinal();

                    myPrinter2.setPrinterInfo(myPrinterInfo2);

                    myPrinter2.startCommunication();
                    PrinterStatus printerStatus = myPrinter2.getPrinterStatus();
                    myPrinter2.endCommunication();
                    if (printerStatus.errorCode == PrinterInfo.ErrorCode.ERROR_NONE) {
                        printerModel = model;
                        labelType = ql700;
                        showToast("printerModel : " + printerModel + " * " +  "labelType: " + labelType, false);
                        return;
                    }
                }
            }
            Log.e("LOG_TAG", "NOT FOUND");
        }
    }



    private void showToast(String text, boolean showToast) {
        runOnUiThread(() -> {
            Log.e("LOG_TAG", "*** " + text);
            binding.textViewErrorCode.setText(text);
            if (showToast) Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        });
    }


    private void findPrinterModel() {
        if (myPrinter2 == null) {
            myPrinter2 = new Printer();
        }

        if (hasUSBPermission(REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE)) {
            FindPrinterModelAndLabelTypeThread findPrinterModelAndLabelTypeThread = new FindPrinterModelAndLabelTypeThread();
            findPrinterModelAndLabelTypeThread.start();
        }
    }


    //************************************//
    private LabelInfo.QL700[] getQL700LabelList() {
        return LabelInfo.QL700.values();
    }

    private PrinterInfo.Model[] getPrinterModelList() {
        return PrinterInfo.Model.values();
    }
    //************************************//



    //************************************//
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
    //TODO

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast(" ********* BroadcastReceiver -> onReceive", true);

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        switch (intent.getIntExtra(REQUEST_CODE_USB_PERMISSION, 0)) {
                            case REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE:
                                findPrinterModel();
                                break;
                            case REQUEST_CODE_PRINT:
                                setupAndPrintWithoutAskingPermission();
                                break;
                        }
                    }
                    else {
                        if (myPrinter2 == null) {
                            myPrinter2 = new Printer();
                        }

                        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                        if (usbManager == null) {
                            return;
                        }

                        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
                        if (usbDevice == null) {
                            return;
                        }

                        PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(usbDevice, permissionIntent);

                        usbManager.requestPermission(usbDevice, permissionIntent);
                    }
                }
            }
        }
    };
    //************************************//

    private void setupAndPrintWithoutAskingPermission() {
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

        PrinterThread printerThread = new PrinterThread();
        printerThread.start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
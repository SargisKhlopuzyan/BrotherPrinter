package com.sargis.kh.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;

import java.util.ArrayList;
import java.util.List;

public class BrotherMainActivity extends AppCompatActivity {

    public static Printer myPrinter2;
    public static Bitmap imageToPrint;
    protected PrinterStatus printResult2;
    protected PrinterInfo myPrinterInfo2;

    EditText fullName;
    EditText company;
    EditText position;
    ImageView imageView;

    Spinner spinner;
    TextView textViewSelected;
    TextView textViewErrorCode;

    int selectedPosition = 0;


    //************************************//
    //TODO
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //************************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brother_main);

        fullName = findViewById(R.id.editTextFullName);
        company = findViewById(R.id.editTextCompany);
        position = findViewById(R.id.editTextPosition);
        imageView  = findViewById(R.id.imageView);

        //************************************//

        spinner = findViewById(R.id.spinner);
        textViewSelected = findViewById(R.id.textViewSelected);
        textViewErrorCode = findViewById(R.id.textViewErrorCode);

        CustomArrayAdapter adapter = new CustomArrayAdapter(this, getLabels());
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = getLabels().get(position).ordinal();
                textViewSelected.setText("" + selectedPosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        //************************************//
    }

    public Bitmap textAsBitmap(String text, String text1, String text2, float textSize, int textColor) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);

        float baseLine = -paint.ascent();
        int width = (int)(paint.measureText(text) + 0.5f);
        int height = (int)(baseLine + paint.descent() + 0.5f);

//        Bitmap image = Bitmap.createBitmap(width + 500, height + 350, Bitmap.Config.ARGB_8888);
        Bitmap image = Bitmap.createBitmap(width + 50, height + 250, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
//        canvas.drawRect(0,0,width + 500,height + 350, paint);
        canvas.drawRect(0,0,width + 50,height + 250, paint);
        paint.setColor(textColor);
        canvas.drawText(text, 0, baseLine, paint);
        canvas.drawText(text1, 0, baseLine + 100, paint);
        canvas.drawText(text2, 0, baseLine + 200, paint);
        return image;
    }

    public void onButtonPreviewClick(View v) {
        imageView.setImageBitmap(textAsBitmap(fullName.getText().toString(), company.getText().toString(), position.getText().toString(), 36, Color.BLACK));
    }

    public void onButtonPrintClick(View v) {

        myPrinter2 = new Printer();

        //************************************//
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            showToast("usbManager == null");
            return;
        }

        UsbDevice usbDevice = myPrinter2.getUsbDevice(usbManager);
        if (usbDevice == null) {
            showToast("usbDevice == null");
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, permissionIntent);
        registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

        while (true) {
            if (!usbManager.hasPermission(usbDevice)) {
                usbManager.requestPermission(usbDevice, permissionIntent);
            } else {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                showToast("InterruptedException: " + e.getMessage());
            }
        }
        //************************************//

        myPrinterInfo2 = new PrinterInfo();
        myPrinterInfo2 = myPrinter2.getPrinterInfo();
        myPrinterInfo2.printerModel = PrinterInfo.Model.QL_800;
        myPrinterInfo2.port = PrinterInfo.Port.USB;
        myPrinterInfo2.paperSize = PrinterInfo.PaperSize.CUSTOM;
        myPrinterInfo2.orientation = PrinterInfo.Orientation.LANDSCAPE;
        myPrinterInfo2.valign = PrinterInfo.VAlign.MIDDLE;
        myPrinterInfo2.align = PrinterInfo.Align.CENTER;
        myPrinterInfo2.halftone = PrinterInfo.Halftone.THRESHOLD; //
        myPrinterInfo2.printMode = PrinterInfo.PrintMode.ORIGINAL;
        myPrinterInfo2.numberOfCopies = 1;
        myPrinterInfo2.labelNameIndex = selectedPosition; // myPrinterInfo2.labelNameIndex = LabelInfo.QL700.W62RB.ordinal();
        myPrinterInfo2.isAutoCut = true;
        myPrinterInfo2.isCutAtEnd = false;
        myPrinterInfo2.isHalfCut = false;
        myPrinterInfo2.isSpecialTape = false;

        myPrinter2.setPrinterInfo(myPrinterInfo2);

        imageToPrint = textAsBitmap(fullName.getText().toString(), company.getText().toString(), position.getText().toString(), 96, Color.BLACK);
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
                //TODO - Alert Message
                textViewErrorCode.setText("errorCode: " + printResult2.errorCode.name());
                showToast("errorCode: " + printResult2.errorCode.name());
            }

            myPrinter2.endCommunication();
        }
    }

    private void showToast(String text) {
        runOnUiThread(() -> {
            Log.e("LOG_TAG", "Toast: " + text);
            textViewErrorCode.setText(text);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
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
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        showToast("USB permission granted");
                    else
                        showToast("USB permission rejected");
                }
            }
        }
    };

    public void onTestClick(View view) {
    }

    //************************************//

}
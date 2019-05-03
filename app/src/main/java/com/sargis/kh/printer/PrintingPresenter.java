package com.sargis.kh.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;

import java.util.ArrayList;

public class PrintingPresenter implements PrinterContract.Presenter {

    private Context context;
    private PrinterContract.View viewCallback;

    PrinterInfo.Model printerModel;
    LabelInfo.QL700 labelType;

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public static final String REQUEST_CODE_USB_PERMISSION = "REQUEST_CODE_USB_PERMISSION";
    public static final int REQUEST_CODE_PRINT = 1;
    public static final int REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE = 2;

    public static Printer myPrinter2;
    public static Bitmap imageToPrint;
    protected PrinterStatus printResult2;
    protected PrinterInfo myPrinterInfo2;

    public PrintingPresenter(Context context, PrinterContract.View viewCallback) {
        this.context = context;
        this.viewCallback = viewCallback;

        printerModel = PrinterInfo.Model.QL_800;
        labelType = LabelInfo.QL700.W62RB;
        imageToPrint = getBitmapFromResource();

        showToast(" ********* PrintingPresenter");

    }

    @Override
    public void startPrinting() {
        if (myPrinter2 == null) myPrinter2 = new Printer();
        if (viewCallback.hasUSBPermission(myPrinter2, REQUEST_CODE_PRINT)) {
            setupAndPrintWithoutAskingPermission();
        }
    }

    @Override
    public void updatePrinterSettings() {
        if (myPrinter2 == null) myPrinter2 = new Printer();
        if (viewCallback.hasUSBPermission(myPrinter2, REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE)) {
            FindPrinterModelAndLabelTypeThread findPrinterModelAndLabelTypeThread = new FindPrinterModelAndLabelTypeThread();
            findPrinterModelAndLabelTypeThread.start();
        }
    }

    public void setupAndPrintWithoutAskingPermission() {
        if (myPrinter2 == null) {
            myPrinter2 = new Printer();
        }
        myPrinterInfo2 = new PrinterInfo();
        myPrinterInfo2 = myPrinter2.getPrinterInfo();
        myPrinterInfo2.printerModel = printerModel;
        myPrinterInfo2.port = PrinterInfo.Port.USB;
        myPrinterInfo2.paperSize = PrinterInfo.PaperSize.CUSTOM;
        myPrinterInfo2.orientation = PrinterInfo.Orientation.PORTRAIT; //LANDSCAPE;
        myPrinterInfo2.valign = PrinterInfo.VAlign.MIDDLE;
        myPrinterInfo2.align = PrinterInfo.Align.CENTER;
        myPrinterInfo2.halftone = PrinterInfo.Halftone.THRESHOLD; //
        myPrinterInfo2.printMode = PrinterInfo.PrintMode.FIT_TO_PAPER; //ORIGINAL;
        myPrinterInfo2.numberOfCopies = 1;
        myPrinterInfo2.labelNameIndex = labelType.ordinal(); //myPrinterInfo2.labelNameIndex = LabelInfo.QL700.W62RB.ordinal();
        myPrinterInfo2.isAutoCut = false; // true
        myPrinterInfo2.isCutAtEnd = false;
        myPrinterInfo2.isHalfCut = false;
        myPrinterInfo2.isSpecialTape = false;
        myPrinter2.setPrinterInfo(myPrinterInfo2);

        PrinterThread printerThread = new PrinterThread();
        printerThread.start();
    }

    protected class PrinterThread extends Thread {

        @Override
        public void run() {
            printResult2 = new PrinterStatus();
            boolean startCommunication = myPrinter2.startCommunication();
            showToast("run() : 1 startCommunication: " + startCommunication);
            Bitmap bitmap1 = imageToPrint.copy(imageToPrint.getConfig(), true);
            printResult2 = myPrinter2.printImage(bitmap1);
            showToast("run() : 2");

            if (printResult2.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
//                runOnUiThread
                showToast("errorCode -> Alert Message : " + printResult2.errorCode.name());
            }
            boolean endCommunication = myPrinter2.endCommunication();
            showToast("run() : 3 : endCommunication: " + endCommunication);
        }
    }

    protected class FindPrinterModelAndLabelTypeThread extends Thread {

        @Override
        public void run() {

            if (myPrinter2 == null)
                myPrinter2 = new Printer();

            myPrinterInfo2 = new PrinterInfo();
            myPrinterInfo2 = myPrinter2.getPrinterInfo();
            myPrinterInfo2.port = PrinterInfo.Port.USB;

            for (PrinterInfo.Model model: getPrinterModelList()) {

                myPrinterInfo2.printerModel =  model;

                for (LabelInfo.QL700 ql700: LabelInfo.QL700.values()) {
                    myPrinterInfo2.labelNameIndex = ql700.ordinal();

                    myPrinter2.setPrinterInfo(myPrinterInfo2);

                    myPrinter2.startCommunication();
                    PrinterStatus printerStatus = myPrinter2.getPrinterStatus();
                    myPrinter2.endCommunication();
                    if (printerStatus.errorCode == PrinterInfo.ErrorCode.ERROR_NONE) {
                        printerModel = model;
                        labelType = ql700;
                        showToast("printerModel : " + printerModel + " * " +  "labelType: " + labelType);
                        return;
                    }
                }
            }
            Log.e("LOG_TAG", "NOT FOUND");
        }
    }

    public void findPrinterModel() {
        if (myPrinter2 == null) myPrinter2 = new Printer();
        //TODO
        if (viewCallback.hasUSBPermission(myPrinter2, REQUEST_CODE_GET_PRINTER_MODEL_AND_LABEL_TYPE)) {
            FindPrinterModelAndLabelTypeThread findPrinterModelAndLabelTypeThread = new FindPrinterModelAndLabelTypeThread();
            findPrinterModelAndLabelTypeThread.start();
        }
    }

    private ArrayList<PrinterInfo.Model> getPrinterModelList() {
        ArrayList<PrinterInfo.Model> models = new ArrayList<PrinterInfo.Model>();
        for (PrinterInfo.Model model : PrinterInfo.Model.values()) {
            if (model.name().startsWith("QL"))
                models.add(model);
        }
        return models;
    }

    private Bitmap getBitmapFromResource() {

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_brother);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;

    }

    private void showToast(String text) {
        Log.e("LOG_TAG", "*** " + text);
    }

}
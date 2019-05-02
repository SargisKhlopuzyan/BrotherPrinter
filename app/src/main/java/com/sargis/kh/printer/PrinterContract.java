package com.sargis.kh.printer;

import com.brother.ptouch.sdk.Printer;

public interface PrinterContract {

    interface View {
        void printingStatusUpdated(String status);
        void printingSucceed();
        void printingError(String errorMessage);

        boolean hasUSBPermission(Printer printer, int requestCode);
    }

    interface Presenter {
        void startPrinting();
        void updatePrinterSettings();
    }

}

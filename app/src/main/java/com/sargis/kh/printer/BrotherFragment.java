package com.sargis.kh.printer;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.brother.ptouch.sdk.Printer;
import com.sargis.kh.printer.databinding.ActivityBrotherMainBinding;
import com.sargis.kh.printer.databinding.FragmentBrotherBinding;

public class BrotherFragment extends Fragment implements PrinterContract.View {

    FragmentBrotherBinding binding;

    PrintingPresenter printingPresenter;


    public BrotherFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_brother, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        printingPresenter = new PrintingPresenter(getContext(), this);

        binding.setOnPrintClick(v -> {
            printingPresenter.startPrinting();
        });

        binding.setOnFindPrinterModelAndLabelTypeClick(v -> {
            printingPresenter.findPrinterModel();
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }





    @Override
    public void printingStatusUpdated(String status) {

    }

    @Override
    public void printingSucceed() {

    }

    @Override
    public void printingError(String errorMessage) {

    }

    @Override
    public boolean hasUSBPermission(Printer printer, int requestAction) {
        UsbManager usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            showToast(" xxxxxxx usbManager == null");
            return false;
        }

        UsbDevice usbDevice = printer.getUsbDevice(usbManager);
        if (usbDevice == null) {
            showToast(" xxxxxxx usbDevice == null");
            return false;
        }

        if (!usbManager.hasPermission(usbDevice)) {
            showToast(" xxxxxxx if (!usbManager.hasPermission(usbDevice)) { **");

            ((BrotherMainActivity)getActivity()).hasUSBPermission(printer, usbManager, usbDevice, requestAction);

            //TODO
//            Bundle payload = new Bundle();
//            payload.putInt(ParkingConstants.PayloadKey.REQUEST_CODE_USB_PERMISSION, requestAction);
//            EventBus.getDefault().post(new Action(ParkingConstants.ActionKey.GET_USB_PERMISSION, payload));
            return false;
        }
        showToast("return *****************");

        return true;
    }

    private void showToast(String text) {
//        runOnUiThread(() -> {
        Log.e("LOG_TAG", "*** " + text);
//            binding.textViewErrorCode.setText(text);
//            if (showToast) Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
//        });
    }
}

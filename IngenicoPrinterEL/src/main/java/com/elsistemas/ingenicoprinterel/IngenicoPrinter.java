package com.elsistemas.ingenicoprinterel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.usdk.apiservice.aidl.vectorprinter.Alignment;
import com.usdk.apiservice.aidl.vectorprinter.OnPrintListener;
import com.usdk.apiservice.aidl.vectorprinter.TextSize;
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter;
import com.usdk.apiservice.aidl.vectorprinter.VectorPrinterData;

public class IngenicoPrinter {
    private final Context context;
    private DeviceHelper deviceHelper = DeviceHelper.me();
    private UVectorPrinter vectorPrinter;

    public IngenicoPrinter(Context context) {
        this.context = context;

        deviceHelper.init(context);
        deviceHelper.bindService();
    }

    public void register() throws RemoteException {
        deviceHelper.register(true);

        vectorPrinter = getVectorPrinter();

        Bundle initFormat = new Bundle();
        initFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0);
        initFormat.putBoolean(VectorPrinterData.AUTO_CUT_PAPER, true);

        vectorPrinter.init(initFormat);
    }

    public void unRegister(){
        deviceHelper.unregister();

        vectorPrinter = null;
    }

    private UVectorPrinter getVectorPrinter(){
        return deviceHelper.getVectorPrinter();
    }

    public void addText(String text) throws RemoteException {
        vectorPrinter.addText(null, text);

        vectorPrinter.feedPix(50);
    }

    public void addTextByte(byte[] textByte) throws RemoteException {
        String text = new String(textByte);

        vectorPrinter.addText(null, text);

        vectorPrinter.feedPix(50);
    }

    public void addNewLine(int count) throws RemoteException {
        vectorPrinter.feedPix(50*count);
    }

    public void addImage(byte[] bmp) throws RemoteException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);

        vectorPrinter.addImage(null, bitmap);

        vectorPrinter.feedPix(50);
    }

    public void startPrint() throws RemoteException {
        vectorPrinter.startPrint(new OnPrintListener.Stub() {
            @Override
            public void onFinish() throws RemoteException {
                System.out.println("time cost = + " + (System.currentTimeMillis()));
            }

            @Override
            public void onStart() throws RemoteException {
                Log.d("OnStart", "=> onStart | sheetNo = ");
            }

            @Override
            public void onError(int error, String errorMsg) throws RemoteException {
                Log.d("onError", "=> onError: " + errorMsg);
            }
        });
    }
}

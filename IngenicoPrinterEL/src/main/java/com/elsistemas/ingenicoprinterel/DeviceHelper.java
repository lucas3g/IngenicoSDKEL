package com.elsistemas.ingenicoprinterel;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.usdk.apiservice.aidl.DeviceServiceData;
import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter;
import com.usdk.apiservice.limited.DeviceServiceLimited;

/**
 * The class of device service auxiliary,
 * implements the connection with the equipment service
 * and provides the interface for accessing each device.
 *
 */
public final class DeviceHelper implements ServiceConnection {
	private static final String TAG = "DeviceHelper";
	// The maximum number of rebinds
	private static final int MAX_RETRY_COUNT = 3;
	// Rebinding interval time
	private static final long RETRY_INTERVALS = 3000;

	private static DeviceHelper me = new DeviceHelper();

	private Context context;
	private ServiceReadyListener serviceListener;

	private int retry = 0;
	private volatile boolean isBinded = false;
	private UDeviceService deviceService;

	public static DeviceHelper me() {
		return me;
	}

	public void init(Context context) {
		this.context = context;
	}

	public void setServiceListener(ServiceReadyListener listener) {
		serviceListener = listener;
		if (isBinded) {
			notifyReady();
		}
	}

	public void bindService() {
		if (isBinded) {
			return;
		}

		Intent service = new Intent("com.usdk.apiservice");
		service.setPackage("com.usdk.apiservice");
		boolean bindSucc = context.bindService(service, me, Context.BIND_AUTO_CREATE);

		// If the binding fails, it is rebinded
		if (!bindSucc && retry++ < MAX_RETRY_COUNT) {
			Log.e(TAG, "=> bind fail, rebind (" + retry +")");
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					bindService();
				}
			}, RETRY_INTERVALS);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.d(TAG, "=> onServiceConnected");

		retry = 0;
		isBinded = true;

		deviceService = UDeviceService.Stub.asInterface(service);

		DeviceServiceLimited.bind(context, deviceService, new DeviceServiceLimited.ServiceBindListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "=> DeviceServiceLimited | bindSuccess");
			}

			@Override
			public void onFail() {
				Log.e(TAG, "=> bind DeviceServiceLimited fail");
			}
		});

		notifyReady();
	}

	private void notifyReady() {
		if (serviceListener != null) {
			try {
				serviceListener.onReady(deviceService.getVersion());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.e(TAG, "=> onServiceDisconnected");

		deviceService = null;
		isBinded = false;
		DeviceServiceLimited.unbind(context);
		bindService();
	}

	public void debugLog(boolean open) {
		try {
			Bundle logOption = new Bundle();
			logOption.putBoolean(DeviceServiceData.COMMON_LOG, open);
			logOption.putBoolean(DeviceServiceData.MASTERCONTROL_LOG, open);
			deviceService.debugLog(logOption);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void register(boolean useEpayModule) throws IllegalStateException {
		try {
			Bundle param = new Bundle();
			param.putBoolean(DeviceServiceData.USE_EPAY_MODULE, useEpayModule);
			deviceService.register(param, new Binder());
		} catch (RemoteException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void unregister() throws IllegalStateException {
		try {
			deviceService.unregister(null);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}

	public UVectorPrinter getVectorPrinter() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getVectorPrinter();
			}
		}.start();
		return UVectorPrinter.Stub.asInterface(iBinder);
	}

    abstract class IBinderCreator {
		IBinder start() throws IllegalStateException {
			if (deviceService == null) {
				bindService();
				throw new IllegalStateException("Servic unbound,please retry latter!");
			}
			try {
				return create();

			} catch (DeadObjectException e) {
				deviceService = null;
				throw new IllegalStateException("Service process has stopped,please retry latter!");

			} catch (RemoteException | SecurityException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		abstract IBinder create() throws RemoteException;
	}

	public interface ServiceReadyListener {
		void onReady(String version);
	}

}

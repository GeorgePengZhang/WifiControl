package com.pachong.wificontrol.util;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiAPEnabler {

	private static String TAG = "WifiAPEnabler";
	
	public static final int OPEN_INDEX = 0;
	public static final int WPA_INDEX = 1;
	public static final int WPA2_INDEX = 2;
	
	private Context mContext;
	private WifiManager mWifiManager;
	private IntentFilter mIntentFilter;
	
	private OnWifiAPListener mOnWifiAPListener;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (MyWifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
				handleWifiApStateChanged(intent.getIntExtra(
						MyWifiManager.EXTRA_WIFI_AP_STATE,
						MyWifiManager.WIFI_AP_STATE_FAILED));
			} else if (MyWifiManager.ACTION_TETHER_STATE_CHANGED
					.equals(action)) {
				ArrayList<String> available = intent
						.getStringArrayListExtra(MyWifiManager.EXTRA_AVAILABLE_TETHER);
				ArrayList<String> active = intent
						.getStringArrayListExtra(MyWifiManager.EXTRA_ACTIVE_TETHER);
				ArrayList<String> errored = intent
						.getStringArrayListExtra(MyWifiManager.EXTRA_ERRORED_TETHER);
				// updateTetherState(available.toArray(), active.toArray(),
				// errored.toArray());
				printMsg("available", available);
				printMsg("active", active);
				printMsg("errored", errored);
			} else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
				// enableWifiCheckBox();
			}
			Log.d(TAG, "onReceive:" + action);
		}
	};

	private void printMsg(String tag, ArrayList<String> list) {
		for (String msg : list) {
			Log.d(TAG, "printMsg:"+tag+",msg:" + msg+",length:"+list.size());
		}
	}

	public WifiAPEnabler(Context context) {
		mContext = context;

		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		mIntentFilter = new IntentFilter(MyWifiManager.WIFI_AP_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(MyWifiManager.ACTION_TETHER_STATE_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	}

	public void resume() {
		mContext.registerReceiver(mReceiver, mIntentFilter);
	}

	public void pause() {
		mContext.unregisterReceiver(mReceiver);
	}
	
	private void handleWifiApStateChanged(int state) {
		Log.d(TAG, "handleWifiApStateChanged:" + state);
		mOnWifiAPListener.onWifiAPStateChanged(state);
		switch (state) {
		case MyWifiManager.WIFI_AP_STATE_ENABLING:
			break;
		case MyWifiManager.WIFI_AP_STATE_ENABLED:
//			UDPService service = new UDPService(mContext);
//			new Thread(service).start();
			break;
		case MyWifiManager.WIFI_AP_STATE_DISABLING:
			break;
		case MyWifiManager.WIFI_AP_STATE_DISABLED:
			break;
		default:
		}
	}

	/**
	 * 是否打开热点
	 * @param enable true打开，false关闭
	 * @return
	 */
	public boolean setSoftAPEnabled(boolean enable) {
		boolean result = false;
		
		int wifiState = mWifiManager.getWifiState();
		if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
			mWifiManager.setWifiEnabled(false);
		}

		result = MyWifiManager.getInstance(mContext).setWifiApEnabled(null, enable);
			
		if (!enable) {
			mWifiManager.setWifiEnabled(true);
		}
		
		return result;
	}
	
	/**
	 * 创建wifi热点
	 * @param ssid 热点名称
	 * @param pwd 热点密码
	 * @param type 热点安全模式
	 */
	public void createWifiAP(String ssid, String pwd, int type) {
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = ssid;

		switch (type) {
		case OPEN_INDEX:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			break;
		case WPA_INDEX:
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.preSharedKey = pwd;
			break;
		case WPA2_INDEX:
			config.allowedKeyManagement.set(MyWifiManager.WPA2_PSK);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.preSharedKey = pwd;
			break;

		default:
			break;
		}

		int wifiState = mWifiManager.getWifiState();
		if ((wifiState == WifiManager.WIFI_STATE_ENABLING)
				|| (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
			mWifiManager.setWifiEnabled(false);
		}

		if (MyWifiManager.getInstance(mContext).getWifiApState() == MyWifiManager.WIFI_AP_STATE_ENABLED) {
			MyWifiManager.getInstance(mContext).setWifiApEnabled(null, false);
			MyWifiManager.getInstance(mContext).setWifiApEnabled(config, true);
		} else {
			MyWifiManager.getInstance(mContext).setWifiApEnabled(config, true);
		}
	}
	
	public void setOnWifiAPListener(OnWifiAPListener onWifiAPListener) {
		mOnWifiAPListener = onWifiAPListener;
	}
	
	public interface OnWifiAPListener {
		public void onWifiAPStateChanged(int state);
	}
}

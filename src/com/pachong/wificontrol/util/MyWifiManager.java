package com.pachong.wificontrol.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class MyWifiManager {

	public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;
    
    public static final int WPA2_PSK = 4;
	
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
	public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
	public static final String EXTRA_ACTIVE_TETHER = "activeArray";
	public static final String EXTRA_ERRORED_TETHER = "erroredArray";
	
	private static volatile MyWifiManager mInstance;
	private WifiManager mWifiManager;
	
	public MyWifiManager(Context context) {
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}

	public static MyWifiManager getInstance(Context context) {
		if (mInstance == null) {
			synchronized (MyWifiManager.class) {
				if (mInstance == null) {
					mInstance = new MyWifiManager(context.getApplicationContext());
				}
			}
		}
		
		return mInstance;
	}

	public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
		boolean result = false;
		try {
			Method method = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			result = (Boolean) method.invoke(mWifiManager, wifiConfig, enabled);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public int getWifiApState() {
		int result = WIFI_AP_STATE_FAILED;
		
		try {
			Method method = WifiManager.class.getMethod("getWifiApState");
			result = (Integer) method.invoke(mWifiManager);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean isWifiApEnabled() {
		boolean result = false;
		
	    try {
	        Method method = WifiManager.class.getMethod("isWifiApEnabled");
	        result = (Boolean)method.invoke(mWifiManager);
	    } catch (NoSuchMethodException e) {
	        e.printStackTrace();
	    } catch (IllegalAccessException e) {
	        e.printStackTrace();
	    } catch (IllegalArgumentException e) {
	    	e.printStackTrace();
	    } catch (InvocationTargetException e) {
	    	e.printStackTrace();
	    }
	    return result;
	}
}

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pachong.wificontrol.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.pachong.wificontrol.R;

public class WifiEnabler {
	public static final int SECURITY_NONE = 0;
	public static final int SECURITY_WEP = 1;
	public static final int SECURITY_PSK = 2;
	public static final int SECURITY_EAP = 3;

    private final Context mContext;
    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private final WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;
	
	private WifiConfiguration config;
	private OnWifiListener mOnWifiListener;
	
	public enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
		@Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            	if (!mConnected.get()) {
            		DetailedState state = WifiInfo.getDetailedStateOf((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
            		boolean hasExtra = intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR);
                	int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            		
            		handleStateChanged(state, hasExtra, error);
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(networkInfo.isConnected());
//                handleStateChanged(networkInfo.getDetailedState());
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            	handleScanResults();
            } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
//            	handleStateChanged(null);
//            	int rssi = intent.getParcelableExtra(WifiManager.EXTRA_NEW_RSSI);
//            	Log.d("TAG", "RSSI_CHANGED_ACTION rssi:"+rssi);
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            	NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            	if (networkInfo != null) {
            		if (NetworkInfo.State.CONNECTED == networkInfo.getState()) {
            			
            		} else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            			
            		}
            	}
            } else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
            }
        }
    };

    public WifiEnabler(Context context) {
        mContext = context;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
    }

	public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
    }
    
    public boolean startScan() {
    	return mWifiManager.startScan();
    }
    
    public boolean setWifiEnable(boolean enable) {
    	boolean result = false;
    	
        int wifiApState = MyWifiManager.getInstance(mContext).getWifiApState();
        if (enable && ((wifiApState == MyWifiManager.WIFI_AP_STATE_ENABLING) ||
                (wifiApState == MyWifiManager.WIFI_AP_STATE_ENABLED))) {
        	MyWifiManager.getInstance(mContext).setWifiApEnabled(null, false);
        }

        boolean wifiEnabled = mWifiManager.isWifiEnabled();
        if (wifiEnabled != enable) {
        	result = mWifiManager.setWifiEnabled(enable);
        } else {
        	result = true;
        }
        
        return result;
    }
    
    public void removeOldWifiConfig() {
    	List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
    	if (configuredNetworks != null) {
    		for (Iterator<WifiConfiguration> iterator = configuredNetworks.iterator(); iterator.hasNext();) {
    			WifiConfiguration config = (WifiConfiguration) iterator.next();
    			Log.d("TAG", "isExistWifi:"+config.SSID);
    			mWifiManager.removeNetwork(config.networkId);
    		}
    	}
    }
    
    public boolean connectWifi(int securityType, String ssid, String password) {
    	boolean result = false;
    	
    	config = getConfig(securityType, ssid, password);
    	int network = mWifiManager.addNetwork(config);
    	Log.d("TAG", "connectWifi:"+network);
    	if (network >= 0) {
    		result = mWifiManager.enableNetwork(network, true);
    		//保存连接配置，当开关wifi时自动连接上次保存的wifi配置
    		mWifiManager.saveConfiguration();
    	} 
    	
    	return result;
    }
    
    private WifiConfiguration getConfig(int securityType, String ssid, String password) {
    	WifiConfiguration config = new WifiConfiguration();
        
        config.SSID = "\"" + ssid + "\"";
        config.hiddenSSID = true;

        switch (securityType) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (password != null && password.length() != 0) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (password != null && password.length() != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;
            default:
                return null;
        }
        
        return config;
    }
    
    public SupplicantState getSupplicantState() {
    	SupplicantState supplicantState = mWifiManager.getConnectionInfo().getSupplicantState();
    	return supplicantState;
    }
    
    public String getCurrentConnectWifiSSID() {
    	return mWifiManager.getConnectionInfo().getSSID();
    }
    
    private void handleWifiStateChanged(int state) {
    	mOnWifiListener.onWifiStateChanged(state);
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
            	break;
            case WifiManager.WIFI_STATE_ENABLED:
                break;
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_DISABLED:
                break;
            default:
                break;
        }
    }

    private void handleStateChanged(NetworkInfo.DetailedState state, boolean hasError, int error) {
    	mOnWifiListener.onWifiSupplicantStateChanged(state, hasError, error);
    }
    
    protected void handleScanResults() {
    	List<ScanResult> scanResults = mWifiManager.getScanResults();
    	List<ScanResult> results = new ArrayList<ScanResult>();
    	
    	for (ScanResult scanResult : scanResults) {
    		int security = getSecurity(scanResult);
    		if (security == SECURITY_EAP) {
    			continue;
    		}
    		results.add(scanResult);
		}
	}
    
    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }
    
    public static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            return PskType.UNKNOWN;
        }
    }
    
    public static String getSecurityString(Context context, int security, PskType pskType, boolean concise) {
        switch(security) {
            case SECURITY_EAP:
                return concise ? context.getString(R.string.wifi_security_short_eap) :
                    context.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        return concise ? context.getString(R.string.wifi_security_short_wpa) :
                            context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) :
                            context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) :
                            context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return concise ? context.getString(R.string.wifi_security_short_psk_generic)
                                : context.getString(R.string.wifi_security_psk_generic);
                }
            case SECURITY_WEP:
                return concise ? context.getString(R.string.wifi_security_short_wep) :
                    context.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
            default:
                return concise ? "" : context.getString(R.string.wifi_security_none);
        }
    }
    
    public void setOnWifiListener(OnWifiListener onWifiListener) {
    	mOnWifiListener = onWifiListener;
    }
    
    public interface OnWifiListener {
    	public void onWifiSupplicantStateChanged(DetailedState state, boolean hasError, int error);
    	public void onWifiStateChanged(int state);
    }
}

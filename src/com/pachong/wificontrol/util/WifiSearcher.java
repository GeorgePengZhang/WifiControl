package com.pachong.wificontrol.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiSearcher {

	private static final int WIFI_SEARCH_TIMEOUT = 20; // ɨ��WIFI�ĳ�ʱʱ��

	private Context mContext;
	private WifiManager mWifiManager;
	private WiFiScanReceiver mWifiReceiver;
	private Lock mLock;
	private Condition mCondition;
	private SearchWifiListener mSearchWifiListener;
	private boolean mIsWifiScanCompleted = false;

	private WifiEnabler mWifiEnabler;

	public static enum ErrorType {
		SEARCH_WIFI_TIMEOUT, NO_WIFI_FOUND,
	}

	public interface SearchWifiListener {
		public void onSearchWifiFailed(ErrorType errorType);

		public void onSearchWifiSuccess(List<ScanResult> results);
	}

	public WifiSearcher(Context context, SearchWifiListener listener) {

		mContext = context;
		mSearchWifiListener = listener;

		mLock = new ReentrantLock();
		mCondition = mLock.newCondition();

		mWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);

		mWifiEnabler = new WifiEnabler(mContext);
		
		mWifiReceiver = new WiFiScanReceiver();
	}

	public void search() {

		new Thread(new Runnable() {

			@Override
			public void run() {

				// ���WIFIû�д򿪣����WIFI
				mWifiEnabler.setWifiEnable(true);

				// ע�����WIFIɨ�����ļ��������
				mContext.registerReceiver(mWifiReceiver, new IntentFilter(
						WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				// ��ʼɨ��
				mWifiManager.startScan();

				mLock.lock();
				// �����ȴ�ɨ����
				try {
					mIsWifiScanCompleted = false;
					mCondition.await(WIFI_SEARCH_TIMEOUT, TimeUnit.SECONDS);
					if (!mIsWifiScanCompleted) {
						mSearchWifiListener
								.onSearchWifiFailed(ErrorType.SEARCH_WIFI_TIMEOUT);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mLock.unlock();

				try {
					// ɾ��ע��ļ��������
					mContext.unregisterReceiver(mWifiReceiver);
				} catch (Exception e) {
					
				}
			}
		}).start();
	}

	protected class WiFiScanReceiver extends BroadcastReceiver {

		public void onReceive(Context c, Intent intent) {

			// ��ȡɨ����
			List<ScanResult> scanResults = mWifiManager.getScanResults();

			// ���ɨ����
			if (scanResults.isEmpty()) {
				mSearchWifiListener.onSearchWifiFailed(ErrorType.NO_WIFI_FOUND);
			} else {
				mSearchWifiListener.onSearchWifiSuccess(scanResults);
			}

			mLock.lock();
			mIsWifiScanCompleted = true;
			mCondition.signalAll();
			mLock.unlock();
		}
	}
}
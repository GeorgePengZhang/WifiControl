package com.pachong.wificontrol.adapter;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pachong.wificontrol.R;
import com.pachong.wificontrol.util.WifiEnabler;
import com.pachong.wificontrol.util.WifiEnabler.PskType;

public class WifiAdapter extends BaseAdapter {
	
	
	private List<ScanResult> mWifiList;
	private Context mContext;

	public WifiAdapter(Context context, List<ScanResult> wifiList) {
		mContext = context;
		mWifiList = wifiList;
	}

	@Override
	public int getCount() {
		return mWifiList.size();
	}

	@Override
	public Object getItem(int position) {
		return mWifiList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHold viewHold = null;
		
		if (convertView == null) {
			viewHold = new ViewHold();
			convertView = View.inflate(mContext, R.layout.wifi_item_layout, null);
			convertView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 64));
			viewHold.ssid = (TextView) convertView.findViewById(R.id.id_ssid);
			viewHold.secured = (TextView) convertView.findViewById(R.id.id_secured);
			viewHold.signal = (ImageView) convertView.findViewById(R.id.id_signal);
			
			convertView.setTag(viewHold);
		} else {
			viewHold = (ViewHold) convertView.getTag();
		}
		
		ScanResult scanResult = mWifiList.get(position);
		
		String ssid = scanResult.SSID;
		int security = WifiEnabler.getSecurity(scanResult);
		boolean wpsAvailable = security != WifiEnabler.SECURITY_EAP && scanResult.capabilities.contains("WPS");
		PskType pskType = WifiEnabler.getPskType(scanResult);
		String secured = WifiEnabler.getSecurityString(mContext, security, pskType, true);
		String securityStrFormat = mContext.getString(R.string.wifi_secured_first_item);
		int level = scanResult.level;
		int signal = WifiManager.calculateSignalLevel(level, 4);
		
		viewHold.ssid.setText(ssid);
		
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(securityStrFormat, secured));
		
		if (security == WifiEnabler.SECURITY_NONE) {
			viewHold.secured.setText("");
			viewHold.signal.setImageResource(R.drawable.wifi_signal_open_light);
		} else {
			if (wpsAvailable) {
				sb.append(mContext.getString(R.string.wifi_wps_available_second_item));
			} 
			
			viewHold.secured.setText(sb.toString());
			viewHold.signal.setImageResource(R.drawable.wifi_signal_lock_light);
		}
		viewHold.signal.setImageLevel(signal);
		
		return convertView;
	}
	
	private static class ViewHold {
		public TextView ssid;
		public TextView secured;
		public ImageView signal;
	}
}

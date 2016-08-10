package com.pachong.wificontrol;

import android.app.Activity;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.pachong.wificontrol.socket.UDPService;
import com.pachong.wificontrol.socket.UDPService.OnResult;
import com.pachong.wificontrol.util.MyWifiManager;
import com.pachong.wificontrol.util.WifiAPEnabler;
import com.pachong.wificontrol.util.WifiEnabler;
import com.pachong.wificontrol.util.WifiAPEnabler.OnWifiAPListener;
import com.pachong.wificontrol.util.WifiEnabler.OnWifiListener;

public class ServiceActivity extends Activity implements OnClickListener, TextWatcher, OnItemSelectedListener, OnWifiAPListener, OnWifiListener, OnResult {

	private WifiAPEnabler mWifiAPEnabler;
	private EditText mSsid;
	private EditText mPassword;
	private Spinner mSpinner;
	private CheckBox mShowPassword;
	private Button mCreateAPWifi;
	
	private int mSecurityIndex;
	private TextView mHint;
	private UDPService service;
	private Thread mUDPServiceThread;
	private WifiEnabler mWifiEnabler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.service_layout);
		
		mSsid = (EditText) findViewById(R.id.ssid);
		mPassword = (EditText) findViewById(R.id.password);
		mSpinner = (Spinner) findViewById(R.id.security);
		mShowPassword = (CheckBox) findViewById(R.id.show_password);
		mCreateAPWifi = (Button) findViewById(R.id.id_create);
		mHint = (TextView) findViewById(R.id.id_hint);
		mHint.setText("");

		mSpinner.setOnItemSelectedListener(this);
		mSsid.addTextChangedListener(this);
		mPassword.addTextChangedListener(this);
		mShowPassword.setOnClickListener(this);
		mCreateAPWifi.setOnClickListener(this);
		
		mWifiAPEnabler = new WifiAPEnabler(this);
		mWifiEnabler = new WifiEnabler(this);
		mWifiAPEnabler.setOnWifiAPListener(this);
		mWifiEnabler.setOnWifiListener(this);
		
		showSecurityFields();
		validate();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (mWifiAPEnabler != null) {
			mWifiAPEnabler.resume();
		}
		
		if (mWifiEnabler != null) {
			mWifiEnabler.resume();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mWifiAPEnabler != null) {
			mWifiAPEnabler.pause();
		}
		
		if (mWifiEnabler != null) {
			mWifiEnabler.pause();
		}
	}
	
	private void showSecurityFields() {
        if (mSecurityIndex == WifiAPEnabler.OPEN_INDEX) {
            findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }
	
	private void validate() {
		if ((mSsid != null && mSsid.length() == 0) || (((mSecurityIndex == WifiAPEnabler.WPA_INDEX) 
				|| (mSecurityIndex == WifiAPEnabler.WPA2_INDEX)) && mPassword.length() < 8)) {
			mCreateAPWifi.setEnabled(false);
		} else {
			mCreateAPWifi.setEnabled(true);
		}
	}
	
	private void startUdpService() {
		if (service != null) {
			service.close();
			service = null;
		}
		
		if (mUDPServiceThread != null && !mUDPServiceThread.isInterrupted()) {
			mUDPServiceThread.interrupt();
			mUDPServiceThread = null;
		}
		
		service = new UDPService(ServiceActivity.this);
		service.setOnResult(this);
		mUDPServiceThread = new Thread(service);
		mUDPServiceThread.start();
	}
	
	private void setSupplicantStateText(DetailedState state) {
		
        if(DetailedState.AUTHENTICATING.equals(state)) {
            mHint.setText("AUTHENTICATING");
        } else if(DetailedState.CONNECTED.equals(state)) {
        	mHint.setText("CONNECTED");
        } else if(DetailedState.DISCONNECTED.equals(state)) {
        	mHint.setText("DISCONNECTED");
        } else if(DetailedState.CONNECTING.equals(state)) {
        	mHint.setText("CONNECTING");
        } else if(DetailedState.DISCONNECTING.equals(state)) {
        	mHint.setText("DISCONNECTING");
        } else if(DetailedState.FAILED.equals(state)) {
        	mHint.setText("FAILED");
        } else if(DetailedState.IDLE.equals(state)) {
        	mHint.setText("wifi连接失败!请确认wifi ssid和密码是否匹配以及正确!");
        } else if(DetailedState.SCANNING.equals(state)) {
        	mHint.setText("SCANNING");
        } else if(DetailedState.OBTAINING_IPADDR.equals(state)) {
        	mHint.setText("wifi连接成功,连接wifi为:"+mWifiEnabler.getCurrentConnectWifiSSID());
        } else if(DetailedState.SUSPENDED.equals(state)) {
        	mHint.setText("SUSPENDED");
        } else {
        	mHint.setText("BAD");
        }
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.id_create:
			mWifiAPEnabler.createWifiAP(mSsid.getText().toString(), mPassword.getText().toString(), mSecurityIndex);
			mHint.setText("开始创建热点...");
			break;
		case R.id.show_password:
			mPassword.setInputType(InputType.TYPE_CLASS_TEXT | (((CheckBox) v).isChecked() ?
	                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
	                        InputType.TYPE_TEXT_VARIATION_PASSWORD));
			break;

		default:
			break;
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		validate();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		mSecurityIndex = position;
		showSecurityFields();
		validate();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}

	@Override	
	public void onWifiAPStateChanged(int state) {
		switch (state) {
		case MyWifiManager.WIFI_AP_STATE_ENABLING:
			mHint.setText("创建中...");
			break;
		case MyWifiManager.WIFI_AP_STATE_ENABLED:
			mHint.setText("创建成功!!!");
			startUdpService();
			break;
		case MyWifiManager.WIFI_AP_STATE_DISABLING:
			mHint.setText("热点正在关闭!");
			break;
		case MyWifiManager.WIFI_AP_STATE_DISABLED:
			mHint.setText("热点已关闭!");
			break;
		default:
			mHint.setText("热点打开失败!");
			break;
		}
	}

	@Override
	public void onWifiSupplicantStateChanged(DetailedState state,
			boolean hasError, int error) {
		setSupplicantStateText(state);
	}

	@Override
	public void onWifiStateChanged(int state) {
		
	}

	@Override
	public void onSuccess(String ssid, String password, int security) {
		mWifiEnabler.setWifiEnable(true);
		mWifiEnabler.removeOldWifiConfig();
		
		int count = 0;
		
		while (!mWifiEnabler.connectWifi(security, ssid, password) && count < 4) {
			count++;
			try {
				Thread.sleep(2*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

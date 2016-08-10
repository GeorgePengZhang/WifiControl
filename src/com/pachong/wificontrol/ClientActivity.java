package com.pachong.wificontrol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.app.Activity;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.pachong.wificontrol.adapter.WifiAdapter;
import com.pachong.wificontrol.socket.UDPService;
import com.pachong.wificontrol.util.WifiEnabler;
import com.pachong.wificontrol.util.WifiEnabler.OnWifiListener;
import com.pachong.wificontrol.util.WifiSearcher;
import com.pachong.wificontrol.util.WifiSearcher.ErrorType;
import com.pachong.wificontrol.util.WifiSearcher.SearchWifiListener;

public class ClientActivity extends Activity implements OnClickListener, SearchWifiListener, OnItemSelectedListener, TextWatcher, OnWifiListener {
	
	private WifiSearcher mWifiSearcher;
	private TextView mHint;
	private Spinner mWifiSpinner;
	
	private int mWifiPointIndex = 0;
	private List<ScanResult> mWifiList;
	private EditText mPassword;
	private Button mSendBtn;
	private Button mConnectBtn;
	private WifiEnabler mWifiEnabler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.client_layout);
		
		Button searchBtn = (Button) findViewById(R.id.id_search);
		mSendBtn = (Button) findViewById(R.id.id_send);
		mConnectBtn = (Button) findViewById(R.id.id_connect);
		mHint = (TextView) findViewById(R.id.id_hint);
		mWifiSpinner = (Spinner) findViewById(R.id.id_wifispinner);
		mPassword = (EditText) findViewById(R.id.password);
		CheckBox showPassword = (CheckBox) findViewById(R.id.show_password);
		
		searchBtn.setOnClickListener(this);
		mSendBtn.setOnClickListener(this);
		mConnectBtn.setOnClickListener(this);
		mWifiSpinner.setOnItemSelectedListener(this);
		showPassword.setOnClickListener(this);
		mPassword.addTextChangedListener(this);
		
		mHint.setText("点击“搜索wifi”开始搜索可用网络");
		mWifiSpinner.setPrompt("可用网络");
		
		mWifiSearcher = new WifiSearcher(this, this);
		mWifiEnabler = new WifiEnabler(this);
		mWifiEnabler.setOnWifiListener(this);
		
		validate();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mWifiEnabler.resume();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mWifiEnabler.pause();
	}

	private void showSecurityFields() {
		int security = WifiEnabler.getSecurity(mWifiList.get(mWifiPointIndex));
		
        if (security == WifiEnabler.SECURITY_NONE) {
            findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }
	
	private void validate() {
		if (mWifiList == null) {
			mSendBtn.setEnabled(false);
			mConnectBtn.setEnabled(false);
			return ;
		}
		int security = WifiEnabler.getSecurity(mWifiList.get(mWifiPointIndex));
		
		if ((security != WifiEnabler.SECURITY_NONE) && mPassword.length() < 8) {
			mSendBtn.setEnabled(false);
			mConnectBtn.setEnabled(false);
		} else {
			mSendBtn.setEnabled(true);
			mConnectBtn.setEnabled(true);
		}
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
		case R.id.id_search:
			mHint.setText("开始搜索可用wifi热点");
			mWifiSearcher.search();
			break;
		case R.id.show_password:
			mPassword.setInputType(InputType.TYPE_CLASS_TEXT | (((CheckBox) v).isChecked() ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_TEXT_VARIATION_PASSWORD));
			break;
		case R.id.id_send:
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						InetAddress address = UDPService.getBroadcastAddress(ClientActivity.this);
						
						ScanResult wifiPoint = mWifiList.get(mWifiPointIndex);
						String info = wifiPoint.SSID+":"+mPassword.getText().toString()+":"+WifiEnabler.getSecurity(wifiPoint);
						final String msg = UDPService.sendUDPMsgAndReceiveMsg(info, address.getHostAddress(), UDPService.UDPSERVICEPORT);
						
						runOnUiThread(new Runnable() {
							public void run() {
								mHint.setText(msg);
							}
						});
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					
				}
			}).start();
			break;
			
		case R.id.id_connect:
			ScanResult scanResult = mWifiList.get(mWifiPointIndex);
			int security = WifiEnabler.getSecurity(scanResult);
			mWifiEnabler.setWifiEnable(true);
			mWifiEnabler.removeOldWifiConfig();
			mWifiEnabler.connectWifi(security, scanResult.SSID, mPassword.getText().toString());
			break;

		default:
			break;
		}
		
	}

	@Override
	public void onSearchWifiFailed(ErrorType errorType) {
		Log.d("TAG", "onSearchWifiFailed:"+errorType);
		switch (errorType) {
		case SEARCH_WIFI_TIMEOUT:
			mHint.setText("搜索超时!");
			break;
		case NO_WIFI_FOUND:
			mHint.setText("当前没有可用热点!");
			break;

		default:
			break;
		}
		
	}

	@Override
	public void onSearchWifiSuccess(List<ScanResult> results) {
		mHint.setText("搜索成功!");
		mWifiList = results;
		WifiAdapter adapter = new WifiAdapter(ClientActivity.this, results);
		mWifiSpinner.setAdapter(adapter);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		mWifiPointIndex = position;
		showSecurityFields();
		validate();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
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
	public void onWifiSupplicantStateChanged(DetailedState state,
			boolean hasError, int error) {
//		if (hasError) {
		Log.d("TAG", "setSupplicantStateText:"+state+",supplicant:"+mWifiEnabler.getSupplicantState()+",hasError:"+hasError);
			
//		} else {
			setSupplicantStateText(state);
//		}
	}

	@Override
	public void onWifiStateChanged(int state) {
		 switch (state) {
         case WifiManager.WIFI_STATE_ENABLING:
        	 break;
         case WifiManager.WIFI_STATE_ENABLED:
        	 break;
         case WifiManager.WIFI_STATE_DISABLING:
        	 break;
         case WifiManager.WIFI_STATE_DISABLED:
             break;
         default:
             break;
     }
	}
}

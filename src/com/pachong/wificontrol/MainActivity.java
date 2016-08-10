package com.pachong.wificontrol;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button serviceBtn = (Button) findViewById(R.id.id_startService);
		Button clientBtn = (Button) findViewById(R.id.id_startClient);
		
		serviceBtn.setOnClickListener(this);
		clientBtn.setOnClickListener(this);
		
		
		String date = DateFormat.getInstance().format(new Date());
		String dateTimeInstance = DateFormat.getDateTimeInstance().format(new Date());
		
		int rotation = getWindowManager().getDefaultDisplay().getRotation(); //旋转角度
		
		int orientation = getResources().getConfiguration().orientation; //横屏还是竖屏
//		int orientationLandscape = Configuration.ORIENTATION_LANDSCAPE; 
		
		Log.d("TAG", "date:"+date+",dateInstance:"+dateTimeInstance+",rotation:"+rotation+",orientation:"+orientation);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		
		switch (v.getId()) {
		case R.id.id_startService:
			intent = new Intent(MainActivity.this, ServiceActivity.class);
			startActivity(intent);
			break;
			
		case R.id.id_startClient:
			intent = new Intent(MainActivity.this, ClientActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}

}

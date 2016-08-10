package com.pachong.wificontrol.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.pachong.wificontrol.util.MyWifiManager;

public class UDPService implements Runnable {

	public static final int UDPSERVICEPORT = 43110;
	public static final int UDPCLIENTPORT = 43111;
	public static final String BROADCASTIP = "255.255.255.255";
	private DatagramSocket mDatagramSocket;
	private volatile boolean mIsStop;
	private OnResult mOnResult;

	public UDPService(Context context) {
		try {
			mIsStop = false;
			if (mDatagramSocket == null) {
				mDatagramSocket = new DatagramSocket(null);
				mDatagramSocket.setReuseAddress(true);
				mDatagramSocket.setBroadcast(true);
				mDatagramSocket.bind(new InetSocketAddress(UDPSERVICEPORT));
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		byte[] data = new byte[1024];
		DatagramPacket pack = new DatagramPacket(data, data.length);

		while (!mIsStop) {
			try {
				Log.d("TAG", "run");
				mDatagramSocket.receive(pack);

				String info = new String(pack.getData(), pack.getOffset(),
						pack.getLength());

				Log.d("TAG", "info:" + info);
				
				// :SSID:Auratech:PASSWORD:aura6300_5g:SECURITY:2
				String[] split = info.split(":");
				int length = split.length;
				if (length == 3) {
					String ssid = split[0];
					String password = split[1];
					int security = Integer.valueOf(split[2]);

					Log.d("TAG", "ssid:" + ssid + ",password:" + password
							+ ",security:" + security);
					sendUDPMsg("发送成功", pack.getAddress().getHostAddress(),
							pack.getPort());
					
					mOnResult.onSuccess(ssid, password, security);
					
					close();
				} else {
					sendUDPMsg("发送无效数据", pack.getAddress().getHostAddress(),
							UDPCLIENTPORT);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		mIsStop = true;

		if (mDatagramSocket != null) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
	}

	public static void sendUDPMsg(final String msg, final String ip,
			final int port) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					DatagramPacket pack = new DatagramPacket(msg.getBytes(),
							msg.getBytes().length, InetAddress.getByName(ip),
							port);
					socket.send(pack);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (socket != null) {
						socket.close();
						socket = null;
					}
				}
			}
		}).start();
	}
	
	public static String sendUDPMsgAndReceiveMsg(String msg, String ip, int sendPort) {
		String result = "没有接收到数据";
		
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			socket.setSoTimeout(5*1000);
			DatagramPacket pack = new DatagramPacket(msg.getBytes(),
					msg.getBytes().length, InetAddress.getByName(ip),
					sendPort);
			socket.send(pack);
			
			byte[] data = new byte[1024];
			DatagramPacket receivePack = new DatagramPacket(data , data.length);
			socket.receive(receivePack);
			result = new String(receivePack.getData(), receivePack.getOffset(), receivePack.getLength());
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}
		
		return result;
	}

	public static InetAddress getBroadcastAddress(Context context)
			throws UnknownHostException {
		boolean wifiAPEnabled = MyWifiManager.getInstance(context)
				.isWifiApEnabled();
		if (wifiAPEnabled) {
			return InetAddress.getByName("192.168.43.255");
		}

		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		if (dhcp == null) {
			return InetAddress.getByName("255.255.255.255");
		}

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);

		return InetAddress.getByAddress(quads);
	}
	
	public void setOnResult(OnResult onResult) {
		mOnResult = onResult;
	}
	
	public interface OnResult {
		public void onSuccess(String ssid, String password, int security);
	}
}

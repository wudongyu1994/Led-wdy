package com.embedsky.hicamera.bean;

import android.content.Context;

import com.hichip.control.HiCamera;

public class MyCamera extends HiCamera {
//	private String nikeName = "";
//	private int videoQuality = HiDataValue.DEFAULT_VIDEO_QUALITY;
//	private int alarmState = HiDataValue.DEFAULT_ALARM_STATE;
//	private int pushState = HiDataValue.DEFAULT_PUSH_STATE;
//	private boolean hasSummerTimer;
//	private boolean isFirstLogin = true;
//	private byte[] bmpBuffer = null;
//	public Bitmap snapshot = null;
//	private long lastAlarmTime;
//	private boolean isSetValueWithoutSave = false;
//	private int style;
//	private String serverData;
//	public int isSystemState = 0;// 1重启中 2恢复出厂设置中 3检查更新中
//	public boolean alarmLog = false;// 用于小红点是否显示

	public MyCamera(Context context, String uid, String username, String password) {//, String nikename
		super(context, uid, username, password);
//		this.nikeName = nikename;
	}

//	public String getServerData() {
//		return this.serverData;
//	}

	public void saveInCameraList() {
		HiDataValue.CameraList.add(this);
	}

	public static MyCamera getByUID(String UID) {
		for (MyCamera myCamera : HiDataValue.CameraList ) {
			if(myCamera.getUid().equals(UID)){
				return myCamera;
			}
		}
		return null;
	}

//	private int curbmpPos = 0;

//	private void createSnapshot() {
//		Bitmap snapshot_temp = BitmapFactory.decodeByteArray(bmpBuffer, 0, bmpBuffer.length);
//		if (snapshot_temp != null)
//			snapshot = snapshot_temp;
//
//		bmpBuffer = null;
//		curbmpPos = 0;
//
//	}


	@Override
	public void connect() {

		if (getUid() != null && getUid().length() > 4) {
			String temp = getUid().substring(0, 5);
			String str = getUid().substring(0, 4);
			if (!temp.equalsIgnoreCase("FDTAA") && !str.equalsIgnoreCase("DEAA") && !str.equalsIgnoreCase("AAES")) {
				super.connect();
			}
		}
	}

//	public interface OnBindPushResult {
//		public void onBindSuccess(MyCamera camera);
//
//		public void onBindFail(MyCamera camera);
//
//		public void onUnBindSuccess(MyCamera camera);
//
//		public void onUnBindFail(MyCamera camera);
//	}

//	private OnBindPushResult onBindPushResult;
//
//	public HiPushSDK push;
//	private HiPushSDK.OnPushResult pushResult = new HiPushSDK.OnPushResult() {
//		@Override
//		public void pushBindResult(int subID, int type, int result) {
//			isSetValueWithoutSave = true;
//
//			if (type == HiPushSDK.PUSH_TYPE_BIND) {
//				if (HiPushSDK.PUSH_RESULT_SUCESS == result) {
//					pushState = subID;
//					if (onBindPushResult != null)
//						onBindPushResult.onBindSuccess(MyCamera.this);
//				} else if (HiPushSDK.PUSH_RESULT_FAIL == result || HiPushSDK.PUSH_RESULT_NULL_TOKEN == result) {
//					if (onBindPushResult != null)
//						onBindPushResult.onBindFail(MyCamera.this);
//				}
//			} else if (type == HiPushSDK.PUSH_TYPE_UNBIND) {
//				if (HiPushSDK.PUSH_RESULT_SUCESS == result) {
//					if (onBindPushResult != null)
//						onBindPushResult.onUnBindSuccess(MyCamera.this);
//				} else if (HiPushSDK.PUSH_RESULT_FAIL == result) {
//					if (onBindPushResult != null)
//						onBindPushResult.onUnBindFail(MyCamera.this);
//				}
//
//			}
//
//		}
//	};


}

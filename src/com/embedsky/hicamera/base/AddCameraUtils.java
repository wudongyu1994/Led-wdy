package cn.assassing.camtest.base;

import android.Manifest;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.hichip.control.HiCamera;
import com.hichip.tools.HiSearchSDK;

import cn.assassing.camtest.R;
import cn.assassing.camtest.bean.HiDataValue;
import cn.assassing.camtest.bean.MyCamera;
import cn.assassing.camtest.main.MainActivity;

/**
 * Created by cm on 2018/6/30.
 */

public class AddCameraUtils {
    private static AddCameraUtils instance;
    private CountDownTimer timer;
    private HiSearchSDK searchSDK;
    private HiThreadConnect connectThread = null;
    private Context mContext;

    private AddCameraUtils() {}

    public static synchronized AddCameraUtils getInstance() {
        if (instance == null) {
            instance = new AddCameraUtils();
        }
        return instance;
    }

    public void startSearch(final Handler handler, final Context mContext) {
        LogUtil.d("startSearch is called");
        this.mContext = mContext;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        int timeLong = 20000;
        final Message message = Message.obtain();
        message.what = MainActivity.HANDLE_MESSAGE_WHAT_ADDCAMERA;

        searchSDK = new HiSearchSDK(new HiSearchSDK.ISearchResult() {
            @Override
            public void onReceiveSearchResult(HiSearchSDK.HiSearchResult result) {
                String temp = result.uid.substring(0, 4);
                if (!TextUtils.isEmpty(temp) && HiDataValue.CameraList.size() < 2) {
//                    String str_nike = "Camera";
                    String str_uid = result.uid.trim().toUpperCase();
                    String str_password = "admin";
                    String str_username = "admin";


                    for (int i = 0; i < HiDataValue.zifu.length; i++) {
                        if (str_uid.contains(HiDataValue.zifu[i])) {
                            message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                            message.obj = R.string.tips_invalid_uid;
                            handler.sendMessage(message);
                            return;
                        }
                    }
                    if (HiDataValue.CameraList != null && HiDataValue.CameraList.size() >= 64) {
                        message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                        message.obj = R.string.tips_limit_add_camera;
                        handler.sendMessage(message);
                        return;
                    }

                    if (TextUtils.isEmpty(str_uid)) {
                        message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                        message.obj = R.string.tips_null_uid;
                        handler.sendMessage(message);
                        return;
                    }

                    str_uid = HiTools.handUid(str_uid);
                    if (str_uid == null) {
                        message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                        message.obj = R.string.tips_invalid_uid;
                        handler.sendMessage(message);
                        return;
                    }
                    // 解决：用户名和密码同时输入：31个特殊字符，应用后app闪退且起不来
                    if (str_username.getBytes().length > 64) {
                        message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                        message.obj = R.string.tips_username_tolong;
                        handler.sendMessage(message);
                        return;
                    }
                    if (str_password.getBytes().length > 64) {
                        message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                        message.obj = R.string.tips_password_tolong;
                        handler.sendMessage(message);
                        return;

                    }

                    for (MyCamera camera : HiDataValue.CameraList) {
                        if (str_uid.equalsIgnoreCase(camera.getUid())) {
                            message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_FAIL;
                            message.obj = R.string.tips_add_camera_exists;
                            handler.sendMessage(message);
                            return;
                        }
                    }
                    MyCamera camera = new MyCamera(mContext.getApplicationContext(), str_uid, str_username, str_password);
                    camera.saveInCameraList();
                    LogUtil.d("add camera:"+camera.getUid()+";size = "+HiDataValue.CameraList.size());
                }
            }
        });
        searchSDK.search2();
        timer = new CountDownTimer(timeLong, 3000) {
            @Override
            public void onFinish() {
                searchSDK.stop();
                connectAllCamera();
                Message message = Message.obtain();
                message.what = MainActivity.HANDLE_MESSAGE_WHAT_ADDCAMERA;
                message.arg1 = MainActivity.HANDLE_MESSAGE_ARG1_ADDCAMERA_COMPELETE;
                handler.sendMessage(message);
            }

            @Override
            public void onTick(long arg0) {
            }
        }.start();
    }


    private void connectAllCamera() {
        LogUtil.d("connectAllCamera is called");
//        if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            return;
//        }
        new Handler().postAtTime(new Runnable() {

            @Override
            public void run() {
                if (connectThread == null) {
                    connectThread = new HiThreadConnect();
                    connectThread.start();
                }
            }
        }, 100);
    }

    private class HiThreadConnect extends Thread {
        private int connnum = 0;

        public synchronized void run() {
            for (connnum = 0; connnum < HiDataValue.CameraList.size(); connnum++) {
                MyCamera camera = HiDataValue.CameraList.get(connnum);
                if (camera != null) {
                    if (camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED) {
                        camera.connect();
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                LogUtil.e("HiThreadConnect:" + connnum);
            }
            if (connectThread != null) {
                connectThread = null;
            }
        }

    }
}

//package cn.assassing.camtest.base;
//
//import android.Manifest;
//import android.content.Context;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.text.TextUtils;
//import android.util.Log;
//
//import com.hichip.control.HiCamera;
//import com.hichip.tools.HiSearchSDK;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import cn.assassing.camtest.R;
//import cn.assassing.camtest.bean.HiDataValue;
//import cn.assassing.camtest.bean.MyCamera;
//
///**
// * Created by cm on 2018/6/30.
// */
//
//public class AddCameraUtils {
//    private static AddCameraUtils instance;
//    private CountDownTimer timer;
//    private HiSearchSDK searchSDK;
//    private List<HiSearchSDK.HiSearchResult> list = new ArrayList<HiSearchSDK.HiSearchResult>();
//    private Context mContext;
//    HiThreadConnect connectThread = null;
//
//    private AddCameraUtils() {
//
//    }
//
//    public static synchronized AddCameraUtils getInstance() {
//        if (instance == null) {
//            instance = new AddCameraUtils();
//        }
//        return instance;
//    }
//
//    public void startSearch(Context context) {
//        LogUtil.d("startSearch is called");
//        mContext = context;
//        list.clear();
//        if (timer != null) {
//            timer.cancel();
//            timer = null;
//        }
//        int timeLong = 20000;
//
//        searchSDK = new HiSearchSDK(new HiSearchSDK.ISearchResult() {
//            @Override
//            public void onReceiveSearchResult(HiSearchSDK.HiSearchResult result) {
//                String temp = result.uid.substring(0, 4);
//                if (!TextUtils.isEmpty(temp)) {
////                    String str_nike = "Camera";
//                    String str_uid = result.uid.trim().toUpperCase();
//                    String str_password = "admin";
//                    String str_username = "admin";
//
//
//                    for (int i = 0; i < HiDataValue.zifu.length; i++) {
//                        if (str_uid.contains(HiDataValue.zifu[i])) {
//                            LogUtil.d(mContext.getText(R.string.tips_invalid_uid).toString());
//                            return;
//                        }
//                    }
//                    if (HiDataValue.CameraList != null && HiDataValue.CameraList.size() >= 64) {
//                        LogUtil.d(mContext.getText(R.string.tips_limit_add_camera).toString());
//                        return;
//                    }
//
//                    if (TextUtils.isEmpty(str_uid)) {
//                        LogUtil.d(mContext.getText(R.string.tips_null_uid).toString());
//                        return;
//                    }
//
//                    String string = HiTools.handUid(str_uid);
//                    str_uid = string;
//                    if (str_uid == null) {
//                        LogUtil.d(mContext.getText(R.string.tips_invalid_uid).toString());
//                        return;
//                    }
//                    // 解决：用户名和密码同时输入：31个特殊字符，应用后app闪退且起不来
//                    if (str_username.getBytes().length > 64) {
//                        LogUtil.d(mContext.getText(R.string.tips_username_tolong).toString());
//                        return;
//                    }
//                    if (str_password.getBytes().length > 64) {
//                        LogUtil.d(mContext.getText(R.string.tips_password_tolong).toString());
//                        return;
//
//                    }
//
//                    for (MyCamera camera : HiDataValue.CameraList) {
//                        if (str_uid.equalsIgnoreCase(camera.getUid())) {
//                            LogUtil.d(mContext.getText(R.string.tips_add_camera_exists).toString());
//                            return;
//                        }
//                    }
//                    LogUtil.d("add camera : " + str_uid);
//                    MyCamera camera = new MyCamera(mContext.getApplicationContext(), str_uid, str_username, str_password);
//                    camera.saveInCameraList();
//                    System.out.println("add camera:"+camera.getUid());
////                    Intent broadcast = new Intent();
////                    broadcast.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
////                    mContext.sendBroadcast(broadcast);
//                    connectAllCamera();
//                }
//            }
//        });
//        searchSDK.search2();
//        timer = new CountDownTimer(timeLong, 1000) {
//            @Override
//            public void onFinish() {
//                if (list == null || list.size() == 0) {
//                    searchSDK.stop();
//                }
//            }
//
//            @Override
//            public void onTick(long arg0) {
//
//            }
//        }.start();
//    }
//
//
//    private void connectAllCamera() {
//        LogUtil.d("connectAllCamera is called");
//        if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            return;
//        }
//        new Handler().postAtTime(new Runnable() {
//
//            @Override
//            public void run() {
//                // TODO Auto-generated method stub
//                if (connectThread == null) {
//                    connectThread = new HiThreadConnect();
//                    connectThread.start();
//                }
//            }
//        }, 100);
//    }
//
//    private class HiThreadConnect extends Thread {
//        private int connnum = 0;
//
//        public synchronized void run() {
//            for (connnum = 0; connnum < HiDataValue.CameraList.size(); connnum++) {
//                MyCamera camera = HiDataValue.CameraList.get(connnum);
//                if (camera != null) {
//                    if (camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED) {
//                        camera.connect();
//                        try {
//                            Thread.sleep(150);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                }
//                Log.e("charming", "HiThreadConnect:" + connnum);
//            }
//            if (connectThread != null) {
//                connectThread = null;
//            }
//        }
//
//    }
//}

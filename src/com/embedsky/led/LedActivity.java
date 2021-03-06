package com.embedsky.led;


import com.embedsky.httpUtils.httpUtils;
import com.embedsky.httpUtils.lockStruct;
import com.embedsky.httpUtils.logInfo;
import com.embedsky.httpUtils.picUpload;
import com.embedsky.httpUtils.tirePressure;

import com.interfaces.mLocalCaptureCallBack;
import com.interfaces.mPictureCallBack;

import com.embedsky.serialport.CH340AndroidDriver;

// import com.lib.funsdk.support.FunSupport;
// import com.Utils.FunDeviceUtils;
import com.Utils.SharedPreferencesNames.SPNames;
import com.Utils.SharedPreferencesNames.UserInfoItems;
import com.Utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;
import org.json.JSONException;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection; 
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IMycanService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.igexin.sdk.PushManager;

public class LedActivity extends Activity /*implements mPictureCallBack*/{
	/** Called when the activity is first created. */

	//加载libled.so库，必须放在最前面
	static {
		System.loadLibrary("led");
	}

	private static final String LOG_TAG = "lock";
	public static String AWAKE_ACTION = "action_awake";
	private AwakeBroadcastReceiver awakeBroadcastReceiver;
	//handler msg.what
	private final int MESSAGE_GETUI = 0x100;
	private final int MESSAGE_HEARTPACKAGE = 0x101;
	private final int MESSAGE_FILEUPLOAD = 0x102;
	private final int MESSAGE_WARNPACKAGE = 0x103;
	private final int MESSAGE_PARAMSPACKAGE = 0x104;
	private final int MESSAGE_TESTPACKAGE = 0x105;
	private final int MESSAGE_LOCKCMDOPERATE = 0x106;
	private final int MESSAGE_USB_INSERT = 0x107;
	private final int MESSAGE_USB_UNINSERT = 0x108;
	private final int MESSAGE_OPPARAMS = 0x109;
	private final int MESSAGE_DELAY = 0x201;
	private final int MESSAGE_NOWARN = 0x202;
	private final int MESSAGE_REQUEST =0X203;

	//初始化led
	public static native boolean ledInit();
	//关闭led
	public static native boolean ledClose();
	//点亮led
	public static native boolean ledSetOn(int number);
	//灭掉led
	public static native boolean ledSetOff(int number);
	
	private String url="http://120.76.219.196:85/trucklogs/add_log";
	//private String url="http://192.168.10.87:8080/MyWeb/MyServlet";
	private String getuiurl = "http://120.76.219.196:85/getui/postcid";
	private String picurl = "http://120.76.219.196:85/file/upload";
	private String testurl = "http://120.76.219.196:85/test/getTruckLogResp";
	private String opurl = "http://120.76.219.196:85/lock/request_car";
	private String requesturl = "http://120.76.219.196:85/lock/request_car";

	private static HashMap<String, String> params = new HashMap<String, String>();
	private static HashMap<String, String> cidparams = new HashMap<String, String>();	//请查询“自贡三辰实业危化品 文档”里的“个推cid上传”
	private static HashMap<String, String> opparams = new HashMap<String, String>();
	private static HashMap<String, String> requestparams = new HashMap<String, String>();
	protected static logInfo loginfo = new logInfo(); //data package uploaded
	private static int[] warntypecnt = new int[10];
	protected static LinkedList<HashMap<String, String>> warnmsgbuf = new LinkedList<HashMap<String, String>>();
	// private static LinkedList<HashMap<String, String>> testmsgbuf = new LinkedList<HashMap<String, String>>();
	private logInfo testloginfo;
	protected static LinkedList<String> serialssendbuf = new LinkedList<String>();
	private static boolean flag;
	private static int cnt;	
	private static boolean hasRecBro;

	//Timer timer = new Timer();
	Timer sendtime = new Timer();
	Timer time = new Timer();
	Timer otherTimer =new Timer();
	ExecutorService executorService = Executors.newFixedThreadPool(3);
	protected HeartpackTask heartpacktask;
	protected ParamspackTask paramspacktask;
	protected WarnpackTask warnpacktask;
	protected TestTask testtask;
	protected WakeUpAppTask mWakeUpAppTask;

	//gps
	Location location;
	private static String gpsx;
	private static String gpsy;
	private static float speed = 0;
	private static int v = 0;


	//can总线
	private static IMycanService mycanservice;
	private static mycanHandler canhandler;
	public static int ret; //can receive result
	private static int distance0; //get the first distance data
	private static int disCnt; //distance counter
	private static double fuelleveltemp = 100;
	private static int cansendPid[] = {0x05,0x0C,0x0D,0x21,0x2F};  //TODO can pid add
	private static int canCnt; //can pid counter
	private static tirePressure[] tirepressure = new tirePressure[2];

	private Context context;
	protected static int sidcnt;
	
	//serials
	private static final String ACTION_USB_PERMISSION = "com.embedsky.USB_PERMISSION";
	protected CH340AndroidDriver ch340AndroidDriver;
	private final int baurt = 4800;
	private final int BUF_SIZE = 64;
	private final int LEN = 64;
	//protected SerialPort mSerialPort;
	//protected OutputStream mOutputStream;
	//private InputStream mInputStream;
	private ReadThread mReadThread;
	private ReGetuiApplication app;//get packdata methods
	private static lockStruct[] lockstruct = new lockStruct[5];
	public static String[] lockstatustemp = new String[5];
	private static int lockwarncnt;
	private static int leakstatuscnt;
	public static long time_old;
	public static long time_new;
	private boolean operateIsOk=false;
	private boolean is_warned_wdy=false;

	//CheckBox数组，用来存放3个test控件
	//CheckBox[] cb = new CheckBox[3];
	TextView[] tv = new TextView[4];
	TextView tv_lock,tv_liquid,tv_gpsx,tv_gpsy,tv_gpsv,tv_lock_warn,tv_liquid_warn,tv_tire_warn;
	
	public static TextView tv_log;
	
	Button btn1,btn2,btn_openlock;

	private LocationManager lm1;















	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// Log.i(LOG_TAG,"begin onCreate()");

		
		
		context = LedActivity.this.getApplicationContext();
		app = (ReGetuiApplication) getApplicationContext();
		

		

		tv_lock = (TextView) findViewById(R.id.tv_lock);
		tv_liquid = (TextView) findViewById(R.id.tv_liquid);
		tv[0] = (TextView) findViewById(R.id.tv_left_tire);
		tv[1] = (TextView) findViewById(R.id.tv_left_temp);
		tv[2] = (TextView) findViewById(R.id.tv_right_tire);
		tv[3] = (TextView) findViewById(R.id.tv_right_temp);
		tv_gpsx = (TextView)findViewById(R.id.tv_gpsx);
		tv_gpsy = (TextView)findViewById(R.id.tv_gpsy);
		tv_gpsv = (TextView)findViewById(R.id.tv_gpsv);
		tv_lock_warn = (TextView)findViewById(R.id.tv_lock_warn);
		tv_liquid_warn = (TextView)findViewById(R.id.tv_liquid_warn);
		tv_tire_warn = (TextView)findViewById(R.id.tv_tire_warn);
		tv_log =(TextView) findViewById(R.id.tv_log);
		tv_log.setSingleLine(false);
		tv_log.setHorizontallyScrolling(false);
		
		btn_openlock = (Button) findViewById(R.id.btn_openlock);
		btn1=(Button) findViewById(R.id.button1);
		btn2=(Button) findViewById(R.id.button2);

		ReGetuiApplication.ReActivity = this;
		
		lockstruct[0] = new lockStruct("up_front","1","0");
		lockstruct[1] = new lockStruct("up_middle","1","0");
		lockstruct[2] = new lockStruct("up_back","1","0");
		lockstruct[3] = new lockStruct("down_left","1","0");
		lockstruct[4] = new lockStruct("down_right","1","0");
		loginfo.lockSet(lockstruct);

		tirepressure[0] = new tirePressure("lefttirepressure","0","lefttiretemp","0");
		tirepressure[1] = new tirePressure("righttirepressure","0","righttiretemp","0");
		loginfo.tireSet(tirepressure);
		
		for (int i = 0; i < 5; i++){
			lockstatustemp[i] = "1";
		}

		for (int i = 0; i < warntypecnt.length; i++){
			warntypecnt[i] = 0;
		}
		
		cnt = 0;
		hasRecBro=false;
		
            
		//个推初始化
		PushManager push=PushManager.getInstance();
		push.initialize(this.getApplicationContext(),GetuiPushService.class);
		push.registerPushIntentService(this.getApplicationContext(),ReIntentService.class);
		// String cid=null;
		// cid=push.getClientid(this.getApplicationContext());
		PushManager.getInstance().initialize(this.getApplicationContext(),GetuiPushService.class);
		PushManager.getInstance().registerPushIntentService(this.getApplicationContext(),ReIntentService.class);
		String cid = PushManager.getInstance().getClientid(this.getApplicationContext());
		//String cid = new String();
		if(cid != null){
			tv_log.append("cid: "+cid+"\n");
			cidparams.put("trucknumber","浙A1234");
			cidparams.put("type", "100");
			cidparams.put("cid", cid);
			Log.d("cid", "cid: "+cid);
			
			httpUtils.doPostAsyn(getuiurl, cidparams, new httpUtils.HttpCallBackListener() {
	            @Override
	            public void onFinish(String result) {
	                Message message = new Message();
	                message.what = MESSAGE_GETUI;
	                message.obj=result;
	                handler.sendMessage(message);
	            }

	            @Override
	            public void onError(Exception e) {
	            	Log.d("cid","cid post error!");
	            }
	        });
		}
		else{
			Log.d("cid","cid failed!");
		}

		//serials initial
		registerReceiver(mUsbDeviceReceiver, new IntentFilter(
				UsbManager.ACTION_USB_DEVICE_ATTACHED));
		registerReceiver(mUsbDeviceReceiver, new IntentFilter(
				UsbManager.ACTION_USB_DEVICE_DETACHED));
		ch340AndroidDriver = new CH340AndroidDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE), this,
				ACTION_USB_PERMISSION);

		//initUSB();
		Intent i = getIntent();
		String action = i.getAction();
		if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
			// Log.d(LOG_TAG, "init USB");
			initUSB();
		}
		lockwarncnt = 0;
		leakstatuscnt = 0;
		
		//gps initial
		lm1 = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		if(lm1.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			//Toast.makeText(LedActivity.this,"GPS has been opened!",Toast.LENGTH_SHORT).show();
		}
		else{
			Toast.makeText(LedActivity.this,"###error: GPS opened failed",Toast.LENGTH_SHORT).show();
		}
		
		final String bestProvider = lm1.getBestProvider(getCriteria(),true);
		// Log.d("GPS", "bestProvider"+bestProvider);

		if(bestProvider!=null || !"".equals(bestProvider.trim())){
			location = lm1.getLastKnownLocation(bestProvider);
			updateLocation(location);
			lm1.requestLocationUpdates(bestProvider, 1000, 1, locationlistener);	//1000ms=1s,1m,
		}
		
		//发送开锁命令
		btn_openlock.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				for(int i = 0; i < 5; i++){
					serialssendbuf.add(app.lockdevid[1]+"|"+"00");//open
				}
			}
		});
		//发送视频监控下载命令，并获取gps信息
		btn1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent=new Intent();
				intent.setAction("action_scsy_warn");
				LedActivity.this.getApplicationContext().sendBroadcast(intent);
				Log.d("wdy","action_scsy_warn");
 
				location = lm1.getLastKnownLocation(bestProvider);
				updateLocation(location);
			}
		});
		//启动hicam
		btn2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				PackageManager packageManager = getPackageManager();
	            Intent intent = packageManager.getLaunchIntentForPackage("cn.assassing.camtest");
	            if (intent != null) {
	                startActivity(intent);
	            }
			}
		});
		
		//can总线初始化
		mycanservice = IMycanService.Stub.asInterface(ServiceManager.getService("mycan"));
		ret = -1;
		
		if (canhandler == null){
			canhandler = new mycanHandler();
		}

		//timer.schedule(task, 5000, 60000); // 5s后执行task,经过60s再次执行
		heartpacktask = new HeartpackTask();
		warnpacktask = new WarnpackTask();
		mWakeUpAppTask =new WakeUpAppTask();
		sendtime.schedule(cansendtask, 10, 1*1000);// 1s后执行 cansendtask,T=1s
		sendtime.schedule(serialssendtask, 1500, 2*1000);// 1.5s后执行 serialssendtask,T=2s
		time.schedule(heartpacktask, 10*1000, 60*1000);// 10s后执行 heartpacktask,T=60s
		time.schedule(warnpacktask, 6*1000, 20*1000);// 6s后执行 warnpacktask,T=20s
		otherTimer.schedule(mWakeUpAppTask,20*1000,90*1000);//20s后执行 ,T=90s
		// timer.start();	// 疲劳驾驶报警timer
		// timerWake.start();
		
		//注册广播接收器
		IntentFilter filter = new IntentFilter();
        filter.addAction(AWAKE_ACTION);
        awakeBroadcastReceiver = new AwakeBroadcastReceiver();
        LedActivity.this.getApplicationContext().registerReceiver(awakeBroadcastReceiver, filter);
		
		
		//don't open hicam so early
		// PackageManager packageManager = getPackageManager();
  //       Intent intent = packageManager.getLaunchIntentForPackage("cn.assassing.camtest");
  //       if (intent != null) {
  //           startActivity(intent);
  //       }
  //       Log.d("wdy","open hicam end");
        
        
		canCnt = 0;
		disCnt = 0;
		ReadThread mReadThread = new ReadThread();
		CanRev mCanRev = new CanRev();
		executorService.execute(mReadThread);
		executorService.execute(mCanRev);
	}//end onCreate

    @Override
    protected void onDestroy(){
    	// fdu.OnDestory();//onDestroy
    	ch340AndroidDriver.CloseDevice();
    	unregisterReceiver(mUsbDeviceReceiver);
    	lm1.removeUpdates(locationlistener);
    	super.onDestroy();
    }

    
















/******************************************************************************************
* 以上时onCreate() & onDestroy()，以下是各种子线程定义
*******************************************************************************************/
	/*
	 * 唤醒hicam app
	 */

    // private CountDownTimer timerWake=new CountDownTimer(2*20*1000,1*20*1000) {
    //     @Override
    //     public void onTick(long l) {
    //     	// Log.d("wdy","isFore = "+isRunningForeground(LedActivity.this));
    //     	// if (!isRunningForeground(LedActivity.this)) {
    //      //        //获取ActivityManager
    //      //        ActivityManager mAm = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
    //      //        //获得当前运行的task
    //      //        List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
    //      //        for (ActivityManager.RunningTaskInfo rti : taskList) {
    //      //            //找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
    //      //            if (rti.topActivity.getPackageName().equals(getPackageName())) {
    //      //                mAm.moveTaskToFront(rti.id, 0);
    //      //                return;
    //      //            }
    //      //        }
    //      //        //若没有找到运行的task，用户结束了task或被系统释放，则重新启动mainactivity
    //      //        Intent resultIntent = new Intent(LedActivity.this, LedActivity.class);
    //      //        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    //      //        startActivity(resultIntent);
    //         }
    //     }

    //     @Override
    //     public void onFinish() {    //5分钟后没有收到广播，则重新开启一个Cam_app
    //         PackageManager packageManager = getPackageManager();
    //         Intent intent = packageManager.getLaunchIntentForPackage("cn.assassing.camtest");
    //         if (intent != null) {
    //             startActivity(intent);
    //         }
    //         timerWake.start();
    //         Log.d("wdy","open hicam end");
    //     }
    // };
    
    /*
     * 疲劳驾驶报警
     */
	// private CountDownTimer timer = new CountDownTimer(4*60*60*1000,20*1000){	//4 hours, tick 1 min
 //    	@Override
 //    	public void onTick(long millisUntilFinished){
	// 		// if(v<3 || speed<3){		
	// 			timer.start();
	// 		// }		
 //    	}
 //    	@Override
 //    	public void onFinish(){    		
	// 		loginfo.haswarnSet("1");
	// 	  	loginfo.typeSet("7");
	// 	  	warnmsgbuf.add(loginfo.logInfoGet());
	// 		loginfo.haswarnSet("0");
	// 		timer.start();
 //    	}
 //    };

	/*
	 * can send 子线程
	 * 线程内容：每过1s周期，进入该子线程，设置收集到的can数据，准备发送给服务器。
	 * 注意：不需要在线程中更新UI，所以直接实现一个timerTask即可。
	 */
	TimerTask cansendtask = new TimerTask(){
		@Override
		public void run(){
			//Mycan send
			try{
				//TODO can pid add
				mycanservice.set_data(0,2);
				mycanservice.set_data(1,1);
				mycanservice.set_data(2,cansendPid[canCnt%5]);
				for(int i = 3; i < 8; i++){
					mycanservice.set_data(i,0);
				}
				mycanservice.mycansend(0x18DB33F1,8,1,0,0,1);
				canCnt += 1;
				canCnt = canCnt==5?0:canCnt;
				// Log.d("wdy", "---> cansendtask ending");
			}catch(RemoteException e){
				e.printStackTrace();
			}
		}
	};

	/*
	 * serials send task 子线程
	 * 线程内容：每过1s周期，进入该子线程，如果有需要发送的数据给终端门锁，则通过USB发送数据给USB收发模块。
	 * 注意：不需要在线程中更新UI，所以直接实现一个timerTask即可。
	 */
	TimerTask serialssendtask = new TimerTask(){
		@Override
		public void run(){
			if(!serialssendbuf.isEmpty()){
				// Log.d(LOG_TAG, serialssendbuf.get(0));
				String[] sendbuftemp = serialssendbuf.get(0).split("\\|");
				serialssendbuf.removeFirst();
				if(sendbuftemp.length == 2){
					//Log.d(LOG_TAG, sendbuftemp[0]+"\t"+sendbuftemp[1]);
					try{
						ch340AndroidDriver.WriteData(app.packdata(sendbuftemp[0], sendbuftemp[1]), 
															app.packdata(sendbuftemp[0], sendbuftemp[1]).length);
					}catch(IOException e){
						e.printStackTrace();
						Log.e(LOG_TAG, "Send failed");
					}
					
				}
			}
			if(app.wdyoprateflag>0)
			{
				// Log.d(LOG_TAG,"here is app.lockoperateflag>0");
				Message msg = new Message();
                msg.what = MESSAGE_DELAY;
                delayhandler.sendMessageDelayed(msg,20000);
                app.wdyoprateflag=0;
			}
		}
	};

	/*
	 * heart package upload 子线程
	 * 线程内容：每过60s=1min周期，进入该子线程，上传心跳包。
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给handler来更新UI
	 */
	public class HeartpackTask extends TimerTask {
		@Override
		public void run(){
			//not necessary
			// LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			// location = manager.getLastKnownLocation(bestProvider);
			// updateLocation(location);

			if(loginfo.haswarnGet().equals("0")){
				for (int i = 0; i < warntypecnt.length; i++){
					warntypecnt[i] = 0;
				}
			}
			HashMap<String, String> tem = loginfo.logInfoGet();
			Log.d("wdy", tem.toString());
			httpUtils.doPostAsyn(url, tem, new httpUtils.HttpCallBackListener() {
                @Override
                public void onFinish(String result) {
               	Message message = new Message();
               		message.what = MESSAGE_HEARTPACKAGE;
                	message.obj = result;
                	handler.sendMessage(message);
                }

                @Override
           	    public void onError(Exception e) {
                }
	        });
		}							
	}

	/*
	 * warnpackage upload 子线程
	 * 线程内容：每过20s周期，进入该子线程，上传报警包。
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给handler来更新UI
	 */
	public class WarnpackTask extends TimerTask {
		@Override
		public void run() {
			if (!warnmsgbuf.isEmpty()){
				Log.d("wdy","---> here is WarnpackTask");
				Log.d("wdy", warnmsgbuf.get(0).toString());
				httpUtils.doPostAsyn(url, warnmsgbuf.get(0), new httpUtils.HttpCallBackListener() {
	                @Override
	                public void onFinish(String result) {
	               	Message message = new Message();
	               		message.what = MESSAGE_WARNPACKAGE;
	                	message.obj = result;
	                	handler.sendMessage(message);
	                	warnmsgbuf.removeFirst();
	                }

	                @Override
	           	    public void onError(Exception e) {
	                }
	        	});
			}else{
				Message message = new Message();
           		message.what = MESSAGE_NOWARN;
            	handler.sendMessage(message);
			}
			
		}
	}

	/*
	 * parameters upload子线程
	 * 线程内容：每过？？周期，进入该子线程，上传变量包。
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给handler来更新UI
	 */
	public class ParamspackTask extends TimerTask {
		@Override
		public void run() {
			// Log.d(LOG_TAG, loginfo.logInfoGet().toString());
			httpUtils.doPostAsyn(url, loginfo.logInfoGet(), new httpUtils.HttpCallBackListener() {
                @Override
                public void onFinish(String result) {
               		Message message = new Message();
               		message.what = MESSAGE_PARAMSPACKAGE;
                	message.obj = result;
                	handler.sendMessage(message);
                }

                @Override
           	    public void onError(Exception e) {
                }
	        });
		}
	}

	/*
	 * 唤醒Hi-Cam子线程
	 * 线程内容：每过90s周期，进入该子线程，检查是否有收到广播。如果没有就要重新唤醒app
	 */
	public class WakeUpAppTask extends TimerTask{
		@Override
		public void run() {
			// Log.d("wdy","hasRecBro= "+hasRecBro);
			if(hasRecBro){
				hasRecBro=false;
				// Log.d("wdy","isFore = "+isRunningForeground(LedActivity.this));
	        	if (!isRunningForeground(LedActivity.this)) {
	                //获取ActivityManager
	                ActivityManager mAm = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
	                //获得当前运行的task
	                List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
	                for (ActivityManager.RunningTaskInfo rti : taskList) {
	                    //找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
	                    if (rti.topActivity.getPackageName().equals(getPackageName())) {
	                        mAm.moveTaskToFront(rti.id, 0);
	                        return;
	                    }
	                }
	                //若没有找到运行的task，用户结束了task或被系统释放，则重新启动mainactivity
	                Intent resultIntent = new Intent(LedActivity.this, LedActivity.class);
	                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	                startActivity(resultIntent);
	            }
			}
			else{
				PackageManager packageManager = getPackageManager();
	            Intent intent = packageManager.getLaunchIntentForPackage("cn.assassing.camtest");
	            if (intent != null) {
	                startActivity(intent);
	            }
	            // Log.d("wdy","open hicam end");
			}
		}
	}
	/*
	 * test upload子线程
	 * 线程内容：每过？？周期，进入该子线程，上传测试包。
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给handler来更新UI
	 */
    public class TestTask implements Runnable {
    	@Override
    	public void run(){
    		// Log.d(LOG_TAG, "testtask");
    		// if(testmsgbuf.isEmpty()){
    		// 	logInfo logInfo_test = new logInfo();
    		// 	testmsgbuf.add(logInfo_test.logInfoGet());

    		// }
			if(testloginfo!=null){
				// Log.d(LOG_TAG, "In TestTask "+testloginfo.toString());
				// Log.d(LOG_TAG, "In TestTask "+testloginfo.logInfoGet().toString());
				httpUtils.doPostAsyn(testurl, testloginfo.logInfoGet(), new httpUtils.HttpCallBackListener() {
                    @Override//testurl
                    public void onFinish(String result) {
                   		// Message message = new Message();
                   		// message.what = MESSAGE_TESTPACKAGE;
                    	// message.obj = result;
                    	// handler.sendMessage(message);
                    	// // testmsgbuf.removeFirst();
                    	// Log.d(LOG_TAG,"result: "+result);
                    }

                    @Override
               	    public void onError(Exception e) {
               	    	Log.e(LOG_TAG, "test SEND FAILED");
                    }
		        });
			}
    		
    	}
    }


	/*
	 * serials read thread子线程
	 * 线程内容：检查接受到的来自终端的数据包正确性，如果正确才会继续发送message给sehandler处理。
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给sehandler来更新UI
	 */
	private class ReadThread implements Runnable{
		@Override
		public void run(){
			List<String> data = new ArrayList<String>();
			LinkedList<Integer> siz = new LinkedList<Integer>();
			int sum = 0x00;
			int mystatus = 0x00;
			int buflen = 0;
			while(!Thread.currentThread().isInterrupted()){
				try{
					Thread.sleep(50);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				int size;
				String hv;
				//try{
					byte[] buffer = new byte[64];
					if (ch340AndroidDriver == null) return;
					size = ch340AndroidDriver.ReadData(buffer, LEN);
					if(size > 0){
						for(int i=0; i<size;i++){
							int s = buffer[i] & 0xFF;
							siz.add(s);
						}
						//Log.d("Serials", "data rev: "+ bytes2HexString(buffer, size));
						//if(siz.size() > 11){
						while(!siz.isEmpty()){
							switch(mystatus){
								case 0x00:
									if(siz.get(0) == 0x80){
										data.clear();
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										buflen = 0;
										sum = 0x00;
										mystatus = 0x01;
									}
									siz.removeFirst();
									break;
								case 0x01:
									if(siz.get(0) == 0x05 || siz.get(0) == 0x07){
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										sum += siz.get(0);
										mystatus = 0x02;
										siz.removeFirst();
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x02:
									if(siz.get(0) == 0x07){
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										sum += siz.get(0);
										mystatus = 0x03;
										buflen = siz.get(0);
										siz.removeFirst();
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x03:
									if(siz.get(0) == 0x02){//netId
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										sum += siz.get(0);
										mystatus = 0x04;
										buflen -= 1;
										siz.removeFirst();
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x04:
									if(siz.size() > 3){
										for(int i = 0; i < 4; i++){
											hv = Integer.toHexString(siz.get(i));
											if(hv.length()<2){
												data.add("0"+hv);
											}else{
												data.add(hv);
											}
											sum += siz.get(i);
											buflen -= 1;
										}
										for(int i = 0; i < 4; i++){
											siz.removeFirst();
										}
										mystatus = 0x05;
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x05:
									hv = Integer.toHexString(siz.get(0));
									if(hv.length()<2){
										data.add("0"+hv);
									}else{
										data.add(hv);
									}
									sum += siz.get(0);
									mystatus = 0x06;
									buflen -= 1;
									siz.removeFirst();
									break;
								case 0x06:
									hv = Integer.toHexString(siz.get(0));
									if(hv.length()<2){
										data.add("0"+hv);
									}else{
										data.add(hv);
									}
									sum += siz.get(0);
									buflen -= 1;
									if(buflen == 0x00){
										mystatus = 0x07;
										siz.removeFirst();
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x07:
									if((sum & 0xFF) == (siz.get(0) & 0xFF)){
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										mystatus = 0x08;
										siz.removeFirst();
									}else{
										data.clear();
										mystatus = 0x00;
									}
									break;
								case 0x08:
									if(siz.get(0) == 0x81){
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										//Log.d(LOG_TAG, "send before"+data.toString());
										ArrayList<String> datatemp = new ArrayList<String>();
										for(int i = 0; i < data.size(); i++){
											datatemp.add(data.get(i));
										}
										Message msg = new Message();
										msg.what = 1;
										msg.obj = datatemp;
										sehandler.sendMessage(msg);
										//data.clear();
										siz.removeFirst();
									}else{
										data.clear();
									}
									mystatus = 0x00;
									break;
								default:
									data.clear();
									if(siz.get(0) == 0x80){
										hv = Integer.toHexString(siz.get(0));
										if(hv.length()<2){
											data.add("0"+hv);
										}else{
											data.add(hv);
										}
										buflen = 0;
										sum = 0x00;
										mystatus = 0x01;
										siz.removeFirst();
									}else{
										mystatus = 0x00;
									}
									break;
							}
						}
							//siz.clear();
						//}	
						//Log.d(LOG_TAG,String.valueOf(size)+" "+siz.toString());
					}

				/*}catch (IOException e){
					e.printStackTrace();
					return;
				}*/
			}
		}
	}

	/*
	 * can总线子线程
	 * 线程内容：
	 * 注意：需要在线程进行同时更新UI，所以通过message传递消息给canhandler来更新UI
	 */
	private class CanRev implements Runnable{
	    @Override
	    public void run(){
    		//
		    while(true){
				try{
					//Thread.sleep(1000);
					// Log.d("wdy","--->in CanRev");
					ret = mycanservice.mycandump(0x00000000,0x00000000);
					if(ret == 0){
						Message msg = new Message();
				    	msg.what = mycanservice.get_id();
				    	// Log.d("can","--->can id: "+msg.what);
						List<Integer> res = new ArrayList<Integer>();
						for(int i=0;i<8;i++){
							res.add(mycanservice.get_data(i));
						}
						msg.obj = res;
						canhandler.sendMessage(msg);
					}
				}catch(RemoteException e){
					//Log.d(TAG,"rev data failed");
					e.printStackTrace();
				}
		    }
		    
    	}
    }









							

/******************************************************************************************
 * 以上是各种子线程定义，以下是各种handler
*******************************************************************************************/
	/*
     * wdytimer handler
     * handler内容：获取USB接收到的数据，并更新UI
     */
	Handler delayhandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			// Log.d(LOG_TAG,"--->here is delayhandler");
			if(msg.what == MESSAGE_DELAY){
				if(!operateIsOk){
					app.reparams.put("operate", "1"); //operate failed
					lockwarncnt = 0;
					// Log.d(LOG_TAG, app.reparams.toString());
					httpUtils.doPostAsyn(app.url, app.reparams, new httpUtils.HttpCallBackListener() {
			            @Override
			            public void onFinish(String result) {
			                Message message = new Message();
			                message.what = MESSAGE_LOCKCMDOPERATE;
			                message.obj=result;
			                handler.sendMessage(message);  
			            }

			            @Override
			            public void onError(Exception e) {
			            }

				    });
				    app.lockoperateflag = 0;
				}
				else
					operateIsOk=false;
			}
		}
	};


    /*
     * serials handler
     * handler内容：获取USB接收到的数据，并更新UI
     */
    Handler sehandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		if(msg.what == 1) {
    			ArrayList<String> data = (ArrayList<String>) msg.obj;
	    		// Log.d("Serials", data.toString());
	    		String flag = data.get(8);
	    		String devid = data.get(4)+data.get(5)+data.get(6)+data.get(7);
	    		if(flag.equals("00")){
	    			if(!data.get(1).equals("05")){
	    				// Log.d(LOG_TAG,"---> here is sehandler 07");
	    				if(devid.equals("55667788")){	
							// if(data.get(9).equals("00")){
							// 	if(lockstruct[0].getlockStatus().equals("1")){
							// 		is_warned_wdy=true;
							// 	}
							// 	lockstruct[0].setlockStatus("0");
							// 	tv[0].setText(lockstruct[0].getlockName()+"\t"+lockstruct[0].getlockStatus());
							// }else if(data.get(9).equals("01")){
							// 	lockstruct[0].setlockStatus("1");
							// 	tv[0].setText(lockstruct[0].getlockName()+"\t"+lockstruct[0].getlockStatus());
							// }
						}else if(devid.equals("55667789")){
							if(data.get(9).equals("00")){
								Log.d(LOG_TAG,"---> before:"+lockstruct[3].getlockStatus());
		    					if(lockstruct[3].getlockStatus().equals("1")){
		    						is_warned_wdy=true;		    						
		    						Log.d(LOG_TAG,"--->is_warned_wdy=true;");
		    					}
								lockstruct[3].setlockStatus("0");
								tv_lock.setText(lockstruct[3].getlockName()+"\t"+"opened");
								//tv_lock.setText(lockstruct[3].getlockName()+"\t"+(lockstruct[3].getlockStatus().equals("1"))?"closed":"opened");
							}else if(data.get(9).equals("01")){
								lockstruct[3].setlockStatus("1");
								tv_lock.setText(lockstruct[3].getlockName()+"\t"+"closed");
							}
							
						}else if(devid.equals("55667790")){
							// if(data.get(9).equals("00")){
		    	// 				if(lockstruct[4].getlockStatus().equals("1"))
		    	// 					is_warned_wdy=true;
							// 	lockstruct[4].setlockStatus("0");
							// 	tv[4].setText(lockstruct[4].getlockName()+"\t"+lockstruct[4].getlockStatus());
							// }else if(data.get(9).equals("01")){
							// 	lockstruct[4].setlockStatus("1");
							// 	tv[4].setText(lockstruct[4].getlockName()+"\t"+lockstruct[4].getlockStatus());
							// }							
						}
	    			}else if(data.get(1).equals("05")){
	    				// Log.d(LOG_TAG,"---> here is sehandler 05");
	    				opparams.put("trucknumber", "浙A1234");
	    				if(devid.equals("55667788")){	
							// if(data.get(9).equals("00")){
							// 	//TODO
							// }else{
							// 	lockstruct[0].setlockStatus("1");
							// 	tv[0].setText(lockstruct[0].getlockName()+"\t"+lockstruct[0].getlockStatus());
							// }
						}else if(devid.equals("55667789")){
							if(data.get(9).equals("00")){
								// opparams.put("type", "0");
								// httpUtils.doPostAsyn(opurl, opparams, new httpUtils.HttpCallBackListener() {
						  //           @Override
						  //           public void onFinish(String result) {
						  //               Message message = new Message();
						  //               message.what = MESSAGE_OPPARAMS;
						  //               message.obj=result;
						  //               handler.sendMessage(message);  
						  //           }

						  //           @Override
						  //           public void onError(Exception e) {
						  //           }

						  //   	});
    operateIsOk=true;
	app.reparams.put("operate", "0"); //operate success
	// Log.d(LOG_TAG, app.reparams.toString());
	httpUtils.doPostAsyn(app.url, app.reparams, new httpUtils.HttpCallBackListener() {
        @Override
        public void onFinish(String result) {
            Message message = new Message();
            message.what = MESSAGE_LOCKCMDOPERATE;
            message.obj=result;
            handler.sendMessage(message);  
        }

        @Override
        public void onError(Exception e) {
        }

    });
    app.wdyoprateflag = 0;
							}else{
								lockstatustemp[3] = "1";
								lockstruct[3].setlockStatus("1");
								tv_lock.setText(lockstruct[3].getlockName()+"\t"+"closed");
							}
						}else if(devid.equals("55667790")){
							if(data.get(9).equals("00")){
								// opparams.put("type", "2");
								// httpUtils.doPostAsyn(opurl, opparams, new httpUtils.HttpCallBackListener() {
						  //           @Override
						  //           public void onFinish(String result) {
						  //               Message message = new Message();
						  //               message.what = MESSAGE_OPPARAMS;
						  //               message.obj=result;
						  //               handler.sendMessage(message);  
						  //           }

						  //           @Override
						  //           public void onError(Exception e) {
						  //           }

						  //   	});
	operateIsOk=true;
	app.reparams.put("operate", "0"); //operate success
	// Log.d(LOG_TAG, app.reparams.toString());
	httpUtils.doPostAsyn(app.url, app.reparams, new httpUtils.HttpCallBackListener() {
        @Override
        public void onFinish(String result) {
            Message message = new Message();
            message.what = MESSAGE_LOCKCMDOPERATE;
            message.obj=result;
            handler.sendMessage(message);  
        }

        @Override
        public void onError(Exception e) {
        }

    });
    app.wdyoprateflag = 0;
							}else{
								lockstatustemp[4] = "1";
								lockstruct[4].setlockStatus("1");
								// tv[4].setText(lockstruct[4].getlockName()+"\t"+lockstruct[4].getlockStatus());
							}
						}
	    			}
					//Compare status 
					//warnflagSet and mlocalcapture.setCapturePath(0)
					//operate success or failed
					loginfo.lockSet(lockstruct);
					Log.d(LOG_TAG,"--->is_warned_wdy: "+is_warned_wdy);
					if(is_warned_wdy){
						is_warned_wdy=false;
						loginfo.typeflagSet("1");
						loginfo.haswarnSet("1");
						loginfo.typeSet("1");
						warnmsgbuf.add(loginfo.logInfoGet());
						tv_lock_warn.setText("异常");
						loginfo.haswarnSet("0");
						Log.d(LOG_TAG,"--->warnmsgbuf added");
						//let camera app catch videos
						Intent intent=new Intent();
						intent.setAction("action_scsy_warn");
						LedActivity.this.getApplicationContext().sendBroadcast(intent);
				Log.d("wdy","action_scsy_warn");
					}
	    		}else if(flag.equals("01")){
    				int leakstatusval = Integer.parseInt(data.get(9),16);
    				loginfo.leakstatusSet(String.valueOf(leakstatusval));
    				tv_liquid.setText(String.valueOf(leakstatusval));
    				// Compare leakstatus and send 
    				Log.d("wdy","leakstatusval= "+leakstatusval+"wirelessflag"+app.wirelessflag);
    				if(leakstatusval > 95 /*&& app.wirelessflag == 1*/){
    					Log.d("wdy","in if 1");
    					leakstatuscnt += 1;
    					if(warntypecnt[2] < 1 && leakstatuscnt > 0){
    						Log.d("wdy","in if 2");
    						loginfo.haswarnSet("1");
    						loginfo.typeSet("2");
    						warnmsgbuf.add(loginfo.logInfoGet());
							tv_liquid_warn.setText("异常");
							loginfo.haswarnSet("0");
    						warntypecnt[2] += 1;
    						leakstatuscnt = 0;
    					}
    					Log.d("wdy","--->leak warn!");
    				}else{
    					leakstatuscnt = 0;
    				}
	    		}
	    		else if(flag.equals("04")){
	    			//code here
	    	/*if(cid != null){
			tv_log.append("cid: "+cid+"\n");
			cidparams.put("trucknumber","浙A1234");
			cidparams.put("type", "100");
			cidparams.put("cid", cid);
			// Log.d(LOG_TAG, cid);
			
			httpUtils.doPostAsyn(getuiurl, cidparams, new httpUtils.HttpCallBackListener() {
	            @Override
	            public void onFinish(String result) {
	                Message message = new Message();
	                message.what = MESSAGE_GETUI;
	                message.obj=result;
	                handler.sendMessage(message);
	            }

	            @Override
	            public void onError(Exception e) {
	            }
	        });
		}
		else{
			Log.d("LOG_TAG","cid failed!");
		}*/

					requestparams.put("trucknumber","浙A1234");
					requestparams.put("type","0");
					requestparams.put("requestdesc","0");

					httpUtils.doPostAsyn(requesturl, requestparams, new httpUtils.HttpCallBackListener() {
			            @Override
			            public void onFinish(String result) {
			                Message message = new Message();
			                message.what = MESSAGE_REQUEST;
			                message.obj=result;
			                handler.sendMessage(message);
			            }
			            @Override
			            public void onError(Exception e) {
			            }
	        		});
					tv_log.append("requestparams sent!\n");
	    		}
    		}
    	}
    };	

	/*
	 * can handler
	 * handler内容：获取can总线子线程发送的message，并更新UI
	 */
	public class mycanHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// Log.d("wdy","--->in mycanHandler");
            if (msg.what != 0) {
				long id = (Integer) msg.what + 2147483648L;
				System.out.println(Long.toHexString(id));
				if(id == 0x18FEF433){
					// Log.d("wdy","--->in 433!");
					ArrayList<Integer> res =(ArrayList<Integer>) msg.obj;
					Log.d("can", res.toString());
					int tirepos = res.get(0);
					int tirepre = (int)res.get(1)*8;
					double tiretem = ((int)res.get(3)*256+(int)res.get(2))*0.03125-273;
					double tirev = ((int)res.get(6)*256+(int)res.get(5))*0.1;
					int tiretype = res.get(7) >> 5;
					if(tirepos < tirepressure.length){
						tirepressure[tirepos].settireVal(String.valueOf(tirepre));
						tv[tirepos*2].setText(String.valueOf(tirepre));
						tirepressure[tirepos].settireTempVal(String.format("%.2f", tiretem));
						tv[1+tirepos*2].setText(String.format("%.2f", tiretem)+" `C");
					}
					loginfo.tireSet(tirepressure);

					if(tiretype!=2 && tiretype!=4){
						if(warntypecnt[3] < 2){
							loginfo.haswarnSet("1");
							loginfo.typeSet("3");
							warnmsgbuf.add(loginfo.logInfoGet());
							tv_tire_warn.setText("异常");
							Log.d("can","---> 433 tire warn! type: "+tiretype);
							loginfo.haswarnSet("0");
							warntypecnt[3] += 1;
						}
					}
					// if(tv_log != null){
					// 	 Log.d(LOG_TAG, Long.toHexString(id)+" "+tirepos+" "+String.format("%.3f",tirepre)+"kPa "+String.format("%.2f", tiretem)
					// 	 	+"\u00b0"+"C "+tirev+"Pa/s "+tiretype+"\n");
					// 	tv_log.append(Long.toHexString(id)+" "+tirepos+" "+tirepre+"kPa "+tiretem+"\u00b0"+"C "+tirev+"Pa/s "+tiretype+"\n");
					// }
					// Compare the tirevalue and tiretemperature
				}else if(id == 0x18FEF533){
					if(warntypecnt[3] < 2){
						loginfo.haswarnSet("1");
						loginfo.typeSet("3");
						warnmsgbuf.add(loginfo.logInfoGet());
						tv_tire_warn.setText("异常");
						Log.d("can","---> 533 tire warn!");
						loginfo.haswarnSet("0");
						warntypecnt[3] += 1;
					}
					
				}else if(id == 0x18DAF100){
					ArrayList<Integer> res = (ArrayList<Integer>) msg.obj;
					int pid = res.get(2);
					switch (pid){
						//TODO pid add
						//case 0x04: int loadval = (int)res.get(3)
						case 0x05: int temp = (int)res.get(3)-40;
							  if(tv_log != null){
								//Log.d("OBD", Long.toHexString(id)+" "+Integer.toHexString(pid)+" "+String.valueOf(temp)+"\u00b0"+"C\n");
							  }
							  break;
						case 0x0C: int w = ((int)res.get(3)*256+(int)res.get(4))/4;
							  if(tv_log != null){
								//Log.d("OBD", Long.toHexString(id)+" "+Integer.toHexString(pid)+" "+String.valueOf(w)+"rpm\n");
							  }
							  break;
						case 0x0D: v = res.get(3);
							  loginfo.speedSet(v);
							  //TODO Compare the speed to get stop/high-speed/exhausted drive/...
							  //and send
							  if(v == 0 && speed == 0 ) {
							  	if(warntypecnt[6] < 2) {
									loginfo.haswarnSet("1");
								  	loginfo.typeSet("6");
								  	warnmsgbuf.add(loginfo.logInfoGet());
									loginfo.haswarnSet("0");
								  	warntypecnt[6] += 1;
							  	}
							  }else if(v > 80 || speed > 80) {
							  	if(warntypecnt[5] < 2) {
									loginfo.haswarnSet("1");
								  	loginfo.typeSet("5");
								  	warnmsgbuf.add(loginfo.logInfoGet());
									loginfo.haswarnSet("0");
								  	warntypecnt[5] += 1;
							  	}
							  }
							  
							  if(tv_log != null){
								//Log.d("OBD", Long.toHexString(id)+" "+Integer.toHexString(pid)+" "+String.valueOf(v)+"km/s\n");
							  }
							  break;
						case 0x21: int distance = (int)res.get(3)*256+(int)res.get(4);
							  if(disCnt == 0){
							  	distance0 = distance;
							  	disCnt = 1;
							  }else{
							  	loginfo.distanceSet(distance-distance0);
							  }
							  if(tv_log != null){
								//Log.d("OBD", Long.toHexString(id)+" "+Integer.toHexString(pid)+" "+String.valueOf(distance)+"km\n");
							  }
							  break;
						case 0x2F: double fuelLevel = (int)res.get(3)*100/255;
							  loginfo.fuelvolSet(fuelLevel);
							  // To Compare the fuellevel and send
							  if(fuelLevel - fuelleveltemp > 1){
							  	if(warntypecnt[4] < 1 ) {
									loginfo.haswarnSet("1");
							  		loginfo.typeflagSet("4");
								  	loginfo.typeSet("4");
								  	warnmsgbuf.add(loginfo.logInfoGet());
									loginfo.haswarnSet("0");
							  		// sidcnt = 0;
							  		// mlocalcapture.setCapturePath(0);
							  		Intent intent=new Intent();
									intent.setAction("action_scsy_warn");
									LedActivity.this.getApplicationContext().sendBroadcast(intent);
									Log.d("wdy","action_scsy_warn");
							  		warntypecnt[4] += 1 ;
							  	}
							  }
							  fuelleveltemp = fuelLevel;
							  if(tv_log != null){
								//Log.d("OBD", Long.toHexString(id)+" "+Integer.toHexString(pid)+" "+String.valueOf(fuelLevel)+"%\n");
							  }
							  break;
						default: break;
					}
				}else{
					if(tv_log != null){
						//tv_log.append(Long.toHexString(id)+"\n");
						//Log.d(LOG_TAG, Long.toHexString(id)+"\n");
					}
				}
				
            }
        }
    }

	/*
	 * http handler
     * handler内容：获取各种其他类型的msg，并更新UI
	 */
	Handler handler = new Handler(){
		@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what){
    			case MESSAGE_HEARTPACKAGE:{
    				testloginfo=loginfo;
    				String s =(String) msg.obj;
	        		//Toast.makeText(LedActivity.this,s,Toast.LENGTH_SHORT).show();
					//System.out.println(s);
					Log.d(LOG_TAG, "heartpacktask"+s);
					tv_log.append("heartpacktask"+s+"\n");
					// if(s != null){				
					// 	tv[5].setText(s+String.valueOf(cnt));	
					// 	cnt += 1;
					// 	//Toast.makeText(LedActivity.this,s,Toast.LENGTH_SHORT).show();
					// } 
    			}break;
    			case MESSAGE_FILEUPLOAD:{
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, s);
    				try{
    					JSONObject js = new JSONObject(s);
    					JSONObject cont = js.getJSONObject("content");
    					int picsid = cont.getInt("sid");
    					// if(sidcnt < 3){
    					// 	snapsid[sidcnt++] = String.valueOf(picsid);
    					// }
    					if(sidcnt == 3){
    						// loginfo.snapshotSet(snapsid);
    						sidcnt = 0;
    						if(loginfo.typeflagGet() != null){
    							Log.d(LOG_TAG, "typeflagGet"+loginfo.typeflagGet());
    							if(loginfo.typeflagGet().equals("0")){
    								loginfo.haswarnSet("1");
    								loginfo.typeSet("1");
									// testmsgbuf.add(loginfo.logInfoGet());
    							}else{
    								loginfo.haswarnSet("1");
    								loginfo.typeSet(loginfo.typeflagGet());
    								warnmsgbuf.add(loginfo.logInfoGet());
    							}
    							loginfo.typeflagSet(null);
    							
    						}
    						//TODO trig capture end and delete the capture file
    						// mlocalcapture.setCapturePath(2);
    					}
    					Log.d(LOG_TAG, String.valueOf(picsid)+"\t"+String.valueOf(sidcnt));
    				}catch(JSONException e){
    					e.printStackTrace();
    				}		
    			}break;
    			case MESSAGE_GETUI:{
    				String s =(String) msg.obj;
    				Log.d("cid", "getuicidup"+s);
    			}break;
    			case MESSAGE_WARNPACKAGE:{
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "warn"+s);
    			}break;
    			case MESSAGE_NOWARN:{
					tv_lock_warn.setText("无");
					tv_liquid_warn.setText("无");
					tv_tire_warn.setText("无");
    			}break;
    			case MESSAGE_PARAMSPACKAGE:{
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "params"+s);
    			}break;
    			case MESSAGE_TESTPACKAGE: {
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "test"+s);
    			}break;
    			case MESSAGE_LOCKCMDOPERATE: {
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "lockcmd"+s);
    			}break;
    			case MESSAGE_OPPARAMS: {
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "opparams"+s);
    			}break;
    			case MESSAGE_USB_INSERT: {
    				initUSB();
    				Log.d(LOG_TAG, "initUSB");
    			}break;
    			case MESSAGE_USB_UNINSERT: {
    				ch340AndroidDriver.CloseDevice();
    				Log.d(LOG_TAG, "CloseDevice");
    			}break;
    			case MESSAGE_REQUEST: {
    				String s =(String) msg.obj;
    				Log.d(LOG_TAG, "requestparams"+s);
    			}break;
    			default: break;
    		}
    	}
	};
	













/******************************************************************************************
 * 以上是各种handler，以下是其他自定义函数
*******************************************************************************************/
	
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			//Toast.makeText(LedActivity.this, action, Toast.LENGTH_LONG).show();
			// Log.e(LOG_TAG, "action:" + action);
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				UsbDevice deviceFound = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				Toast.makeText(
						LedActivity.this,
						"ACTION_USB_DEVICE_ATTACHED: \n"
								+ deviceFound.toString(), Toast.LENGTH_LONG)
						.show();
				handler.sendEmptyMessage(MESSAGE_USB_INSERT);
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				Toast.makeText(LedActivity.this,
						"ACTION_USB_DEVICE_DETACHED: \n" + device.toString(),
						Toast.LENGTH_LONG).show();
				handler.sendEmptyMessage(MESSAGE_USB_UNINSERT);
			}
		}
	};
	
	//init USB to serials
	private void initUSB() {
		UsbDevice device = ch340AndroidDriver.EnumerateDevice();// 枚举设备，获取USB设备
		ch340AndroidDriver.OpenDevice(device);// 打开并连接USB
		if (ch340AndroidDriver.isConnected())           {
			boolean flags = ch340AndroidDriver.UartInit();// 初始化串口
			if (!flags) {
				Log.e(LOG_TAG, "Init Uart Error");
				/*Toast.makeText(LedActivity.this, "Init Uart Error",
						Toast.LENGTH_SHORT).show();*/
			} else {// 配置串口
				if (ch340AndroidDriver.SetConfig(baurt, (byte) 8, (byte) 1,
						(byte) 0, (byte) 0)) {
					Log.e(LOG_TAG, "Uart Configed");
				}
			}
		} else {
			Log.e(LOG_TAG, "ch340AndroidDriver not connected");
		}
	}

	public String bytes2HexString(byte[] buf, int length){
		String result = new String();
		if(buf != null){
			for(int i = 0; i < length; i++) {
				result = result + ((Integer.toHexString(buf[i] < 0? buf[i]+256 : buf[i])).length() == 1 ? 
							"0"+(Integer.toHexString(buf[i] < 0? buf[i]+256 : buf[i])) : 
							(Integer.toHexString(buf[i] < 0? buf[i]+256 : buf[i]))) + " ";
			}
			return result;
		}
		return "";
	}

	private Criteria getCriteria(){
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);	//other choices: ACCURACY_COARSE
		criteria.setAltitudeRequired(true);
		criteria.setSpeedRequired(true);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		return criteria;
	}
	
	/*
     * gps update函数
     * 函数内容：每当GPS位置变化突破临界值后，记录下GPS数据。
     */
	private void updateLocation(Location location){
		if(location != null){
			//tv_log.append(location.toString());
			gpsx = String.format("%.9f", location.getLongitude());
			gpsy = String.format("%.9f", location.getLatitude());
			speed = location.getSpeed();
			//String gpsInfo="gpsx: "+gpsx+" "+"gpsy: "+gpsy+" "+"gpsspeed: "+speed;
			//Toast.makeText(LedActivity.this, gpsInfo, Toast.LENGTH_SHORT).show();
			tv_gpsx.setText(gpsx);
			tv_gpsy.setText(gpsy);
			tv_gpsv.setText(speed+"");
			loginfo.gpsSet(gpsx,gpsy);
			loginfo.gpsspeedSet((int)speed);
		}else{
			Log.d("GPS", "no location object");
		}
	}
	//gps监听器
	LocationListener locationlistener = new LocationListener(){

		@Override
		public void onLocationChanged(Location location){
			// Log.i("GPS","onLocationChanged");
			updateLocation(location);
		}

		@Override
		public void onProviderDisabled(String arg0){
			// Log.e("GPS", arg0);
		}

		@Override
		public void onProviderEnabled(String provider){
			// Log.i("GPS", provider);
			location = lm1.getLastKnownLocation(provider);
			updateLocation(location);
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2){
			// Log.i("GPS", "onStatusChanged");
		}
	};
	
	private class AwakeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(AWAKE_ACTION)){
            	hasRecBro=true;
            	// Toast.makeText(LedActivity.this,"Broadcast Received!",Toast.LENGTH_SHORT).show();
            	// Log.d("wdy","Broadcast Received!");
            }
        }
    }
	
	public static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager.getRunningAppProcesses();
        // 枚举进程
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfos) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(context.getApplicationInfo().processName)) {
                    return true;
                }
            }
        }
        return false;
    }


}

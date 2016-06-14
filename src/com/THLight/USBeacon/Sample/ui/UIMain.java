/** ============================================================== */
package com.THLight.USBeacon.Sample.ui;
/** ============================================================== */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.THLight.USBeacon.App.Lib.*;
import com.THLight.USBeacon.Sample.R;
import com.THLight.USBeacon.Sample.ScanediBeacon;
import com.THLight.USBeacon.Sample.THLApp;
import com.THLight.USBeacon.Sample.THLConfig;
import com.THLight.Util.THLLog;


/** ============================================================== */
public class UIMain extends Activity implements iBeaconScanManager.OniBeaconScan, USBeaconConnection.OnResponse
{
	/** this UUID is generate by Server while register a new account. */
	final UUID QUERY_UUID		= UUID.fromString("4E62E99E-5DD2-4E6E-A1CB-4A09882381AA");
	/** server http api url. */
	final String HTTP_API		= "http://www.usbeacon.com.tw/api/func";
	
	//**
	final String DSGBEACON_UUID   = "EF7CBAAC-0E15-4ECA-9429-957334A3C5BD";
	final static String DSG_MACADDRESS = "B4:99:4C:4C:16:41";
	public  String imageHeadURL = "http://120.114.186.13/photo/member/";
//	public  String imageHeadURL = "http://59.125.213.197/photo/member/";

	//drawer menu
	 private DrawerLayout layDrawer;
	 private ListView lstDrawer;
	 private ActionBarDrawerToggle drawerToggle;
	 private CharSequence mDrawerTitle;
	 private CharSequence mTitle;
	
	LayoutInflater inflater;
	View layout_above;
	private AlertDialog GHeadUI;
	private AlertDialog switch_UI;
	private int DelayTime=10000;
	private Handler NearPoP_handler = new Handler();
	private Handler PopUpThreadHandler;
	private HandlerThread PopUpThread;
	TextView text_status;
	TextView tv_userinfo;
	TextView tv_acount;
	TextView tv_position;
	EditText ev_acount;
	EditText ev_mail;
	Button btn_submitid;
	ImageView img_userhead;
	double RangeBound =20;
	String record="";
	String WEB_URL="http://120.114.186.13/app_/";
//	String WEB_URL="http://59.125.213.197/app_/";
	boolean blankvar=false;
	URL head_url ;
	String imei ="mikelin";
	boolean flag =false;
	static String instatus="";
	Button btn_info;
	CheckBox CB_service;
	String enterTime;
	int LogCode=0;
	int StateCode=0;
	boolean editOn=false;
	/*
	 * 0 haven't upload enter and didn't their memberNumber 
	 * 1 complete one of above terms
	 * 2 finish upload enter
	 * 3 finish upload leave
	 */
	
	private HttpUtils user_data;
	
	String startTime;
	long updateTime;
	int globalCounter=0;
	//**
	
	
			
	static String STORE_PATH	= Environment.getExternalStorageDirectory().toString()+ "/USBeaconSample/";
	
	final int REQ_ENABLE_BT		= 2000;
	final int REQ_ENABLE_WIFI	= 2001;
	
	final int MSG_SCAN_IBEACON			= 1000;
	final int MSG_UPDATE_BEACON_LIST	= 1001;
	final int MSG_START_SCAN_BEACON		= 2000;
	final int MSG_STOP_SCAN_BEACON		= 2001;
	final int MSG_SERVER_RESPONSE		= 3000;
	
	final int TIME_BEACON_TIMEOUT		= 10000;
	

	BeaconCheck BS ;
	
	THLApp App		= null;
	THLConfig Config= null;
	
	BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();

	/** scaner for scanning iBeacon around. */
	iBeaconScanManager miScaner	= null;
	
	/** USBeacon server. */
	USBeaconConnection mBServer	= new USBeaconConnection();
	
	USBeaconList mUSBList		= null;
	
	ListView mLVBLE= null;
	
	BLEListAdapter mListAdapter		= null;
	
	List<ScanediBeacon> miBeacons	= new ArrayList<ScanediBeacon>();
	
    
	
	/** ================================================ */
	Handler mHandler= new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case MSG_SCAN_IBEACON:
					{
						int timeForScaning		= msg.arg1;
						int nextTimeStartScan	= msg.arg2;
						
						miScaner.startScaniBeacon(timeForScaning);
						this.sendMessageDelayed(Message.obtain(msg), nextTimeStartScan);
					}
					break;
					
				case MSG_UPDATE_BEACON_LIST:
					synchronized(mListAdapter)
					{
						verifyiBeacons();
						mListAdapter.notifyDataSetChanged();
						mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
					}
					break;
				
				case MSG_SERVER_RESPONSE:
					switch(msg.arg1)
					{
						case USBeaconConnection.MSG_NETWORK_NOT_AVAILABLE:
							break;
							
						case USBeaconConnection.MSG_HAS_UPDATE:
//							Log.d("tag msg", msg.arg1+" "+msg.arg2);
							mBServer.downloadBeaconListFile();
							Toast.makeText(UIMain.this, "HAS_UPDATE.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_HAS_NO_UPDATE:
							Toast.makeText(UIMain.this, "No new BeaconList.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DOWNLOAD_FINISHED:
							break;
		
						case USBeaconConnection.MSG_DOWNLOAD_FAILED:
							Toast.makeText(UIMain.this, "Download file failed!", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FINISHED:
							{
								USBeaconList BList= mBServer.getUSBeaconList();

								if(null == BList)
								{
									Toast.makeText(UIMain.this, "Data Updated failed.", Toast.LENGTH_SHORT).show();
									THLLog.d("debug", "update failed.");
								}
								else if(BList.getList().isEmpty())
								{
									Toast.makeText(UIMain.this, "Data Updated but empty.", Toast.LENGTH_SHORT).show();
									Log.d("debug", "this account doesn't contain any devices.");
									THLLog.d("debug", "this account doesn't contain any devices.");
								}
								else
								{
									Toast.makeText(UIMain.this, "Data Updated("+ BList.getList().size()+ ")", Toast.LENGTH_SHORT).show();
									
									
									for(USBeaconData data : BList.getList())
									{	
										Log.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ "), "+" Note("+ data.DistData+")");
										THLLog.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ ")");
									}
								}
							}
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FAILED:
							Toast.makeText(UIMain.this, "UPDATE_FAILED!", Toast.LENGTH_SHORT).show();
							break;
						
					}
					break;
			}
//			Log.d("tag msg", msg.arg1+" "+msg.arg2);
		}
	};
	
	 private Runnable determinPoP= new Runnable(){
	    	@Override
	    	public void run(){
	    		NearPoP_handler.postDelayed(this, DelayTime);
	    		if(BS.InZone==1){
	    			if(blankvar){
	    			tv_position.setText("HotZone!!");
	    			blankvar=false;}
	    			else{
//	    			tv_position.setText("");
	    			blankvar=true;
	    			}
	    		}else{
	    		tv_position.setText("unknown");}
	    		Log.d("Doing",String.valueOf(BS.InZone)+"；"+StateCode);
	    		//判斷有正確進入區域，且尚未進行過登陸，打開介面然後休息30秒
	    		if(BS.InZone==1 && editOn!=true){
	    			PopUpInterface();
	            	Toast.makeText(UIMain.this, "位置: "+BS.getInZone(),Toast.LENGTH_SHORT).show();
	    		}
	    		
	    		
	    		}
	    		
	    		
	    	};
	
	/** ================================================ */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

//		BS = new BeaconCheck(this);
		startService(new Intent(this,BeaconService.class)); 
		inflater = getLayoutInflater();
		layout_above = inflater.inflate(R.layout.userinfo,null);
		
		//drawer initial
		initActionBar();
        initDrawer();
        initDrawerList();
		
//		CB_service(CheckBox);
		
		BS = new BeaconCheck();
		instatus="leave";
		
		App		= THLApp.getApp();
		Config	= THLApp.Config;
		
		/** create instance of iBeaconScanManager. */
		miScaner		= new iBeaconScanManager(this, this);
		
		mListAdapter	= new BLEListAdapter(this);
		
//		mLVBLE			= (ListView)findViewById(R.id.beacon_list);
//		mLVBLE.setAdapter(mListAdapter);
		text_status = (TextView)findViewById(R.id.tv_status);
		tv_acount = (TextView)findViewById(R.id.tv_acount);
		ev_acount = (EditText)findViewById(R.id.et_ID);
		ev_mail = (EditText)findViewById(R.id.et_mail);
		tv_position =(TextView)findViewById(R.id.tv_position);
		btn_info =(Button)findViewById(R.id.btn_info);
		btn_info.setVisibility(4);
		btn_submitid=(Button)findViewById(R.id.btn_submitid);
		btn_submitid.setVisibility(View.INVISIBLE);
		ev_acount.setVisibility(View.INVISIBLE);
		ev_mail.setVisibility(View.INVISIBLE);
		tv_position.setTextColor(android.graphics.Color.RED);
		
		try {
			if(checkStoreinfo()==1){
				Log.d("fr","read file success.");
				
				tv_acount.setText("Hi,"+ev_acount.getText());
			}
			else{
				first_enter();
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		btn_submitid.setOnClickListener(new Button.OnClickListener(){
//			@Override
//			public void onClick(View v){
//				try{
//                    FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+"Log_USBeacon.txt", false);
//                    BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
//                    bw.write(ev_acount.getText()+"\n");
//                    bw.write(ev_mail.getText()+"\n");bw.newLine();
//                    bw.close();
//                }catch(IOException e){
//                   e.printStackTrace();
////                }
//				
////				ev_acount.setVisibility(View.INVISIBLE);
////				ev_mail.setVisibility(View.INVISIBLE);
////				btn_submitid.setVisibility(View.INVISIBLE);
//			}
//		});
		PopUpThread = new HandlerThread("determinePopUp");
		PopUpThread.start();
		PopUpThreadHandler=new Handler(PopUpThread.getLooper());
        
		PopUpThreadHandler.post(determinPoP);
		
//		btn_info.setOnClickListener(new Button.OnClickListener(){ 
//
//            @Override
//
//            public void onClick(View v) {
//            	
//                
//                // TODO Auto-generated method stub
////            	PopUpInterface();
//            	Toast.makeText(UIMain.this, "位置: "+BS.getInZone(),Toast.LENGTH_SHORT).show();
//            	Log.d("Doing",String.valueOf(BS.getInZone()));}
//            	
//                     
////Log.d("hold","wait");
//        }); 

		GHeadUI =new AlertDialog.Builder(UIMain.this)
		.setTitle("進入提示")
	   .setMessage("確定登錄健身房紀錄")
	   .setView(layout_above)
	   .setNegativeButton("離開",new DialogInterface.OnClickListener() {
	   @Override
	   public void onClick(DialogInterface arg0, int arg1) {
	         // TODO Auto-generated method stub
		   Log.d("Doing",String.valueOf(StateCode)+BS.getInZone());
			  
				  Toast.makeText(UIMain.this, "我了解了",Toast.LENGTH_SHORT).show();
				  
				  updateRecord();
				  StateCode =0;
		      if(StateCode==0){
		    	  Toast.makeText(UIMain.this, "已更新離開資料",Toast.LENGTH_SHORT).show(); 
		      }
	         
	   }

	  })
	  .setPositiveButton("進入",new DialogInterface.OnClickListener() {
	  @Override
	  public void onClick(DialogInterface arg0, int arg1) {
	      // TODO Auto-generated method stub
		  Log.d("Doing",String.valueOf(StateCode)+BS.getInZone());
		  
			  Toast.makeText(UIMain.this, "我了解了",Toast.LENGTH_SHORT).show();
			  String currTime= getDateTime();
			  enterTime = currTime;
			  uploadRecord(currTime);
			  Log.d("dd","do upload function");
			  StateCode =1;
	      if(StateCode==1){
	    	  Toast.makeText(UIMain.this, "已上傳過進入資料",Toast.LENGTH_SHORT).show(); 
	      }
	  }

	 })
	  .setNeutralButton("更換使用者",new DialogInterface.OnClickListener() {
	  @Override
	  public void onClick(DialogInterface arg0, int arg1) {
	      // TODO Auto-generated method stub
		 
	      Toast.makeText(UIMain.this, "在下方表格填寫更改",Toast.LENGTH_SHORT).show();
//	      String url = WEB_URL + "saveIdRecord.php?" + "ar_fm_number=1" +    "&ar_enter_date=2005/1/1%2008:00:00" + 
//					 "&ar_leave_date=2005/1/1%2010:00:00"  ;
//	      HttpUtils.uploadRecord(url);
			Log.d("Doing","ok");
	  }
	  
	 }).create();
	}
	
	private void first_enter(){
//		ev_acount.setVisibility(View.VISIBLE);
//		ev_mail.setVisibility(View.VISIBLE);
//		btn_submitid.setVisibility(View.VISIBLE);
		
		editOn=true;
    	final View item = LayoutInflater.from(UIMain.this).inflate(R.layout.editlayout_acount, null);
        switch_UI =new AlertDialog.Builder(UIMain.this)
            .setTitle("請輸入")
            .setView(item)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText et_MID = (EditText) item.findViewById(R.id.et_changeID);
                    EditText et_Mmail = (EditText) item.findViewById(R.id.et_changeemail);
                    Toast.makeText(getApplicationContext(), getString(R.string.app_name) + et_MID.getText().toString()+" saved", Toast.LENGTH_SHORT).show();
                    ev_acount.setText(et_MID.getText());
                    ev_mail.setText(et_Mmail.getText());
                    
                    try{
                        FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+"Log_USBeacon.txt", false);
                        BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                        bw.write(et_MID.getText()+"\n");
                        bw.write(et_Mmail.getText()+"\n");bw.newLine();
                        bw.close();
                    }catch(IOException e){
                       e.printStackTrace();
                    }
                    
                }
            })
        .show();
        editOn=false;
		
	}
	
	private void initActionBar(){
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }
    private void initDrawer(){
        setContentView(R.layout.ui_main);
        layDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        lstDrawer = (ListView) findViewById(R.id.left_drawer);
        layDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mTitle = mDrawerTitle = getTitle();
        drawerToggle = new ActionBarDrawerToggle(
                this, 
                layDrawer,
                R.drawable.ic_drawer, 
                R.string.drawer_open,
                R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
            }
        };
        drawerToggle.syncState();
        layDrawer.setDrawerListener(drawerToggle);
    }
    
    private void initDrawerList(){
        String[] drawer_menu = this.getResources().getStringArray(R.array.drawer_menu);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawer_menu);
        lstDrawer.setAdapter(adapter);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        //home  undone
    	if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        //action buttons
        switch (item.getItemId()) {
        case R.id.action_refresh:
            //....
            break;
        case R.id.patch:
            //....
            break;
        case R.id.developer:
        	//....
        	break;
        default:
            break;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(PopUpThreadHandler !=null){
    		PopUpThreadHandler.removeCallbacks(determinPoP);
    		   	}
    	
    	if(PopUpThread != null){
    		PopUpThread.quit();
    	}
    }
	
//	private void selectItem(int position) {
//	    Fragment fragment = null;
//	    switch (position) {
//	    case 0:
//	        fragment = new FragmentApple();
//	        break;
//	    case 1:
//	        fragment = new FragmentBook();
//	        break;
//	    case 2:
//	        fragment = new FragmentCat();
//	        Bundle args = new Bundle();
//	        args.putString(FragmentCat.CAT_COLOR, "Brown");
//	        fragment.setArguments(args);
//	        break;
//	    default:
//	        //還沒製作的選項，fragment 是 null，直接返回
//	        return;
//	    }
//	    FragmentManager fragmentManager = getFragmentManager();
//	    fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
//	    // 更新被選擇項目，換標題文字，關閉選單
//	    lstDrawer.setItemChecked(position, true);
//	    setTitle(drawer_menu[position]);
//	    layDrawer.closeDrawer(lstDrawer);
//	}
//	@Override
//	public void setTitle(CharSequence title) {
//	    mTitle = title;
//	    getActionBar().setTitle(mTitle);
//	}
	
	public int checkStoreinfo() throws IOException{
		
		FileReader fr = null;
		try {
			fr = new FileReader(Environment.getExternalStorageDirectory().getPath()+"/"+"Log_USBeacon.txt");
			LogCode=1;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LogCode=0;
			e.printStackTrace();
			return LogCode;
		}
		BufferedReader br = new BufferedReader(fr);
		
		if (br.ready()) {
			ev_acount.setText(br.readLine());
		    Log.d("fr","ID"+ev_acount.getText());
			//System.out.println(br.readLine());
		 }
		if (br.ready()) {
			ev_mail.setText(br.readLine());
		    Log.d("fr","mail"+ev_mail.getText());
			//System.out.println(br.readLine());
		 }
		
		
		        fr.close();
		return LogCode;

	}
	
	public void PopUpInterface(){
		user_data = new HttpUtils(UIMain.this); 
    	if(ev_acount!=null&&ev_mail!=null){
    	user_data.getDatabaseInfo(ev_acount,ev_mail);}
    	while(user_data.isFinishQuery!=1 ){Log.d("hold","wait");};
    	user_data.isFinishQuery=0;
    	Log.d("dedialog","open");
    	if(user_data.m_first_name!=null){
    	Dialogcheck(user_data.m_first_name,user_data.m_last_name,user_data.m_head);}
	}
	
	/** ================================================ */
	@Override
	public void onResume()
	{
		super.onResume();
	}
	
	/** ================================================ */
	@Override
	public void onPause()
	{
		super.onPause();
	}

	/** ================================================ */
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}
	
	/** ================================================ */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
  	{
  		THLLog.d("DEBUG", "onActivityResult()");

  		switch(requestCode)
  		{
  			case REQ_ENABLE_BT:
	  			if(RESULT_OK == resultCode)
	  			{
	  				Log.d("DATA BT",data.toString());			
				}
	  			break;
	  			
  			case REQ_ENABLE_WIFI:
  				if(RESULT_OK == resultCode)
	  			{
  					Log.d("DATA WIFI",data.toString());	
				}
  				break;
  		}
  	}

    /** ================================================ */
    /** implementation of {@link iBeaconScanManager#OniBeaconScan } */
	@Override
	public void onScaned(iBeaconData iBeacon)
	{
		synchronized(mListAdapter)
		{
			addOrUpdateiBeacon(iBeacon);
		}
	}
	
	/** ========================================================== */
	public void onResponse(int msg)
	{
		THLLog.d("debug", "Response("+ msg+ ")");
		mHandler.obtainMessage(MSG_SERVER_RESPONSE, msg, 0).sendToTarget();
	}
	
	/** ========================================================== */
	public void dlgNetworkNotAvailable()
	{
		final AlertDialog dlg = new AlertDialog.Builder(UIMain.this).create();
		
		dlg.setTitle("Network");
		dlg.setMessage("Please enable your network for updating beacon list.");

		dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dlg.dismiss();
			}
		});
		
		dlg.show();
	}
	
	/** ========================================================== */
	public void dlgNetwork3G()
	{
		final AlertDialog dlg = new AlertDialog.Builder(UIMain.this).create();
		
		dlg.setTitle("3G");
		dlg.setMessage("App will send/recv data via 3G, this may result in significant data charges.");

		dlg.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				Config.allow3G= true;
				dlg.dismiss();
				USBeaconServerInfo info= new USBeaconServerInfo();
				
				info.serverUrl		= HTTP_API;
				info.queryUuid		= QUERY_UUID;
				info.downloadPath	= STORE_PATH;
				
				mBServer.setServerInfo(info, UIMain.this);
				mBServer.checkForUpdates();
			}
		});
		
		dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Reject", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				Config.allow3G= false;
				dlg.dismiss();
			}
		});
	
		dlg.show();
	}
	public void Dialogcheck(String f_name,String l_name,String head){
		
		img_userhead = (ImageView)layout_above.findViewById(R.id.img_checkinfo);
		tv_userinfo = (TextView)layout_above.findViewById(R.id.tv_checkinfo);
//		Log.d("ttsw", head);
		//建立一個AsyncTask執行緒進行圖片讀取動作，並帶入圖片連結網址路徑
        new AsyncTask<String, Void, Bitmap>() 
        {
          @Override
          protected Bitmap doInBackground(String... params) 
          {
            String url = params[0];
            return getBitmapFromURL(url);
          }
                    
          @Override
          protected void onPostExecute(Bitmap result) 
          {
            img_userhead. setImageBitmap (result);
            super.onPostExecute(result);
          }       
        }.execute(imageHeadURL+head);
		
    	tv_userinfo.setText(user_data.m_first_name+user_data.m_last_name);

		
		GHeadUI.show();
	}
	//讀取網路圖片，型態為Bitmap
	 private static Bitmap getBitmapFromURL(String imageUrl) 
	   {
	      try 
	      {
	         URL url = new URL(imageUrl);
	         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	         connection.setDoInput(true);
	         connection.connect();
	         InputStream input = connection.getInputStream();
	         Bitmap bitmap = BitmapFactory.decodeStream(input);
	         return bitmap;
	      } 
	      catch (IOException e) 
	      {
	         e.printStackTrace();
	         return null;
	      }
	   }
	/** ========================================================== */
	public void addOrUpdateiBeacon(iBeaconData iBeacon)
	{
		
		long currTime= System.currentTimeMillis();
		double beacon_dist=0;
		ScanediBeacon beacon= null;

		int checkforEnter =0;
		
		for(ScanediBeacon b : miBeacons)
		{	checkforEnter=0;
			if(b.equals(iBeacon, false))
			{
				beacon= b;
				beacon_dist= beacon.calDistance(b.rssi);
				
				if(b.beaconUuid.equals(DSGBEACON_UUID)){
					checkforEnter++;}
//				Log.d("check",String.valueOf(checkforEnter)+b.beaconUuid+":"+DSGBEACON_UUID);
				
				break;
			}
		}
		Log.d("openface", "ww "+ BS.InZone +StateCode);
		if(null == beacon)
		{
			beacon= ScanediBeacon.copyOf(iBeacon);
			miBeacons.add(beacon);
//			Log.d("check",String.valueOf(checkforEnter)+beacon.beaconUuid+":"+DSGBEACON_UUID);

		}
		else
		{
			beacon.rssi= iBeacon.rssi;
			beacon_dist= beacon.calDistance(beacon.rssi);
//			Log.d("check",String.valueOf(checkforEnter)+beacon.beaconUuid);
			
		}
		
		
		
		
		updateTime =System.currentTimeMillis();

		
		
		beacon.lastUpdate= currTime;
	}
	
	
	public boolean checkfordisttance(iBeaconData beacon){
		if(beacon.calDistance()>RangeBound ){
			flag = false;
			Log.d("rekk","2"+beacon.calDistance());
			return false;}
		if(beacon.calDistance()<RangeBound && flag==false){
			flag =true;
			Log.d("rekk","1"+beacon.calDistance());
			return true;}
		else
			return true;
			
		}
	private void updateRecord(){
		String currTime= getDateTime();
		
		String url = WEB_URL +"updateLeaveRecord.php?" + "ar_fm_number=" + user_data.getusernumber() + "&ar_enter_date=" +
				enterTime+ "&ar_leave_date="+currTime ;
		user_data.uploadRecord(url);
		Log.d("upload","update: "+url);
	}
	private void uploadRecord(String starttime){
    	// 資料上傳
			String currTime = getDateTime() ;
				String str_acount = ev_acount.getText().toString();
//				if(updateTime-currTime==5){
					
//					Toast.makeText(UIMain.this, " Data has uploaded! ", Toast.LENGTH_LONG).show();
//				}
				enterTime = starttime;
    			String url = WEB_URL + "saveIdRecord.php?" + "ar_fm_number=" + user_data.getusernumber() + "&ar_enter_date=" + 
    					starttime + "&ar_leave_date="+ currTime ;
    			user_data.uploadRecord(url);
                Toast.makeText(UIMain.this, " Data has uploaded! ", Toast.LENGTH_LONG).show();
                Log.d("upload",url);
    }
	
	
	public String getDateTime(){
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd%20hh:mm:ss");
		Date date = new Date();
		String strDate = sdFormat.format(date);
		//System.out.println(strDate);
		return strDate;
		}
	/** ========================================================== */
	public void verifyiBeacons()
	{
		{
			long currTime	= System.currentTimeMillis();
			
			int len= miBeacons.size();
			ScanediBeacon beacon= null;
			
			for(int i= len- 1; 0 <= i; i--)
			{
				beacon= miBeacons.get(i);
				
				if(null != beacon && TIME_BEACON_TIMEOUT < (currTime- beacon.lastUpdate))
				{
					miBeacons.remove(i);
				}
			}
			
			if(miBeacons.isEmpty()){
				Log.d("sta","empty");
//				String url = WEB_URL + "saveIdRecord.php?" + "UUID=" + beacon.beaconUuid + "&user=" + 
//    					imei + "&log="+ "leave" ;
//    			HttpUtils.uploadRecord(url);
//                Toast.makeText(UIMain.this, " Data has uploaded! ", Toast.LENGTH_LONG).show();
			}
		}
		
		{
			mListAdapter.clear();
			
			
			int major=0,minor=0;
			byte omr=0,rssi=0;
			
			for(ScanediBeacon beacon : miBeacons)
			{	
				mListAdapter.addItem(new ListItem(beacon.beaconUuid.toString().toUpperCase(), ""+ beacon.major, ""+ beacon.minor, ""+ beacon.rssi, ""+beacon.calDistance()));
				Log.d("change dist",String.valueOf(beacon.calDistance())+" "+beacon.beaconUuid);
				
				
				major=beacon.major;
				minor=beacon.minor;
				omr=beacon.oneMeterRssi;
				rssi=beacon.rssi;
			}
//			iBeaconData b = new iBeaconData();
//			b.beaconUuid=DSGBEACON_UUID;
//			Log.d("edit",b.beaconUuid);
//			b.mac_status=DSG_MACADDRESS;
//			b.major=major;b.minor=minor;
//			b.oneMeterRssi=omr;b.rssi=rssi;
//			mListAdapter.addItem(new ListItem(b.beaconUuid, ""+ b.major, ""+ b.minor, ""+ b.rssi, ""+b.calDistance()));
		}
	}
	
	/** ========================================================== */
	public void cleariBeacons()
	{
		mListAdapter.clear();
	}
}

/** ============================================================== */
class ListItem
{
	public String text1= "";
	public String text2= "";
	public String text3= "";
	public String text4= "";
	public String text5= "";
	
	public ListItem()
	{
	}
	
	public ListItem(String text1, String text2, String text3, String text4,String text5)
	{
		this.text1= text1;
		this.text2= text2;
		this.text3= text3;
		this.text4= text4;
		this.text5= text5;
	}
}

/** ============================================================== */
class BLEListAdapter extends BaseAdapter
{
	private Context mContext;
	  
	List<ListItem> mListItems= new ArrayList<ListItem>();

	/** ================================================ */
	public BLEListAdapter(Context c) { mContext= c; }
	
	/** ================================================ */
	public int getCount() { return mListItems.size(); }
	
	/** ================================================ */
	public Object getItem(int position)
	{
		if((!mListItems.isEmpty()) && mListItems.size() > position)
		{
			return mListItems.toArray()[position];
		}
		
		return null;
	}
	  
	public String getItemText(int position)
	{
		if((!mListItems.isEmpty()) && mListItems.size() > position)
		{
			return ((ListItem)mListItems.toArray()[position]).text1;
		}
		
		return null;
	}
	
	/** ================================================ */
	public long getItemId(int position) { return 0; }
	
	/** ================================================ */
	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent)
	{
	    View view= (View)convertView;
	     
	    if(null == view)
	    	view= View.inflate(mContext, R.layout.item_text_3, null);
	
	    // view.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

	    if((!mListItems.isEmpty()) && mListItems.size() > position)
	    {
		    TextView text1	= (TextView)view.findViewById(R.id.it3_text1);
		    TextView text2	= (TextView)view.findViewById(R.id.it3_text2);
		    TextView text3	= (TextView)view.findViewById(R.id.it3_text3);
		    TextView text4	= (TextView)view.findViewById(R.id.it3_text4);
		    TextView text5  = (TextView)view.findViewById(R.id.it3_text5); 
		    
		    
	    	ListItem item= (ListItem)mListItems.toArray()[position];

			text1.setText(item.text1);
			//Log.d("tag", item.text1);
			text2.setText(item.text2);
			text3.setText(item.text3);
			text4.setText(item.text4+ " dbm");
			text5.setText(item.text5+ " m");
		}
	    else
	    {
	    	view.setVisibility(View.GONE);
	    }

	    return view;
	}

	/** ================================================ */
	@Override
    public boolean isEnabled(int position) 
    {
		if(mListItems.size() <= position)
			return false;

        return true;
    }

	/** ================================================ */
	public boolean addItem(ListItem item)
	{
		mListItems.add(item);
	  	return true;
	}
  
	/** ================================================ */
	public void clear()
	{
		mListItems.clear();
	}
	/** ================================================ */
	
}

/** ============================================================== */

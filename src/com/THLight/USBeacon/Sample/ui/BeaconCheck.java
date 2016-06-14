package com.THLight.USBeacon.Sample.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.THLight.USBeacon.App.Lib.USBeaconConnection;
import com.THLight.USBeacon.App.Lib.USBeaconData;
import com.THLight.USBeacon.App.Lib.USBeaconList;
import com.THLight.USBeacon.App.Lib.USBeaconServerInfo;
import com.THLight.USBeacon.App.Lib.iBeaconData;
import com.THLight.USBeacon.App.Lib.iBeaconScanManager;
import com.THLight.USBeacon.Sample.R;
import com.THLight.USBeacon.Sample.ScanediBeacon;
import com.THLight.USBeacon.Sample.THLApp;
import com.THLight.USBeacon.Sample.THLConfig;
import com.THLight.Util.THLLog;

public class BeaconCheck implements iBeaconScanManager.OniBeaconScan, USBeaconConnection.OnResponse{
	/** this UUID is generate by Server while register a new account. */
	final UUID QUERY_UUID		= UUID.fromString("4E62E99E-5DD2-4E6E-A1CB-4A09882381AA");
	/** server http api url. */
	final String HTTP_API		= "http://www.usbeacon.com.tw/api/func";
	
	//**
	final String DSGBEACON_UUID   = "EF7CBAAC-0E15-4ECA-9429-957334A3C5BD";
	final static String DSG_MACADDRESS = "B4:99:4C:4C:16:41";
	public  String imageHeadURL = "http://120.114.186.13/photo/member/";
	
	public int G[]={0,0,0};
	String ev_acount;
	ImageView img_userhead;
	TextView tv_userinfo;
	double RangeBound =20;
	String record="";
	String WEB_URL="http://120.114.186.13/app_/";
	URL head_url ;
	String imei ="mikelin";
	boolean flag =false;
	static String instatus="";
	public static int  InZone;
	public int StateCode;
	
	private HttpUtils user_data;
	
	float remaindistanceA=0;
	float remaindistanceB=0;
	
	
	int searchLimit=0;
	long startTime;
	long updateTime;
	int globalCounter=0;
	
	private Context context;
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
	
	public int getInZone(){
		return InZone;
	}
	BeaconCheck(){
		InZone =0;
	}
	
	THLApp App		= null;
	THLConfig Config= null;
	
	BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();

	/** scaner for scanning iBeacon around. */
	iBeaconScanManager miScaner	= null;
	
	/** USBeacon server. */
	USBeaconConnection mBServer	= new USBeaconConnection();
	
	USBeaconList mUSBList		= null;
	
	ListView mLVBLE= null;
	
	_BLEListAdapter mListAdapter		= null;
	
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
//							Toast.makeText(UIMain.this, "HAS_UPDATE.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_HAS_NO_UPDATE:
//							Toast.makeText(UIMain.this, "No new BeaconList.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DOWNLOAD_FINISHED:
							break;
		
						case USBeaconConnection.MSG_DOWNLOAD_FAILED:
//							Toast.makeText(UIMain.this, "Download file failed!", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FINISHED:
							{
								USBeaconList BList= mBServer.getUSBeaconList();

								if(null == BList)
								{
//									Toast.makeText(UIMain.this, "Data Updated failed.", Toast.LENGTH_SHORT).show();
									THLLog.d("debug", "update failed.");
								}
								else if(BList.getList().isEmpty())
								{
//									Toast.makeText(UIMain.this, "Data Updated but empty.", Toast.LENGTH_SHORT).show();
									Log.d("debug", "this account doesn't contain any devices.");
									THLLog.d("debug", "this account doesn't contain any devices.");
								}
								else
								{
//									Toast.makeText(UIMain.this, "Data Updated("+ BList.getList().size()+ ")", Toast.LENGTH_SHORT).show();
									
									
									for(USBeaconData data : BList.getList())
									{	
										Log.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ "), "+" Note("+ data.DistData+")");
										THLLog.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ ")");
									}
								}
							}
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FAILED:
//							Toast.makeText(UIMain.this, "UPDATE_FAILED!", Toast.LENGTH_SHORT).show();
							break;
						
					}
					break;
			}
//			Log.d("tag msg", msg.arg1+" "+msg.arg2);
		}
	};
	
	
	
	/** ================================================ */
	BeaconCheck(Context context)
	{
		this.context = context;
		instatus="leave";
		
		App		= THLApp.getApp();
		Config	= THLApp.Config;
		
		
		/** create instance of iBeaconScanManager. */
		miScaner		= new iBeaconScanManager(context, this);
		
		mListAdapter	= new _BLEListAdapter(context);
		
//		mLVBLE			= (ListView)findViewById(R.id.beacon_list);
//		mLVBLE.setAdapter(mListAdapter);
//		tv_acount = (TextView)findViewById(R.id.tv_acount);
//		ev_acount = (EditText)findViewById(R.id.editText_acount);
//		
//		btn_info =(Button)findViewById(R.id.btn_info);
//		
//		btn_info.setOnClickListener(new Button.OnClickListener(){ 
//
//            @Override
//
//            public void onClick(View v) {
//	
//                // TODO Auto-generated method stub
//
//            	
//            	user_data = new HttpUtils(UIMain.this); 
//            	if(ev_acount!=null){
//            	user_data.getDatabaseInfo(ev_acount);}
//            	while(user_data.isFinishQuery!=1 ){Log.d("hold","wait");};
//            	user_data.isFinishQuery=0;
//            	//Log.d("datav",user_data.m_head);
//            	if(user_data.m_first_name!=null){
//            	Dialogcheck(user_data.m_first_name,user_data.m_last_name,user_data.m_head);}
//            }         
////Log.d("hold","wait");
//        }); 
		
		  
		
		if(!mBLEAdapter.isEnabled())
		{
			Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			startActivityForResult(intent, REQ_ENABLE_BT);
		}
		else
		{
			Message msg= Message.obtain(mHandler, MSG_SCAN_IBEACON, 1000, 1100);
			msg.sendToTarget();
		}

		/** create store folder. */
		File file= new File(STORE_PATH);
		if(!file.exists())
		{
			if(!file.mkdirs())
			{
//				Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
			}
		}
		
		/** check network is available or not. */
//		ConnectivityManager cm	= (ConnectivityManager)getSystemService(UIMain.CONNECTIVITY_SERVICE);
//		if(null != cm)
//		{
//			NetworkInfo ni = cm.getActiveNetworkInfo();
//			if(null == ni || (!ni.isConnected()))
//			{
//				dlgNetworkNotAvailable();
//			}
//			else
//			{
//				THLLog.d("debug", "NI not null");
//
//				NetworkInfo niMobile= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//				if(null != niMobile)
//				{
//					boolean is3g	= niMobile.isConnectedOrConnecting();
//					
//					if(is3g)
//					{
//						dlgNetwork3G();
//					}
//					else
//					{
//						USBeaconServerInfo info= new USBeaconServerInfo();
//						
//						info.serverUrl		= HTTP_API;
//						info.queryUuid		= QUERY_UUID;
//						info.downloadPath	= STORE_PATH;
//						
//						mBServer.setServerInfo(info, this);
//						mBServer.checkForUpdates();
//					}
//				}
//			}
//		}
//		else
//		{
//			THLLog.d("debug", "CM null");
//		}
		
		
		mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
		
		
	}
	
//	/** ================================================ */
//	@Override
//	public void onResume()
//	{
//		super.onResume();
//	}
//	
//	/** ================================================ */
//	@Override
//	public void onPause()
//	{
//		super.onPause();
//	}
//
//	/** ================================================ */
//	@Override
//	public void onBackPressed()
//	{
//		super.onBackPressed();
//	}
//	
	/** ================================================ */
//    protected void onActivityResult(int requestCode, int resultCode, Intent data)
//  	{
//  		THLLog.d("DEBUG", "onActivityResult()");
//
//  		switch(requestCode)
//  		{
//  			case REQ_ENABLE_BT:
//	  			if(RESULT_OK == resultCode)
//	  			{
//	  				Log.d("DATA BT",data.toString());			
//				}
//	  			break;
//	  			
//  			case REQ_ENABLE_WIFI:
//  				if(RESULT_OK == resultCode)
//	  			{
//  					Log.d("DATA WIFI",data.toString());	
//				}
//  				break;
//  		}
//  	}

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
		final AlertDialog dlg = new AlertDialog.Builder(context).create();
		
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
//	public void dlgNetwork3G()
//	{
//		final AlertDialog dlg = new AlertDialog.Builder(context).create();
//		
//		dlg.setTitle("3G");
//		dlg.setMessage("App will send/recv data via 3G, this may result in significant data charges.");
//
//		dlg.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener()
//		{
//			public void onClick(DialogInterface dialog, int id)
//			{
//				Config.allow3G= true;
//				dlg.dismiss();
//				USBeaconServerInfo info= new USBeaconServerInfo();
//				
//				info.serverUrl		= HTTP_API;
//				info.queryUuid		= QUERY_UUID;
//				info.downloadPath	= STORE_PATH;
//				
//				mBServer.setServerInfo(info, context);
//				mBServer.checkForUpdates();
//			}
//		});
//		
//		dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Reject", new DialogInterface.OnClickListener()
//		{
//			public void onClick(DialogInterface dialog, int id)
//			{
//				Config.allow3G= false;
//				dlg.dismiss();
//			}
//		});
//	
//		dlg.show();
//	}
//	public void Dialogcheck(String f_name,String l_name,String head){
//		LayoutInflater inflater = getLayoutInflater(); View layout = inflater.inflate(R.layout.userinfo,null);
//		img_userhead = (ImageView)layout.findViewById(R.id.img_checkinfo);
//		tv_userinfo = (TextView)layout.findViewById(R.id.tv_checkinfo);
////		Log.d("ttsw", head);
//		//建立一個AsyncTask執行緒進行圖片讀取動作，並帶入圖片連結網址路徑
//        new AsyncTask<String, Void, Bitmap>() 
//        {
//          @Override
//          protected Bitmap doInBackground(String... params) 
//          {
//            String url = params[0];
//            return getBitmapFromURL(url);
//          }
//                    
//          @Override
//          protected void onPostExecute(Bitmap result) 
//          {
//            img_userhead. setImageBitmap (result);
//            super.onPostExecute(result);
//          }       
//        }.execute(imageHeadURL+head);
//		
//    	tv_userinfo.setText(user_data.m_first_name+user_data.m_last_name);
//
//		
//		new AlertDialog.Builder(UIMain.this)
//		.setTitle("進入提示")
//	   .setMessage("確定登錄健身房紀錄")
//	   .setView(layout)
//	   .setNegativeButton("離開",new DialogInterface.OnClickListener() {
//	   @Override
//	   public void onClick(DialogInterface arg0, int arg1) {
//	         // TODO Auto-generated method stub
//	         Toast.makeText(UIMain.this, "上傳成功",Toast.LENGTH_SHORT).show();
//	   }
//
//	  })
//	  .setPositiveButton("進入",new DialogInterface.OnClickListener() {
//	  @Override
//	  public void onClick(DialogInterface arg0, int arg1) {
//	      // TODO Auto-generated method stub
//	      Toast.makeText(UIMain.this, "我了解了",Toast.LENGTH_SHORT).show();
//	      long currTime= System.currentTimeMillis();
//	  }
//
//	 })
//	  .setNeutralButton("更換使用者",new DialogInterface.OnClickListener() {
//	  @Override
//	  public void onClick(DialogInterface arg0, int arg1) {
//	      // TODO Auto-generated method stub
//	      Toast.makeText(UIMain.this, "切換ID",Toast.LENGTH_SHORT).show();
//	  }
//	  
//	 }).show();
//	}
//	//讀取網路圖片，型態為Bitmap
//	 private static Bitmap getBitmapFromURL(String imageUrl) 
//	   {
//	      try 
//	      {
//	         URL url = new URL(imageUrl);
//	         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//	         connection.setDoInput(true);
//	         connection.connect();
//	         InputStream input = connection.getInputStream();
//	         Bitmap bitmap = BitmapFactory.decodeStream(input);
//	         return bitmap;
//	      } 
//	      catch (IOException e) 
//	      {
//	         e.printStackTrace();
//	         return null;
//	      }
//	   }
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
//				beacon=b;
				beacon_dist= b.calDistance( );
				Log.d("check",String.valueOf(b.rssi)+" "+b.calDistance());
				if(b.beaconUuid.equals(DSGBEACON_UUID)){
					checkforEnter++;
				
				if(b.major==0 && b.minor==1){
					remaindistanceA = (float) b.calDistance(); //每次刷新判斷她最後距離門口的距離
				}
				if(b.major==0 && b.minor==2){
					remaindistanceB = (float) b.calDistance(); //每次刷新判斷她最後距離門口的距離
				}
					
				if((b.major==99 && b.minor==0 && remaindistanceA>10 && remaindistanceA>remaindistanceB) && iBeacon==null){
//					updateRecord(); //有bug
					Log.d("AL","auto log out");
				}
				if(beacon_dist>3){
					
					G[0]=G[1];
					G[1]=G[2];
					G[2]=0;
					Log.d("check","outter: "+String.valueOf(beacon_dist)+" "+b.calDistance());
				}
				else{
					G[0]=G[1];
					G[1]=G[2];
					G[2]=1;
					Log.d("check","inner"+String.valueOf(beacon_dist)+ ":"+StateCode+":"+ev_acount);
//					context.startActivity(new Intent(context,UIMain.class));
//					if(StateCode<3){
//						ev_acount="13";
////					PopUpInterface();
////					Log.d("Doing","OK");
//					}
					}
				if(G[0]==G[1]||G[1]==G[2])
					InZone=G[1];
				}Log.d("dd",String.valueOf(InZone));
				break;
			}
		}
		
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
			//Log.d("check",String.valueOf(beacon_dist));
			
		}
		
		
		
		
		updateTime =System.currentTimeMillis();
		if(checkforEnter>0  && checkfordisttance(beacon)){
				if(instatus=="leave"){
				startTime =System.currentTimeMillis();
				globalCounter++;
				instatus="enter";
				}
//				text_status.setText(checkforEnter +":Near DSG Lab."+globalCounter);
//				Log.d("sta","n");
		}else {
				if(instatus=="enter"){
//				uploadRecord(beacon,startTime);
				instatus="leave";
				}
//				text_status.setText(checkforEnter +":Leave DSG Lab.");
//				Log.d("sta","l");
			}
		
		
		
		
		if(checkforEnter==0)
//			text_status.setText(checkforEnter +"Leave DSG Lab.");
		
		
		
		beacon.lastUpdate= currTime;
	}
	private void updateRecord(){
		String currTime= getDateTime();
		
		String url = WEB_URL +"updateLeaveRecord.php?" + "ar_fm_number=" + user_data.getusernumber() +"&ar_leave_date="+currTime ;
		user_data.uploadRecord(url);
		Log.d("upload","update: "+url);
	}
	public String getDateTime(){
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd%20hh:mm:ss");
		Date date = new Date();
		String strDate = sdFormat.format(date);
		//System.out.println(strDate);
		return strDate;
		}
	public boolean checkfordisttance(iBeaconData beacon){
		if(beacon.calDistance()>RangeBound ){
			flag = false;
//			Log.d("rekk","2"+beacon.calDistance());
			return false;}
		if(beacon.calDistance()<RangeBound && flag==false){
			flag =true;
//			Log.d("rekk","1"+beacon.calDistance());
			return true;}
		else
			return true;
			
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
			
			if(miBeacons.isEmpty() && instatus=="Near"){
//				Log.d("sta","empty");
//				text_status.setText("0:Leave");
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
				mListAdapter.addItem(new _ListItem(beacon.beaconUuid.toString().toUpperCase(), ""+ beacon.major, ""+ beacon.minor, ""+ beacon.rssi, ""+beacon.calDistance()));
//				Log.d("change dist",String.valueOf(beacon.calDistance())+" "+beacon.beaconUuid);
				
				
				major=beacon.major;
				minor=beacon.minor;
				omr=beacon.oneMeterRssi;
				rssi=beacon.rssi;
			}

		}
	}
	
	/** ========================================================== */
	public void cleariBeacons()
	{
		mListAdapter.clear();
	}
}

/** ============================================================== */
class _ListItem
{
	public String text1= "";
	public String text2= "";
	public String text3= "";
	public String text4= "";
	public String text5= "";
	
	public _ListItem()
	{
	}
	
	public _ListItem(String text1, String text2, String text3, String text4,String text5)
	{
		this.text1= text1;
		this.text2= text2;
		this.text3= text3;
		this.text4= text4;
		this.text5= text5;
	}
}

/** ============================================================== */
class _BLEListAdapter extends BaseAdapter
{
	private Context mContext;
	  
	List<_ListItem> mListItems= new ArrayList<_ListItem>();

	/** ================================================ */
	public _BLEListAdapter(Context c) { mContext= c; }
	
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
			return ((_ListItem)mListItems.toArray()[position]).text1;
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
//		    TextView text1	= (TextView)view.findViewById(R.id.it3_text1);
//		    TextView text2	= (TextView)view.findViewById(R.id.it3_text2);
//		    TextView text3	= (TextView)view.findViewById(R.id.it3_text3);
//		    TextView text4	= (TextView)view.findViewById(R.id.it3_text4);
//		    TextView text5  = (TextView)view.findViewById(R.id.it3_text5); 
		    
		    
	    	_ListItem item= (_ListItem)mListItems.toArray()[position];

//			text1.setText(item.text1);
//			//Log.d("tag", item.text1);
//			text2.setText(item.text2);
//			text3.setText(item.text3);
//			text4.setText(item.text4+ " dbm");
//			text5.setText(item.text5+ " m");
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
	public boolean addItem(_ListItem item)
	{
		mListItems.add(item);
	  	return true;
	}
  
	/** ================================================ */
	public void clear()
	{
		mListItems.clear();
	}
	
}

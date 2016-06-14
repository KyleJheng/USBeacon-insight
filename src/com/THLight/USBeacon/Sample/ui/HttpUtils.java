package com.THLight.USBeacon.Sample.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;


public class HttpUtils {
	protected static final Context HttpUtils = null;

	static JSONArray jArray;

	static String result = null;

	static InputStream is = null;

	static StringBuilder sb = null;

	public static String[] selFesture;

	public int[] sFesture = new int[15];
	
	public int isFinishQuery=0;
	private String imageHeadURL = "http://120.114.186.13/photo/member/";
//	private String imageHeadURL = "http://59.125.213.197/photo/member/";
	public String user = null;
	public String user_number = null;
	public String m_first_name = null;
	public String m_last_name = null;
	public String m_head = null;
	public boolean foundss =false;

	Context context;
	
	public String getHead(){
		return m_head;
	}
	public String getLastname(){
		return m_last_name;
	}
	public String getFirstname(){
		return m_first_name;
	}
	public String getusernumber(){
		return user_number;
	}
	public HttpUtils(Context context) {
		this.context = context;
	}

	// 挑選特徵值上傳到資料庫
	public static void uploadRecord(final String url) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection connection;
				try {
					connection = (HttpURLConnection) new URL(url)
							.openConnection();
					connection.setRequestMethod("GET");
					connection.getInputStream();
					Log.d("dd","work! "+url);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
			}
		}).start();
	}

	// 得到資料庫相對應的特徵值
	public    void getDatabaseInfo(EditText ev_acount,EditText ev_mail) {
 
		final String acount=ev_acount.getText().toString();
		final String mail=ev_mail.getText().toString();
		final String user = null;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpClient httpclient = new DefaultHttpClient();
					HttpGet httpget = new HttpGet(
							"http://120.114.186.13/app_/userinfo_query.php");
					HttpResponse response = httpclient.execute(httpget);
					HttpEntity entity = response.getEntity();

					is = entity.getContent();
				} catch (Exception e) {
					Log.e("HTTP ERROR",
							"Error in http connection" + e.toString());
				}

				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(is, "iso-8859-1"), 8);

					sb = new StringBuilder();
					sb.append(reader.readLine() + "\n");

					String line = "0";
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}

					is.close();

					result = sb.toString();
				} catch (Exception e) {
					Log.e("log_tag", "Error converting result " + e.toString());
				}
			
//				Log.d("datav",acount);

				try {
					jArray = new JSONArray(result);
					JSONObject json_data = null;

					for (int i = 0; i < jArray.length(); i++) {
						json_data = jArray.getJSONObject(i);
						if(json_data.getString("m_id_number").equals(acount) && json_data.getString("m_email").equals(mail)){
						user_number = json_data.getString("m_number");
						m_first_name = json_data.getString("m_first_name");
						m_last_name = json_data.getString("m_last_name");
						m_head = json_data.getString("m_head");
						foundss=true;
						}	      
					}doneQuery(1);
					Log.d("datav",m_head);
					
					
					if(foundss){
						Log.d("ttsw"," found");
					}
					else{
					      Toast.makeText(HttpUtils, "請確認會員ID",Toast.LENGTH_SHORT );
					      Log.d("ttsw","no found");
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		}).start();
	
	}
	public  void doneQuery(int step){
		isFinishQuery = step;
	}
}
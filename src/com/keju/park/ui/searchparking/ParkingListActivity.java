package com.keju.park.ui.searchparking;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.keju.park.CommonApplication;
import com.keju.park.Constants;
import com.keju.park.R;
import com.keju.park.Urls;
import com.keju.park.adapter.NearbyParkAdapter;
import com.keju.park.bean.NearbyParkBean;
import com.keju.park.ui.base.BaseActivity;
import com.keju.park.util.DateUtil;
import com.keju.park.util.NetUtil;
import com.keju.park.view.XListView;
import com.keju.park.view.XListView.IXListViewListener;

/**
 * 停车场列表
 * 
 * @author Zhoujun
 * @version 创建时间：2014-5-4 下午4:50:44
 */
public class ParkingListActivity extends BaseActivity implements OnClickListener,IXListViewListener {

	private CommonApplication app;
	private TextView tvLocation;

	private XListView lvParkding;
	private NearbyParkAdapter adapter;
	private ArrayList<NearbyParkBean> parkingList;
	private int pageIndex;
	private boolean isLoad = false;// 是否正在加载数据
	private boolean isComplete = false;// 是否加载完了；
	private boolean isRefresh = false;// 是否刷新
	private Handler mHandler = new Handler();

	private double longtitude;
	private double latitude;
	
	private RequestQueue mQueue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.park_list);
		app = (CommonApplication) getApplication();
		
		longtitude = getIntent().getExtras().getDouble("longtitude");
		latitude = getIntent().getExtras().getDouble("latitude");
		
		initBar();
		findView();
		fillData();
	}

	/**
	 * 初始化控件
	 */
	private void findView() {
		tvLeft.setOnClickListener(this);
		tvTitle.setText("附近停车场");

		tvLocation = (TextView) findViewById(R.id.tvLocation);
		tvLocation.setText("您当前的位置：" + app.getUserAddress());

		lvParkding = (XListView) findViewById(R.id.lvlocationList);
		parkingList = new ArrayList<NearbyParkBean>();

		mQueue = Volley.newRequestQueue(ParkingListActivity.this);
	}
	private void onLoad() {
		lvParkding.stopRefresh();
		lvParkding.stopLoadMore();
		lvParkding.setRefreshTime(DateUtil.dateToString("HH:mm", new Date()));
	}
	/**
	 * 初始化数据
	 */
	private void fillData() {
		getData();
		lvParkding.setPullLoadEnable(true);
		lvParkding.setXListViewListener(this);
		lvParkding.setOnItemClickListener(itemListener);
		adapter = new NearbyParkAdapter(ParkingListActivity.this,parkingList,app);
		lvParkding.setAdapter(adapter);
	}
	/**
	 * listview点击事件
	 */
	OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if (arg2 >= parkingList.size()) {
				return;
			}
		}
	};
	/**
	 * 获取数据
	 */
	private void getData() {
		mQueue.add(new StringRequest(Method.GET, Urls.URL_PARK_LIST + "latitude="
				+ latitude + "&longitude=" + longtitude + "&page_show" + pageIndex, new Listener<String>() {
			@Override
			public void onResponse(String arg0) {
				try {
					if (isRefresh) {
						parkingList.clear();
					}
					JSONObject jsonObject = new JSONObject(arg0);
					JSONArray array = jsonObject.getJSONArray("data");
					List<NearbyParkBean> tempList = NearbyParkBean.constractList(array);
					boolean isLastPage = false;
					if (tempList.size() > 0) {
						parkingList.addAll(tempList);
						pageIndex ++;
					} else {
						isLastPage = true;
					}
					if (isLastPage) {
						isComplete = true;
					} else {
						if (tempList.size() > 0 && tempList.size() < Constants.PAGE_SIZE) {
							isComplete = true;
						}
					}
					if (isComplete) {
						lvParkding.setFooterViewInvisible(true);
					}else{
						lvParkding.setFooterViewInvisible(false);
					}
					if (pageIndex == 1 && tempList.size() == 0) {
						showShortToast("无数据");
					}
					if (pageIndex >= 2 && tempList.size() == 0) {
						showShortToast("无更多数据");
					}
					adapter.notifyDataSetChanged();
				}catch(JSONException e){
					showShortToast(R.string.connect_server_exception);
				}catch (Exception e) {
					showShortToast(R.string.connect_server_exception);
				}
				isLoad = false;
				isRefresh = false;
			}
		}, new ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError arg0) {
				showShortToast(R.string.connect_server_exception);
				isLoad = false;
				isRefresh = false;
			}
		}));

		mQueue.start();
	}
	@Override
	protected void onStop() {
		super.onStop();
		if(mQueue != null){
			mQueue.cancelAll(this);
		}
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tvLeft:
			finish();
			break;

		default:
			break;
		}
	}

	@Override
	public void onRefresh() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(!NetUtil.checkNet(ParkingListActivity.this)){
					showShortToast(R.string.NoSignalException);
					return;
				}
				if(isRefresh){
					return;
				}
				isRefresh = true;
				pageIndex = 1;
				onLoad();
				getData();
			}
		}, 2000);
	}

	@Override
	public void onLoadMore() {
		if(!NetUtil.checkNet(ParkingListActivity.this)){
			showShortToast(R.string.NoSignalException);
			return;
		}
		if (!isLoad && !isComplete) {
			getData();
		}
	}
	
}

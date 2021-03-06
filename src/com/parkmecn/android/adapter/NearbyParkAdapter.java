package com.parkmecn.android.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.baidu.mapapi.navi.BaiduMapAppNotSupportNaviException;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.navi.NaviPara;
import com.baidu.navisdk.BaiduNaviManager;
import com.baidu.navisdk.BaiduNaviManager.OnStartNavigationListener;
import com.baidu.navisdk.comapi.routeplan.RoutePlanParams.NE_RoutePlan_Mode;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.parkmecn.android.CommonApplication;
import com.parkmecn.android.Constants;
import com.parkmecn.android.R;
import com.parkmecn.android.bean.NearbyParkBean;
import com.parkmecn.android.ui.searchparking.BNavigatorActivity;
import com.parkmecn.android.ui.searchparking.ShowRouteActivity;
import com.parkmecn.android.util.StringUtil;

/**
 * 停车场 适配器
 * 
 * @author zhouyong
 * @data 创建时间：2014-5-10 下午2:35:08
 */
public class NearbyParkAdapter extends BaseAdapter {
	private List<NearbyParkBean> nearbyLists;
	private Activity activity;
	private CommonApplication app;

	/**
	 * @param list
	 */
	public NearbyParkAdapter(Activity activity, List<NearbyParkBean> list, CommonApplication app) {
		this.nearbyLists = list;
		this.activity = activity;
		this.app = app;
	}

	@Override
	public int getCount() {

		return nearbyLists.size();
	}

	@Override
	public Object getItem(int position) {

		return nearbyLists.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		final NearbyParkBean bean = nearbyLists.get(position);
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = activity.getLayoutInflater().inflate(R.layout.park_item, null);
			holder.tvLocationPark = (TextView) convertView.findViewById(R.id.tvLocationPark);
			holder.tvAddress = (TextView) convertView.findViewById(R.id.tvAddress);
			holder.tvDistance = (TextView) convertView.findViewById(R.id.tvDistance);
			holder.viewNavigation = convertView.findViewById(R.id.viewNavigation);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.tvLocationPark.setText(bean.getName());
		holder.tvAddress.setText("地址：" + bean.getAddress());

		if (app.getLastLocation() != null) {
			double distance = StringUtil.getDistance(app.getLastLocation().getLatitude(), app.getLastLocation().getLongitude(), bean
					.getLocationList().get(0).getLatitude(), bean.getLocationList().get(0).getLongitude());
			String distanceStr = null;
			if (distance > 1000) {
				distance = distance / 1000;
				distanceStr = String.format("%.0f", distance) + "km";
			} else {
				distanceStr = String.format("%.0f", distance) + "m";
			}
			if (bean.getLast() != -1) {
				holder.tvDistance.setText(Html.fromHtml("<a>距离<font color='#fe0000'>" + distanceStr + "</a>    空车位"
						+ "<a><font color='#fe0000'>" + bean.getLast() + "</a>个"));
			} else {
				holder.tvDistance.setText(Html.fromHtml("<a>距离<font color='#fe0000'>" + distanceStr));
			}

		} else {
			holder.tvDistance.setText("");
		}

		holder.viewNavigation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// showRoute(bean);
				showNavigation(bean);
			}
		});

		return convertView;
	}

	class ViewHolder {
		private TextView tvLocationPark, tvAddress, tvDistance;
		private View viewNavigation;
	}

	private void showNavigation(NearbyParkBean bean) {
		BaiduNaviManager.getInstance().launchNavigator(activity, app.getLastLocation().getLatitude(), app.getLastLocation().getLongitude(),
				app.getUserAddress(), bean.getLocationList().get(0).getLatitude(), bean.getLocationList().get(0).getLongitude(),
				bean.getAddress(), NE_RoutePlan_Mode.ROUTE_PLAN_MOD_MIN_TIME, // 算路方式
				true, // 真实导航
				BaiduNaviManager.STRATEGY_FORCE_ONLINE_PRIORITY, // 在离线策略
				new OnStartNavigationListener() { // 跳转监听

					@Override
					public void onJumpToNavigator(Bundle configParams) {
						Intent intent = new Intent(activity, BNavigatorActivity.class);
						intent.putExtras(configParams);
						activity.startActivity(intent);
					}

					@Override
					public void onJumpToDownloader() {
					}
				});

	}

	/**
	 * 显示导航路线；
	 * 
	 * @param bean
	 */
	private void showRoute(NearbyParkBean bean) {
		double mLatStart = app.getLastLocation().getLatitude();
		double mLonStart = app.getLastLocation().getLongitude();
		// 服务器经纬度弄反了
		double mLatEnd = bean.getLocationList().get(0).getLatitude();
		double mLonEnd = bean.getLocationList().get(0).getLongitude();
		int lat = (int) (mLatStart * 1E6);
		int lon = (int) (mLonStart * 1E6);
		GeoPoint pt1 = new GeoPoint(lat, lon);
		lat = (int) (mLatEnd * 1E6);
		lon = (int) (mLonEnd * 1E6);
		GeoPoint pt2 = new GeoPoint(lat, lon);
		/*
		 * 
		 * 导航参数
		 * 
		 * 导航起点和终点不能为空，当GPS可用时启动从用户位置到终点间的导航，
		 * 
		 * 当GPS不可用时，启动从起点到终点间的模拟导航。
		 */
		NaviPara para = new NaviPara();
		para.startPoint = pt1; // 起点坐标
		para.startName = "从这里开始";
		para.endPoint = pt2; // 终点坐标
		para.endName = "到这里结束";
		try {
			// 调起百度地图客户端导航功能,参数this为Activity。
			BaiduMapNavigation.openBaiduMapNavi(para, activity);
		} catch (BaiduMapAppNotSupportNaviException e) {
			// Bundle b = new Bundle();
			// b.putSerializable(Constants.EXTRA_DATA, bean);
			// activity.openActivity(ShowRouteActivity.class, b);
			Intent intent = new Intent();
			intent.putExtra(Constants.EXTRA_DATA, bean);
			intent.setClass(activity, ShowRouteActivity.class);
			activity.startActivity(intent);
		}

	}

}

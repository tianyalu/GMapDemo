package com.sty.gmap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.sty.gmap.utils.AMapUtil;
import com.sty.gmap.utils.PermissionUtils;
import com.sty.gmap.utils.ToastUtil;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource{
    private Context mContext;
    private MapView mapView;
    private AMap aMap;

    private LatLng latLng;
    private LatLng latLng2;
    private LatLng latLng3;
    private LatLng defaultLatLng;
    private Marker marker;
    private MarkerOptions markerOptions;

    private LatLonPoint startPoint;
    private LatLonPoint endPoint;
    private RouteSearch routeSearch;
    private RouteSearch.DriveRouteQuery driveRouteQuery;
    private DriveRouteResult mDriveRouteResult;

    private LocationSource.OnLocationChangedListener mListener;
    public AMapLocationClient mLocationClient; //声明AMapLocationClient类对象
    //public AMapLocationListener mLocationListener; //声明定位回调监听器

    public AMapLocationClientOption mLocationOption; //声明AMapLocationClientOption对象

    private double longitude;
    private double latitude;

    private RelativeLayout mBottomLayout;
    private TextView mRotueTimeDes, mRouteDetailDes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        mContext = this;

        requestPermission();
        initMap(savedInstanceState);
        initLocation();
        drawMarker();
    }

    private void requestPermission(){
        PermissionUtils.requestPermissions(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH
        );
    }

    private void initMap(Bundle savedInstanceState){
        mBottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        mRotueTimeDes = (TextView) findViewById(R.id.firstline);
        mRouteDetailDes = (TextView) findViewById(R.id.secondline);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState); //此方法必须重写
        aMap = mapView.getMap();

        aMap.setTrafficEnabled(true);  //显示实时交通状况
        //地图模式可选类型：MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_NIGHT
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        //设置地图缩放比例
        aMap.moveCamera(CameraUpdateFactory.zoomTo(12));

        // defaultLatLng = new LatLng(22.575159 , 113.863079);
        // aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 11));
    }

    private void drawMarker(){
        latLng = new LatLng(22.575159 , 113.863079);
        markerOptions = new MarkerOptions().position(latLng).title("西边").snippet("深圳宝安西乡地铁站");

        latLng2 = new LatLng(22.581587 , 113.919169);
        aMap.addMarker(markerOptions);
        aMap.addMarker(new MarkerOptions().position(latLng2).title("北边").snippet("深圳兴东地铁站"));
        latLng3 = new LatLng(22.532139, 113.996675);
        aMap.addMarker(new MarkerOptions().position(latLng3).title("东边").snippet("深圳侨城东地铁站"));

        aMap.setOnMarkerClickListener(markerClickListener);
    }

    //定义Marker点击后的回调
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {  //注意这个参数
            marker.showInfoWindow();
            //Toast.makeText(MainActivity.this, marker.getTitle() + " is clicked", Toast.LENGTH_SHORT).show();
            endPoint = new LatLonPoint(marker.getPosition().latitude, marker.getPosition().longitude);
            searchRouteSearch(startPoint, endPoint);

            mapView.setVisibility(View.VISIBLE);

            return true;  //true:marker不会成为地图中心坐标 false:会
        }
    };

    private void initLocation(){
        //设置监听对象
        aMap.setLocationSource(this);
        //设置默认定位按钮是否显示
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        //设置为true表示启动显示定位蓝点 false表示隐藏点位蓝点并不进行定位，默认false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        //设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        //aMap.setMyLocationStyle(myLocationStyle);
    }

    //激活定位
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;

        if(mLocationClient == null) {
            //初始化定位
            mLocationClient = new AMapLocationClient(this);
            //设置定位回调监听
            mLocationClient.setLocationListener(this);

            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            //设置定位模式为AMapLocationMode.Hight_Accuracy,高精度模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

            //获取一次定位结果：该方法默认为false
            //mLocationOption.setOnceLocation(true);
            //获取最近3s内精度最高的一次定位结果：
            //设置setOnceLocationLatest(boolean b)接口为true,启动定位时SDK会返回最近3s内精度最高的一次定位结果。
            //如果设置其为true,setOnceLocation(boolean b)接口也会被设置未true, 反之不会，默认为false.
            //mLocationOption.setOnceLocationLatest(true);

            //设置定位间隔，单位毫秒，默认为2000ms，最低为1000ms
            mLocationOption.setInterval(1000);

            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);

            //设置是否允许模拟位置，默认为true，允许模拟位置
            mLocationOption.setMockEnable(false);

            //设置超时时间，单位毫秒，默认30000毫秒，建议超时时间不低于8000毫秒
            mLocationOption.setHttpTimeOut(20000);

            //关闭缓存机制
            mLocationOption.setLocationCacheEnable(false);

            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            //启动定位
            mLocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if(mLocationClient != null){
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * 驾车路线搜索
     * @param startPoint 起点
     * @param endPoint 终点
     */
    public void searchRouteSearch(LatLonPoint startPoint, LatLonPoint endPoint){
        routeSearch = new RouteSearch(this);
        routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int errorCode) {
                aMap.clear();// 清理地图上的所有覆盖物
                if (errorCode == 1000) {
                    if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                        if (driveRouteResult.getPaths().size() > 0) {
                            mDriveRouteResult = driveRouteResult;
                            final DrivePath drivePath = mDriveRouteResult.getPaths()
                                    .get(0);
                            DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                                    mContext, aMap, drivePath,
                                    mDriveRouteResult.getStartPos(),
                                    mDriveRouteResult.getTargetPos(), null);
                            drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                            drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                            drivingRouteOverlay.removeFromMap();
                            drivingRouteOverlay.addToMap();
                            drivingRouteOverlay.zoomToSpan();
                            mBottomLayout.setVisibility(View.VISIBLE);
                            int dis = (int) drivePath.getDistance();
                            int dur = (int) drivePath.getDuration();
                            String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
                            mRotueTimeDes.setText(des);
                            mRouteDetailDes.setVisibility(View.VISIBLE);
                            int taxiCost = (int) mDriveRouteResult.getTaxiCost();
                            mRouteDetailDes.setText("打车约"+taxiCost+"元");
                            mBottomLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(mContext,
                                            DriveRouteDetailActivity.class);
                                    intent.putExtra("drive_path", drivePath);
                                    intent.putExtra("drive_result",
                                            mDriveRouteResult);
                                    startActivity(intent);
                                }
                            });
                        } else if (driveRouteResult != null && driveRouteResult.getPaths() == null) {
                            ToastUtil.show(mContext, "no result");
                        }

                    } else {
                        ToastUtil.show(mContext, "no result");
                    }
                } else {
                    ToastUtil.showerror(mContext, errorCode);
                }
            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

            }
        });

        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);
        // fromAndTo包含路径规划的起点和终点，drivingMode表示驾车模式
        // 第三个参数表示途经点（最多支持16个），第四个参数表示避让区域（最多支持32个），第五个参数表示避让道路
        driveRouteQuery = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DRIVING_SINGLE_DEFAULT, null, null, "");
        // 异步路径规划驾车模式查询
        routeSearch.calculateDriveRouteAsyn(driveRouteQuery);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if(mListener != null && aMapLocation != null){
            if(aMapLocation != null && aMapLocation.getErrorCode() == 0){ //定位成功
                mListener.onLocationChanged(aMapLocation);
                Log.i("sty", "----------");
                startPoint = new LatLonPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            }
        }else {
            String errText = "定位失败，" + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
            Log.e("sty", errText);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

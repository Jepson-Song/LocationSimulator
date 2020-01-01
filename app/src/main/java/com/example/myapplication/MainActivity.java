package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.DialogTitle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    public LocationClient mLocationClient;
    public BDLocationListener myListener = new MyLocationListener();
    private Button btReset, btMock;
    private TextView tvLatLng;
    private LatLng nowLocation;
    private boolean isFirstLoc = true; // 是否首次定位
    private LatLng chooseLocation;
    private Marker mMarker;
    private Thread thread;// 需要一个线程一直刷新
    private Boolean RUN = true;
    private LocationManager locationManager;
    private String mMockProviderName = LocationManager.GPS_PROVIDER;;
    private double initLat = 34.03647225653339;
    private double initLng = 108.77183469000138;
    private LatLng schoolLocation = new LatLng(initLat, initLng);
    private NumberFormat formatter = new DecimalFormat("0.0000000000");
    private double shiftLat = 34.03624040945055 - 34.031880069390816;
    private double shiftLng = 108.77207723248907 - 108.76074960000825;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        //权限申请
        permissionRequest();

        //初始化控件
        initView();
        //初始化地图
        initMap();
        //初始化Marker
        initMarker();
        //初始化定位
        initLocation();

    }

    private void permissionRequest(){
        //动态申请权限
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions =permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        btReset = (Button) findViewById(R.id.btReset);
        btReset.setOnClickListener(this);
        btMock = (Button) findViewById(R.id.btMock);
        btMock.setOnClickListener(this);
        tvLatLng = (TextView) findViewById(R.id.tvLatLng);
    }

    private void setTvLatLng(LatLng tmplatlng){
        tvLatLng.setText("纬度："+formatter.format(tmplatlng.latitude)+"\n精度："+formatter.format(tmplatlng.longitude));
    }

    private void initMap() {
        //获取地图控件引用
        mBaiduMap = mMapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);

        //默认显示普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //开启交通图
        //mBaiduMap.setTrafficEnabled(true);
        //开启热力图
        //mBaiduMap.setBaiduHeatMapEnabled(true);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                changeMarker(latLng);
            }
            @Override
            public void onMapPoiClick(MapPoi mapPoi)
            {
                changeMarker(mapPoi.getPosition());//POI点坐标
            }
        });


    }

    /**
     * 添加marker
     */
    private void initMarker() {
        //定义Maker坐标点
        LatLng point = schoolLocation;//new LatLng(latitude, longitude);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions().position(point).icon(bitmap);
        //在地图上添加Marker，并显示
        mMarker = (Marker) (mBaiduMap.addOverlay(option));
        setTvLatLng(point);
        Log.v("pcw","initMarker : lat : "+ point.latitude+" lon : " + point.longitude);
    }

    private void changeMarker(LatLng tmplatlang){
        chooseLocation = tmplatlang;
        mMarker.setPosition(chooseLocation);
        setTvLatLng(tmplatlang);
        Toast.makeText(MainActivity.this, "纬度："+chooseLocation.latitude+"  经度："+chooseLocation.longitude, Toast.LENGTH_SHORT).show();
    }


    /**
     * initLocation 初始化 位置模拟
     *
     */
    private void initLocation() {
        Toast.makeText(MainActivity.this, mMockProviderName, Toast.LENGTH_LONG).show();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(mMockProviderName, false, true, false,
                false, true, true, true, 0, 5);
        locationManager.setTestProviderEnabled(mMockProviderName, true);
        //locationManager.requestLocationUpdates(mMockProviderName, 0, 0, this);
        //locationManager.requestLocationUpdates("gps", 1000L, 0.1F, this);


        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        //配置定位SDK参数
        initLocationParameter();
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
        //开启定位
        mLocationClient.start();
        //图片点击事件，回到定位点
        mLocationClient.requestLocation();
        
    }

    //配置定位SDK参数
    private void initLocationParameter() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation
        // .getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);
        option.setOpenGps(true); // 打开gps

        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    /**
     * setLocation 设置GPS的位置
     *
     */
    private void setLocation(double latitude, double longitude) {
        Location location = new Location(mMockProviderName);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude); // 纬度
        location.setLongitude(longitude); // 经度
        location.setAltitude(2.0f); // 高度
        location.setAccuracy(3.0f); // 精度
        location.setSpeed(0); // 速度
        if (Build.VERSION.SDK_INT > 16) {
            //api 16以上的需要加上这一句才能模拟定位 , 也就是targetSdkVersion > 16
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        locationManager.setTestProviderLocation(mMockProviderName, location);
    }

//    实现BDLocationListener接口,BDLocationListener为结果监听接口，异步获取定位结果
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            nowLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);
            // 当不需要定位图层时关闭定位图层
            //mBaiduMap.setMyLocationEnabled(false);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                if (location.getLocType() == BDLocation.TypeGpsLocation) {
                    // GPS定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                    // 网络定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();

                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                    // 离线定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();

                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    Toast.makeText(MainActivity.this, "服务器错误，请检查", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    Toast.makeText(MainActivity.this, "网络错误，请检查", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    Toast.makeText(MainActivity.this, "手机模式错误，请检查是否飞行", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        thread = null;
        //bitmap.recycle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btReset:
                //把定位点再次显现出来
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(nowLocation);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                break;
            case R.id.btMock:
                //模拟地点
                //mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                // 开启线程，一直修改GPS坐标
                if(thread!=null) thread = null;
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (RUN) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            setLocation(chooseLocation.latitude-shiftLat, chooseLocation.longitude-shiftLng);
                        }
                    }
                });
                thread.start();
                Toast.makeText(MainActivity.this, "MOCK!", Toast.LENGTH_SHORT).show();
                break;
            /*case R.id.buttons:
                //普通地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
             */
        }
    }


}
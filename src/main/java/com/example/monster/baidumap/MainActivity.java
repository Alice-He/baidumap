package com.example.monster.baidumap;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.example.monster.baidumap.overlayutil.PoiOverlay;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnGetSuggestionResultListener, OnGetPoiSearchResultListener {

    private static final String TAG = "MainActivity";

    //View相关的变量
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private EditText editCity;
    private Button btn_search;
    private ImageButton btn_position;
    private TextView tv_address;
    private AutoCompleteTextView keyWorldsView;
    private ArrayAdapter<String> sugAdapter;//AutoCompleteTextView的适配器
    private List<String> suggest;//适配器的数据

    //定位相关的变量
    public LocationClient mLocationClient;
    private BDLocation mCurrentPosition;
    private BitmapDescriptor mCurrentMarker;//标识当前位置的图形
    private boolean isFirstIn;//是否是第一次进入

    //POI热词建议检索相关变量
    private SuggestionSearch mSuggestionSearch;

    //POI检索相关变量
    private PoiSearch mPoiSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //初始化MapView，不然无法使用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        initView();
        initLocation();
        initSearch();
    }

    /**
     * 初始化搜索
     */
    private void initSearch() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.mmap);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 当不需要定位图层时关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        editCity = (EditText) findViewById(R.id.city);
        keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        keyWorldsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() <= 0) {
                    return;
                }
                //使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(s.toString()).city(editCity.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        btn_search = (Button) findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_address.setText("");
                String citystr = editCity.getText().toString();
                String keystr = keyWorldsView.getText().toString();
                mPoiSearch.searchInCity((new PoiCitySearchOption())
                        .city(citystr).keyword(keystr).pageNum(10));
            }
        });
        btn_position = (ImageButton) findViewById(R.id.btn_position);
        btn_position.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocation();
            }
        });
        tv_address = (TextView)findViewById(R.id.tv_address);
        tv_address.setText("");
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        isFirstIn = true;
        mCurrentPosition = new BDLocation();
        mLocationClient = new LocationClient(this);
        mLocationClient.registerLocationListener(new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                mCurrentPosition = bdLocation;
                if(isFirstIn){
                    isFirstIn = false;
                    showLocation();
                }
            }
        });
        LocationClientOption option = new LocationClientOption();
        //定位模式
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，设置返回经纬度坐标类型，默认gcj02
        option.setCoorType("bd09ll");
        //可选，设置发起定位请求的间隔，int类型，单位ms
        option.setScanSpan(1000);
        //可选，设置是否使用gps，默认false
        option.setOpenGps(true);
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setLocationNotify(true);
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(false);
        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.SetIgnoreCacheException(false);
        //如果设置了该接口，首次启动定位时，会先判断当前WiFi是否超出有效期，若超出有效期，会先重新扫描WiFi，然后定位
        option.setWifiCacheTimeOut(5 * 60 * 1000);
        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        option.setEnableSimulateGps(false);
        //可选，是否需要地址信息，默认为不需要，即参数为false
        option.setIsNeedAddress(true);
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(option);
        //发起请求，在BDAbstractLocationListener接口中，便可获得定位地址相关的信息。
        mLocationClient.start();
    }


    private void showLocation() {
        mBaiduMap.clear();
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(mCurrentPosition.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(100).latitude(mCurrentPosition.getLatitude())
                .longitude(mCurrentPosition.getLongitude()).build();
        // 设置定位数据
        mBaiduMap.setMyLocationData(locData);
        // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
        mCurrentMarker = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_geo);
        MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
        mBaiduMap.setMyLocationConfiguration(config);

        LatLng ll = new LatLng(mCurrentPosition.getLatitude(), mCurrentPosition.getLongitude());
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLngZoom(ll, 18);
        mBaiduMap.animateMapStatus(msu);
        //定义Maker坐标点
        LatLng point = new LatLng(mCurrentPosition.getLatitude(), mCurrentPosition.getLongitude());
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(mCurrentMarker);
        //在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);
        tv_address.setText(mCurrentPosition.getAddrStr());
    }

    /**
     * 从互联网上获得的POI热词的数据
     *
     * @param suggestionResult
     */
    @Override
    public void onGetSuggestionResult(SuggestionResult suggestionResult) {
        if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
            return;
        }
        suggest = new ArrayList<String>();
        for (SuggestionResult.SuggestionInfo info : suggestionResult.getAllSuggestions()) {
            if (info.key != null) {
                suggest.add(info.key);
            }
        }
        sugAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, suggest);
        keyWorldsView.setAdapter(sugAdapter);
        sugAdapter.notifyDataSetChanged();
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
            mBaiduMap.clear();
            PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(poiResult);
            overlay.addToMap();
            overlay.zoomToSpan();
            return;
        }
        if (poiResult.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

            // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
            String strInfo = "在";
            for (CityInfo cityInfo : poiResult.getSuggestCityList()) {
                strInfo += cityInfo.city;
                strInfo += ",";
            }
            strInfo += "找到结果";
            Toast.makeText(MainActivity.this, strInfo, Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
        if (poiDetailResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(MainActivity.this, poiDetailResult.getName() + ": " + poiDetailResult.getAddress(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    /**
     * 对区域检索的范围进行绘制
     *
     * @param bounds
     */
    public void showBound(LatLngBounds bounds) {
        BitmapDescriptor bdGround = BitmapDescriptorFactory
                .fromResource(R.drawable.ground_overlay);

        OverlayOptions ooGround = new GroundOverlayOptions()
                .positionFromBounds(bounds).image(bdGround).transparency(0.8f);
        mBaiduMap.addOverlay(ooGround);

        MapStatusUpdate u = MapStatusUpdateFactory
                .newLatLng(bounds.getCenter());
        mBaiduMap.setMapStatus(u);

        bdGround.recycle();
    }

    private class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {
            super.onPoiClick(index);
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
                    .poiUid(poi.uid));
            return true;
        }
    }

    //覆写几个方法，对百度地图的SDK组件的生命周期进行控制
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mSuggestionSearch.destroy();
        mPoiSearch.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

}

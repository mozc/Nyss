package com.example.nsyy.utils;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.example.nsyy.permission.NsyyLocationListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 获取手机当前位置
 */
public class LocationUtil {
    private volatile static LocationUtil uniqueInstance;
    private LocationManager locationManager;
    private Context context;
    private AddressCallback addressCallback;
    private NsyyLocationListener locationListener = new NsyyLocationListener();

    @Override
    public String toString() {
        return "LocationUtil{" +
                "locationManager=" + locationManager +
                ", mContext=" + context +
                ", addressCallback=" + addressCallback +
                ", locationListener=" + locationListener +
                '}';
    }

    private LocationUtil() {

    }

    public void setContext(Context context) {
        this.context = context;
        addressCallback = new LocationUtil.AddressCallback() {
            @Override
            public void onGetAddress(Address address) {
                String countryName = address.getCountryName();//国家
                String adminArea = address.getAdminArea();//省
                String locality = address.getLocality();//市
                String subLocality = address.getSubLocality();//区
                String featureName = address.getFeatureName();//街道
                Log.e("定位地址: ",countryName+adminArea+locality+subLocality+featureName);
            }

            @Override
            public void onGetLocation(double lat, double lng) {
                Log.e("定位经纬度: ",lat + "\n" + lng);
            }
        };
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    //采用Double CheckLock(DCL)实现单例
    public static LocationUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (LocationUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new LocationUtil();
                }
            }
        }
        return uniqueInstance;
    }

    public String getLocation(boolean turnOnGPS) {
        // 检查位置权限
        PermissionUtil.checkLocationPermission(context);

        Location location = null;
        if (gpsEnabled() && getGPSLocation(locationManager) != null) {
            //GPS 定位的精准度比较高，但是非常耗电。
            System.out.println("=====GPS_PROVIDER=====");

            // 获取上次的位置，一般第一次运行，此值为null
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistace
            //LocationManager 每隔 5 秒钟会检测一下位置的变化情况，当移动距离超过 10 米的时候，
            // 就会调用 LocationListener 的 onLocationChanged() 方法，并把新的位置信息作为参数传入。
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener, Looper.getMainLooper());

        } else if (netWorkEnabled() && getNetWorkLocation(locationManager) != null) {//Google服务被墙不可用
            //网络定位的精准度稍差，但耗电量比较少。
            System.out.println("=====NETWORK_PROVIDER=====");

            //从网络获取经纬度
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 50000, 10, locationListener, Looper.getMainLooper());
        } else {
            System.out.println("=====NO_PROVIDER=====");
            if (turnOnGPS) {
                initGPS();
            }
        }

        // 由于第一次访问 getLastKnownLocation， 或者手机处于室内，或者信号不好 获取的 location 有可能为空，，所以需要主动去进行位置更新
        if (gpsEnabled()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener, Looper.getMainLooper());
        }
        if (netWorkEnabled()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener, Looper.getMainLooper());
        }

        return getAddress(location);
    }

    private boolean gpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean netWorkEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private Location getGPSLocation(LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private Location getNetWorkLocation(LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * 判断GPS是否开启
     */
    public void initGPS() {
        List<String> allProviders = locationManager.getAllProviders();
        if (!allProviders.contains(LocationManager.GPS_PROVIDER)) {
            // 如果不支持 gps ，则通过 network 判断
            if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                openGPSDialog();
            }
        } else {
            //判断GPS是否开启，没有开启，则开启
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                openGPSDialog();
            }
        }

    }

    /**
     * 打开GPS对话框
     */
    private void openGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示")
                .setMessage("打开定位功能，可以提高定位精确度。 \n 请点击\"设置\"-\"定位服务\"-打开定位功能。")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //跳转到手机打开GPS页面
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .setNeutralButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    /**
     * 将 location 转换为具体地址 TODO 这里需要根据前端需求确定返回类型
     * @param location
     * @return
     */
    private String getAddress(Location location) {
        if (location == null) {
            return "unknown address";
        }

        //Geocoder通过经纬度获取具体信息
        Geocoder gc = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> locationList = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (locationList != null && locationList.size() > 0) {
                Address address = locationList.get(0);
//                String countryName = address.getCountryName();//国家
//                String countryCode = address.getCountryCode();
//                String adminArea = address.getAdminArea();//省
//                String locality = address.getLocality();//市
//                String subLocality = address.getSubLocality();//区
//                String featureName = address.getFeatureName();//街道

                for (int i = 0; address.getAddressLine(i) != null; i++) {
                    String addressLine = address.getAddressLine(i);
                    System.out.println("addressLine=====" + addressLine);
                }
                if(addressCallback != null){
                    addressCallback.onGetAddress(address);
                }
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface AddressCallback{
        void onGetAddress(Address address);
        void onGetLocation(double lat,double lng);
    }
}


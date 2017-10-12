package jp.co.conol.wifihelper_admin_lib.device_manager;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import jp.co.conol.wifihelper_admin_lib.device_manager.model.Device;
import jp.co.conol.wifihelper_admin_lib.lib.ApiInterface;
import jp.co.conol.wifihelper_admin_lib.lib.ClientHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class DeviceManager {

//    public static List<Device> getDeviceInfoList() {
//
//        String serviceKey = "H7Pa7pQaVxxG";
//
//        ApiInterface apiInterface = ClientHelper.createService(ApiInterface.class);
//        Call<List<Device>> call = apiInterface.getDeviceInfoList(serviceKey);
//
//        call.enqueue(new Callback<List<Device>>() {
//            @Override
//            public void onResponse(@NonNull Call<List<Device>> call, @NonNull Response<List<Device>> response) {
//                Log.d(TAG, "onResponse: " + response.isSuccessful());
//                if(response.isSuccessful()) {
//
//                    // TODO デバイス一覧をDBに保存
//                    List<Device> deviceInfoList = response.body();
//
//
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<List<Device>> call, @NonNull Throwable t) {
//                Log.d(TAG, "onFailure: " + t.getCause() + ", " + t.getMessage());
//                t.printStackTrace();
//            }
//        });
//    }
}

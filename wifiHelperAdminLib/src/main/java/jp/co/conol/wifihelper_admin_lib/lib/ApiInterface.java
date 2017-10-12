package jp.co.conol.wifihelper_admin_lib.lib;

import java.util.List;

import jp.co.conol.wifihelper_admin_lib.device_manager.model.Device;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Masafumi_Ito on 2017/09/04.
 */

public interface ApiInterface {

    // サービスキーに紐付いているデバイス情報一覧を取得
//    @GET(Constants.ApiUrl.DEVICES + "{serviceKey}")
//    Call<List<Device>> getDeviceInfoList(
//            @Path("service_key") String serviceKey
//    );

//    // ログを送信
//    @POST(Constants.ApiUrl.LOG + "{serviceKey}")
//    Call<LogResponse> postLogs(
//            @Body Logs logs,
//            @Path("service_key") String serviceKey
//    );
}

package jp.co.conol.wifihelper_android.lib;

import jp.co.conol.wifihelper_android.Constants;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Masafumi_Ito on 2017/09/28.
 */

// Retrofitクライアントの共通設定
public class ClientHelper {

    public static <S> S createService(Class<S> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.ApiUrl.END_POINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(serviceClass);
    }
}

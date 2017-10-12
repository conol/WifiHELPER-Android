package jp.co.conol.wifihelper_admin_lib.lib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

/**
 * Created by Masafumi_Ito on 2017/09/28.
 */

// Retrofitの共通処理
public class CustomRetrofitCallback<T> implements Callback<T> {

    private final Context mContext;

    public CustomRetrofitCallback(Context context) {
        mContext = context;
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        Log.d(TAG, "onResponse: " + response.isSuccessful());
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        Log.d(TAG, "onFailure: " + t.getCause() + ", " + t.getMessage());
        t.printStackTrace();
    }

}

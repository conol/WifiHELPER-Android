package jp.co.conol.wifihelper_lib.wifi_helper;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_lib.Util;

/**
 * Created by Masafumi_Ito on 2017/10/12.
 */

public class GetAvailableDevices extends AsyncTask<Void, Void, List<String>> {

    private AsyncCallback mAsyncCallback = null;

    public interface AsyncCallback{
        void onSuccess(List<String> deviceIdList);
        void onFailure(Exception e);
    }

    public GetAvailableDevices(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

    protected List<String> doInBackground(Void... params){

        String responseJsonString = null;
        try {
            responseJsonString = Util.Http.get("http://13.112.232.171/api/services/H7Pa7pQaVxxG.json", null);
        } catch (Exception e) {
            onFailure(e);
        }

        List<String> deviceIdList = new ArrayList<>();

        if(responseJsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(responseJsonString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jRec = jsonArray.getJSONObject(i);
                    String deviceId = jRec.getString("device_id").replace(" ", "").toLowerCase();
                    deviceIdList.add(deviceId);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return deviceIdList;
    }

    @Override
    protected void onPostExecute(List<String> deviceIdList) {
        super.onPostExecute(deviceIdList);
        onSuccess(deviceIdList);
    }

    private void onSuccess(List<String> deviceIdList) {
        this.mAsyncCallback.onSuccess(deviceIdList);
    }

    private void onFailure(Exception e) {
        this.mAsyncCallback.onFailure(e);
    }
}

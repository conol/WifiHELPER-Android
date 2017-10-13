package jp.co.conol.wifihelper_admin_lib.device_manager;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Created by Masafumi_Ito on 2017/10/13.
 */

public class SendLogAsyncTask extends AsyncTask<String, Void, Void> {

    private AsyncCallback mAsyncCallback = null;

    public interface AsyncCallback{
        void onSuccess();
        void onFailure(Exception e);
    }

    public SendLogAsyncTask(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

    @Override
    protected Void doInBackground(String... params) {

        // ログ送信用のjsonを作成
        JSONObject deviceLogsJson = new JSONObject();
        JSONArray deviceLogJsonArray = new JSONArray();
        try {
            JSONObject jsonOneData;
            jsonOneData = new JSONObject();
            jsonOneData.put("device_id", params[0]);
            jsonOneData.put("used_at", params[1]);
            jsonOneData.put("lat_lng", params[2]);
            jsonOneData.put("notes", params[3]);
            jsonOneData.put("shop", null);
            deviceLogJsonArray.put(jsonOneData);

            deviceLogsJson.put("device_logs", deviceLogJsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // サーバーにログを送信
        try {
            String buffer = "";
            HttpURLConnection con = null;
            URL url = new URL("http://13.112.232.171/api/device_logs/H7Pa7pQaVxxG.json");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept-Language", "jp");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream os = con.getOutputStream();
            PrintStream ps = new PrintStream(os);
            ps.print(deviceLogsJson.toString());
            ps.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            buffer = reader.readLine();

//            JSONArray jsonArray = new JSONArray(buffer);
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonObject = jsonArray.getJSONObject(i);
////                Log.d("HTTP REQ", jsonObject.getString("name"));
//            }
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ServerLog", e.toString());
            onFailure(e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        super.onPostExecute(params);
        onSuccess();
    }

    private void onSuccess() {
        this.mAsyncCallback.onSuccess();
    }

    private void onFailure(Exception e) {
        this.mAsyncCallback.onFailure(e);
    }

    private String convertToString(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

package jp.co.conol.wifihelper_lib.cuona.wifi_helper_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.util.Date;

import jp.co.conol.wifihelper_lib.cuona.WifiHelper;


/**
 * Created by Masafumi_Ito on 2017/10/04.
 */

// Wifi有効期限のアラームが切れている場合でも、Wifiの状態切替をトリガーにしてWifi有効期限を確認
public class WifiStateWatcher extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

            // 変化前の状態を取得
            int previousState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // 変化後の状態を取得
            int currentState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // wifi接続時に期限が切れていたらAP削除
            if(currentState == WifiManager.WIFI_STATE_ENABLED) {

                // 有効期限の日時とSSIDを取得
                SharedPreferences pref = context.getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
                Date expireDateTime = new Date(pref.getLong("expireDateTime", -1));
                String ssid = pref.getString("ssid", null);

                // 現在の日時を取得
                Date currentDateTime = new Date(System.currentTimeMillis());

                if(0 <= currentDateTime.compareTo(expireDateTime) && ssid != null) {

                    // wifiを解除
                    WifiHelper.deleteAccessPoint(context, ssid);
                    WifiHelper.tryDisconnect(context); // removeWifiSetting後に実行

                    // メッセージを表示
                    Toast.makeText(context, "Wi-Fi Helperで設定したWi-Fiの有効期限が切れました", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}


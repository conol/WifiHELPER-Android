package jp.co.conol.wifihelper_android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * Created by Masafumi_Ito on 2017/10/14.
 */

public class WifiStateBroadcastReceiver extends BroadcastReceiver {

    private Listener mListener;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context instanceof Listener)
            mListener = (Listener) context;

        if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            // 変化前の状態を取得
            int previousState = intent.getIntExtra(
                    WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // 変化後の状態を取得
            int currentSate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        }
    }

    public interface Listener {
        void getWifiState(int currentSate);
    }
}

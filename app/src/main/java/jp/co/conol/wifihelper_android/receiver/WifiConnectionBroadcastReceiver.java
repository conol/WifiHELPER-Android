package jp.co.conol.wifihelper_android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by Masafumi_Ito on 2017/10/12.
 */

public class WifiConnectionBroadcastReceiver extends BroadcastReceiver {

    private Listener mListener;

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (context instanceof Listener)
            mListener = (Listener) context;

        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo.State state = null;
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            switch (info.getState()) {
                case DISCONNECTED:
                    state = NetworkInfo.State.DISCONNECTED;
                    break;
                case SUSPENDED:
                    state = NetworkInfo.State.SUSPENDED;
                    break;
                case CONNECTING:
                    state = NetworkInfo.State.CONNECTING;
                    break;
                case CONNECTED:
                    state = NetworkInfo.State.CONNECTED;
                    break;
                case DISCONNECTING:
                    state = NetworkInfo.State.DISCONNECTING;
                    break;
                case UNKNOWN:
                    Log.e(TAG, "Wifi connection state is UNKNOWN");
                    break;
                default:
                    Log.e(TAG, "Wifi connection state is OTHER");
                    break;
            }
            mListener.getWifiConnectionState(state);
        }
    }

    public interface Listener {
        void getWifiConnectionState(NetworkInfo.State state);
    }
}

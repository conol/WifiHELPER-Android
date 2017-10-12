package jp.co.conol.wifihelper_android.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.json.JSONException;

import java.util.Calendar;
import java.util.Date;

import jp.co.conol.wifihelper_admin_lib.corona.CoronaNfc;
import jp.co.conol.wifihelper_admin_lib.corona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.wifi_connector.WifiConnector;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;
import jp.co.conol.wifihelper_android.R;
import jp.co.conol.wifihelper_android.receiver.WifiConnectionBroadcastReceiver;
import jp.co.conol.wifihelper_android.receiver.WifiStateBroadcastReceiver;

import static android.net.NetworkInfo.State.CONNECTED;

public class MainActivity extends AppCompatActivity
        implements WifiConnectionBroadcastReceiver.Listener, WifiStateBroadcastReceiver.Listener {

    WifiConnectionBroadcastReceiver mWifiConnectionBroadcastReceiver = new WifiConnectionBroadcastReceiver();
    WifiStateBroadcastReceiver mWifiStateBroadcastReceiver = new WifiStateBroadcastReceiver();
    private CoronaNfc mCoronaNfc;
    private boolean isScanning = false;
    private boolean isWifiEnable = false;
    private ConstraintLayout mScanConstraintLayout;
    private ConstraintLayout mScanDialogConstraintLayout;
    private ConstraintLayout mConnectingProgressConstraintLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanConstraintLayout = (ConstraintLayout) findViewById(R.id.ScanConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) findViewById(R.id.scanDialogConstraintLayout);
        mConnectingProgressConstraintLayout = (ConstraintLayout) findViewById(R.id.connectingProgressConstraintLayout);

        try {
            mCoronaNfc = new CoronaNfc(this);
        } catch (NFCNotAvailableException e) {
            Log.d("CoronaNfc", e.toString());
            finish();
        }

        // nfcがオフの場合はダイアログを表示
        if(!mCoronaNfc.isEnable()) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.nfc_dialog))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        // wifiの状態を取得
        if(WifiConnector.isEnable(getApplicationContext()))
            isWifiEnable = true;

        // アプリ未起動でNFCタグが呼ばれた場合も読み込み
        if (getIntent() != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            startScan(false);
            scanNfc(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(isScanning) {
            scanNfc(intent);
        }
    }

    private void scanNfc(Intent intent) {
        CNFCReaderTag tag = null;
        Wifi wifi = null;

        try {
            tag = mCoronaNfc.getReadTagFromIntent(intent);
        } catch (CNFCReaderException e) {
            Log.d("CNFCReader", e.toString());
            return;
        }

        if (tag != null) {
            String chipId = tag.getChipIdString();
            String serviceId = tag.getServiceIdString();
//                Toast.makeText(this, "deviceId=" + chipId + "\njson=" + serviceId, Toast.LENGTH_LONG).show();

            try {
                wifi = WifiHelper.parseJsonToObj(serviceId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        final Wifi readWifi = wifi;

        // 接続期限日時の算出
        final Calendar expirationDay  = Calendar.getInstance();
        expirationDay.setTime(new Date(System.currentTimeMillis()));
        expirationDay.add(Calendar.DATE, wifi.getDays());

        // nfc設定の確認
        if(!WifiConnector.isEnable(getApplicationContext())) {
            new AlertDialog.Builder(this)
                    .setTitle("Wi-Fi設定")
                    .setMessage("Wi-Fiがオフになっています\nWi-Fiをオンにしてもよろしいですか？")
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WifiConnector.setEnable(MainActivity.this, true);
                            connectWifi(readWifi, expirationDay);
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        } else {
            connectWifi(wifi, expirationDay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // AP接続監視用のレシーバーを登録
        IntentFilter wifiConnectionIntentFilter = new IntentFilter();
        wifiConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectionBroadcastReceiver, wifiConnectionIntentFilter);

        // Wifi監視用のレシーバーを登録
        IntentFilter wifiStateIntentFilter = new IntentFilter();
        wifiStateIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mWifiStateBroadcastReceiver, wifiStateIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCoronaNfc.disableForegroundDispatch(MainActivity.this);

        // レシーバーを解除
        unregisterReceiver(mWifiConnectionBroadcastReceiver);
        unregisterReceiver(mWifiStateBroadcastReceiver);
    }

    public void onStartScanButtonClicked(View view) {
        if(!isScanning) {
            startScan(true);
        }
    }

    private void startScan(boolean enableForegroundDispatch) {
        // nfc読み込み待機
        if(enableForegroundDispatch)    // enableForegroundDispatchの実行有無を判定
            mCoronaNfc.enableForegroundDispatch(MainActivity.this);
        isScanning = true;

        // 読み込み画面を表示
        mScanDialogConstraintLayout.setVisibility(View.VISIBLE);
        mScanConstraintLayout.setVisibility(View.VISIBLE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom));
        mScanConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slowly));
    }

    public void onCancelScanButtonClicked(View view) {
        if(isScanning) {
            cancelScan(true);
        }
    }

    private void cancelScan(boolean disableForegroundDispatch) {
        // nfc読み込み待機を解除
        if(disableForegroundDispatch)    // enableForegroundDispatchの実行有無を判定
            mCoronaNfc.disableForegroundDispatch(MainActivity.this);
        isScanning = false;

        // 読み込み画面を非表示
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));
        mScanConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));
    }

    private void connectWifi(Wifi wifi, Calendar expirationDay) {

        // 読み込み画面を非表示（背景は残す）
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));

        // Wifi設定
        WifiConnector wifiConnector = new WifiConnector(
                MainActivity.this,
                wifi.getSsid(),
                wifi.getPass(),
                WifiConnector.WPA_WPA2PSK,
                expirationDay
        );

        // Wifi接続
        wifiConnector.tryConnect();

        // wifi接続中を示すプログレスバーの表示
        mConnectingProgressConstraintLayout.setVisibility(View.VISIBLE);
    }

    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK) {

            // 読み込み中に戻るタップでスキャン中止
            if(isScanning) {
                cancelScan(true);
            } else {
                finish();
            }
        }
        return false;
    }

    @Override
    public void getWifiState(int currentState) {
//        if(currentState == WifiManager.WIFI_STATE_ENABLED) {
//            isWifiEnable = true;
//        } else if(currentState == WifiManager.WIFI_STATE_DISABLED) {
//            isWifiEnable = false;
//        }
    }

    @Override
    public void getWifiConnectionState(NetworkInfo.State state) {
        if(isScanning && state == CONNECTED ) {
            isScanning = false;

            // wifi接続中を示すプログレスバーの非表示
            mConnectingProgressConstraintLayout.setVisibility(View.GONE);

            // 読み込み画面を非表示
            mScanConstraintLayout.setVisibility(View.GONE);
            mScanConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));

            // 表示メッセージの作成
            String dialogMessage;
            if(state == CONNECTED)
                dialogMessage = getString(R.string.connecting_wifi_done);
            else
                dialogMessage = getString(R.string.connecting_wifi_failed);

            // wifi接続後のメッセージを表示
            new AlertDialog.Builder(this)
                    .setMessage(dialogMessage)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }
    }
}

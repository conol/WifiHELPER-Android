package jp.co.conol.wifihelper_android.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_android.custom.CuonaUtil;
import jp.co.conol.wifihelper_android.custom.ProgressDialog;
import jp.co.conol.wifihelper_android.custom.ScanCuonaDialog;
import jp.co.conol.wifihelper_android.custom.SimpleAlertDialog;
import jp.co.conol.wifihelper_lib.cuona.Cuona;
import jp.co.conol.wifihelper_android.MyUtil;
import jp.co.conol.wifihelper_android.R;
import jp.co.conol.wifihelper_android.receiver.WifiConnectionBroadcastReceiver;
import jp.co.conol.wifihelper_lib.cuona.NFCNotAvailableException;
import jp.co.conol.wifihelper_lib.cuona.WifiHelper;
import jp.co.conol.wifihelper_lib.cuona.cuona_reader.CuonaReaderException;
import jp.co.conol.wifihelper_lib.cuona.wifi_helper_model.Wifi;

import static android.net.NetworkInfo.State.CONNECTED;

public class MainActivity extends AppCompatActivity implements WifiConnectionBroadcastReceiver.Listener {

    private ScanCuonaDialog mScanCuonaDialog;
    private ProgressDialog mWifiConnectingProgressDialog;   // Wifi接続中のプログレスダイアログ
    private Handler mConnectingTimeoutHandler = new Handler();  // Wifi接続実行のタイムアウトハンドラー
    private WifiConnectionBroadcastReceiver mWifiConnectionBroadcastReceiver = new WifiConnectionBroadcastReceiver();
    private Cuona mCuona;
    private boolean isSucceededConnectingAp = false;  // WifiでAPに接続成功したか否か
    private boolean isWifiConnectingByApp = false;   // wifi接続か否か
    private List<String> mAvailableDeviceIdList = new ArrayList<>();    // WifiHelperのサービスに登録されているデバイスのID一覧
    private final int PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mCuona = new Cuona(this);
        } catch (NFCNotAvailableException e) {
            Log.d("CuonaNfc", e.toString());
            finish();
        }

        // CUONAスキャンダイアログのインスタンスを生成
        mScanCuonaDialog = new ScanCuonaDialog(MainActivity.this, mCuona, 60000, false);

        // 本体に登録されているデバイスIDを取得
        final Gson gson = new Gson();
        mAvailableDeviceIdList = gson.fromJson(MyUtil.SharedPref.getString(this, "deviceIds"), new TypeToken<List<String>>(){}.getType());
        if(mAvailableDeviceIdList == null) mAvailableDeviceIdList = new ArrayList<>();

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        CuonaUtil.checkAccessCoarseLocationPermission(this, PERMISSION_REQUEST_CODE);

        // nfcがオフの場合はダイアログを表示
        CuonaUtil.checkNfcSetting(this, mCuona);

        // サーバーに登録されているデバイスIDを取得
        if (MyUtil.Network.isEnable(this)) {

            // 読み込みダイアログを表示
            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.progress_dialog));
            progressDialog.show();

            new WifiHelper(new WifiHelper.AsyncCallback() {
                @Override
                public void onSuccess(Object object) {
                    mAvailableDeviceIdList = (List<String>) object;

                    // 読み込みダイアログを非表示
                    progressDialog.dismiss();

                    // 接続成功してもデバイスID一覧が無ければエラー
                    if(mAvailableDeviceIdList == null || mAvailableDeviceIdList.size() == 0) {
                        new SimpleAlertDialog(MainActivity.this, getString(R.string.error_fail_get_device_ids)).show();
                    } else {

                        // デバイスIDを保存
                        MyUtil.SharedPref.saveString(MainActivity.this, "deviceIds", gson.toJson(mAvailableDeviceIdList));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {

                            // 読み込みダイアログを非表示
                            progressDialog.dismiss();

                            new SimpleAlertDialog(MainActivity.this, getString(R.string.error_fail_get_device_ids)).show();
                        }
                    });
                }
            }).execute(WifiHelper.Task.GetAvailableDevices);
        }
        else if(!MyUtil.Network.isEnable(this) && mAvailableDeviceIdList.size() == 0) {
            new SimpleAlertDialog(MainActivity.this, getString(R.string.error_network_disable)).show();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if(mScanCuonaDialog.isShowing()) {

            // wifi設定かオフになっている場合は確認ダイアログを表示
            if (!WifiHelper.isEnable(getApplicationContext())) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.wifi_dialog))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                WifiHelper.setEnable(MainActivity.this, true);
                                new SimpleAlertDialog(MainActivity.this, getString(R.string.wifi_dialog_done)).show();
                                if(!mScanCuonaDialog.isShowing()) mScanCuonaDialog.show();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return;
            }

            // CUONAスキャンダイアログを非表示
            mScanCuonaDialog.dismiss();

            // nfc読み込み処理実行
            String deviceId;
            Wifi wifi;
            try {
                deviceId = mCuona.readDeviceId(intent);
                wifi = WifiHelper.readWifiSetting(intent, mCuona);
            } catch (CuonaReaderException e) {
                e.printStackTrace();
                new SimpleAlertDialog(MainActivity.this, getString(R.string.error_incorrect_touch_nfc)).show();
                return;
            }

            // サーバーに登録されているWifiHelper利用可能なデバイスに、タッチされたNFCが含まれているか否か確認
            if(mAvailableDeviceIdList != null && deviceId != null) {
                if (!mAvailableDeviceIdList.contains(deviceId)) {
                    new SimpleAlertDialog(MainActivity.this, getString(R.string.error_not_exist_in_devise_ids)).show();
                }
                // 含まれていればWifi接続を開始
                else {
                    connectWifi(wifi);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCuona != null) mCuona.enableForegroundDispatch(MainActivity.this);

        // AP接続監視用のレシーバーを登録
        IntentFilter wifiConnectionIntentFilter = new IntentFilter();
        wifiConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectionBroadcastReceiver, wifiConnectionIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCuona != null) mCuona.disableForegroundDispatch(MainActivity.this);

        // AP接続監視用のレシーバーを解除
        unregisterReceiver(mWifiConnectionBroadcastReceiver);
    }

    public void onStartScanButtonClicked(View view) {

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        CuonaUtil.checkAccessCoarseLocationPermission(this, PERMISSION_REQUEST_CODE);

        // nfcがオフの場合はダイアログを表示
        CuonaUtil.checkNfcSetting(this, mCuona);

        // BluetoothとNFCが許可されている場合処理を進める
        if(mCuona.isNfcEnabled()) {
            mScanCuonaDialog.show();
        }
    }

    private void connectWifi(Wifi wifi) {

        // wifi接続中を示すプログレスバーの表示
        mWifiConnectingProgressDialog = new ProgressDialog(MainActivity.this);
        mWifiConnectingProgressDialog.setMessage(getString(R.string.connecting_wifi));
        mWifiConnectingProgressDialog.show();

        // アプリからWifi接続を開始
        isWifiConnectingByApp = true;

        // Wifi設定
        WifiHelper wifiHelper = new WifiHelper(
                MainActivity.this,
                wifi.getSsid(),
                wifi.getPassword(),
                wifi.getKind(),
                wifi.getDays()
        );

        // Wifi接続
        isSucceededConnectingAp = wifiHelper.tryConnect();

        // 15秒間でタイムアウト
        mConnectingTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.connecting_wifi_failed_not_ap))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissWifiConnectingProgressDialog();
                            }
                        })
                        .show();
            }
        }, 15000);
    }

    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mScanCuonaDialog != null && mScanCuonaDialog.isShowing()) {
                mScanCuonaDialog.dismiss();
            } else if(mWifiConnectingProgressDialog != null && mWifiConnectingProgressDialog.isShowing()) {
                dismissWifiConnectingProgressDialog();
            } else {
                finish();
            }
        }
        return false;
    }

    @Override
    public void getWifiConnectionState(NetworkInfo.State state) {

        // アプリからWifi接続が行われて、接続成功した場合
        if(isWifiConnectingByApp && state == CONNECTED) {

            // Wifi接続中のプログレスダイアログを閉じる
            dismissWifiConnectingProgressDialog();

            // 表示メッセージの作成
            String dialogMessage;
            if(isSucceededConnectingAp)
                dialogMessage = getString(R.string.connecting_wifi_done);
            else
                dialogMessage = getString(R.string.connecting_wifi_failed);

            // wifi接続後のメッセージを表示
            new SimpleAlertDialog(this, dialogMessage).show();
        }

    }

    // Wifi接続中のプログレスダイアログを閉じる
    private void dismissWifiConnectingProgressDialog() {
        mConnectingTimeoutHandler.removeCallbacksAndMessages(null);
        mWifiConnectingProgressDialog.dismiss();
        isWifiConnectingByApp = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {

            // パーミッションを許可しない場合
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.grant_permission, Toast.LENGTH_LONG).show();
            }
        }
    }
}

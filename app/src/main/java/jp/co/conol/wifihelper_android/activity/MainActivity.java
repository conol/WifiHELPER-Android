package jp.co.conol.wifihelper_android.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jp.co.conol.wifihelper_admin_lib.corona.CoronaNfc;
import jp.co.conol.wifihelper_admin_lib.corona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.device_manager.GetDevicesAsyncTask;
import jp.co.conol.wifihelper_admin_lib.wifi_connector.WifiConnector;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;
import jp.co.conol.wifihelper_android.MyUtil;
import jp.co.conol.wifihelper_android.R;
import jp.co.conol.wifihelper_android.receiver.WifiConnectionBroadcastReceiver;

import static android.net.NetworkInfo.State.CONNECTED;

public class MainActivity extends AppCompatActivity implements WifiConnectionBroadcastReceiver.Listener {

    private SharedPreferences mPref;
    private Gson mGson = new Gson();
    private Handler mScanDialogAutoCloseHandler = new Handler();
    private WifiConnectionBroadcastReceiver mWifiConnectionBroadcastReceiver = new WifiConnectionBroadcastReceiver();
    private CoronaNfc mCoronaNfc;
    private boolean mConnectedAp = false;  // WifiでAPに接続成功したか否か
    private boolean mWifiStateChange = false;  // 本アプリからWifiをオンに切り替えたか否か
    private boolean mAvailableService = false;  // 読み込んだタグがWifiHelperに対応しているか否か
    private boolean isScanning = false;
    private List<String> mDeviceIds = new ArrayList<>();    // WifiHelperのサービスに登録されているデバイスのID一覧
    private final int PERMISSION_REQUEST_CODE = 1000;
    private ConstraintLayout mScanBackgroundConstraintLayout;
    private ConstraintLayout mScanDialogConstraintLayout;
    private ConstraintLayout mConnectingProgressConstraintLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanBackgroundConstraintLayout = (ConstraintLayout) findViewById(R.id.ScanBackgroundConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) findViewById(R.id.scanDialogConstraintLayout);
        mConnectingProgressConstraintLayout = (ConstraintLayout) findViewById(R.id.connectingProgressConstraintLayout);

        try {
            mCoronaNfc = new CoronaNfc(this);
        } catch (NFCNotAvailableException e) {
            Log.d("CoronaNfc", e.toString());
            finish();
        }

        // 本体に登録されているデバイスIDを取得
        mPref = getSharedPreferences("deviceIds", Context.MODE_PRIVATE);
        Date deviceIdsSavedTime = new Date(mPref.getLong("deviceIdsSavedTime", -1));
        mDeviceIds = mGson.fromJson(mPref.getString("deviceIds", null), new TypeToken<List<String>>(){}.getType());
        if(mDeviceIds == null) mDeviceIds = new ArrayList<>();

        // 現在日時とデバイスID有効期限を算出（一時間）
        Calendar currentTime  = Calendar.getInstance();
        Calendar expireTime  = Calendar.getInstance();
        currentTime.setTime(new Date(System.currentTimeMillis()));
        expireTime.setTime(deviceIdsSavedTime);
        expireTime.add(Calendar.HOUR, 1);

        // サーバーに登録されているデバイスIDを取得
        final Handler handler = new Handler();
        if ((mDeviceIds.size() == 0 || 1 == currentTime.compareTo(expireTime)) && MyUtil.Network.isConnected(this)) {
            new GetDevicesAsyncTask(new GetDevicesAsyncTask.AsyncCallback() {
                @Override
                public void onSuccess(List<List<String>> deviceIdList) {

                    // 接続成功してもデバイスID一覧が無ければエラー
                    if(deviceIdList == null || deviceIdList.size() == 0) {
                        showAlertDialog();
                    } else {

                        // デバイスIDのリストを作成
                        for(int i = 0; i < deviceIdList.size(); i++) {
                            mDeviceIds.add(deviceIdList.get(i).get(0));
                        }

                        // デバイスIDと取得日時を保存
                        SharedPreferences.Editor editor = mPref.edit();
                        editor.putLong("deviceIdsSavedTime", new Date(System.currentTimeMillis()).getTime());
                        editor.putString("deviceIds", mGson.toJson(mDeviceIds));
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    showAlertDialog();
                }

                // デバイスID取得失敗でアラートを表示
                private void showAlertDialog() {
                    handler.post(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(getString(R.string.error_fail_get_device_ids))
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .show();
                        }
                    });
                }

            }).execute();
        }
        // デバイスIDが未取得か期限が切れていて、ネットに未接続の場合はエラー
        else if ((mDeviceIds.size() == 0 || 1 == currentTime.compareTo(expireTime)) && !MyUtil.Network.isConnected(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_network_disable))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要（wifi接続時）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 許可されていない場合
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // 許可を求めるダイアログを表示
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_CODE
                );
            }
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

        // アプリ未起動でNFCタグが呼ばれた場合も読み込み
        if (getIntent() != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {

            // 読み込み画面を表示し読み込み処理を開始
            isScanning = true;
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

        try {
            tag = mCoronaNfc.getReadTagFromIntent(intent);
        } catch (CNFCReaderException e) {
            Log.d("CNFCReader", e.toString());
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(getString(R.string.error_not_exist_in_devise_ids))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            return;
        }

        if (tag != null) {
            String deviceId = tag.getChipIdString().toLowerCase();
            String serviceId = tag.getServiceIdString();
//                Toast.makeText(this, "deviceId=" + chipId + "\njson=" + serviceId, Toast.LENGTH_LONG).show();

            // サーバーに登録されているWifiHelper利用可能なデバイスに、タッチされたNFCが含まれているか否か確認
            if(mDeviceIds != null) {
                if (!mDeviceIds.contains(deviceId)) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.error_not_exist_in_devise_ids))
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                }
                // 含まれていれば処理を進める
                else {
                    try {
                        final Wifi wifi = WifiHelper.parseJsonToObj(serviceId);

                        // 接続期限日時の算出
                        Calendar expirationDay = Calendar.getInstance();
                        if(wifi.getDays() != null && 1 <= wifi.getDays() && wifi.getDays() <= 365) {
                            expirationDay.setTime(new Date(System.currentTimeMillis()));
                            expirationDay.add(Calendar.DATE, wifi.getDays());
                        } else {
                            expirationDay = null;
                        }

                        // wifi設定の確認
                        if (!WifiConnector.isEnable(getApplicationContext())) {
                            new AlertDialog.Builder(this)
                                    .setMessage(getString(R.string.wifi_dialog))
                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mWifiStateChange = true;
                                            WifiConnector.setEnable(MainActivity.this, true);
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setMessage(getString(R.string.wifi_dialog_done))
                                                    .setPositiveButton(getString(R.string.ok), null)
                                                    .show();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // 読み込み画面を非表示
                                            isScanning = false;
                                            closeScanPage();
                                        }
                                    })
                                    .show();
                        } else {
                            mWifiStateChange = false;
                            connectWifi(wifi, expirationDay);
                        }

                        mScanBackgroundConstraintLayout.setVisibility(View.VISIBLE);
                        mAvailableService = true;
                    }
                    // 読み込んだnfcがWifiHelperに未対応の場合
                    catch (JSONException e) {
                        e.printStackTrace();
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(getString(R.string.error_read_service_failed))
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        isScanning = false;
                                        closeScanPage();
                                    }
                                }).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // AP接続監視用のレシーバーを登録
        IntentFilter wifiConnectionIntentFilter = new IntentFilter();
        wifiConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectionBroadcastReceiver, wifiConnectionIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCoronaNfc.disableForegroundDispatch(MainActivity.this);

        // レシーバーを解除
        unregisterReceiver(mWifiConnectionBroadcastReceiver);
    }

    public void onStartScanButtonClicked(View view) {
        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要（wifi接続時）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // 許可を求めるダイアログを表示
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_CODE
                );

        } else {
            if (!isScanning) {

                // nfc読み込み待機
                mCoronaNfc.enableForegroundDispatch(MainActivity.this);
                isScanning = true;
                openScanPage();

                // 60秒後に自動で閉じる
                mScanDialogAutoCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isScanning) {
                            cancelScan();
                        }
                    }
                }, 60000);
            }
        }
    }

    public void onCancelScanButtonClicked(View view) {
        if(isScanning) {
            cancelScan();

            // 60秒後に閉じる予約をキャンセル
            mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
        }
    }

    private void cancelScan() {
        // nfc読み込み待機を解除
        mCoronaNfc.disableForegroundDispatch(MainActivity.this);
        isScanning = false;
        closeScanPage();
    }

    private void connectWifi(Wifi wifi, Calendar expirationDay) {

        // 読み込み画面を非表示（背景は残す）
        closeScanDialog();

        // wifi接続中を示すプログレスバーの表示
        mConnectingProgressConstraintLayout.setVisibility(View.VISIBLE);

        // Wifi設定
        WifiConnector wifiConnector = new WifiConnector(
                MainActivity.this,
                wifi.getSsid(),
                wifi.getPass(),
                wifi.getKind(),
                expirationDay
        );

        // Wifi接続
        mConnectedAp = wifiConnector.tryConnect();
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
                cancelScan();
            } else {
                finish();
            }
        }
        return false;
    }

    @Override
    public void getWifiConnectionState(NetworkInfo.State state) {
        if(isScanning && mAvailableService && !mWifiStateChange && state == CONNECTED ) {
            isScanning = false;

            // wifi接続中を示すプログレスバーの非表示
            mConnectingProgressConstraintLayout.setVisibility(View.GONE);

            // 読み込み画面を非表示
            closeScanBackground();

            // 表示メッセージの作成
            String dialogMessage;
            if(mConnectedAp || (!mConnectedAp && mWifiStateChange))
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {

            // パーミッションを許可しない場合
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.grant_permission, Toast.LENGTH_LONG).show();
            }
        }
    }

    // 読み込み画面を非表示
    private void closeScanPage() {
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanBackgroundConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を非表示（背景は残す）
    private void closeScanDialog() {
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を非表示（背景）
    private void closeScanBackground() {
        mScanBackgroundConstraintLayout.setVisibility(View.GONE);
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を表示
    private void openScanPage() {
        mScanDialogConstraintLayout.setVisibility(View.VISIBLE);
        mScanBackgroundConstraintLayout.setVisibility(View.VISIBLE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom));
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slowly));
    }
}

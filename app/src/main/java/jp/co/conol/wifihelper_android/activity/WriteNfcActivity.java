package jp.co.conol.wifihelper_android.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

import jp.co.conol.wifihelper_admin_lib.corona.CoronaNfc;
import jp.co.conol.wifihelper_admin_lib.corona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.wifi_connector.WifiConnector;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;
import jp.co.conol.wifihelper_android.R;

public class WriteNfcActivity extends AppCompatActivity {

    private CoronaNfc mCoronaNfc;
    private boolean isWriting = false;
    private boolean isConnecting = false;
    private EditText mSsidText;
    private EditText mPasswordText;
    private EditText mExpireText;
    private Button mWriteButton;
    private Button mConnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_nfc);

        mSsidText      = (EditText) findViewById(R.id.ssidText);
        mPasswordText  = (EditText) findViewById(R.id.passwordText);
        mExpireText    = (EditText) findViewById(R.id.expireText);
        mWriteButton   = (Button) findViewById(R.id.writeButton);
        mConnectButton = (Button) findViewById(R.id.connectButton);

//        String ssid = "conolAir";
//        String pass = "RaePh2oh";
        String ssid = "pr500m-98b038-1";
        String pass = "21425a9fb852b";

        // EditTextに文字セット
        mSsidText.setText(ssid);
        mPasswordText.setText(pass);
        mExpireText.setText("10");

        try {
            mCoronaNfc = new CoronaNfc(this);
        } catch (NFCNotAvailableException e) {
            Log.d("CoronaNfc", e.toString());
            finish();
        }

        // nfcがオフの場合はダイアログを表示
        if(!mCoronaNfc.isEnable()) {
            new AlertDialog.Builder(this)
                    .setTitle("NFC設定")
                    .setMessage("NFCがオフになっています\nNFCをオンに設定してください")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        // 書き込み
        if(isWriting) {
            String ssid = String.valueOf(mSsidText.getText());
            String pass = String.valueOf(mPasswordText.getText());
            int expireDate = Integer.parseInt(mExpireText.getText().toString());
            CNFCTag tag = mCoronaNfc.getWriteTagFromIntent(intent);

            if (tag != null) {

                // nfcに書き込むjson
                String serviceIdString = WifiHelper.createJson(ssid, pass, 1, expireDate);
//                String serviceIdString = "";
                byte[] serviceId = serviceIdString.getBytes(StandardCharsets.UTF_8);

                boolean success = false;
                try {
                    tag.writeServiceID(serviceId);
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (success) {
                    Toast.makeText  (this, "SUCCESS！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "FAILED！", Toast.LENGTH_SHORT).show();
                }

                mWriteButton.setText("WRITE");
                isWriting = false;
            }
        }
        // 読み込み（wifi接続）
        else if(isConnecting) {
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
                Toast.makeText(this, "deviceId=" + chipId + "\njson=" + serviceId, Toast.LENGTH_LONG).show();

                try {
                    wifi = WifiHelper.parseJsonToObj(serviceId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            final Wifi readWifi = wifi;

            // 接続期限日時のインスタンスを作成
            final Calendar calendarExpire  = Calendar.getInstance();

            // 接続期限日時の算出
            calendarExpire.setTime(new Date(System.currentTimeMillis()));
            calendarExpire.add(Calendar.DATE, wifi.getDays());

            if(!WifiConnector.isEnable(getApplicationContext())) {
                new AlertDialog.Builder(this)
                        .setTitle("Wi-Fi設定")
                        .setMessage("Wi-Fiがオフになっています\nWi-Fiをオンにしてもよろしいですか？")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // wifiをオンにする
                                WifiConnector.setEnable(getApplicationContext(), true);

                                // Wifi設定
                                WifiConnector wifiConnector = new WifiConnector(
                                        WriteNfcActivity.this,
                                        readWifi.getSsid(),
                                        readWifi.getPass(),
                                        WifiConnector.WPA_WPA2PSK,
                                        calendarExpire
                                );

                                // Wifi接続
                                wifiConnector.tryConnect();

                                mConnectButton.setText("CONNECT");
                                isConnecting = false;
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {

                // Wifi設定
                WifiConnector wifiConnector = new WifiConnector(
                        WriteNfcActivity.this,
                        wifi.getSsid(),
                        wifi.getPass(),
                        WifiConnector.WPA_WPA2PSK,
                        calendarExpire
                );

                // Wifi接続
                wifiConnector.tryConnect();

                mConnectButton.setText("CONNECT");
                isConnecting = false;
            }
        }
    }

    public void onWriteButtonClicked(View view) {
        if (isWriting) {
            // キャンセル
            mWriteButton.setText("WRITE");
            isWriting = false;
        } else {
            // 書き込み
            mWriteButton.setText("CANCEL");
            isWriting = true;
        }
    }

    public void onConnectButtonClicked(View view) {
        if (isConnecting) {
            // キャンセル
            mConnectButton.setText("CONNECT");
            isConnecting = false;
        } else {
            // 書き込み
            mConnectButton.setText("CANCEL");
            isConnecting = true;
        }
    }
}

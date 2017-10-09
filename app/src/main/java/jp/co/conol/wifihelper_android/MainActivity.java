package jp.co.conol.wifihelper_android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReader;
import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona_writer.CNFCDetector;
import jp.co.conol.wifihelper_admin_lib.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.wifi_connect.WifiConnect;
import jp.co.conol.wifihelper_admin_lib.wifi_connect.receiver.WifiExpiredBroadcastReceiver;

public class MainActivity extends AppCompatActivity {

    private CNFCDetector mCnfcDetector;
    private CNFCReader mCnfcReader;
    private boolean isWriting;
    private boolean isConnectiong;
    private EditText mSsidText;
    private EditText mPasswordText;
    private EditText mExpireText;
    private Button mWriteButton;
    private Button mConnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            mCnfcDetector = new CNFCDetector(this);
            mCnfcReader = new CNFCReader(this);
        } catch (jp.co.conol.wifihelper_admin_lib.corona_writer.NFCNotAvailableException e) {
            Toast.makeText(this, "NFC is not available (writer)", Toast.LENGTH_LONG).show();
            finish();
            return;
        } catch (jp.co.conol.wifihelper_admin_lib.corona_reader.NFCNotAvailableException e) {
            Toast.makeText(this, "NFC is not available (reader)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        String ssid = String.valueOf(mSsidText.getText());
        String pass = String.valueOf(mPasswordText.getText());

        // 書き込み
        if(isWriting) {
            CNFCTag tag = mCnfcDetector.getTagFromIntent(intent);
            if (tag != null) {
                String serviceIdString = ssid;
                byte[] serviceId = serviceIdString.getBytes(StandardCharsets.UTF_8);

                boolean success = false;
                try {
                    tag.writeServiceID(serviceId);
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (success) {
                    Toast.makeText(this, "SUCCESS！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "FAILED！", Toast.LENGTH_SHORT).show();
                }

                isWriting = false;
            }
        }
        // 読み込み（wifi接続）
        else if(isConnectiong) {
            CNFCReaderTag tag = null;
            try {
                tag = mCnfcReader.getTagFromIntent(intent);
            } catch (CNFCReaderException e) {
                Log.d("CNFCReader", e.toString());
                return;
            }
            if (tag != null) {
                String chipId = tag.getChipIdString();
                String serviceId = tag.getServiceIdString();
                Toast.makeText(this, "chipId=" + chipId + "\nserviceId=" + serviceId, Toast.LENGTH_SHORT).show();
            } else {
//                super.onNewIntent(intent);
            }

            // 接続期限日時のインスタンスを作成
            final Calendar calendarExpire = Calendar.getInstance();

            // 接続期限日時の算出
            calendarExpire.setTime(new Date(System.currentTimeMillis()));
            calendarExpire.add(Calendar.SECOND, Integer.parseInt(mExpireText.getText().toString()));

            // Wifiをオン
            if(!WifiConnect.isEnable(getApplicationContext()))
                WifiConnect.setEnable(getApplicationContext(), true);

            // Wifi設定
            WifiConnect wifiConnect = new WifiConnect(
                    this,
                    ssid,
                    pass,
                    WifiConnect.WPA_WPA2PSK,
                    calendarExpire
            );

            // Wifi接続
            wifiConnect.tryConnect();
        }
    }

    public void onWriteButtonClicked(View view) {
        if (isWriting) {
            // キャンセル
            mCnfcDetector.disableForegroundDispatch(this);
            mWriteButton.setText("WRITE");
            isWriting = false;
        } else {
            // 書き込み
            mCnfcDetector.enableForegroundDispatch(this);
            mWriteButton.setText("CANCEL");
            isWriting = true;
        }
    }

    public void onConnectButtonClicked(View view) {
        if (isConnectiong) {
            // キャンセル
            mCnfcReader.disableForegroundDispatch(this);
            mConnectButton.setText("CONNECT");
            isConnectiong = false;
        } else {
            // 書き込み
            mCnfcReader.enableForegroundDispatch(this);
            mConnectButton.setText("CANCEL");
            isConnectiong = true;
        }
    }
}

package jp.co.conol.wifihelper_android;

import android.content.Intent;
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

import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReader;
import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona_reader.NFCToReadNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.corona_writer.CNFCDetector;
import jp.co.conol.wifihelper_admin_lib.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.corona_writer.NFCToWriteNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.wifi_connect.WifiConnect;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;

public class MainActivity extends AppCompatActivity {

    private CNFCDetector mCnfcDetector;
    private CNFCReader mCnfcReader;
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
        setContentView(R.layout.activity_main);

        mSsidText      = (EditText) findViewById(R.id.ssidText);
        mPasswordText  = (EditText) findViewById(R.id.passwordText);
        mExpireText    = (EditText) findViewById(R.id.expireText);
        mWriteButton   = (Button) findViewById(R.id.writeButton);
        mConnectButton = (Button) findViewById(R.id.connectButton);

        String ssid = "conolAir";
        String pass = "RaePh2oh";
//        String ssid = "pr500m-98b038-1";
//        String pass = "21425a9fb852b";

        // EditTextに文字セット
        mSsidText.setText(ssid);
        mPasswordText.setText(pass);
        mExpireText.setText("10");

        try {
            mCnfcDetector = new CNFCDetector(this);
            mCnfcReader = new CNFCReader(this);
        } catch (NFCToWriteNotAvailableException e) {
            Log.d("CNFCWriter", e.toString());
            finish();
        } catch (NFCToReadNotAvailableException e) {
            Log.d("CNFCReader", e.toString());
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCnfcDetector.enableForegroundDispatch(this);
        mCnfcReader.enableForegroundDispatch(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCnfcDetector.disableForegroundDispatch(this);
        mCnfcReader.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        // 書き込み
        if(isWriting) {
            String ssid = String.valueOf(mSsidText.getText());
            String pass = String.valueOf(mPasswordText.getText());
            int expireDate = Integer.parseInt(mExpireText.getText().toString());
            CNFCTag tag = mCnfcDetector.getTagFromIntent(intent);

            if (tag != null) {

                // nfcに書き込むjson
                String serviceIdString = WifiHelper.createJson(ssid, pass, 1, expireDate);
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
                tag = mCnfcReader.getTagFromIntent(intent);
            } catch (CNFCReaderException e) {
                Log.d("CNFCReader", e.toString());
                return;
            }
            if (tag != null) {
                String chipId = tag.getChipIdString();
                String serviceId = tag.getServiceIdString();
                Toast.makeText(this, "chipId=" + chipId + "\njson=" + serviceId, Toast.LENGTH_LONG).show();

                try {
                    wifi = WifiHelper.parseJsonToObj(serviceId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 接続期限日時のインスタンスを作成
            Calendar calendarExpire;

            // 接続期限日時の算出
            calendarExpire = Calendar.getInstance();
            calendarExpire.setTime(new Date(System.currentTimeMillis()));
            calendarExpire.add(Calendar.DATE, wifi.getDays());

            // Wifiをオン
            if(!WifiConnect.isEnable(getApplicationContext()))
                WifiConnect.setEnable(getApplicationContext(), true);

            // Wifi設定
            WifiConnect wifiConnect = new WifiConnect(
                    this,
                    wifi.getSsid(),
                    wifi.getPass(),
                    WifiConnect.WPA_WPA2PSK,
                    calendarExpire
            );

            // Wifi接続
            wifiConnect.tryConnect();

            mConnectButton.setText("CONNECT");
            isConnecting = false;
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

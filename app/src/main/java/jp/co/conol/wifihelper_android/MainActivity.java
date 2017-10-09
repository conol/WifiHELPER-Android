package jp.co.conol.wifihelper_android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import jp.co.conol.wifihelper_admin_lib.wifi_connect.WifiConnect;
import jp.co.conol.wifihelper_android.receiver.WifiExpiredBroadcastReceiver;

public class MainActivity extends AppCompatActivity {

    private TextView mSsidText;
    private TextView mPasswordText;
    private TextView mExpireText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSsidText     = (TextView) findViewById(R.id.ssidText);
        mPasswordText = (TextView) findViewById(R.id.passwordText);
        mExpireText   = (TextView) findViewById(R.id.expireText);

//        String ssid = "conolAir";
//        String pass = "RaePh2oh";
        String ssid = "pr500m-98b038-1";
        String pass = "21425a9fb852b";

        // EditTextに文字セット
        mSsidText.setText(ssid);
        mPasswordText.setText(pass);
        mExpireText.setText("10");
    }

    public void onConnectButtonClicked(View view) {

        String ssid = String.valueOf(mSsidText.getText());
        String pass = String.valueOf(mPasswordText.getText());

        // Wifiをオン
        if(!WifiConnect.isEnable(getApplicationContext()))
            WifiConnect.setEnable(getApplicationContext(), true);

        // Wifi設定
        WifiConnect wifiConnect = new WifiConnect(
                this,
                ssid,
                pass,
                WifiConnect.WPA_WPA2PSK
        );

        // Wifi接続
        wifiConnect.tryConnect();

        // タップ時の日時を取得
        final Date tappedDateTime = new Date(System.currentTimeMillis());

        // 接続期限日時のインスタンスを作成
        final Calendar calendarExpire = Calendar.getInstance();

        // 接続期限日時の算出
        calendarExpire.setTime(tappedDateTime);
        calendarExpire.add(Calendar.SECOND, Integer.parseInt(mExpireText.getText().toString()));

        // 有効期限の日時とSSIDを保存
        SharedPreferences pref = getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong("expireDateTime", Util.Transform.calendarTodate(calendarExpire).getTime());
        editor.putString("ssid", ssid);
        editor.apply();

        // アラームを受信するレシーバーを作成
        Intent alarmIntent = new Intent(getApplicationContext(), WifiExpiredBroadcastReceiver.class);
        alarmIntent.putExtra("ssid", ssid);
        PendingIntent pending = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // アラームをセットする
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendarExpire.getTimeInMillis(), pending);
    }
}

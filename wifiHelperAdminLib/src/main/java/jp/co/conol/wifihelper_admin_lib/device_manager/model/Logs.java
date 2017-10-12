package jp.co.conol.wifihelper_admin_lib.device_manager.model;

/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class Logs {

    private Log[] device_logs;

    private class Log {
        private String device_id;
        private String used_at;
        private String shop;
        private String lat_lng;
        private String notes;
    }
}

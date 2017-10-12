package jp.co.conol.wifihelper_android.model;

/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class Device {

    private int id;
    private String device_id;
    private String name;
    private String status;
    private Service[] services;
    private String created_at;
    private String updated_at;

    private class Service {
        private int id;
        private String service_key;
        private String name;
    }
}

package com.example.sduapplication.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.sduapplication.R;
import com.example.sduapplication.item.Item;

import org.json.JSONObject;
import org.jsoup.Jsoup;

public class MyService extends Service {

    private ServiceBinder binder=new ServiceBinder();//活动和服务的通信桥梁
    private MyReceiver receiver=new MyReceiver();
    private boolean going=true;
    private int index=0;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //动态注册内部广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("stop");
        registerReceiver(receiver,filter);

    }

    public class ServiceBinder extends Binder{

        public void begin(final Item item){
            SharedPreferences preferences=getSharedPreferences("setting",0);
            SharedPreferences.Editor editor=preferences.edit();
            editor.putBoolean("foreground",true);
            editor.commit();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (going){
                        try {
                            String body = Jsoup.connect("http://bkjwxk.sdu.edu.cn/b/xk/xs/add/#/#".replaceFirst("#",item.getClass1()).replaceFirst("#",item.getClass2()))
                                    .ignoreContentType(true)
                                    .cookies(item.getCookies())
                                    .execute().body();
                            JSONObject object=new JSONObject(body);
                            String result=object.get("result").toString();
                            String reason=object.getString("msg");
                            if (reason.contains("选课成功")){
                                going=false;
                                notificationSuccess();
                                return;
                            }
                            index++;
                            notification();
                            Thread.sleep(item.getTime());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case "stop":
                    going=false;
                    Log.v("tag", String.valueOf(index));
                    destory();
                    break;
            }
        }
    }

    public void notification(){
        Intent intent=new Intent("stop");
        PendingIntent pendingIntent=PendingIntent.getBroadcast(MyService.this,0,intent,0);
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.service_fore);
        remoteViews.setTextViewText(R.id.tv,"抢课："+index+"次");
        remoteViews.setOnClickPendingIntent(R.id.btnStop,pendingIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setTicker("正在抢课")
                .setOngoing(true)
                .setSmallIcon(R.drawable.school)
                .setDefaults(Notification.FLAG_LOCAL_ONLY);
        Notification notification = builder.build();
        notification.contentView = remoteViews;
        startForeground(1,notification);
    }

    public void notificationSuccess(){
        Intent intent=new Intent("stop");
        PendingIntent pendingIntent=PendingIntent.getBroadcast(MyService.this,0,intent,0);
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.service_fore);
        remoteViews.setTextViewText(R.id.tv,"恭喜你抢课成功啦");
        remoteViews.setOnClickPendingIntent(R.id.btnStop,pendingIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setTicker("正在抢课")
                .setOngoing(true)
                .setSmallIcon(R.drawable.school)
                .setDefaults(Notification.FLAG_LOCAL_ONLY);
        Notification notification = builder.build();
        notification.contentView = remoteViews;
        startForeground(1,notification);
    }



    public void destory(){
        unregisterReceiver(receiver);
        stopForeground(true);

        SharedPreferences preferences=getSharedPreferences("setting",0);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putBoolean("foreground",false);
        editor.commit();

        super.onDestroy();
    }
}

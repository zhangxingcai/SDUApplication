package com.example.sduapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sduapplication.item.Item;
import com.example.sduapplication.service.MyService;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private LinearLayout ll;
    private Button btnGo,btnChange;
    private EditText etClass1,etClass2,etTime;
    private Map<String, String> cookies = null;
    private TextView tvState,tvReason,tvIndex;
    private Handler handler=new Handler();
    private final int DEFAULT=1;
    private final int GOING=2;
    private int state=DEFAULT;//当前状态
    private int time=0;//抢课周期
    private int index=0;//抢课次数
    private String class1,class2;

    private boolean bind=false;
    //与后台服务进行通信
    private MyService.ServiceBinder binder= null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MyService.ServiceBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String id=getIntent().getStringExtra("id");
        String password=getIntent().getStringExtra("password");
        login(id,password);

        init();

        SharedPreferences preferences=getSharedPreferences("setting",0);
        boolean fore=preferences.getBoolean("foreground",false);
        if (fore){
            Toast.makeText(this,"请先停止后台的抢课任务",Toast.LENGTH_SHORT).show();
        }

        //绑定服务
        Intent bindIntent = new Intent(this,MyService.class);
        startService(bindIntent);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);
        bind=true;
    }

    private void init() {
        setTitle("抢课");
        etClass1= (EditText) findViewById(R.id.etClass1);
        etClass2= (EditText) findViewById(R.id.etClass2);
        etTime= (EditText) findViewById(R.id.etTime);
        btnGo= (Button) findViewById(R.id.btnGo);
        btnGo.setOnClickListener(this);
        btnChange= (Button) findViewById(R.id.btnChange);
        btnChange.setOnClickListener(this);
        tvState= (TextView) findViewById(R.id.tvState);
        tvReason= (TextView) findViewById(R.id.tvReason);
        tvIndex= (TextView) findViewById(R.id.tvIndex);
        ll= (LinearLayout) findViewById(R.id.ll);
        ll.setVisibility(View.INVISIBLE);
    }

    public void login(final String id,final String password){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection.Response response = Jsoup.connect("http://bkjwxk.sdu.edu.cn/b/ajaxLogin")
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .data("j_username",id)
                            .data("j_password",password)
                            .execute();
                    cookies = response.cookies();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }).start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnGo:
                if (state==GOING){
                    stop();
                    return;
                }
                state=GOING;
                btnGo.setText("停止");
                class1=etClass1.getText().toString();
                class2=etClass2.getText().toString();
                String etime=etTime.getText().toString();
                if (class1.length()>0&&class2.length()>0&&etime.length()>0){
                    go(class1,class2);
                    time= Integer.parseInt(etime);
                } else {
                    Toast.makeText(MainActivity.this,"不能为空",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnChange:
                state=DEFAULT;
                SharedPreferences pre=getSharedPreferences("setting",0);
                SharedPreferences.Editor edit=pre.edit();
                edit.putString("id","");
                edit.putString("password","");
                edit.commit();
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                finish();
        }
    }

    private void stop() {
        state=DEFAULT;
        handler.post(new Runnable() {
            @Override
            public void run() {
                btnGo.setText("开始抢课！");
                ll.setVisibility(View.INVISIBLE);
                index=0;
            }
        });
    }

    private void go(final String class1, final String class2) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (state==GOING){
                    index++;
                    try {
                        String body = Jsoup.connect("http://bkjwxk.sdu.edu.cn/b/xk/xs/add/#/#".replaceFirst("#",class1).replaceFirst("#",class2))
                                .ignoreContentType(true)
                                .cookies(cookies)
                                .execute().body();
                        JSONObject object=new JSONObject(body);
                        final String result=object.get("result").toString();
                        final String reason=object.getString("msg").toString();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (index==1){
                                    ll.setVisibility(View.VISIBLE);
                                }
                                if (!reason.contains("选课成功")){
                                    tvState.setText("上一次抢课状态：抢课失败");
                                    tvReason.setText("失败原因:"+reason);
                                } else {
                                    success();
                                    Log.v("TAG",result+reason);
                                }
                                tvIndex.setText("抢课次数："+index+"次");
                            }
                        });
                        Thread.sleep(time);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void success() {
        stop();
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("抢课成功");
                builder.setMessage("恭喜你！抢课成功啦");
                builder.setPositiveButton("确定",null);
                builder.create().show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("退出")
                .setMessage("是否允许抢课程序后台运行")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        binder.begin(new Item(class1,class2,time,cookies));
                        MainActivity.super.onBackPressed();
                    }
                }).setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.super.onBackPressed();
            }
        });
        if (class1!=null&&class2!=null&&time>0&&cookies!=null&&state==GOING){
            builder.create().show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bind){
            unbindService(connection);
            bind=false;
        }
    }
}

package com.example.sduapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.Map;
import java.util.concurrent.FutureTask;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText etId,etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("登录");
        etId= (EditText) findViewById(R.id.etId);
        etPassword= (EditText) findViewById(R.id.etPassword);
        btnLogin= (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);

        SharedPreferences preferences=getSharedPreferences("setting",0);
        String id=preferences.getString("id","");
        String passwprd=preferences.getString("password","");
        if (id.length()>0){
            login(id,passwprd);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnLogin:
                String id=etId.getText().toString();
                String password=etPassword.getText().toString();
                if (id.length()>0&&password.length()>0){
                    login(id,password);
                } else {
                    Toast.makeText(LoginActivity.this,"学号密码不能为空",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void login(final String id,final String password){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> cookies = null;
                try {
                    Connection.Response response = Jsoup.connect("http://bkjwxk.sdu.edu.cn/b/ajaxLogin")
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .data("j_username",id)
                            .data("j_password",password)
                            .execute();
                    if(!response.body().contains("success")){
                        Log.v("tag",id);
                        Log.v("tag",password);
                        //Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                    } else {
                        SharedPreferences preferences=getSharedPreferences("setting",0);
                        SharedPreferences.Editor editor=preferences.edit();
                        editor.putString("id",id);
                        editor.putString("password",password);
                        editor.commit();
                        Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                        intent.putExtra("id",id);
                        intent.putExtra("password",password);
                        startActivity(intent);
                        finish();
                    }
//            cookies = response.cookies();
//            String body = Jsoup.connect("http://bkjwxk.sdu.edu.cn/b/xk/xs/kcsearch")
//                    .method(Connection.Method.POST)
//                    .ignoreContentType(true)
//                    .cookies(cookies)
//                    .execute()
//                    .body();
//            Log.v("tag",body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

package com.xingyu.smartrefrigerator;

import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {


    private static final String DeviceID = "29705982";
    private static final String ApiKey = "FSRpoAy7c86eJbM1nzSCr4qhTfM=";



    //接收到的信息
    private String receivedInfo;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button Button_PostData = (Button)findViewById(R.id.Button_PostData);
        final EditText EditText_DataToPost = (EditText)findViewById(R.id.EditText_DataToPost);

        final TextView Text_InfoReceived = (TextView)findViewById(R.id.Text_InfoReceived);
        Button Button_GetInfo = (Button)findViewById(R.id.Button_GetInfo);


        Text_InfoReceived.setText("");


        //获取数据线程
        final Runnable getRunable = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                receivedInfo = null;
                receivedInfo = getInfo("Temperature");

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Text_InfoReceived.append(receivedInfo + "\n\n" );
                    }
                });
                Looper.loop();
            }
        };

        //获取数据
        Button_GetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(getRunable).start();
            }
        });



        //上传数据线程
        final Runnable postRunable = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                int DataToPost = Integer.parseInt(EditText_DataToPost.getText().toString());
                postData("Temperature",DataToPost);
                Looper.loop();
            }
        };
        //上传数据
        Button_PostData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(EditText_DataToPost.getText())) {
                    Toast.makeText(MainActivity.this, "请输入要上传的数据！", Toast.LENGTH_SHORT).show();
                }else{
                    new Thread(postRunable).start();
                }

            }
        });



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    //从云服务器获取数据
    public String getInfo(String dataStream){
//        Toast.makeText(MainActivity.this, "开始从云服务器获取数据", Toast.LENGTH_SHORT).show();    //提示
        String response = null;
        try{
            URL url = new URL("http://api.heclouds.com/devices/" + DeviceID + "/datastreams/" + dataStream);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(15*1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("api-key",ApiKey);
            if (connection.getResponseCode() == 200){   //返回码是200，网络正常
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int len = 0;
                byte buffer[] = new byte[1024];
                while((len = inputStream.read(buffer))!=-1){
                    os.write(buffer,0,len);
                }
                inputStream.close();
                os.close();
                response = os.toString();
            }else{
                //返回码不是200，网络异常
                Toast.makeText(MainActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
            }

        }catch (IOException e){
            Toast.makeText(MainActivity.this, "获取数据失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return response;

    }


    //向云服务器上传数据
    public void postData(String dataStream,int dataToPost){
        Toast.makeText(MainActivity.this, "开始上传", Toast.LENGTH_SHORT).show();   //提示
        String dataNew = new String(",;"+dataStream+","+dataToPost);
        String response = null;
        byte[] data = dataNew.getBytes();
        try{
            URL url = new URL("http://api.heclouds.com/devices/"+DeviceID+"/datapoints?type=5");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(15*1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("api-key",ApiKey);
            connection.setRequestProperty("Content-Length",String.valueOf(data.length));
            connection.setChunkedStreamingMode(5);
            connection.connect();
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
            if(connection.getResponseCode()==200){
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int len = 0;
                byte buffer[] = new byte[1024];
                while ((len = inputStream.read(buffer))!=-1){
                    os.write(buffer,0,len);
                }
                inputStream.close();
                os.close();
                response = os.toString();   //正常则返回{"errno":0,"error":"succ"}，此函数为void，用不上这个
            }
            Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

}

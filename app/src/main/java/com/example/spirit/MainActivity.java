package com.example.spirit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
//import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextToSpeech.OnInitListener {
    public static final int OPEN_FILE = 100;
    private TextToSpeech textToSpeech; // TTS对象
    /*自己选择路径*/
    private EditText filePath;
    /*输入的路径对应的url*/
    private String fileUrl;
    /*自己选择路径*/
    private Button choosePath;
    /*打开文件按钮*/
    private Button openFile;
    /*开始按钮*/
    private Button startSpeech;
    /*暂停按钮*/
    private Button suspendSpeech;
    /*重新开始按钮*/
    private Button reStartSpeech;
    /*要听写的词语数组*/
    private String[] speechList;
    /*已经听写的词语数量*/
    private AtomicInteger speechNum;
    /*正在听写的词语*/
    private String speechContent;

    /*读取的时间间隔*/
    private int intervalValue;
    /*重复次数*/
    private int frequencyValue;

    private static ReentrantLock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    private MyThread myThread = new MyThread(false);
    /*选中的uri*/
    private Uri uri;
    private static String[] mimeTypes = {"application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",//docx
            "application/vnd.ms-excel application/x-excel",//xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",//xlsx
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化全局变量
        filePath = findViewById(R.id.file_path);
        choosePath = findViewById(R.id.choose_path);
        openFile = findViewById(R.id.open_file);
        startSpeech = findViewById(R.id.btn_start_speech);
        reStartSpeech = findViewById(R.id.btn_stop_speech);
        suspendSpeech = findViewById(R.id.btn_suspend_speech);
        //注册监听器
        choosePath.setOnClickListener(this);
        openFile.setOnClickListener(this);
        startSpeech.setOnClickListener(this);
        reStartSpeech.setOnClickListener(this);
        suspendSpeech.setOnClickListener(this);
        textToSpeech = new TextToSpeech(this, this); // 参数Context,TextToSpeech.OnInitListener

    }


    /**
     * 用来初始化TextToSpeech引擎
     * status:SUCCESS或ERROR这2个值
     * setLanguage设置语言，帮助文档里面写了有22种
     * TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失。
     * TextToSpeech.LANG_NOT_SUPPORTED:不支持
     */
    @Override
    public void onInit(int status) {
        //判断tts回调是否成功
        if (status == TextToSpeech.SUCCESS) {
            int result1 = textToSpeech.setLanguage(Locale.US);
            int result2 = textToSpeech.setLanguage(Locale.CHINESE);
            if (result1 == TextToSpeech.LANG_MISSING_DATA || result1 == TextToSpeech.LANG_NOT_SUPPORTED
                    || result2 == TextToSpeech.LANG_MISSING_DATA || result2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_path://"选择路径"
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                if (mimeTypes != null) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                }
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_FILE);
                break;
            case R.id.open_file:
                /*if (isValid(fileUrl, "请您先选择文件路径，单击上方“选择路径”按钮哦")) {
                    readWord(fileUrl);
                }*/
                try {
                 InputStream inputStream = getContentResolver().openInputStream(uri);

                    // this dynamically extends to take the bytes you read
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

                    // this is storage overwritten on each iteration with bytes
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];

                    // we need to know how may bytes were read to write them to the byteBuffer
                    int len = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                        Log.i("welhzh_f", "" + len);
                    }
                    // and then we can return your byte array.
                    alert(byteBuffer.toByteArray().toString());
                    alert(byteBuffer.toString("GBK"));
                }catch (Exception e){
                    alert(e.getMessage());
                }
                //Log.d("zhsy","read=="+read+"byte="+"text=="+result);

               /* // and then we can return your byte array.
                try {
                    String result = new String(byteBuffer.toByteArray(), "GB2312");
                    alert(byteBuffer.size()+"");
                    alert(result);
                } catch (UnsupportedEncodingException e) {
                    alert(e.getMessage());
                    e.printStackTrace();
                }*/
                break;
            case R.id.btn_start_speech://"开始"
                myThread.start();
                startSpeech.setEnabled(false);
                break;
            case R.id.btn_suspend_speech://"暂停"
                final String status = (String) v.getTag();
                if ("1".equals(status)) {
                    myThread.continu();
                    suspendSpeech.setText("暂停");
                    v.setTag("0"); //pause
                } else {
                    myThread.pause();
                    suspendSpeech.setText("继续");
                    v.setTag("1");
                }
                break;
            case R.id.btn_stop_speech://"停止"
                myThread.stopMe();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    //读取word文件内容,返回数组，支持docx
    public void readWord(String file) {
        alert(file);

        //创建输入流用来<a title="读取doc文件" href="http://www.android-study.com/pingtaikaifa/38.html">读取doc文件</a>
        FileInputStream in = null;
        try {
            File fs = new File(file);
            alert(file);
            if (!fs.exists()) {
                try {
                    fs.createNewFile();
                } catch (IOException e) {
                    alert("创建时，异常喽" + e.getMessage());
                }
            }
            in = new FileInputStream(fs);
            alert("" + 217);
            XWPFDocument xdoc = new XWPFDocument(in);
            alert("" + 219);
            XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
            alert("" + 221);
            String doc1 = extractor.getText();
            alert(doc1);
            speechList = doc1.split("，");
            alert(speechList.toString());
            System.out.println(doc1);
        } catch (FileNotFoundException e) {
            alert(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            alert("异常喽！" + e.getMessage());
            e.printStackTrace();
        } finally {
            //关闭输入流
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*接收到刚才选择的文件路径*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == OPEN_FILE) {
                uri = data.getData();
                String path = PathUtils.getPath(this, uri);
                filePath.setText(path);
                fileUrl = filePath.getText().toString();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*校验参数是否符合，符合返回true，不符合返回false*/
    private boolean isValid(Object params, String message) {
        if (params == null) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("温馨提示")
                    .setMessage(message)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            System.out.println("点了确定");
                        }
                    }).create();
            alertDialog.show();
            return false;
        }
        return true;
    }

    /*校验参数是否符合，符合返回true，不符合返回false*/
    private void alert(String params) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("温馨提示")
                .setMessage(params)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.out.println("点了确定");
                    }
                }).create();
        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    class MyThread extends Thread {
        private boolean pauseFlag;

        public MyThread(boolean flag) {
            this.pauseFlag = flag;
        }

        public void run() {
            synchronized (myThread) {
                try {
                    Integer interval = Integer.valueOf(((EditText) findViewById(R.id.intreval_value)).getText().toString());
                    intervalValue = interval == null ? 2 : interval.intValue();
                    Integer frequence = Integer.valueOf(((EditText) findViewById(R.id.frequency_value)).getText().toString());
                    frequencyValue = frequence == null ? 2 : frequence.intValue();
                    speechList = new String[]{"你好", "寂寞", "同情", "北京"};
                    if (isValid(speechList, "莫着急，请先导入词语哦，哈哈哈哈")) {
                        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
                            // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
                            textToSpeech.setPitch(1.0f);
                            //设定语速 ，默认1.0正常语速
                            textToSpeech.setSpeechRate(1.0f);
                            for (int i = 0; i < speechList.length; i++) {
                                speechNum = new AtomicInteger(i);//目前读取的位置
                                for (int j = 0; j < frequencyValue; j++) {//每个词语读取次数
                                    while (pauseFlag) {
                                        myThread.wait();
                                    }
                                    //朗读，注意这里三个参数的added in API level 4   四个参数的added in API level 21
                                    textToSpeech.speak(speechList[i], TextToSpeech.QUEUE_FLUSH, null);
                                    //停顿intervalValue秒
                                    try {
                                        Thread.sleep(intervalValue * 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        alert("已经停止");
                    }
                }
            }
        }

        public void pause() {
            pauseFlag = true;
        }

        public void continu() {
            if (getState() == State.WAITING) {
                myThread.notify();
            }
            pauseFlag = false;
        }

        /**
         * 停止线程运行。
         */
        public void stopMe() {
            if (myThread != null) {
                myThread.interrupt();
            }
        }
    }

    ;


}


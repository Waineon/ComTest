package com.bjw.ComAssistant;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bjw.bean.ComBean;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;

/**
 * Created by Lenovo on 2018/4/12.
 */

public class ComTestActivity extends Activity {

    private EditText et_res,show_lines;
    private Button btn_clear, btn_send, btn_stop;
    private CheckBox autoClear;
    private Spinner SpinnerCOM, spinnerBaudRate;
    private ToggleButton toggleButtonCOM;
    SerialControl Com;// 串口
    DispQueueThread DispQueue;//刷新显示线程
    SerialPortFinder mSerialPortFinder;//串口设备搜索
    int iRecLines = 0;//接收区行数

    /*扫码声音播放*/
    private SoundPool mSound;
    private HashMap<Integer, Integer> soundPoolMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com);
        Com = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        setUpView();
        InitSounds();
    }

    private void InitSounds() {
        // 第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        mSound = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(1, mSound.load(this, R.raw.beep, 1));
        //可以在后面继续put音效文件
    }
    private void playSound(int sound, int loop) {
        AudioManager mgr = (AudioManager) this
                .getSystemService(Context.AUDIO_SERVICE);
        // 获取系统声音的当前音量
        float currentVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        // 获取系统声音的最大音量
        float maxVolume = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取当前音量的百分比
        float volume = currentVolume / maxVolume;

        // 第一个参数是声效ID,第二个是左声道音量，第三个是右声道音量，第四个是流的优先级，最低为0，第五个是是否循环播放，第六个播放速度(1.0 =正常播放,范围0.5 - 2.0)
        mSound.play(soundPoolMap.get(sound), volume, volume, 1, loop, 2f);
    }

    private void setUpView() {
        et_res = (EditText) findViewById(R.id.et_res);
        show_lines = (EditText) findViewById(R.id.show_lines);

        /*按钮*/
        btn_clear = (Button) findViewById(R.id.btn_Clear);
        btn_send = (Button) findViewById(R.id.ButtonSend);
        btn_stop = (Button) findViewById(R.id.ButtonStop);
        btn_clear.setOnClickListener(new ButtonClickEvent());
        btn_send.setOnClickListener(new ButtonClickEvent());
        btn_stop.setOnClickListener(new ButtonClickEvent());

        autoClear = (CheckBox) findViewById(R.id.check_clear);

        /*端口开关*/
        toggleButtonCOM = (ToggleButton) findViewById(R.id.toggleButtonCOM);
        toggleButtonCOM.setOnCheckedChangeListener(new ToggleButtonCheckedChangeEvent());

        /*波特率选择*/
        spinnerBaudRate = (Spinner) findViewById(R.id.SpinnerBaudRateCOM);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.baudrates_value,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBaudRate.setAdapter(adapter);
        spinnerBaudRate.setSelection(0);
        spinnerBaudRate.setOnItemSelectedListener(new ItemSelectedEvent());

        /*----------------端口号----------------*/
        SpinnerCOM = (Spinner) findViewById(R.id.SpinnerCOM);
        mSerialPortFinder= new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        List<String> allDevices = new ArrayList<String>();
        for (int i = 0; i < entryValues.length; i++) {
            allDevices.add(entryValues[i]);
        }
        ArrayAdapter<String> aspnDevices = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, allDevices);
        aspnDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerCOM.setAdapter(aspnDevices);
        SpinnerCOM.setOnItemSelectedListener(new ItemSelectedEvent());
        /*-------------------------------------*/
    }

    //----------------------------------------------------清除按钮、发送按钮
    class ButtonClickEvent implements View.OnClickListener {
        public void onClick(View v)
        {
            if (v == btn_clear){
                et_res.setText("");
            } else if (v== btn_send){
//        TODO        startLoop();
//                SetiDelayTime(Com,"500");
//                SetLoopData(Com,"1B31");
//                SetAutoSend(Com,true);
                sendPortTextData(Com,"nls0006010;0302020;0313040=10;");
            }else if (v== btn_stop){
//         TODO       stopLoop();
//                SetAutoSend(Com,false);
//                sendPortHexData(Com,"1B30");
                sendPortTextData(Com,"nls0006010;0302000;");
            }
        }
    }
    //----------------------------------------------------串口发送
    private void sendPortTextData(SerialHelper ComPort,String sOut){
        if (ComPort!=null && ComPort.isOpen())
        {
            ComPort.sendTxt(sOut);
        }
    }

    //----------------------------------------------------串口发送
    private void sendPortHexData(SerialHelper ComPort,String sOut){
        if (ComPort!=null && ComPort.isOpen())
        {
            ComPort.sendHex(sOut);
        }
    }
    //----------------------------------------------------设置自动发送延时
    private void SetiDelayTime(SerialHelper ComPort,String sTime){
        ComPort.setiDelay(Integer.parseInt(sTime));
    }

    //----------------------------------------------------设置自动发送数据
    private void SetLoopData(SerialHelper ComPort,String sLoopData){
        if (ComPort!=null && ComPort.isOpen())
        {
            ComPort.setHexLoopData(sLoopData);
        }
    }

    //----------------------------------------------------设置自动发送模式开关
    private void SetAutoSend(SerialHelper ComPort,boolean isAutoSend){
        if (isAutoSend)
        {
            ComPort.startSend();
        } else
        {
            ComPort.stopSend();
        }
    }

    class ToggleButtonCheckedChangeEvent implements ToggleButton.OnCheckedChangeListener{
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            if (buttonView == toggleButtonCOM){
                if (isChecked){
//						Com=new SerialControl("/dev/s3c2410_serial0", "9600");
                        Com.setPort(SpinnerCOM.getSelectedItem().toString());
                        Com.setBaudRate(spinnerBaudRate.getSelectedItem().toString());
                        OpenComPort(Com);
                }else {
                    CloseComPort(Com);
                }
            }
        }
    }

    class ItemSelectedEvent implements Spinner.OnItemSelectedListener{
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            if ((arg0 == SpinnerCOM) || (arg0 == spinnerBaudRate))
            {
                CloseComPort(Com);
                toggleButtonCOM.setChecked(false);
            }
        }
        public void onNothingSelected(AdapterView<?> arg0)
        {}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CloseComPort(Com);
        setContentView(R.layout.activity_com);
        setUpView();
    }

    @Override
    public void onDestroy(){
        CloseComPort(Com);
        super.onDestroy();
    }

    //----------------------------------------------------关闭串口
    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //----------------------------------------------------打开串口
    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
            ShowMessage("打开串口失败:没有串口读/写权限!");
        } catch (IOException e) {
            ShowMessage("打开串口失败:未知错误!");
        } catch (InvalidParameterException e) {
            ShowMessage("打开串口失败:参数错误!");
        }
    }
    //------------------------------------------显示消息
    private void ShowMessage(String sMsg)
    {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }

    private class SerialControl extends SerialHelper {

        //		public SerialControl(String sPort, String sBaudRate){
//			super(sPort, sBaudRate);
//		}
        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            //数据接收量大或接收时弹出软键盘，界面会卡顿,可能和6410的显示性能有关
            //直接刷新显示，接收数据量大时，卡顿明显，但接收与显示同步。
            //用线程定时刷新显示可以获得较流畅的显示效果，但是接收数据速度快于显示速度时，显示会滞后。
            //最终效果差不多-_-，线程定时刷新稍好一些。
            DispQueue.AddQueue(ComRecData);//线程定时刷新显示(推荐)
            /*
			runOnUiThread(new Runnable()//直接刷新显示
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
        }
    }

    //----------------------------------------------------刷新显示线程
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
                        }
                    });
                    try {
                        Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

    private void DispRecData(ComBean ComRecData) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(ComRecData.sRecTime);
        sMsg.append("[");
        sMsg.append(ComRecData.sComPort);
        sMsg.append("]");
        String res = new String(ComRecData.bRec);
        sMsg.append(new String(ComRecData.bRec));
        Log.e("RES length:","length:" + res.length());
        if(res.length()>2){
            playSound(1,0);
//            startAlarm(this);
        }
        sMsg.append("\r\n");
        et_res.append(sMsg);
        iRecLines++;
        show_lines.setText(String.valueOf(iRecLines));
        if ((iRecLines > 500) && (autoClear.isChecked()))//达到500项自动清除
        {
            et_res.setText("");
            show_lines.setText("0");
            iRecLines = 0;
        }
    }

//    private static void startAlarm(Context context) {
//        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        if (notification == null) return;
//        Ringtone r = RingtoneManager.getRingtone(context, notification);
//        r.play();
//    }
}

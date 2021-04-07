package com.hzmct.can;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.x6.mc_cantest.CanFrame;
import com.example.x6.mc_cantest.CanUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CanActivity extends AppCompatActivity {
    private final static String TAG = "CanActivity";

    private Button btnClear;
    private Button btnOpen;
    private Button btnSend;
    private Spinner spinnerCan;
    private Spinner spinnerBaudRate;
    private TextView tvRecv;
    private EditText editSend;

    private CanUtils canUtil;
    private boolean openFlag = false;
    private String selectCan = "can0";
    private int selectBaudRate = 5000;
    private String recvStr = "";
    private AtomicBoolean recvAlive = new AtomicBoolean(true);
    private AtomicInteger frameCount = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can);

        btnClear = findViewById(R.id.btn_clear);
        btnSend = findViewById(R.id.btn_send);
        btnOpen = findViewById(R.id.btn_open);
        spinnerCan = findViewById(R.id.spinner_can);
        spinnerBaudRate = findViewById(R.id.spinner_baudRate);
        tvRecv = findViewById(R.id.tv_recv);
        editSend = findViewById(R.id.edit_send);

        tvRecv.setMovementMethod(ScrollingMovementMethod.getInstance());

        initListener();
    }

    private void initListener() {
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (openFlag) {
                    closeCan();
                } else {
                    openCan();
                }

                frameCount.set(0);
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRecvInfo("", true);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 发送串口数据
                if (canUtil != null) {
                    String sendStr = editSend.getText().toString();
                    if (sendStr.length() % 2 != 0) {
                        Toast.makeText(CanActivity.this, "发送数据长度必须为2的倍数", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    byte[] bytes = hexString2Bytes(sendStr);
                    CanFrame canFrame = new CanFrame();
                    canFrame.canId = 123;
                    canFrame.idExtend = false;
                    canFrame.len = bytes.length;
                    canFrame.data = bytes;
                    canUtil.canWriteBytes(canFrame);
                }
            }
        });

        spinnerCan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectCan = getResources().getStringArray(R.array.can)[position];
                closeCan();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectBaudRate = Integer.parseInt(getResources().getStringArray(R.array.baudRate)[position]);
                closeCan();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        closeCan();
        recvAlive.set(false);
        super.onDestroy();
    }

    private void updateRecvInfo(final String recv, final boolean clear) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recvStr = clear ? recv : recvStr + recv + "\n";
                tvRecv.setText(recvStr);

                int scrollAmount = tvRecv.getLayout().getLineTop(tvRecv.getLineCount()) - tvRecv.getHeight();
                if (scrollAmount > 0) {
                    tvRecv.scrollTo(0, scrollAmount);
                } else {
                    tvRecv.scrollTo(0, 0);
                }
            }
        });
    }

    /**
     * 打开串口
     */
    public void openCan() {
        if (canUtil != null) {
            canUtil = null;
        }

        btnOpen.setText("关闭");
        openFlag = true;
        canUtil = new CanUtils(selectCan, String.valueOf(selectBaudRate));
        canUtil.canOpen();
        recvAlive.set(true);

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                while (recvAlive.get()) {
                    if (canUtil != null) {
                        CanFrame recvFrame = canUtil.canReadBytes(1, false);
                        if (recvFrame != null && recvFrame.data != null && recvFrame.data.length > 0) {
                            Log.i(TAG, "recv can == " + recvFrame.toString() + "can frameCount == " + frameCount.incrementAndGet());
                            updateRecvInfo(bytesToHexString(recvFrame.data, recvFrame.data.length), false);
                        }
                    }

                    SystemClock.sleep(100);
                }
            }
        });
    }

    /**
     * 关闭串口
     */
    public void closeCan() {
        btnOpen.setText("开启");
        openFlag = false;

        if (canUtil != null) {
            recvAlive.set(false);
            SystemClock.sleep(200);
            canUtil.canClose();
        }

        canUtil = null;
    }


    private String toHexString(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

    private byte[] hexString2Bytes(String src) {
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < tmp.length / 2; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    private byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    private String bytesToHexString(byte[] src, int size) {
        String ret = "";
        if (src == null || size <= 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            String hex = Integer.toHexString(src[i] & 0xFF);
            if (hex.length() < 2) {
                hex = "0" + hex;
            }
            hex += " ";
            ret += hex;
        }
        return ret.toUpperCase();
    }
}

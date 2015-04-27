package com.example.lgf.bluetouch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private BluetoothAdapter bluetoothadapter;

    private RecyclerView recycler;
    private DeviceAdapter adapter;
    private Device device;


    private Map<BluetoothDevice, BluetoothSocket> socketmap = new HashMap();
    private String uuid = "4e3a500b-1ba9-4c3f-a5fe-76cb46608b5f";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    BluetoothDevice device1 = msg.getData().getParcelable(BluetoothDevice.EXTRA_DEVICE);
                    adapter.add(device1);
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, ((String) msg.obj), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recycler = ((RecyclerView) findViewById(R.id.recycler));
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this, new ArrayList<BluetoothDevice>());
        recycler.setAdapter(adapter);

        //获取蓝牙适配器
        bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothadapter == null) {
            Toast.makeText(this, "本设备没有蓝牙设备", Toast.LENGTH_LONG).show();
            finish();
        }

        //检查蓝牙是否开启
        if (!bluetoothadapter.isEnabled()) {
            //第一种  直接代码开启
//            bluetoothadapter.enable();//调用方法开启
            //通过意图开启蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        } else {
            //获取已佩戴的设备列表
            adapter.addAll(bluetoothadapter.getBondedDevices());
            discovery();
        }


    }

    private BluetoothServerSocket server;

    private void discovery() {
     //开始扫描
        bluetoothadapter.startDiscovery();
        device=new Device(handler);
        //找到蓝牙设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //注册
        registerReceiver(device, filter);
        try {
            server = bluetoothadapter.listenUsingInsecureRfcommWithServiceRecord("", UUID.fromString(uuid));
            new Thread() {
                @Override
                public void run() {
                    try {
                        BluetoothSocket socket;
                        while ((socket = server.accept()) != null) {
                            BluetoothDevice device1 = socket.getRemoteDevice();
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device1);
                            Message message = handler.obtainMessage(0);
                            message.setData(bundle);
                            message.sendToTarget();
                            socketmap.put(device1, socket);
                            new ReadThread(socket, handler);
//                            Log.d("BluetoothSocket", device1.getName());
//                            DataInputStream stream = new DataInputStream(socket.getInputStream());
//
//                            Log.d("BluetoothSocket", stream.readUTF());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (recycler != null) {
            //解注册
            unregisterReceiver(device);
        }
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Set<Map.Entry<BluetoothDevice, BluetoothSocket>> entries = socketmap.entrySet();
        for (Map.Entry<BluetoothDevice, BluetoothSocket> entry : entries) {

            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "开启成功", Toast.LENGTH_SHORT).show();
            adapter.addAll(bluetoothadapter.getBondedDevices());
            discovery();

        } else {

            Toast.makeText(this, "开启失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        int position = recycler.getChildPosition(view);
        final BluetoothDevice item = adapter.getItem(position);
//        ParcelUuid[] uuids = item.getUuids();
//        if (uuids != null) {
//            for (ParcelUuid uuid:uuids){
//
//                Log.d("ParcelUuid", uuid.toString());
//            }
//        }
        //蓝牙的收发不要关流
        new Thread() {
            @Override
            public void run() {
                BluetoothSocket socket = socketmap.get(item);
                try {
                    if (socket == null) {
                        socket = item.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));

                        //发起链接
                        socket.connect();
                        new ReadThread(socket, handler).start();
                        socketmap.put(item, socket);
                    }
                    DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                    stream.writeUTF("发送测试");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}

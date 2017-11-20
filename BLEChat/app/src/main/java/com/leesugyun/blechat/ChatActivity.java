package com.leesugyun.blechat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private ListView chatListView;
    private ArrayAdapter<String> chatArrayAdapter;

    private Button chatSetTextButton;
    private Switch chatAdvertisingSwitch;

    private BluetoothDevice targetDevice;
    private BluetoothGatt bluetoothGatt;

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            chatArrayAdapter.insert(targetDevice.getName() + " : " + characteristic.getValue(),0);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intentFromScanDeviceActivity = getIntent();
        targetDevice = (BluetoothDevice) intentFromScanDeviceActivity.getParcelableExtra("targetDevice");



        chatListView = (ListView) findViewById(R.id.chatListView);
        chatArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        chatListView.setAdapter(chatArrayAdapter);


        bluetoothGatt = targetDevice.connectGatt(this, false, bluetoothGattCallback);
    }
}

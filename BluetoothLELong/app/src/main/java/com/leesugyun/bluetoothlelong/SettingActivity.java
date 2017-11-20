package com.leesugyun.bluetoothlelong;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    // 스캔 대상 UUID, 발신 정보 UUID

    private EditText scanUuidEditText;
    private EditText advertiseUuidEditText;

    private TextView myDeviceTextView;

    private Button confirmButton;
    private Button getRandomUuid;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // UUID 초기값설정
        scanUuidEditText = (EditText) findViewById(R.id.scanUuidEditText);
        scanUuidEditText.setText("0000b81d-0000-1000-8000-00805f9b34fb");
        advertiseUuidEditText = (EditText) findViewById(R.id.advertiseUuidEditText);
        advertiseUuidEditText.setText("0000b81d-0000-1000-8000-00805f9b34fb");

        // BluetoothAdapter초기화
        bluetoothAdapter =
                ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        // Bluetooth, 동시 Advertisement가 가능한 기기인지 확인
        if (!bluetoothAdapter.isEnabled() || !bluetoothAdapter.isMultipleAdvertisementSupported()) {
            // 경고창 실행 및 종료
            AlertDialog.Builder alert = new AlertDialog.Builder(SettingActivity.this);
            alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alert.setMessage("Bluetooth or Advertisements NOT Available");
            alert.show();
        }
        // 내 기기 이름 표시
        myDeviceTextView = (TextView) findViewById(R.id.myDevice);
        myDeviceTextView.setText("My Device Name : " + bluetoothAdapter.getName());

        // 다음 액티비티로 진행하는 버튼
        confirmButton = (Button) findViewById(R.id.setting_confirmButton);
        confirmButton.setOnClickListener(this); // 클래스가 Implement 하여 onClick 을 아래 구현

        // 기본 UUID를 랜덤하게 생성해주는 버튼
        getRandomUuid = (Button) findViewById(R.id.setting_randomButton);
        getRandomUuid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String stringUuid = UUID.randomUUID().toString();
                scanUuidEditText.setText(stringUuid);
                advertiseUuidEditText.setText(stringUuid);
            }
        });
    }

    // 각 EditText의 UUID 문자열을 Intent에 담아 보냄
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("scanUuid", scanUuidEditText.getText().toString());
        intent.putExtra("advertiseUuid", advertiseUuidEditText.getText().toString());
        startActivity(intent);
    }
}

package com.leesugyun.blechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView myDeviceName;
    private EditText myServiceEditText;
    private EditText targetServiceEditText;
    private Button goToSearchButton;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // BluetoothAdapter초기화
        bluetoothAdapter =
                ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        // Bluetooth, 동시 Advertisement가 가능한 기기인지 확인
        if (!bluetoothAdapter.isEnabled() || !bluetoothAdapter.isMultipleAdvertisementSupported()) {
            // 경고창 실행 및 종료
            AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
            alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alert.setMessage("Bluetooth or Advertisements NOT Available");
            alert.show();
        }

        // 내 기기의 정보를 TextView에 보여줌
        myDeviceName = (TextView) findViewById(R.id.myDeviceTextView);
        myDeviceName.setText(bluetoothAdapter.getName() + " : " + bluetoothAdapter.getAddress());

        // 다음 Activity로 진행하기 위한 버튼
        goToSearchButton = (Button) findViewById(R.id.goToSearchButton);
        goToSearchButton.setOnClickListener(this);

        // Advertise UUID와 Scan UUID의 초기값 설정
        myServiceEditText = (EditText) findViewById(R.id.myServiceEditText);
        myServiceEditText.setText("0000b81d-0000-1000-8000-00805f9b34fb");
        targetServiceEditText = (EditText) findViewById(R.id.targetServiceEditText);
        targetServiceEditText.setText("0000b81d-0000-1000-8000-00805f9b34fb");
    }

    // 버튼을 클릭하면, Intent에 UUID String을 넣어 다음 Activity 실행
    @Override
    public void onClick(View view) {
        Intent intentForScanDeviceActivity = new Intent(StartActivity.this, ScanDeviceActivity.class);
        intentForScanDeviceActivity.putExtra("myServiceUUID", myServiceEditText.getText().toString());
        intentForScanDeviceActivity.putExtra("targetServiceUUID", targetServiceEditText.getText().toString());
        startActivity(intentForScanDeviceActivity);
    }
}

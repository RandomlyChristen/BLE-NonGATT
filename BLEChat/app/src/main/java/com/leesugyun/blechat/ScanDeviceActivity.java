package com.leesugyun.blechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScanDeviceActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ParcelUuid myServiceUUID;
    private ParcelUuid targetServiceUUID;

    // Advertise 관련
    private Button checkSetTextButton;
    private EditText checkAdvertisingEditText;
    private Switch checkAdvertisingSwitch;

    private BluetoothAdapter adBluetoothAdapter;
    private BluetoothManager adbluetoothManager;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback advertiseCallback;
    String textString = "";

    // Scan 관련
    private Button discoverButton;
    private ListView deviceListView;
    private ArrayList<BluetoothDevice> deviceArrayList;
    private ArrayList<HashMap<String, String>> deviceInfoArrayList;
    private SimpleAdapter deviceInfoArrayAdapter;

    private BluetoothAdapter scbluetoothAdapter;
    private BluetoothLeScanner scbluetoothLeScanner;
    private Handler handler;
    private ScanCallback scanCallback;

    // TODO : aa
    HashMap<String, String> addressAndData = new HashMap<>();


    // Override Method Error Toast
    public void errorToast (int errorCode) {
        Toast.makeText(this, "Error Declared by " + errorCode, Toast.LENGTH_LONG).show();
    }
    // Override Method String Toast
    public void stringToast (String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Advertise Method 구현
    private void advertise () {
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingBuilder.setTimeout(0);
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                stringToast("Advertising " + textString);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                errorToast(errorCode);
            }
        };

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(myServiceUUID);
        dataBuilder.setIncludeDeviceName(true);
        // CharSet : UTF-8
        try {
            dataBuilder.addServiceData(myServiceUUID, textString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 나의 Advertise 정보를 List에 보여줌
        bluetoothLeAdvertiser.startAdvertising(settingBuilder.build(), dataBuilder.build(), advertiseCallback);

        HashMap<String, String> hashMap = new HashMap<>();
        // NAME : DATA
        hashMap.put("firstLine", "ME : " + textString);
        // ADDRESS  RSSI
        hashMap.put("secondLine", adBluetoothAdapter.getAddress());

        deviceArrayList.add(null);
        deviceInfoArrayList.add(hashMap);
        deviceInfoArrayAdapter.notifyDataSetChanged();
    }
    // StopAdvertise Method 구현
    private void stopAdvertise () {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        advertiseCallback = null;
    }

    // Scan Method 구현
    private void scan() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                String address = result.getDevice().getAddress();
                String name = result.getDevice().getName();
                String dataString = null;
                int rssi = result.getRssi();
                byte[] data = result.getScanRecord().getServiceData().get(targetServiceUUID);
                try {
                    dataString = new String(data, "UTF-8");
                    Log.d("stringData", dataString);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    stringToast("Encoding Data is Unsupported");
                }

                // 직전에 같은 ADDRESS 로 같은 DATA 를 받은적이 있는지 확인
                // DATA 가 바뀔 때만 List 에 보여준다.
                if (addressAndData.containsKey(address)){
                    if (addressAndData.get(address).equals(dataString)){
                        // 같은 ADDRESS 로 같은 DATA 를 받은적이 있으면, 즉시 반환
                        return;
                    } else {
                        // 같은 ADDRESS 로 받은 DATA 가 직전것과 다르면, 갱신후 진행
                        addressAndData.remove(address);
                        addressAndData.put(address, dataString);
                    }
                } else {
                    // 같은 ADDRESS 로 받은 DATA 가 없다면, 추가후 진행
                    addressAndData.put(address, dataString);
                }

                HashMap<String, String> hashMap = new HashMap<>();
                // NAME : DATA
                hashMap.put("firstLine", name + " : " + dataString);
                // ADDRESS  RSSI
                hashMap.put("secondLine", address + "   " + rssi);

                deviceArrayList.add(result.getDevice());
                deviceInfoArrayList.add(hashMap);
                deviceInfoArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);

                for (ScanResult result : results) {
                    String address = result.getDevice().getAddress();
                    String name = result.getDevice().getName();
                    String dataString = null;
                    int rssi = result.getRssi();
                    byte[] data = result.getScanRecord().getServiceData().get(targetServiceUUID);
                    try {
                        dataString = new String(data, "UTF-8");
                        Log.d("stringData", dataString);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        stringToast("Encoding Data is Unsupported");
                    }

                    // 직전에 같은 ADDRESS 로 같은 DATA 를 받은적이 있는지 확인
                    // DATA 가 바뀔 때만 List 에 보여준다.
                    if (addressAndData.containsKey(address)){
                        if (addressAndData.get(address).equals(dataString)){
                            // 같은 ADDRESS 로 같은 DATA 를 받은적이 있으면, 즉시 반환
                            return;
                        } else {
                            // 같은 ADDRESS 로 받은 DATA 가 직전것과 다르면, 갱신후 진행
                            addressAndData.remove(address);
                            addressAndData.put(address, dataString);
                        }
                    } else {
                        // 같은 ADDRESS 로 받은 DATA 가 없다면, 추가후 진행
                        addressAndData.put(address, dataString);
                    }

                    HashMap<String, String> hashMap = new HashMap<>();
                    // NAME : DATA
                    hashMap.put("firstLine", name + " : " + dataString);
                    // ADDRESS  RSSI
                    hashMap.put("secondLine", address + "   " + rssi);

                    deviceArrayList.add(result.getDevice());
                    deviceInfoArrayList.add(hashMap);
                    deviceInfoArrayAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                errorToast(errorCode);
            }
        };

        // ScanFilter 설정
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        filterBuilder.setServiceUuid(targetServiceUUID);
        scanFilters.add(filterBuilder.build());
        // ScanSettings 설정
        ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);

        // BluetoothLeScanner Scan시작
        scbluetoothLeScanner.startScan(scanFilters, settingBuilder.build(), scanCallback);
        Toast.makeText(this, "Scanning for 60sec...", Toast.LENGTH_LONG).show();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        // StartActivity로 부터 받아온 Intent받아 데이터를 UUID타입으로 초기화
        Intent intentFromStartActivity = getIntent();
        myServiceUUID = ParcelUuid.fromString(
                (String) intentFromStartActivity.getSerializableExtra("myServiceUUID"));
        targetServiceUUID = ParcelUuid.fromString(
                (String) intentFromStartActivity.getSerializableExtra("targetServiceUUID"));

        // View 와 ArrayList, ArrayAdapter 초기화
        deviceArrayList = new ArrayList<>();
        deviceInfoArrayList = new ArrayList<>();
        deviceInfoArrayAdapter = new SimpleAdapter(
                    this, deviceInfoArrayList, android.R.layout.simple_list_item_2, new String[] {"firstLine", "secondLine"}, new int[]{android.R.id.text1, android.R.id.text2});
        deviceListView = (ListView) findViewById(R.id.deviceListView);
        deviceListView.setAdapter(deviceInfoArrayAdapter);

        // Scan ------------------------------------------------------------------------------------
        scbluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        scbluetoothLeScanner = scbluetoothAdapter.getBluetoothLeScanner();

        discoverButton = (Button) findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ListView, ArrayList 초기화
                addressAndData.clear();
                deviceInfoArrayList.clear();
                deviceArrayList.clear();
                deviceInfoArrayAdapter.notifyDataSetChanged();

                // 10초후에 정지되는 Runnable
                if (scanCallback == null) {
                    discoverButton.setEnabled(false);
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 10초이후 Scan 정지
                            scbluetoothLeScanner.stopScan(scanCallback);
                            scanCallback = null;
                            discoverButton.setEnabled(true);
                            stringToast("Scan Done!");
                        }
                    }, 10000);
                    // scan 실행
                    scan();
                }
            }
        });

        // Advertise -------------------------------------------------------------------------------
        adbluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adBluetoothAdapter = adbluetoothManager.getAdapter();
        bluetoothLeAdvertiser = adBluetoothAdapter.getBluetoothLeAdvertiser();

        checkAdvertisingEditText = (EditText) findViewById(R.id.checkAdvertisingEditText);

        // Switch 기능 구현 : True 이면 Advertise Strat, False 이면 Stop
        checkAdvertisingSwitch = (Switch) findViewById(R.id.checkAdvertisingSwitch);
        checkAdvertisingSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean switchOn = ((Switch) view).isChecked();
                if (switchOn) {
                    advertise();
                } else {
                    stopAdvertise();
                }
            }
        });
        // Button 기능 구현 : Switch가 True이면 data를 새로운 EditText 값으로 Advertise
        checkSetTextButton = (Button) findViewById(R.id.checkSetTextButton);
        checkSetTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // data 초기화
                if (checkAdvertisingEditText.getText().toString().getBytes().length > 2) {
                    stringToast("Error : Input Data Size Maximum - 2 Bytes");
                } else {
                    // Advertise 재설정 및 재시작
                    if (checkAdvertisingSwitch.isChecked()) {
                        stopAdvertise();
                        textString = checkAdvertisingEditText.getText().toString();
                        advertise();
                    }
                }
                checkAdvertisingEditText.setText("");
            }
        });

        // ListView 를 클릭했을때 다음 Activity로 Device정보와 함께 넘어감
        // Intent 정보를 위해 아래 구현
        deviceListView.setOnItemClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        textString = "";
        // Activity가 중지되면 Advertise를 멈춤
        if (checkAdvertisingSwitch.isChecked()) {
            stopAdvertise();
            checkAdvertisingSwitch.setChecked(false);
        }

    }

    // ListView onItemClick 구현
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        BluetoothDevice device = deviceArrayList.get(i);
        String name = ((HashMap<String, String>) deviceInfoArrayList.get(i)).get("name");
        Intent intentForNextActivity = new Intent(ScanDeviceActivity.this, ChatActivity.class);
        intentForNextActivity.putExtra("targetDevice", device);
        startActivity(intentForNextActivity);
    }
}

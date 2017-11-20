package com.leesugyun.bluetoothlelong;

import android.bluetooth.BluetoothAdapter;
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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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

import static android.os.Build.VERSION_CODES.O;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    // ArrayAdapter Index
    static final String LIST_ADAPTER_FIRST_LINE = "1";
    static final String LIST_ADAPTER_SECOND_LINE = "2";

    // Scan 시 사용하는 UUID와 Advertise 시 사용하는 UUID ( Intent에서 받아옴 )
    private ParcelUuid scanUuid;
    private ParcelUuid advertiseUuid;

    // ListView & ArrayAdapter & ArrayList
    private ListView chatListView;
    private SimpleAdapter chatListArrayAdapter;
    private ArrayList<HashMap<String, String>> chatArrayList;

    // Scan
    private Switch scanSwitch;
    private ScanCallback scanCallback;
    private BluetoothAdapter scanBTAdapter;
    private BluetoothLeScanner scannerBLE;
    private HashMap<String, String> addressAndData;

    // Advertise
    private AdvertiseCallback advertiseCallback;
    private BluetoothManager advertiseBTManager;
    private BluetoothAdapter advertiseBTAdapter;
    private BluetoothLeAdvertiser advertiserBLE;
    private EditText chatEditText;
    private Button chatSendButton;
    private String wholeData;
    private String oneData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Intent 에서 Setting 시 입력받았던 UUID를 받아와 ParcelUuid객체로 저장
        Intent intentFromSetting = getIntent();
        scanUuid = ParcelUuid.fromString(
                (String) intentFromSetting.getSerializableExtra("scanUuid"));
        advertiseUuid = ParcelUuid.fromString(
                (String) intentFromSetting.getSerializableExtra("advertiseUuid"));

        // ListView, ArrayList, ArrayAdapter 초기화
        chatListView = (ListView) findViewById(R.id.chat_listView);
        chatArrayList = new ArrayList<>();
        chatListArrayAdapter =
                new SimpleAdapter(
                        this, chatArrayList, android.R.layout.simple_list_item_2, new String[] {LIST_ADAPTER_FIRST_LINE, LIST_ADAPTER_SECOND_LINE}, new int[]{android.R.id.text1, android.R.id.text2});
        chatListView.setAdapter(chatListArrayAdapter);

        addressAndData = new HashMap<>();

        // 스캔 구현, 블루투스 LE 스캐너 초기화 및 온클릭리스터구현
        scanBTAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        scannerBLE = scanBTAdapter.getBluetoothLeScanner();
        scanSwitch = (Switch) findViewById(R.id.scanSwitch);
        scanSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((Switch) view).isChecked()) {
                    // 스캔 함수 이 클래스에 구현
                    scan();
                } else {
                    scannerBLE.stopScan(scanCallback);
                    scanCallback = null;
                    stringToast("Scan Stopped");
                }

            }
        });

        // 에드버타이즈 구현, 블루투스 LE 에드버타이저 초기화 및 온클릭리스너구현
        advertiseBTManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        advertiseBTAdapter = advertiseBTManager.getAdapter();
        advertiserBLE = advertiseBTAdapter.getBluetoothLeAdvertiser();

        chatEditText = (EditText) findViewById(R.id.chat_editText);
        chatSendButton = (Button) findViewById(R.id.chat_sendButton);
        chatSendButton.setOnClickListener(this);


    }

    Handler handler;
    int indicator = 0;
    boolean b = true;
    @Override
    public void onClick(View view) {
        // 전송버튼 비활성화
        chatSendButton.setEnabled(false);
        // EditText 값을 변수로 저장
        wholeData = chatEditText.getText().toString();
        final String[] substringArray = new String[wholeData.length()];
        // 저장된 String 을 한글자 단위로 나눔 ( 1회 에드버타이징 패킷이 2byte )
        for (int i = 0; i < wholeData.length(); i ++) {
            String substring = wholeData.substring(i, i + 1);
            try {
                // 한글자가 2byte를 초과하지않으면, 배열에 추가해줌
                if (!(substring.getBytes("EUC-KR").length > 2)) {
                    substringArray[i] = substring;
                } else {
                    stringToast("One character can not be larger than 2 bytes");
                    substringArray[i] = "";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                stringToast("Exception : UnsupportedEncodingException");
            }
        }
        // 쓰레드를 정의하여 연산, UI 쓰레드에서 UI 작업
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < substringArray.length; i ++) {
                    advertise(substringArray[i]); // 해당 인덱스에 대한 String을 byte로 변환하여 advertise ( 아래에 함수로 구현 )
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    advertiserBLE.stopAdvertising(advertiseCallback); // advertise 를 멈추고 callback 초기화
                    advertiseCallback = null;
                }
                advertise("*"); // 마지막에 * 로 문장의 끝을 수신자에게 알림
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                advertiserBLE.stopAdvertising(advertiseCallback);
                advertiseCallback = null;

                // UI 쓰레드 접속
                ChatActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatEditText.setText("");
                        chatSendButton.setEnabled(true);
                        stringToast("Done !!");
                        // 에드버타이징 내용을 리스트에 올려줌
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(LIST_ADAPTER_FIRST_LINE, "ME" + " : " + wholeData);
                        hashMap.put(LIST_ADAPTER_SECOND_LINE, "");
                        chatArrayList.add(hashMap);
                        chatListArrayAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
        thread.start();
    }

    /**
     * Advertise 함수구현
     */
    private void advertise(String stringData) {
        // Advertise Settings
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingBuilder.setTimeout(0);

        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                stringToast("Error Declared : " + errorCode + "Failed Advertising Data : " + oneData);
            }
        };



        // Advertise 데이터 빌드
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(advertiseUuid);
        dataBuilder.setIncludeDeviceName(true);
        // CharSet : UTF-8
        try {
            dataBuilder.addServiceData(advertiseUuid, stringData.getBytes("EUC-KR"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        advertiserBLE.startAdvertising(settingBuilder.build(), dataBuilder.build(), advertiseCallback);
    }


    /**
     * ScanCallback에서 데이터를 가공하는 함수구현
     */
    @Nullable
    private String scanResultDataParse(ScanResult scanResult, ParcelUuid uuid, HashMap<String, String> checkMap) {
        // 스캔결과의 기기주소
        String address = scanResult.getDevice().getAddress();
        // 스캔결과에 해당하는 값이 해쉬맵에 없으면, 빈 문자열로 하나 추가해준다
        if (!checkMap.containsKey(address)) checkMap.put(address, "");
        // 스캔한 서비스데이터에서 해당 UUID의 데이터를 바이트행렬로 가져온다
        byte[] data = scanResult.getScanRecord().getServiceData().get(uuid);
        // 바이트행렬을 문자열로 변환, 불가능할 경우 빈문자열로 초기화
        String dataToString = "";
        try {
            dataToString = new String(data, "EUC-KR");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            stringToast("Encoding Data is Unsupported");
        }
        // 시리얼데이터의 한 단락을 알리는 *가 검색되고 데이터가 존재할때, 해쉬맵에서 해당 데이터를 함수값으로 반환하고 해당 데이터삭제
        if (dataToString.equals("*") && !checkMap.get(address).equals("")) {
            String returnValue = checkMap.get(address);
            checkMap.remove(address);
            return returnValue;
        } else {
            // 해쉬맵의 해당 데이터와 검색되어있는 데이터가 중복이 아닐경우, 검색된 데이터를 이어붙임
            if (!checkMap.get(address).endsWith(dataToString) && !dataToString.equals("*")) {
                String previous = checkMap.get(address);
                checkMap.remove(address);
                checkMap.put(address, previous + dataToString);
                return null;
            }
            return null;
        }
    }
    /**
     * Scan 함수구현
     * scanResultDataParse 함수를 사용해 String을 ListView에 올림
     */
    private void scan() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                String name = result.getDevice().getName();
                String address = result.getDevice().getAddress();
                int rssi = result.getRssi();
                String data = scanResultDataParse(result, scanUuid, addressAndData);
                if (data != null) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(LIST_ADAPTER_FIRST_LINE, name + " : " + data);
                    hashMap.put(LIST_ADAPTER_SECOND_LINE, address + " : " + rssi);
                    chatArrayList.add(hashMap);
                    chatListArrayAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);

                for (ScanResult result: results) {
                    String name = result.getDevice().getName();
                    String address = result.getDevice().getAddress();
                    int rssi = result.getRssi();
                    String data = scanResultDataParse(result, scanUuid, addressAndData);
                    if (data != null) {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(LIST_ADAPTER_FIRST_LINE, name + " : " + data);
                        hashMap.put(LIST_ADAPTER_SECOND_LINE, address + " : " + rssi);
                        chatArrayList.add(hashMap);
                        chatListArrayAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                stringToast("Scan Failed : " + errorCode);
            }
        };

        // ScanFilter 설정
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        filterBuilder.setServiceUuid(scanUuid);
        scanFilters.add(filterBuilder.build());
        // ScanSettings 설정
        ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        // 스캔 시작
        scannerBLE.startScan(scanFilters, settingBuilder.build(), scanCallback);
        stringToast("Scanning Bluetooth LE Devices..");
    }

    // 이너클래스에서 Toast
    private void stringToast (String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


}

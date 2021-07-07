package pl.hypeapp.endoscope.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.grandcentrix.thirtyinch.TiActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.hypeapp.endoscope.R;
import pl.hypeapp.endoscope.presenter.PlayStreamPresenter;
import pl.hypeapp.endoscope.util.SettingsPreferencesUtil;
import pl.hypeapp.endoscope.view.PlayStreamView;

public class PlayStreamActivity extends TiActivity<PlayStreamPresenter, PlayStreamView>
        implements PlayStreamView, SurfaceHolder.Callback, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    public static final String INTENT_EXTRA_IP_CONNECT = "ip_connect";
    private MediaPlayer mediaPlayer;
/*
    // 블루투스 추가 부분
    private EditText mInputEditText;
    private Button mButton;
    private String mButtonValue = null;
    private String mConnectedDeviceName = null;
    private TextView mConnectionStatus;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BluetoothClient";
    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    ConnectedTask mConnectedTask = null;
    static boolean isConnectionError = false;
*/

    //

    @BindView(R.id.surface_play) SurfaceView surfaceView;

    @NonNull
    @Override
    public PlayStreamPresenter providePresenter() {
        SettingsPreferencesUtil settingsPreferencesUtil = new SettingsPreferencesUtil(PreferenceManager.getDefaultSharedPreferences(this));
        String ipAddress = getIntent().getStringExtra(INTENT_EXTRA_IP_CONNECT);
        return new PlayStreamPresenter(ipAddress, settingsPreferencesUtil);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_stream);

        ButterKnife.bind(this);
        surfaceView.getHolder().addCallback(this);

        // 추가부분
/*
        Button frontButton = (Button)findViewById(R.id.front);
        frontButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                sendMessage("f");
            }
        });
        Button BackButton = (Button)findViewById(R.id.back);
        BackButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("b");

            }
        });
        Button StopButton = (Button)findViewById(R.id.stop);
        StopButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("s");
            }
        });
        Button LeftButton = (Button)findViewById(R.id.left);
        LeftButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("l");
            }
        });
        Button RightButton = (Button)findViewById(R.id.right);
        RightButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("r");
            }
        });
        Button PlusButton = (Button)findViewById(R.id.plus);
        PlusButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("+");
            }
        });
        Button MinusButton = (Button)findViewById(R.id.minus);
        MinusButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                sendMessage("-");
            }
        });



        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);
        ListView mMessageListview = (ListView) findViewById(R.id.message_listview);


        mConversationArrayAdapter = new ArrayAdapter<>( this,
                android.R.layout.simple_list_item_1 );
        mMessageListview.setAdapter(mConversationArrayAdapter);


        Log.d( TAG, "Initalizing Bluetooth adapter...");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialisation successful.");

            showPairedDevicesListDialog();
        }
        // 여기까지
    }
    // theads 대신 안드로이드는 UI를 담당하는 메인쓰레드가 존재하는데 사용자 접근 불가.
    //안드로이드는 Background 작업을 할 수 있도록 AsyncTask를 지원한다.
    // AsyncTask는 쓰레드와 핸들러를 통해 UI를 처리했던 것을 한번에 작업할 수 있도록 지원해준다.
    // 시리얼 통신(SPP)을 하기 위한 RFCOMM 블루투스 소켓을 생성합니다.(ConnectTask)
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        //AsyncTask 첫번째 인자 :excute()메소드가 호출될때 전달한 인자를 doInBackground 메소드에서 파라메터로써 전달 받게되는떼 이 때 사용되는 타입이다.
        // 두번쨰 인자 : doln메소드에서 백그라운드 처리 중에 publishProgress 메소드를 호출하여 전달한 인자를 opProgressUpdate 메소드에서 파라메터로 받게 되는데 이때 사용되는 타입이다.
        // 세번쨰 인자 : dolnBackground 메소드에서 리턴한 값은 onPostExecute 메소드에서 파라메터로써 받게 되는데, 이때 사용되는 타입이다.

        // ,doInBackground()에서 사용할 매개 변수로 Void, onPostExecute()에서 사용할 매개변수를 Boolean으로 사용

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }

            mConnectionStatus.setText("connecting...");
        }


        @Override
        protected Boolean doInBackground(Void... params) { //

            // Always cancel discovery because it will slow down a connection
            //주변 블루투스 디바이스 찾는 것을 중지한다.
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }

                return false;
            }

            return true;
        }


        @Override
        //블루투스 소켓을 성공적으로 생성했다면 ConnectedTask AsyncTask를 실행합니다.
        protected void onPostExecute(Boolean isSucess) {

            if ( isSucess ) {
                connected(mBluetoothSocket);
            }
            else{

                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }

    public void connected( BluetoothSocket socket ) {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }


    // 실제 데이터를 주고 받는 처리를 ConnectedTask에서 한다.
    //dolnBackground 메소드에서 대기하며 수신되는 문자열이 있으면 받아서 버퍼에 저장합니다.

    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket =  null;

        ConnectedTask(BluetoothSocket socket){

            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }

            Log.d( TAG, "connected to "+mConnectedDeviceName);
            mConnectionStatus.setText( "connected to "+mConnectedDeviceName);
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;


            while (true) {

                if ( isCancelled() ) return false;

                try {

                    int bytesAvailable = mInputStream.available();

                    if(bytesAvailable > 0) {

                        byte[] packetBytes = new byte[bytesAvailable];

                        mInputStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++) {

                            byte b = packetBytes[i];
                            if(b == '\n')
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {

                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }

        }

        @Override
        protected void onProgressUpdate(String... recvMessage) {

            mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);

            if ( !isSucess ) {


                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                showErrorDialog("Device connection was lost");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);

            closeSocket();
        }

        void closeSocket(){

            try {

                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2) {

                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }
        //write 메소드는 문자열으로 전송할 때 호출되어 진다.
        void write(String msg){
            // 뒤에 \n 이 붙어도 정상적동 될까>..?
            msg += "\n";

            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e );
            }

            //주의하기
            mInputEditText.setText(" ");
        }
    }

    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 ){
            showQuitDialog( "No devices have been paired.\n"
                    +"You must pair it with another device.");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
            items[i] = pairedDevices[i].getName();
        }


        //선택한 디바이스를 인자로 하여 ConnectTask AsyncTask 실행한다.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                    finish();
                }
            }
        });
        builder.create().show();
    }

    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }


    void sendMessage(String msg){

        if ( mConnectedTask != null ) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
            mConversationArrayAdapter.insert("Me:  " + msg, 0);
            //여기 수정
            // TextView textView1 = (TextView) findViewById(R.id.TextView1);
            //textView1.setText(msg);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "You need to enable bluetooth");
            }
        }
        */
    }
    // 여기까지 수정함



    @Override
    public void setFullscreenWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        getPresenter().onSurfaceCreated();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        getPresenter().onSurfaceDestroyed();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        getPresenter().onMediaPlayerPrepared(mp);
    }

    @Override
    public void configureMediaPlayer(Uri videoUri) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setDisplay(surfaceView.getHolder());
        mediaPlayer.setOnPreparedListener(this);
        try {
            mediaPlayer.setDataSource(this, videoUri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void releaseMediaPlayer() {
        surfaceView.getHolder().removeCallback(this);
        mediaPlayer.release();
    }

    @Override
    public void logError(String msg) {
        String error = (msg == null) ? "Error unknown" : msg;
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_dialog_title)
                .setCancelable(false)
                .setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(PlayStreamActivity.this, ConnectToStreamActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }).setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK) {
                    Intent intent = new Intent(PlayStreamActivity.this, ConnectToStreamActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                return false;
            }
        }).create().show();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (extra == MediaPlayer.MEDIA_ERROR_IO) {
            logError("MEDIA ERROR");
        } else if (extra == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            logError("SERVER DIED ERROR");
        } else if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
            logError("MEDIA UNSUPPORTED");
        } else if (extra == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            logError("MEDIA ERROR UNKNOWN");
        } else if (extra == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            logError("NOT VALID PROGRESSIVE PLAYBACK");
        } else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
            logError("MEDIA ERROR TIMED OUT");
        } else {
            logError("ERROR UNKNOWN (" + what + ")" + "(" + extra + ")");
        }
        return false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
}

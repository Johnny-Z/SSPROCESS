package com.example.nan.ssprocess.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.example.nan.ssprocess.R;
import com.example.nan.ssprocess.app.SinSimApp;
import com.example.nan.ssprocess.app.URL;
import com.example.nan.ssprocess.net.Network;
import com.example.nan.ssprocess.util.ShowMessage;
import com.example.nan.ssprocess.bean.response.LoginResponseData;
import com.google.gson.Gson;

import java.util.LinkedHashMap;

/**
 * @author nan 2017/11/14
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "nlgLoginActivity";

    private TextView mSystemVersionTv;
    private EditText mAccountText;
    private EditText mPasswordText;
    private Button mLoginButton;
    private ProgressDialog mLoadingProcessDialog;
    private Network mNetwork;
    private LoginHandler mLoginHandler;
    private SinSimApp mApp;
    private AlertDialog mIPSettngDialog = null;
    private String mPassword=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mApp = (SinSimApp) getApplication();
        mNetwork = Network.Instance(getApplication());
        mLoginHandler = new LoginHandler();

        mLoginButton = (Button) findViewById(R.id.btn_login);
        mPasswordText = (EditText) findViewById(R.id.input_password);
        mAccountText = (EditText) findViewById(R.id.input_account);
        if(SinSimApp.getApp().getAccount() != null && !"".equals(SinSimApp.getApp().getAccount())) {
            mAccountText.setText(SinSimApp.getApp().getAccount());
        }
        mSystemVersionTv = findViewById(R.id.system_version);
        mSystemVersionTv.setText(getVersion());

        mLoginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public String getVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return "V" + version;
        } catch (Exception e) {
            e.printStackTrace();
            return "无版本信息";
        }
    }

    private void login() {

        mLoginButton.setEnabled(false);

        if( mLoadingProcessDialog == null) {
            mLoadingProcessDialog = new ProgressDialog(LoginActivity.this);
            mLoadingProcessDialog.setCancelable(false);
            mLoadingProcessDialog.setCanceledOnTouchOutside(false);
            mLoadingProcessDialog.setMessage("登录中...");
        }
        mLoadingProcessDialog.show();
        LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
        mPassword=mPasswordText.getText().toString();
        mPostValue.put("account", mAccountText.getText().toString());
        mPostValue.put("password", mPassword);
        mPostValue.put("meid", SinSimApp.getApp().getIMEI());
        Log.d(TAG, "login: IMEI: "+SinSimApp.getApp().getIMEI());
        if(TextUtils.isEmpty(SinSimApp.getApp().getServerIP())){
            if(mLoadingProcessDialog.isShowing()) {
                mLoadingProcessDialog.dismiss();
            }
            mLoginButton.setEnabled(true);
            ToastUtils.showShort("服务端IP为空，请设置IP地址");
            Log.d(TAG, "login: 服务端IP为空，请设置IP地址");
        } else if (SinSimApp.getApp().getIMEI()==null){
            if(mLoadingProcessDialog.isShowing()) {
                mLoadingProcessDialog.dismiss();
            }
            mLoginButton.setEnabled(true);
            ToastUtils.showShort("未获取到手机识别码,请重启软件");
            Log.d(TAG, "login: 未获取到手机IMEI");
        }
        else {
            String loginUrl = URL.HTTP_HEAD + SinSimApp.getApp().getServerIP() + URL.USER_LOGIN;
            mNetwork.fetchLoginData(loginUrl, mPostValue, mLoginHandler);
        }

    }

    @SuppressLint("HandlerLeak")
    class LoginHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {

            if(mLoadingProcessDialog != null && mLoadingProcessDialog.isShowing()) {
                mLoadingProcessDialog.dismiss();
            }

            if (msg.what == Network.OK) {
                onLoginSuccess((LoginResponseData)msg.obj);
            } else {
                String errorMsg = (String)msg.obj;
                onLoginFailed(errorMsg);
            }
        }
    }

    public void onLoginSuccess(LoginResponseData data) {
        mLoginButton.setEnabled(true);
        if( data != null) {
            //Store to memory and preference
            mApp.setIsLogined(true,
                    data.getAccount(),
                    data.getFullName(),
                    mPassword,
                    data.getRole().getId(),
                    data.getId(),
                    data.getGroup() != null ? data.getGroup().getId():0,
                    data.getGroup() != null ? data.getGroup().getGroupName():"",
                    data.getGroup() != null ? data.getGroup().getType():""
            );

            //在登陆完成后检查人员role进入不同界面：生产部管理员：2，质检员：11, 安装组长：3
            Log.d(TAG, "onLoginSuccess: role name "+SinSimApp.getApp().getGroupName());
            Intent it = new Intent();
            if (SinSimApp.getApp().getGroupType().equals("部装")){
                it.setClass(LoginActivity.this, MenuActivity.class);
                startActivity(it);
                finish();
            }else {
                switch (SinSimApp.getApp().getRole()) {
                    case SinSimApp.LOGIN_FOR_ADMIN:
                        it.setClass(LoginActivity.this, ProcessToAdminActivity.class);
                        startActivity(it);
                        finish();
                        break;
                    case SinSimApp.LOGIN_FOR_QA:
                        it.setClass(LoginActivity.this, ProcessToCheckoutActivity.class);
                        startActivity(it);
                        finish();
                        break;
                    case SinSimApp.LOGIN_FOR_INSTALL:
                        it.setClass(LoginActivity.this, ProcessToInstallActivity.class);
                        startActivity(it);
                        finish();
                        break;
                    default:
                        ToastUtils.showShort("您没有登入权限！");
                        break;
                }
            }
        }
    }

    public void onLoginFailed(String msg) {
        if( msg != null) {
            ShowMessage.showDialog(LoginActivity.this, msg);
        } else {
            ShowMessage.showDialog(LoginActivity.this, "登入错误，请检查网络！");
        }
        mLoginButton.setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ip_settings:
                LinearLayout layout = (LinearLayout) View.inflate(LoginActivity.this, R.layout.dialog_ip_setting, null);
                final EditText editText = (EditText)layout.findViewById(R.id.ip_value);
                //读取保存的IP地址
                String temp = SinSimApp.getApp().getServerIP();
                Log.d(TAG, "onOptionsItemSelected: "+temp);
                mIPSettngDialog = new AlertDialog.Builder(LoginActivity.this).create();
                mIPSettngDialog.setTitle("服务端IP设置");
                mIPSettngDialog.setView(layout);
                if(!TextUtils.isEmpty(temp)) {
                    editText.setText(temp);
                }
                mIPSettngDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                mIPSettngDialog.setButton(AlertDialog.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Log.d(TAG, "onClick: 输入ip："+editText.getText().toString());
                            SinSimApp.getApp().setServerIP(editText.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                mIPSettngDialog.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLoadingProcessDialog != null) {
            mLoadingProcessDialog.dismiss();
        }
        if(mIPSettngDialog != null) {
            mIPSettngDialog.dismiss();
        }
    }
}

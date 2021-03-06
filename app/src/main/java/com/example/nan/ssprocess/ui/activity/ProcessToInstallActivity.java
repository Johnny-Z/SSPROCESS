package com.example.nan.ssprocess.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nan.ssprocess.R;
import com.example.nan.ssprocess.adapter.TaskRecordAdapter;
import com.example.nan.ssprocess.app.SinSimApp;
import com.example.nan.ssprocess.app.URL;
import com.example.nan.ssprocess.bean.basic.AttendanceData;
import com.example.nan.ssprocess.bean.basic.InstallPlanData;
import com.example.nan.ssprocess.bean.basic.MachineProcessData;
import com.example.nan.ssprocess.bean.basic.TaskNodeData;
import com.example.nan.ssprocess.bean.basic.TaskRecordMachineListData;
import com.example.nan.ssprocess.bean.basic.UserData;
import com.example.nan.ssprocess.net.Network;
import com.example.nan.ssprocess.service.MyMqttService;
import com.example.nan.ssprocess.util.ShowMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import cn.bingoogolapple.refreshlayout.BGAMoocStyleRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;

/**
 * @author nan  2017/11/16
 */
public class ProcessToInstallActivity extends AppCompatActivity implements BGARefreshLayout.BGARefreshLayoutDelegate{
    private static String TAG = "nlgProcessToInstall";
    private Intent mqttIntent;
    private ArrayList<TaskRecordMachineListData> mProcessToInstallPlanList = new ArrayList<>();
    private TaskRecordAdapter mTaskRecordAdapter;
    private ProgressDialog mLoadingProcessDialog;

    private static final int SCAN_QRCODE_START = 1;

    private int mPage;
    private BGARefreshLayout mRefreshLayout;

    final String IP = SinSimApp.getApp().getServerIP();
    private ArrayList<TaskRecordMachineListData> mScanResultList;
    private String mMachineNamePlate = "";
    private AttendanceData mAttendanceData;
    private boolean mAttendanceFlag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_to_install);

        //启动MQTT服务
        mqttIntent = new Intent(this, MyMqttService.class);
        startService(mqttIntent);

        //列表
        RecyclerView mProcessToAdminRV = findViewById(R.id.process_to_install_rv);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mProcessToAdminRV.setLayoutManager(manager);
        mTaskRecordAdapter = new TaskRecordAdapter(mProcessToInstallPlanList);
        mProcessToAdminRV.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mProcessToAdminRV.setAdapter(mTaskRecordAdapter);
        //点击跳转，把所有接收到的数据传递给下一个activity
        mTaskRecordAdapter.setOnItemClickListener(new TaskRecordAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(int position){
                Log.d(TAG, "onItemClick: gson :"+new Gson().toJson(mProcessToInstallPlanList.get(position)));
                Intent intent=new Intent(ProcessToInstallActivity.this,DetailToInstallActivity.class);
                intent.putExtra("mTaskRecordMachineListData", mProcessToInstallPlanList.get(position));
                startActivity(intent);
            }
        });

        //点击扫码
        Button scanQrcodeBotton = findViewById(R.id.planed_install_scan_qrcode_button);
        scanQrcodeBotton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ProcessToInstallActivity.this,ScanQrcodeActivity.class);
                startActivityForResult(intent,SCAN_QRCODE_START);
            }
        });

        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setDelegate(this);
        mPage=0;
        // 设置下拉刷新和上拉加载更多的风格     参数1：应用程序上下文，参数2：是否具有上拉加载更多功能
        BGAMoocStyleRefreshViewHolder moocStyleRefreshViewHolder = new BGAMoocStyleRefreshViewHolder(this, true);
        moocStyleRefreshViewHolder.setOriginalImage(R.drawable.bga_refresh_moooc);
        moocStyleRefreshViewHolder.setUltimateColor(R.color.colorAccent);
        mRefreshLayout.setRefreshViewHolder(moocStyleRefreshViewHolder);
    }

    @Override
    public void onResume() {
        super.onResume();
        //第一次进入刷新页面， 加载loading页面
        if( mLoadingProcessDialog == null) {
            mLoadingProcessDialog = new ProgressDialog(ProcessToInstallActivity.this);
            mLoadingProcessDialog.setCancelable(false);
            mLoadingProcessDialog.setCanceledOnTouchOutside(false);
            mLoadingProcessDialog.setMessage("获取信息中...");
        }
        mLoadingProcessDialog.show();
        fetchProcessData(mPage);
    }


    private void fetchProcessData(int page) {
        LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
        mPostValue.put("userAccount", SinSimApp.getApp().getAccount());
        mPostValue.put("page", ""+page);
        String fetchProcessRecordUrl = URL.HTTP_HEAD + IP + URL.FETCH_TASK_RECORD_TO_INSTALL;
        Network.Instance(SinSimApp.getApp()).fetchProcessTaskRecordData(fetchProcessRecordUrl, mPostValue, new FetchProcessDataHandler());
    }

    @SuppressLint("HandlerLeak")
    private class FetchProcessDataHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            if(mLoadingProcessDialog != null && mLoadingProcessDialog.isShowing()) {
                mLoadingProcessDialog.dismiss();
            }
            mRefreshLayout.endRefreshing();
            mRefreshLayout.endLoadingMore();

            if (msg.what == Network.OK) {
                mProcessToInstallPlanList=(ArrayList<TaskRecordMachineListData>)msg.obj;
                Log.d(TAG, "handleMessage: size: "+mProcessToInstallPlanList.size());

                if (mProcessToInstallPlanList.size() > 0) {
                    ArrayList<TaskRecordMachineListData> tempList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> urgentList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> beyondList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> arriveList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> abnormalList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> orderChangeList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> skipList = new ArrayList<>();
                    ArrayList<TaskRecordMachineListData> normalList = new ArrayList<>();
                    for (int position = 0; position < mProcessToInstallPlanList.size(); position++) {
                        float daySum = (mProcessToInstallPlanList.get(position).getMachineOrderData().getPlanShipDate() - new Date().getTime())/(1000*60*60*24);

                        if (mProcessToInstallPlanList.get(position).getMachineData().getIsUrgent()){
                            urgentList.add(mProcessToInstallPlanList.get(position));
                        } else if (daySum < 0) {
                            beyondList.add(mProcessToInstallPlanList.get(position));
                        } else if (daySum < 3) {
                            arriveList.add(mProcessToInstallPlanList.get(position));
                        } else if (mProcessToInstallPlanList.get(position).getMachineData().getStatus()==SinSimApp.MACHINE_CHANGED
                                ||mProcessToInstallPlanList.get(position).getMachineData().getStatus()==SinSimApp.MACHINE_SPLITED) {
                            orderChangeList.add(mProcessToInstallPlanList.get(position));
                        } else {
                            switch (mProcessToInstallPlanList.get(position).getStatus()) {
                                case SinSimApp.TASK_INITIAL:
                                case SinSimApp.TASK_PLANED:
                                case SinSimApp.TASK_INSTALL_WAITING:
                                case SinSimApp.TASK_INSTALLING:
                                case SinSimApp.TASK_INSTALLED:
                                case SinSimApp.TASK_QUALITY_DOING:
                                case SinSimApp.TASK_QUALITY_DONE:
                                    normalList.add(mProcessToInstallPlanList.get(position));
                                    break;
                                case SinSimApp.TASK_INSTALL_ABNORMAL:
                                case SinSimApp.TASK_QUALITY_ABNORMAL:
                                    abnormalList.add(mProcessToInstallPlanList.get(position));
                                    break;
                                case SinSimApp.TASK_SKIP:
                                    skipList.add(mProcessToInstallPlanList.get(position));
                                    break;
                                default:
                                    break;

                            }
                        }
                    }
                    //排序：加急-》超期-》临期-》异常-》改单拆单-》跳过-》正常
                    tempList.addAll(urgentList);
                    tempList.addAll(beyondList);
                    tempList.addAll(arriveList);
                    tempList.addAll(abnormalList);
                    tempList.addAll(orderChangeList);
                    tempList.addAll(skipList);
                    tempList.addAll(normalList);
                    mProcessToInstallPlanList=tempList;
                }
                if (mProcessToInstallPlanList.size()==0){
                    mTaskRecordAdapter.setProcessList(mProcessToInstallPlanList);
                    mTaskRecordAdapter.notifyDataSetChanged();
                    Toast.makeText(ProcessToInstallActivity.this,"没有更多了...",Toast.LENGTH_SHORT).show();
                } else {
                    mTaskRecordAdapter.setProcessList(mProcessToInstallPlanList);
                    mTaskRecordAdapter.notifyDataSetChanged();
                }
            } else {
                String errorMsg = (String)msg.obj;
                Log.d(TAG, "handleMessage: "+errorMsg);
                Toast.makeText(ProcessToInstallActivity.this,"更新失败!"+errorMsg,Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
        Log.d(TAG, "onBGARefreshLayoutBeginRefreshing: 下划刷新");
        fetchProcessData(mPage);
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        Log.d(TAG, "onBGARefreshLayoutBeginLoadingMore: 上划刷新");
        mPage=mPage+1;
        fetchProcessData(mPage);
        return true;
    }

    /**
     * 按nameplate查询该机器当前的安装信息
     * @param nameplate 机器编号
     */
    private void selectProcessMachine(String nameplate){
        LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
        mPostValue.put("nameplate", nameplate);
        String fetchProcessMachineUrl = URL.HTTP_HEAD + IP + URL.FETCH_PROCESS_MACHINE;
        Network.Instance(SinSimApp.getApp()).fetchProcessMachine(fetchProcessMachineUrl, mPostValue, new SelectProcessMachineHandler());
    }

    @SuppressLint("HandlerLeak")
    private class SelectProcessMachineHandler extends Handler{
        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == Network.OK) {
                ArrayList<MachineProcessData> processMachineList=(ArrayList<MachineProcessData>)msg.obj;
                if (processMachineList.size() != 1) {
                    ShowMessage.showDialog(ProcessToInstallActivity.this,"找不到该机器，请重新扫码！");
                } else {
                    Gson gson = new Gson();
                    ArrayList<TaskNodeData> taskNodeDataArrayList = gson.fromJson(processMachineList.get(0).getNodeData(), new TypeToken<ArrayList<TaskNodeData>>(){}.getType());
                    Log.d(TAG, "taskNodeDataArrayList: " + gson.toJson(taskNodeDataArrayList));
                    ArrayList<TaskNodeData> currentTaskList = new ArrayList<>();
                    int taskStatus = 0;
                    for (int index = 2; index < taskNodeDataArrayList.size(); index++){//去掉开始和结束
                        taskStatus = Integer.parseInt(taskNodeDataArrayList.get(index).getTaskStatus());
                        if (taskStatus > SinSimApp.TASK_PLANED && taskStatus != SinSimApp.TASK_QUALITY_DONE ){
                            currentTaskList.add(taskNodeDataArrayList.get(index));
                        }
                    }
                    if (currentTaskList.size()<1){
                        ShowMessage.showDialog(ProcessToInstallActivity.this,"该机器不在安装流程中！");
                    }else {
                        Intent intent = new Intent(ProcessToInstallActivity.this, ScanResultActivity.class);
                        intent.putExtra("currentTaskList", (Serializable) currentTaskList);//机器当前的安装步骤
                        intent.putExtra("mScanResultList", (Serializable) mScanResultList);//当前机器的信息，如果不在当前用户的安装列表中，则为空
                        intent.putExtra("mMachineNamePlate", mMachineNamePlate);
                        startActivity(intent);
                    }
                }
            }else {
                String errorMsg = (String)msg.obj;
                Log.d(TAG, "handleMessage: "+errorMsg);
                Toast.makeText(ProcessToInstallActivity.this,"网络问题，请重试!"+errorMsg,Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SCAN_QRCODE_START:
                if (resultCode == RESULT_OK)
                {
                    mScanResultList = new ArrayList<TaskRecordMachineListData>();
                    // 取出Intent里的扫码结果去当前安装组的待执行机器列表里面查找
                    mMachineNamePlate = data.getStringExtra("mMachineNamePlate");
                    Log.d(TAG, "onActivityResult: "+mMachineNamePlate);
                    for (int i=0;i<mProcessToInstallPlanList.size();i++){
                        if (mMachineNamePlate.equals(mProcessToInstallPlanList.get(i).getMachineData().getNameplate())){
                            mScanResultList.add(mProcessToInstallPlanList.get(i));
                        }
                    }
                    selectProcessMachine(mMachineNamePlate);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        if(mLoadingProcessDialog != null) {
            mLoadingProcessDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_logout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LinkedHashMap<String, String> mPostValue;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");// HH:mm:ss
        Date date;
        switch (item.getItemId()) {
//            case R.id.plan://后续计划
//                mPostValue = new LinkedHashMap<>();
//                mPostValue.put("installGroupName", ""+SinSimApp.getApp().getGroupName());
//                Calendar c = Calendar.getInstance();
//                c.add(Calendar.DAY_OF_MONTH,1);
//                String tomorrow = simpleDateFormat.format(c.getTime());
//                Log.d(TAG, "onCreate: "+tomorrow);
//                mPostValue.put("queryStartTime", tomorrow);
//
//                String fetchInstallPlanUrl = URL.HTTP_HEAD + SinSimApp.getApp().getServerIP() + URL.FATCH_INSTALL_PLAN;
//                Network.Instance(SinSimApp.getApp()).fetchInstallPlan(fetchInstallPlanUrl, mPostValue, new FetchInstallPlanHandler());
//                break;
//            case R.id.plan_actual://今日完成
//                mPostValue = new LinkedHashMap<>();
//                mPostValue.put("installGroupName", ""+SinSimApp.getApp().getGroupName());
//                date = new Date(System.currentTimeMillis());
//                mPostValue.put("isNotFinished", "true");
//                mPostValue.put("queryFinishTime", simpleDateFormat.format(date));
//
//                String fetchInstallPlanUrl2 = URL.HTTP_HEAD + SinSimApp.getApp().getServerIP() + URL.FATCH_INSTALL_PLAN;
//                Network.Instance(SinSimApp.getApp()).fetchInstallPlan(fetchInstallPlanUrl2, mPostValue, new FetchInstallActualHandler());
//                break;
            case R.id.attendance_settings://考勤信息
                mAttendanceData = new AttendanceData();
                mPostValue = new LinkedHashMap<>();
                mPostValue.put("installGroupName", ""+SinSimApp.getApp().getGroupName());
                date = new Date(System.currentTimeMillis());
                mPostValue.put("queryStartTime", simpleDateFormat.format(date));
                mPostValue.put("queryFinishTime", simpleDateFormat.format(date));
                String fetchAttendanceUrl = URL.HTTP_HEAD + IP + URL.FATCH_ATTENDANCE;
                Network.Instance(SinSimApp.getApp()).fetchAttendance(fetchAttendanceUrl, mPostValue, new FetchAttendanceHandler());
                break;
            case R.id.logout:
                stopService(mqttIntent);
                SinSimApp.getApp().setLogOut();
                Intent it = new Intent();
                it.setClass(ProcessToInstallActivity.this, LoginActivity.class);
                startActivity(it);
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("HandlerLeak")
    private class FetchInstallPlanHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Network.OK) {
                ArrayList<InstallPlanData> mInstallPlanList = (ArrayList<InstallPlanData>) msg.obj;
                Log.d(TAG, "handleMessage: "+(new Gson().toJson(mInstallPlanList)));

                if (mInstallPlanList.size()<1){
                    ShowMessage.showDialog(ProcessToInstallActivity.this,"尚未安排后续计划！");
                }else {
                    Intent intent=new Intent(ProcessToInstallActivity.this,InstallPlanActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("mInstallPlanList", (Serializable) mInstallPlanList);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        }
    }
    @SuppressLint("HandlerLeak")
    private class FetchInstallActualHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Network.OK) {
                ArrayList<InstallPlanData> mInstallPlanActualList = (ArrayList<InstallPlanData>) msg.obj;
                Log.d(TAG, "handleMessage: "+(new Gson().toJson(mInstallPlanActualList)));

                if (mInstallPlanActualList.size()<1){
                    ShowMessage.showDialog(ProcessToInstallActivity.this,"没有待完成计划！");
                }else {
                    Intent intent=new Intent(ProcessToInstallActivity.this,InstallActualActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("mInstallPlanActualList", (Serializable) mInstallPlanActualList);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private class FetchAttendanceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Network.OK) {
                Log.d(TAG, "handleMessage: "+(new Gson().toJson(msg.obj)));
                ArrayList<AttendanceData> attendanceDataArrayList = (ArrayList<AttendanceData>) msg.obj;
                if (attendanceDataArrayList.size()>0){
                    mAttendanceData = attendanceDataArrayList.get(0);
                    mAttendanceFlag = true;
                    AlertDialog continueDialog = new AlertDialog.Builder(ProcessToInstallActivity.this).create();
                    continueDialog.setTitle("考勤");
                    continueDialog.setMessage("今天传过考勤了");
                    continueDialog.setButton(AlertDialog.BUTTON_POSITIVE,"重新上传", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
                            mPostValue.put("id", "" + SinSimApp.getApp().getAppUserId());
                            String fetchInstallerListUrl = URL.HTTP_HEAD + IP + URL.FATCH_GROUP_BY_USERID;
                            Network.Instance(SinSimApp.getApp()).fetchInstallerList(fetchInstallerListUrl, mPostValue, new FetchInstallerGroupHandler());
                        }
                    });
                    continueDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"取消", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    continueDialog.show();
//                    ShowMessage.showDialog(ProcessToInstallActivity.this,"今天传过考勤了！");
                }else {
                    mAttendanceFlag = false;
                    LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
                    mPostValue.put("id", "" + SinSimApp.getApp().getAppUserId());
                    String fetchInstallerListUrl = URL.HTTP_HEAD + IP + URL.FATCH_GROUP_BY_USERID;
                    Network.Instance(SinSimApp.getApp()).fetchInstallerList(fetchInstallerListUrl, mPostValue, new FetchInstallerGroupHandler());
                }
            }
        }
    }
    @SuppressLint("HandlerLeak")
    private class FetchInstallerGroupHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {

            if (msg.what == Network.OK) {
                final ArrayList<UserData> mInstallerList = (ArrayList<UserData>) msg.obj;
                Log.d(TAG, "安装组人数: "+mInstallerList.size());

                AlertDialog attendanceSettingDialog = null;
                LinearLayout layout = (LinearLayout) View.inflate(ProcessToInstallActivity.this, R.layout.dialog_attendance_setting, null);
                TextView textViewTp = (TextView)layout.findViewById(R.id.total_population);
                final EditText editTextWp = (EditText)layout.findViewById(R.id.work_population);
                final EditText editTextOp = (EditText)layout.findViewById(R.id.overtime_population);
                final EditText editTextLp = (EditText)layout.findViewById(R.id.leave_population);
                final EditText editTextTwp = (EditText)layout.findViewById(R.id.tomorrow_work_population);
                
                attendanceSettingDialog = new AlertDialog.Builder(ProcessToInstallActivity.this).create();
                attendanceSettingDialog.setTitle("考勤信息");
                attendanceSettingDialog.setView(layout);
                textViewTp.setText(String.valueOf(mInstallerList.size()));
                if (mAttendanceFlag){
                    editTextWp.setText(mAttendanceData.getAttendanceMember());
                    editTextOp.setText(mAttendanceData.getOvertimeMember());
                    editTextLp.setText(mAttendanceData.getAbsenceMember());
                    editTextTwp.setText(mAttendanceData.getAttendanceTomorrow());
                }
                attendanceSettingDialog.setButton(AlertDialog.BUTTON_POSITIVE, "上传", new DialogInterface.OnClickListener() {
                    class CreateAttendenceHandler extends Handler {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            if (msg.what == Network.OK) {
                                ShowMessage.showToast(ProcessToInstallActivity.this,"考勤上传成功！",ShowMessage.MessageDuring.SHORT);
                            }else {
                                ShowMessage.showDialog(ProcessToInstallActivity.this,"出错！请检查网络！");
                            }
                        }
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (editTextWp.getText().toString().length()<1
                                    || editTextOp.getText().toString().length()<1
                                    || editTextLp.getText().toString().length()<1
                                    || editTextTwp.getText().toString().length()<1
                                    ){
                                ShowMessage.showDialog(ProcessToInstallActivity.this,"出错！请填写完整再上传！");
                            } else if (Integer.parseInt(editTextWp.getText().toString()) > mInstallerList.size()
                                    || Integer.parseInt(editTextOp.getText().toString()) > mInstallerList.size()
                                    || Integer.parseInt(editTextLp.getText().toString()) > mInstallerList.size()
                                    || Integer.parseInt(editTextTwp.getText().toString()) > mInstallerList.size()
                                    ){
                                ShowMessage.showDialog(ProcessToInstallActivity.this,"出错！人数不能大于实际人数！");
                            }else {
                                mAttendanceData.setAttendanceMember(editTextWp.getText().toString());
                                mAttendanceData.setOvertimeMember(editTextOp.getText().toString());
                                mAttendanceData.setAbsenceMember(editTextLp.getText().toString());
                                mAttendanceData.setAttendanceTomorrow(editTextTwp.getText().toString());
                                mAttendanceData.setUserId(SinSimApp.getApp().getAppUserId());
                                mAttendanceData.setInstallGroupId(SinSimApp.getApp().getGroupId());

                                LinkedHashMap<String, String> mPostValue = new LinkedHashMap<>();
                                mPostValue.put("attendance", new Gson().toJson(mAttendanceData));
                                if (mAttendanceFlag) {
                                    String updateAttendenceUrl = URL.HTTP_HEAD + IP + URL.UPDATE_ATTENDANCE;
                                    Network.Instance(SinSimApp.getApp()).updateProcessRecordData(updateAttendenceUrl, mPostValue, new CreateAttendenceHandler());
                                }else{
                                    String createAttendenceUrl = URL.HTTP_HEAD + IP + URL.CREATE_ATTENDANCE;
                                    Network.Instance(SinSimApp.getApp()).updateProcessRecordData(createAttendenceUrl, mPostValue, new CreateAttendenceHandler());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                attendanceSettingDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"取消", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                attendanceSettingDialog.show();
            } else {
                String errorMsg = (String)msg.obj;
                Log.d(TAG, "FetchInstallerGroupHandler handleMessage: "+errorMsg);
                ShowMessage.showToast(ProcessToInstallActivity.this,"网络错误！"+errorMsg, ShowMessage.MessageDuring.SHORT);
            }
        }
    }

}

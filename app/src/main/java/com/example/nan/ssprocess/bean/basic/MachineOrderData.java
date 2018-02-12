package com.example.nan.ssprocess.bean.basic;

import java.io.Serializable;

/**
 * Created by nan on 2017/12/18.
 */

public class MachineOrderData implements Serializable {
    private int headNum;//头数
    private int machineType;//类型
    private String contractShipDate;//合同日期
    private long planShipDate;//计划日期

    public int getHeadNum() {
        return headNum;
    }

    public int getMachineType() {
        return machineType;
    }

    public String getContractShipDate() {
        return contractShipDate;
    }

    public long getPlanShipDate() {
        return planShipDate;
    }
}

package com.jjg.game.hall.casino.data;

import com.jjg.game.hall.casino.pb.bean.CasinoEmploymentInfo;

/**
 * 雇员信息
 *
 * @author lm
 * @date 2025/8/19 14:11
 */
public class CasinoEmployment {
    //雇员id
    private long id;
    //雇员配置表id
    private int employmentId;
    //雇员结束时间
    private long employmentEndTime;
    //雇员开始时间
    private long employmentStartTime;


    public long getEmploymentStartTime() {
        return employmentStartTime;
    }

    public void setEmploymentStartTime(long employmentStartTime) {
        this.employmentStartTime = employmentStartTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getEmploymentId() {
        return employmentId;
    }

    public void setEmploymentId(int employmentId) {
        this.employmentId = employmentId;
    }

    public long getEmploymentEndTime() {
        return employmentEndTime;
    }

    public void setEmploymentEndTime(long employmentEndTime) {
        this.employmentEndTime = employmentEndTime;
    }

    public CasinoEmploymentInfo buildNewCasinoEmploymentInfo() {
        CasinoEmploymentInfo employmentInfo = new CasinoEmploymentInfo();
        employmentInfo.employmentId = employmentId;
        employmentInfo.employmentEndTime = employmentEndTime;
        return employmentInfo;
    }
}

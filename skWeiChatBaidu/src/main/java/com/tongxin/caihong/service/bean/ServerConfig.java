package com.tongxin.caihong.service.bean;

import java.util.List;

public class ServerConfig {
    private int robotModel; //机器人模式  1:人工客服+机器人模式   2: 纯机器人模式
    private List<Question> questions; // 常见问题
    private List<Menu> firstMnues;
    private ServiceInfo serviceInfo; // 客服信息，包含分配的客服id，及在线状态
    private int visitorModel;  //访客模式  1 仅限无身份访客   2 仅限有身份访客 3 均支持

    public List<Menu> getFirstMnues() {
        return firstMnues;
    }

    public void setFirstMnues(List<Menu> firstMnues) {
        this.firstMnues = firstMnues;
    }

    public int getRobotModel() {
        return robotModel;
    }

    public void setRobotModel(int robotModel) {
        this.robotModel = robotModel;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public int getVisitorModel() {
        return visitorModel;
    }

    public void setVisitorModel(int visitorModel) {
        this.visitorModel = visitorModel;
    }

    @Override
    public String toString() {
        return
                "ServerConfig{" +
                        "robotModel = '" + robotModel + '\'' +
                        ",questions = '" + questions + '\'' +
                        ",serviceInfo = '" + serviceInfo + '\'' +
                        ",visitorModel = '" + visitorModel + '\'' +
                        "}";
    }
}
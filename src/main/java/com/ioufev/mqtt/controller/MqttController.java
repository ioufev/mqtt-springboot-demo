package com.ioufev.mqtt.controller;

import com.ioufev.mqtt.domain.MyMessage;
import com.ioufev.mqtt.mqtt.MqttGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
public class MqttController {

    @Resource
    private MqttGateway mqttGateway;

    @PostMapping("/send")
    public String send(@RequestBody MyMessage myMessage) {
        // 发送消息到指定主题
        mqttGateway.sendToMqtt(myMessage.getTopic(), 1, myMessage.getContent());
        return "send topic: " + myMessage.getTopic()+ ", message : " + myMessage.getContent();
    }

}

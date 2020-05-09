package com.ww.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className MQTT_demo
 * @description TODO
 * @date 2020/5/9-16:55
 **/
public class MQTT_Publisher {
    public static void main(String[] args) {
        String broker = "tcp://120.24.103.229:1883";
        String clientId = "Publisher";
        //Use the memory persistence
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker:" + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            String topic = "demo/topics";
            int qos = 2;
            MqttMessage message = null;
            while (true){
                TimeUnit.SECONDS.sleep(1);
                String content = LocalDateTime.now().toString();
                message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.err.println("Message published：" + content);
            }
        } catch (MqttException me) {
            System.out.println("reason" + me.getReasonCode());
            System.out.println("msg" + me.getMessage());
            System.out.println("loc" + me.getLocalizedMessage());
            System.out.println("cause" + me.getCause());
            System.out.println("excep" + me);
            me.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

## springboot 实现 mqtt 客户端

### 接收消息

先配置连接

然后订阅主题

可以设置接收消息的内容格式：是否是字节

收到订阅的主题消息后，对应主题写响应处理


```java
    /**
     * 创建MqttPahoClientFactory，设置MQTT Broker连接属性，如果使用SSL验证，也在这里设置。
     * @return factory
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 设置代理端的URL地址，可以是多个
        options.setServerURIs(new String[]{"tcp://127.0.0.1:1883"});

        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * 入站通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * 入站
     */
    @Bean
    public MessageProducer inbound() {
        // Paho客户端消息驱动通道适配器，主要用来订阅主题
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("consumerClient-paho",
                mqttClientFactory(), "boat", "collector", "battery", "+/sensor");
        adapter.setCompletionTimeout(5000);

        // Paho消息转换器
        DefaultPahoMessageConverter defaultPahoMessageConverter = new DefaultPahoMessageConverter();
        // 按字节接收消息
//        defaultPahoMessageConverter.setPayloadAsBytes(true);
        adapter.setConverter(defaultPahoMessageConverter);
        adapter.setQos(1); // 设置QoS
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    // ServiceActivator注解表明：当前方法用于处理MQTT消息，inputChannel参数指定了用于消费消息的channel。
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            String payload = message.getPayload().toString();

            // byte[] bytes = (byte[]) message.getPayload(); // 收到的消息是字节格式
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();

            // 根据主题分别进行消息处理。
            if (topic.matches(".+/sensor")) { // 匹配：1/sensor
                String sensorSn = topic.split("/")[0];
                System.out.println("传感器" + sensorSn + ": 的消息： " + payload);
            } else if (topic.equals("collector")) {
                System.out.println("采集器的消息：" + payload);
            } else {
                System.out.println("丢弃消息：主题[" + topic  + "]，负载：" + payload);
            }

        };
    }
```

### 发送消息

先配置连接，使用相同的工厂类MqttPahoClientFactory，因为都是连接相同的Broker。

可以设置发送消息的内容格式：是否是字节

然后是供使用的MqttGateway接口

MqttConfig类

```java
/**
     * 出站通道
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * 出站
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler outbound() {

        // 发送消息和消费消息Channel可以使用相同MqttPahoClientFactory
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler("publishClient", mqttClientFactory());
        messageHandler.setAsync(true); // 如果设置成true，即异步，发送消息时将不会阻塞。
        messageHandler.setDefaultTopic("command");
        messageHandler.setDefaultQos(1); // 设置默认QoS

        // Paho消息转换器
        DefaultPahoMessageConverter defaultPahoMessageConverter = new DefaultPahoMessageConverter();

        // defaultPahoMessageConverter.setPayloadAsBytes(true); // 发送默认按字节类型发送消息
        messageHandler.setConverter(defaultPahoMessageConverter);
        return messageHandler;
    }
```

MqttGateway接口

```java
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {
    // 定义重载方法，用于消息发送
    void sendToMqtt(String payload);
    // 指定topic进行消息发送
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String payload);
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos, String payload);
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos, byte[] payload);
}
```


使用 MqttGateway接口



```java

    @Resource
    private MqttGateway mqttGateway;

    mqttGateway.sendToMqtt("主题", 1, "内容");

```

注意，有些类自动装配找不到接口的相应Bean，可以使用相关工具类获取Bean。 传入`MqttGateway.class`。

```java
private MqttGateway mqttGateway = SpringUtils.getBean(MqttGateway.class);
```

### 使用的相关工具

接口工具
http://api.crap.cn/

服务端：mosquitto
下载页面：https://mosquitto.org/download/

MQTTX
下载页面：https://mqttx.app/#download

MQTT.fx
下载链接：http://www.jensd.de/apps/mqttfx/1.7.1/mqttfx-1.7.1-windows-x64.exe

视频说明：https://www.bilibili.com/video/BV1qf4y1n7js/
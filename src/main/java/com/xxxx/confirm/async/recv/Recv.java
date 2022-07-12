package com.xxxx.confirm.async.recv;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * 确认模式-异步-消息消费者
 */
public class Recv {
    //定义队列
    private final static String QUEUE_NAME = "confirm_async";

    public static void main(String[] argv) throws Exception {
        //创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.31.52");
        factory.setUsername("yeb");
        factory.setVirtualHost("/yeb");
        factory.setPassword("yeb");
        factory.setPort(5672);
        //连接工厂创建连接
        Connection connection = factory.newConnection();
        //创建信道
        Channel channel = connection.createChannel();
        //绑定队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };
        //监听队列消费消息
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}
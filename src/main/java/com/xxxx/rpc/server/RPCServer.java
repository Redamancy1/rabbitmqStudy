package com.xxxx.rpc.server;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * RPC模式队列-服务端
 */
public class RPCServer {

    // 队列名称
    private static final String RPC_QUEUE_NAME = "rpc_queue";

    /**
     * 计算斐波那契数列
     *
     * @param n
     * @return
     */
    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public static void main(String[] args) {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.31.52");
        factory.setPort(5672);
        factory.setUsername("yeb");
        factory.setPassword("yeb");
        factory.setVirtualHost("/yeb");

        try {
            // 通过工厂创建连接
            final Connection connection = factory.newConnection();
            // 获取通道
            final Channel channel = connection.createChannel();
            // 声明队列
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            /*
                限制RabbitMQ只发不超过1条的消息给同一个消费者。
                当消息处理完毕后，有了反馈，才会进行第二次发送。
             */
            int prefetchCount = 1;
            channel.basicQos(prefetchCount);

            System.out.println(" [x] Awaiting RPC requests");

            Object monitor = new Object();
            // 获取消息
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                // 获取replyTo队列和correlationId请求标识
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";
                try {
                    // 接收客户端消息
                    String message = new String(delivery.getBody(), "UTF-8");
                    int n = Integer.parseInt(message);

                    System.out.println(" [.] fib(" + message + ")");
                    // 服务端根据业务需求处理
                    response += fib(n);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                } finally {
                    // 将处理结果发送至replyTo队列同时携带correlationId属性
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                            response.getBytes("UTF-8"));
                    // 手动回执消息
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    // RabbitMq消费者工作线程通知RPC服务器其他所有线程运行
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };
            // 监听队列
            /*
                autoAck = true代表自动确认消息
                autoAck = false代表手动确认消息
             */
            boolean autoAck = false;
            channel.basicConsume(RPC_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> {
            });
            // Wait and be prepared to consume the message from RPC client.
            // 线程等待并准备接收来自RPC客户端的消息
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
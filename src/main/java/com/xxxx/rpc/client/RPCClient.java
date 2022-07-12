package com.xxxx.rpc.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * RPC模式队列-客户端
 */
public class RPCClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    // 队列名称
    private String requestQueueName = "rpc_queue";

    // 初始化连接
    public RPCClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.31.52");
        factory.setPort(5672);
        factory.setUsername("yeb");
        factory.setPassword("yeb");
        factory.setVirtualHost("/yeb");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] args) {
        try (RPCClient fibonacciRpc = new RPCClient()) {
            for (int i = 0; i < 10; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [x] Requesting fib(" + i_str + ")");
                // 请求服务端
                String response = fibonacciRpc.call(i_str);
                System.out.println(" [.] Got '" + response + "'");
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 请求服务端
    public String call(String message) throws IOException, InterruptedException {
        // correlationId请求标识ID
        final String corrId = UUID.randomUUID().toString();

        // 获取队列名称
        String replyQueueName = channel.queueDeclare().getQueue();

        // 设置replyTo队列和correlationId请求标识
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        // 发送消息至队列
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        // 设置线程等待，每次只接收一个响应结果
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        // 接受服务器返回结果
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                // 将给定的元素在给定的时间内设置到线程队列中，如果设置成功返回true, 否则返回false
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        // 从线程队列中获取值，如果线程队列中没有值，线程会一直阻塞，直到线程队列中有值，并且取得该值
        String result = response.take();
        // 从消息队列中丢弃该值
        channel.basicCancel(ctag);
        return result;
    }

    // 关闭连接
    public void close() throws IOException {
        connection.close();
    }
}
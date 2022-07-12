package com.xxxx.topic.recv;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * 主题队列-消息消费者
 */
public class Recv02 {
	//定义队列
	private final static String EXCHANGE_NAME = "exchange_topic";

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

		//绑定交换机
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
		//获取队列(排他队列)
		String queueName = channel.queueDeclare().getQueue();
		//绑定队列和交换机
		String routingKey = "*.rabbit.*";
		channel.queueBind(queueName, EXCHANGE_NAME, routingKey);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [x] Received '" + message + "'");
		};
		//监听队列消费消息
		channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
		});
	}
}
package com.xxxx.work.fair.recv;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * 工作队列-公平-消息消费者
 */
public class Recv02 {
	//定义队列
	private final static String QUEUE_NAME = "work_fair";

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
		int prefetchCount = 1;
		//限制消费者每次只能接受一条消息，处理完才能接受下一条消息
		channel.basicQos(prefetchCount);
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			//模拟消费耗时
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [x] Received '" + message + "'");
			/**
			 * 手动确认
			 * multiple：是否确认多条
			 */
			channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

		};
		//监听队列消费消息
		channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
		});
	}
}
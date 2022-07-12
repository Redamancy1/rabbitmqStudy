package com.xxxx.topic.send;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * 主题队列-消息生产者
 */
public class Send {
	//定义交换机名称
	private final static String EXCHANGE_NAME = "exchange_topic";

	public static void main(String[] argv) throws Exception {
		//创建连接工厂
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("192.168.31.52");
		factory.setUsername("yeb");
		factory.setVirtualHost("/yeb");
		factory.setPassword("yeb");
		factory.setPort(5672);

		try (
				//连接工厂创建连接
				Connection connection = factory.newConnection();
				//创建信道
				Channel channel = connection.createChannel()) {
			//绑定交换机
			channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
			String infoMessage = "普通信息！";
			String errorMessage = "错误信息！";
			String warningMessage = "警告信息！";
			String infoRoutingKey = "info.message.orange";
			String errorRoutingKey = "error.rabbit.lazy";
			String warningRoutingKey = "orange.warning.message";
			//发送消息
			channel.basicPublish(EXCHANGE_NAME, infoRoutingKey, null, infoMessage.getBytes(StandardCharsets.UTF_8));
			channel.basicPublish(EXCHANGE_NAME, errorRoutingKey, null, errorMessage.getBytes(StandardCharsets.UTF_8));
			channel.basicPublish(EXCHANGE_NAME, warningRoutingKey, null, warningMessage.getBytes(StandardCharsets.UTF_8));
			System.out.println(" [x] Sent '" + infoMessage + "'");
			System.out.println(" [x] Sent '" + errorMessage + "'");
			System.out.println(" [x] Sent '" + warningMessage + "'");
		}
	}
}
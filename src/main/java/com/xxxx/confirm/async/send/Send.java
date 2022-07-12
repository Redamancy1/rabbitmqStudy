package com.xxxx.confirm.async.send;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

/**
 * 信道确认模式-异步-生产者
 */
public class Send {

	// 队列名称
	public static final String QUEUE_NAME = "confirm_async";

	public static void main(String[] args) {
		// 定义连接工厂
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("192.168.31.52");
		factory.setUsername("yeb");
		factory.setVirtualHost("/yeb");
		factory.setPassword("yeb");
		factory.setPort(5672);

		Connection connection = null;
		Channel channel = null;
		try {
			// 维护信息发送回执deliveryTag
			final SortedSet<Long> confirmSet=Collections.synchronizedSortedSet(new TreeSet<Long>());
			// 创建连接
			connection = factory.newConnection();
			// 获取通道
			channel = connection.createChannel();
			// 开启confirm确认模式
			channel.confirmSelect();
			// 声明队列
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			// 添加channel 监听
			channel.addConfirmListener(new ConfirmListener() {
				// 已确认
				@Override
				public void handleAck(long deliveryTag, boolean multiple) throws IOException {
					// multiple=true已确认多条 false已确认单条
					if (multiple) {
						System.out.println("handleAck--success-->multiple" + deliveryTag);
						// 清除前 deliveryTag 项标识id
						confirmSet.headSet(deliveryTag + 1L).clear();
					} else {
						System.out.println("handleAck--success-->single" + deliveryTag);
						confirmSet.remove(deliveryTag);
					}
				}

				// 未确认
				@Override
				public void handleNack(long deliveryTag, boolean multiple) throws IOException {
					// multiple=true未确认多条 false未确认单条
					if (multiple) {
						System.out.println("handleNack--failed-->multiple-->" + deliveryTag);
						// 清除前 deliveryTag 项标识id
						confirmSet.headSet(deliveryTag + 1L).clear();
					} else {
						System.out.println("handleNack--failed-->single" + deliveryTag);
						confirmSet.remove(deliveryTag);
					}
				}
			});
			// 循环发送消息演示消息确认
			while (true) {
				// 创建消息
				String message = "Hello World!";
				// 获取unconfirm的消息序号deliveryTag
				Long seqNo = channel.getNextPublishSeqNo();
				channel.basicPublish("", QUEUE_NAME, null, message.getBytes("utf-8"));
				// 将消息序号deliveryTag添加至SortedSet
				confirmSet.add(seqNo);
			}
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		} finally {
			try {
				// 关闭通道
				if (null != channel && channel.isOpen())
					channel.close();
				// 关闭连接
				if (null != connection && connection.isOpen())
					connection.close();
			} catch (TimeoutException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
/**
 * 
 */
package org.snova.http.client.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

import org.snova.http.client.HttpClientConnector;

/**
 * @author qiyingwang
 * 
 */
public class DefaultHttpConnector implements HttpClientConnector
{
	private EventLoopGroup loop;

	public DefaultHttpConnector(EventLoopGroup loop)
	{
		this.loop = loop;
	}

	@Override
	public ChannelFuture connect(String host, int port)
	{
		Bootstrap b = new Bootstrap();
		b.group(loop).channel(NioSocketChannel.class).remoteAddress(host, port)
		        .handler(new HttpClientCodec());
		return b.connect();
	}

}

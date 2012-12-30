/**
 * 
 */
package org.snova.http.client.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

import org.snova.http.client.Connector;

/**
 * @author qiyingwang
 * 
 */
public class DefaultHttpConnector implements Connector
{
	private EventLoopGroup	loop;
	
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

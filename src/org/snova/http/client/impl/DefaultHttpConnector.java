/**
 * 
 */
package org.snova.http.client.impl;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.snova.http.client.Connector;

/**
 * @author yinqiwen
 * 
 */
public class DefaultHttpConnector implements Connector
{
	private ClientBootstrap bootstrap;

	public DefaultHttpConnector(ClientBootstrap boot)
	{
		this.bootstrap = boot;
	}

	@Override
	public ChannelFuture connect(String host, int port)
	{
		return bootstrap.connect(new InetSocketAddress(host, port));
	}

}

/**
 * 
 */
package org.snova.http.client;

import io.netty.channel.ChannelFuture;

/**
 * @author qiyingwang
 *
 */
public interface HttpClientConnector
{
	public ChannelFuture connect(String host, int port);
}

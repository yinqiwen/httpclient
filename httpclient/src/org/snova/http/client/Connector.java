/**
 * 
 */
package org.snova.http.client;

import org.jboss.netty.channel.ChannelFuture;

/**
 * @author qiyingwang
 *
 */
public interface Connector
{
	public ChannelFuture connect(String host, int port);
}

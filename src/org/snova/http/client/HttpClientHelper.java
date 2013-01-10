/**
 * 
 */
package org.snova.http.client;

import org.snova.http.client.common.SimpleSocketAddress;


/**
 * @author yinqiwen
 * 
 */
public class HttpClientHelper
{
	public static SimpleSocketAddress getHttpRemoteAddress(boolean isHttps,
	        String hostport)
	{
		String[] ss = hostport.split(":");
		
		if (ss.length == 1)
		{
			if (isHttps)
			{
				return new SimpleSocketAddress(hostport, 443);
			}
			else
			{
				return new SimpleSocketAddress(hostport, 80);
			}
		}
		return new SimpleSocketAddress(ss[0], Integer.parseInt(ss[1]));
	}
}

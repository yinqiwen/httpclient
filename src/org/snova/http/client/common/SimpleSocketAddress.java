/**
 * This file is part of the hyk-proxy-framework project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: SimpleSocketAddress.java 
 *
 * @author wqy [ 2010-8-12 | 07:43:13 PM ]
 *
 */
package org.snova.http.client.common;

/**
 *
 */
public class SimpleSocketAddress
{

	public String	host;
	public int	  port;
	
	public SimpleSocketAddress()
	{
		
	}
	
	public SimpleSocketAddress(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	public String toString()
	{
		return host + ":" + port;
	}
	
	@Override
	public int hashCode()
	{
		return host.hashCode() + port;
	}
	
	@Override
	public boolean equals(Object anObject)
	{
		if (this == anObject)
		{
			return true;
		}
		if(null == anObject)
		{
			return false;
		}
		if (anObject instanceof SimpleSocketAddress)
		{
			SimpleSocketAddress anotherString = (SimpleSocketAddress) anObject;
			if (anotherString.host.equals(host) && anotherString.port == port)
			{
				return true;
			}
		}
		return false;
	}
}

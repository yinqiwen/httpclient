/**
 * 
 */
package org.snova.http.client;


import java.net.URL;

import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * @author qiyingwang
 *
 */
public interface ProxyCallback
{
	public URL getProxy(HttpRequest request);
}

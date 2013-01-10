/**
 * 
 */
package org.snova.http.client;


import java.net.URL;

import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * @author yinqiwen
 *
 */
public interface ProxyCallback
{
	public URL getProxy(HttpRequest request);
}

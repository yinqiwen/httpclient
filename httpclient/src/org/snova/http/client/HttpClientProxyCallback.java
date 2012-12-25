/**
 * 
 */
package org.snova.http.client;

import io.netty.handler.codec.http.HttpRequest;

import java.net.URL;

/**
 * @author qiyingwang
 *
 */
public interface HttpClientProxyCallback
{
	public URL getProxy(HttpRequest request);
}

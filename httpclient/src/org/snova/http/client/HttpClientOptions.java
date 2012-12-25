/**
 * 
 */
package org.snova.http.client;

/**
 * @author qiyingwang
 * 
 */
public class HttpClientOptions
{
	public int maxIdleConnsPerHost = 2;
	public HttpClientProxyCallback proxyCB;
	public HttpClientConnector connector;
}

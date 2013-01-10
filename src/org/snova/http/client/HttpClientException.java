/**
 * 
 */
package org.snova.http.client;

/**
 * @author yinqiwen
 * 
 */
public class HttpClientException extends Exception
{
	public HttpClientException(Throwable e)
	{
		super(e);
	}

	public HttpClientException(String message)
	{
		super(message);
	}
}

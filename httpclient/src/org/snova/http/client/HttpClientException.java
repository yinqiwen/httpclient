/**
 * 
 */
package org.snova.http.client;

/**
 * @author qiyingwang
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

/**
 * 
 */
package org.snova.http.client;

import io.netty.handler.codec.http.HttpChunk;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author qiyingwang
 * 
 */
public interface HttpClientCallback
{
	public void onResponse(HttpResponse res);
	public void onBody(HttpChunk chunk);
	public void onResponseComplete();
	public void onError(String error);
	
	public static class HttpClientCallbackAdapter implements HttpClientCallback
	{
		@Override
        public void onResponse(HttpResponse res)
        {
        }

		@Override
        public void onBody(HttpChunk chunk)
        {  
        }

		@Override
        public void onResponseComplete()
        {

        }

		@Override
        public void onError(String error)
        {
 
        }
		
	}
}

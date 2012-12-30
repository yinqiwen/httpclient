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
public interface FutureCallback
{
	public void onResponse(HttpResponse res);
	
	public void onBody(HttpChunk chunk);
	
	public void onComplete(HttpResponse res);
	
	public void onError(String error);
	
	public static class FutureCallbackAdapter implements FutureCallback
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
		public void onComplete(HttpResponse res)
		{
			
		}
		
		@Override
		public void onError(String error)
		{
			
		}
		
	}
}

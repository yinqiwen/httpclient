/**
 * 
 */
package org.snova.http.client;

import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;


/**
 * @author yinqiwen
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

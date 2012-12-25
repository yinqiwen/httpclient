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

	public void onError(String error);
}

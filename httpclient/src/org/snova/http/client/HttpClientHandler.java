/**
 * 
 */
package org.snova.http.client;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * @author qiyingwang
 * 
 */
public class HttpClientHandler extends SimpleChannelUpstreamHandler
{
	private HttpClient client;
	private FutureCallback callback;

	private boolean inPool;
	private boolean keepalive = true;
	private boolean readingChunks;
	private boolean answered;
	private HttpResponse clientResponse;
	ChannelFuture channelFuture;

	void setChannelFuture(ChannelFuture channelFuture)
	{
		this.channelFuture = channelFuture;
	}

	String remote;

	public HttpClientHandler(HttpClient client, String remote)
	{
		this.client = client;
		this.remote = remote;
	}

	void setCallback(FutureCallback future)
	{
		this.callback = future;
	}

	void setRequest(HttpRequest request)
	{
		keepalive = HttpHeaders.isKeepAlive(request);
		if(request.getMethod().equals(HttpMethod.CONNECT))
		{
			keepalive = false;
		}
	}


	public boolean writeBody(final HttpChunk chunk)
	{
		if (null == channelFuture)
		{
			return false;
		}
		channelFuture.addListener(new ChannelFutureListener()
		{

			@Override
			public void operationComplete(ChannelFuture future)
			        throws Exception
			{
				if (future.isDone())
				{
					channelFuture = channelFuture.getChannel().write(chunk);
				}
			}
		});
		return true;
	}

	public void closeChannel()
	{
		if (null != channelFuture)
		{
			channelFuture.getChannel().close();
			channelFuture = null;
		}
		inPool = false;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	        throws Exception
	{
		boolean resComplete = false;
		answered = true;
		FutureCallback cacheCB = this.callback;
		HttpResponse cacheRes = clientResponse;
		Object msg = e.getMessage();
		if (!readingChunks)
		{
			HttpResponse response = (HttpResponse) msg;
			clientResponse = response;
			cacheRes = clientResponse;
			if (keepalive)
			{
				keepalive = HttpHeaders.isKeepAlive(response);
			}
			if (response.isChunked())
			{
				readingChunks = true;
			}
			else
			{
				long length = HttpHeaders.getContentLength(response, -1);
				if (length >= 0
				        && response.getContent().readableBytes() == length)
				{
					if (keepalive)
					{
						inPool = client.putIdleConnection(remote, this);
					}
					resComplete = true;
				}
			}
			cacheCB.onResponse(response);
		}
		else
		{
			HttpChunk chunk = (HttpChunk) msg;
			callback.onBody(chunk);
			if (chunk.isLast())
			{
				readingChunks = false;
				if (keepalive)
				{
					inPool = client.putIdleConnection(remote, this);
				}
				resComplete = true;
			}
		}
		if (resComplete)
		{
			cacheCB.onComplete(cacheRes);
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
	        throws Exception
	{
		if (inPool)
		{
			client.removeIdleConnection(remote, this);
		}
		if (readingChunks || !answered)
		{
			callback.onError("Body not finished.");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	        throws Exception
	{
		ctx.getChannel().close();
	}

}

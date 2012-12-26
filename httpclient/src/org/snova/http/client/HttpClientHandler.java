/**
 * 
 */
package org.snova.http.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.HttpChunk;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author qiyingwang
 * 
 */
public class HttpClientHandler extends
        ChannelInboundMessageHandlerAdapter<Object>
{
	private HttpClient client;
	private HttpClientCallback callback;

	private boolean inPool;
	private boolean keepalive = true;
	private boolean readingChunks;
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

	void setCallback(HttpClientCallback future)
	{
		this.callback = future;
	}

	void setRequest(HttpRequest request)
	{
		keepalive = HttpHeaders.isKeepAlive(request);
	}

	public void removeHttpCodec()
	{
		if (null != channelFuture)
		{
			channelFuture.channel().pipeline().remove("codec");
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
					channelFuture = channelFuture.channel().write(chunk);
				}
			}
		});
		return true;
	}

	void close()
	{
		if (null != channelFuture)
		{
			channelFuture.channel().close();
			channelFuture = null;
		}
		inPool = false;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Object msg)
	        throws Exception
	{
		if (!readingChunks)
		{
			HttpResponse response = (HttpResponse) msg;
			if (keepalive)
			{
				keepalive = HttpHeaders.isKeepAlive(response);
			}

			if (response.getTransferEncoding().isMultiple())
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

				}
			}
			callback.onResponse(response);
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
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		super.channelInactive(ctx);
		if (inPool)
		{
			client.removeIdleConnection(remote, this);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	        throws Exception
	{
		ctx.close();
	}

}

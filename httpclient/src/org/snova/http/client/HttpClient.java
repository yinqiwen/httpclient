/**
 * 
 */
package org.snova.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snova.http.client.common.SimpleSocketAddress;
import org.snova.http.client.impl.DefaultHttpConnector;

/**
 * @author qiyingwang
 * 
 */
public class HttpClient
{
	private static Logger	                           logger	  = LoggerFactory
	                                                                      .getLogger(HttpClient.class);
	private EventLoopGroup	                           loop;
	
	private Map<String, LinkedList<HttpClientHandler>>	idleConns	= new HashMap<String, LinkedList<HttpClientHandler>>();
	private Options	                                   options;
	
	public HttpClient(Options options)
	{
		this(options, null);
	}
	
	public HttpClient(Options options, EventLoopGroup loop)
	{
		this.loop = loop;
		this.options = options;
		if (null == this.loop)
		{
			this.loop = new NioEventLoopGroup();
		}
		if (null == this.options)
		{
			this.options = new Options();
		}
		if (this.options.connector == null)
		{
			this.options.connector = new DefaultHttpConnector(this.loop);
		}
	}
	
	boolean putIdleConnection(String address, HttpClientHandler handler)
	{
		synchronized (idleConns)
		{
			LinkedList<HttpClientHandler> list = getIdleConnList(address);
			if (list.size() < options.maxIdleConnsPerHost)
			{
				list.addLast(handler);
				return true;
			}
		}
		handler.closeChannel();
		return false;
	}
	
	void removeIdleConnection(String address, HttpClientHandler handler)
	{
		synchronized (idleConns)
		{
			LinkedList<HttpClientHandler> list = getIdleConnList(address);
			list.remove(handler);
		}
		handler.closeChannel();
	}
	
	private LinkedList<HttpClientHandler> getIdleConnList(String address)
	{
		synchronized (idleConns)
		{
			LinkedList<HttpClientHandler> list = idleConns.get(address);
			if (null == list)
			{
				list = new LinkedList<HttpClientHandler>();
				idleConns.put(address, list);
				
			}
			return list;
		}
	}
	
	private HttpClientHandler getIdleConnection(String address)
	{
		synchronized (idleConns)
		{
			LinkedList<HttpClientHandler> list = getIdleConnList(address);
			if (!list.isEmpty())
			{
				
				return list.removeFirst();
			}
		}
		return null;
	}
	
	private void prepareHandler(boolean isHttps, ChannelFuture future,
	        HttpClientHandler handler) throws HttpClientException
	{
		if (future.channel().pipeline().get(HttpClientCodec.class) == null)
		{
			future.channel().pipeline().addLast("codec", new HttpClientCodec());
		}
		if (isHttps
		        && null == future.channel().pipeline().get(SslHandler.class))
		{
			SSLContext sslContext;
			try
			{
				sslContext = SSLContext.getDefault();
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new HttpClientException(e);
			}
			SSLEngine sslEngine = sslContext.createSSLEngine();
			sslEngine.setUseClientMode(true);
			future.channel().pipeline()
			        .addBefore("codec", "ssl", new SslHandler(sslEngine));
		}
		future.channel().pipeline().addLast("handler", handler);
		handler.setChannelFuture(future);
	}
	
	public HttpClientHandler doGet(String url, FutureCallback cb)
	        throws HttpClientException
	{
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
		        HttpMethod.GET, url);
		URL u;
		try
		{
			u = new URL(url);
		}
		catch (MalformedURLException e)
		{
			throw new HttpClientException(e);
		}
		request.setHeader("Host", u.getHost());
		return execute(request, cb);
	}
	
	public HttpClientHandler execute(final HttpRequest req,
	        final FutureCallback cb) throws HttpClientException
	{
		String remote = req.getHeader("Host");
		if (null == remote)
		{
			throw new HttpClientException("No Host header.");
		}
		URL proxy = null;
		boolean isHttps = req.getUri().startsWith("https://");
		if (null != options.proxyCB)
		{
			proxy = options.proxyCB.getProxy(req);
			if (null != proxy)
			{
				remote = proxy.getHost();
				if (proxy.getPort() > 0)
				{
					remote = proxy.getHost() + ":" + proxy.getPort();
				}
				isHttps = proxy.getProtocol().equalsIgnoreCase("https");
			}
			
		}
		SimpleSocketAddress address = HttpClientHelper.getHttpRemoteAddress(
		        isHttps, remote);
		HttpClientHandler handler = getIdleConnection(address.toString());
		if (null == handler)
		{
			handler = new HttpClientHandler(this, address.toString());
			handler.setRequest(req);
			ChannelFuture f = options.connector.connect(address.host,
			        address.port);
			prepareHandler(isHttps, f, handler);
		}
		handler.setCallback(cb);
		String url = req.getUri();
		if (proxy != null)
		{
			if (proxy.getUserInfo() != null)
			{
				ByteBuf buf = Unpooled.buffer();
				buf.writeBytes(proxy.getUserInfo().getBytes());
				req.setHeader("Proxy-Authorization", Base64.encode(buf)
				        .toString(Charset.forName("utf-8")));
			}
			if (url.indexOf("://") == -1)
			{
				req.setUri("http://" + req.getHeader("Host") + url);
			}
		}
		else
		{
			int idx = url.indexOf("://");
			if (idx != -1)
			{
				if (url.indexOf('/', idx + 3) != -1)
				{
					req.setUri(url.substring(url.indexOf('/', idx + 3)));
				}
				else
				{
					req.setUri("/");
				}
			}
		}
		handler.channelFuture.addListener(new ChannelFutureListener()
		{
			@Override
			public void operationComplete(ChannelFuture future)
			        throws Exception
			{
				if (!future.isSuccess())
				{
					cb.onError("###Write failed!");
				}
				else
				{
					future.channel().write(req);
				}
			}
		});
		return handler;
	}
	
}

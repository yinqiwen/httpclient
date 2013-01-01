/**
 * 
 */
package org.snova.http.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessageDecoder;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.ssl.SslHandler;
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
	
	private ClientBootstrap	                           bootstrap;
	private Map<String, LinkedList<HttpClientHandler>>	idleConns	= new HashMap<String, LinkedList<HttpClientHandler>>();
	private Options	                                   options;
	
	public HttpClient(Options options)
	{
		this(options, null);
	}
	
	public HttpClient(Options options, ClientBootstrap boot)
	{
		
		this.options = options;
		this.bootstrap = boot;
		if (null == this.options)
		{
			this.options = new Options();
		}
		if (null == bootstrap)
		{
			bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
			        Executors.newCachedThreadPool(),
			        Executors.newCachedThreadPool()));
		}
		if (this.options.connector == null)
		{
			this.options.connector = new DefaultHttpConnector(bootstrap);
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
			while (!list.isEmpty())
			{
				HttpClientHandler h = list.removeFirst();
				if (h.channelFuture.getChannel().isConnected())
				{
					return h;
				}
			}
		}
		return null;
	}
	
	private void prepareHandler(boolean sslEnable,
	        final HttpClientHandler handler) throws HttpClientException
	{
		ChannelFuture future = handler.channelFuture;
		
		if (future.getChannel().getPipeline().get(HttpClientCodec.class) == null)
		{
			future.getChannel().getPipeline()
			        .addLast("codec", new HttpClientCodec());
		}
		if (sslEnable
		        && null == future.getChannel().getPipeline()
		                .get(SslHandler.class))
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
			final SslHandler ssl = new SslHandler(sslEngine);
			future.getChannel().getPipeline().addBefore("codec", "ssl", ssl);
			handler.channelFuture = new DefaultChannelFuture(
			        future.getChannel(), false);
			future.addListener(new ChannelFutureListener()
			{
				public void operationComplete(ChannelFuture future)
				        throws Exception
				{
					if (future.isSuccess())
					{
						ssl.handshake().addListener(new ChannelFutureListener()
						{
							public void operationComplete(ChannelFuture future)
							        throws Exception
							{
								if (future.isSuccess())
								{
									handler.channelFuture.setSuccess();
								}
								else
								{
									handler.channelFuture.setFailure(future.getCause());
								}	
							}
						});
					}
					else
					{
						handler.channelFuture.setFailure(future.getCause());
					}
				}
			});
		}
		if (future.getChannel().getPipeline().get(HttpClientHandler.class) == null)
		{
			future.getChannel().getPipeline().addLast("handler", handler);
		}
		
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
	
	private String adjustProxyRequest(URL proxy, HttpRequest req)
	{
		String url = req.getUri();
		String pa = null;
		if (proxy != null)
		{
			HttpRequest proxyReq = req;
			if (proxy.getUserInfo() != null)
			{
				ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
				buf.writeBytes(proxy.getUserInfo().getBytes());
				pa = Base64.encode(buf).toString(Charset.forName("utf-8"));
				proxyReq.setHeader("Proxy-Authorization", pa);
			}
			
			if (url.indexOf("://") == -1
			        && !req.getMethod().equals(HttpMethod.CONNECT))
			{
				proxyReq.setUri("http://" + req.getHeader("Host") + url);
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
		return pa;
	}
	
	private void doRequest(final HttpClientHandler handler,
	        final HttpRequest req, final FutureCallback cb)
	{
		handler.setCallback(cb);
		handler.setRequest(req);
		handler.channelFuture.addListener(new ChannelFutureListener()
		{
			@Override
			public void operationComplete(ChannelFuture future)
			        throws Exception
			{
				if (!future.isSuccess())
				{
					cb.onError("###Connect failed!");
				}
				else
				{
					// SslHandler ssl = future.getChannel().getPipeline()
					// .get(SslHandler.class);
					// if(ssl.handshake())
					future.getChannel().write(req);
				}
			}
		});
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
		boolean sslEnable = req.getUri().startsWith("https://");
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
				sslEnable = proxy.getProtocol().equalsIgnoreCase("https");
			}
		}
		String cacheConnKey = remote;
		if (sslEnable)
		{
			cacheConnKey = remote;
		}
		SimpleSocketAddress address = HttpClientHelper.getHttpRemoteAddress(
		        sslEnable, remote);
		HttpClientHandler handler = getIdleConnection(cacheConnKey);
		boolean reuse = false;
		if (null == handler)
		{
			handler = new HttpClientHandler(this, cacheConnKey);
			handler.setRequest(req);
			ChannelFuture f = options.connector.connect(address.host,
			        address.port);
			handler.setChannelFuture(f);
			prepareHandler(sslEnable, handler);
		}
		else
		{
			reuse = true;
		}
		
		String pa = adjustProxyRequest(proxy, req);
		if (null != proxy && req.getUri().startsWith("https://")
		        && !req.getMethod().equals(HttpMethod.CONNECT) && !reuse)
		{
			String addr = HttpHeaders.getHost(req);
			final HttpRequest connReq = new DefaultHttpRequest(
			        HttpVersion.HTTP_1_1, HttpMethod.CONNECT, addr);
			connReq.setHeader(HttpHeaders.Names.HOST, addr);
			if (null != pa)
			{
				connReq.setHeader(HttpHeaders.Names.PROXY_AUTHORIZATION, pa);
			}
			final HttpClientHandler tmp = handler;
			handler.setCallback(new FutureCallback.FutureCallbackAdapter()
			{
				public void onResponse(HttpResponse res)
				{
					if (res.getStatus().getCode() != 200)
					{
						cb.onError("Failed to connect proxy server.");
						return;
					}
					try
					{
						tmp.channelFuture.getChannel().getPipeline()
						        .remove(HttpClientCodec.class);
						tmp.channelFuture.getChannel().getPipeline()
						        .remove(HttpClientHandler.class);
						prepareHandler(true, tmp);
					}
					catch (Exception e)
					{
						cb.onError(e.toString());
						e.printStackTrace();
						return;
					}
					doRequest(tmp, req, cb);
				}
				
				public void onError(String error)
				{
					cb.onError(error);
				}
			});
			handler.channelFuture.addListener(new ChannelFutureListener()
			{
				public void operationComplete(ChannelFuture future)
				        throws Exception
				{
					if (!future.isSuccess())
					{
						cb.onError("###Connect failed!");
					}
					else
					{
						future.getChannel().write(connReq);
						
					}
				}
			});
			return handler;
		}
		else
		{
			doRequest(handler, req, cb);
		}
		return handler;
	}
}

package org.snova.http.client;

import static org.junit.Assert.*;

import java.net.MalformedURLException;


import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Test;

public class HttpClientTest
{

	
	
	@Test
	public void testDoGet() throws HttpClientException, InterruptedException
	{
		HttpClient client = new HttpClient(null);
		client.doGet("http://www.qq.com/", new FutureCallback()
		{
			@Override
			public void onResponse(HttpResponse res)
			{
				System.out.println("####Recv response:" + res);

			}

			@Override
			public void onError(String error)
			{
				System.out.println("####Recv error:" + error);

			}

			@Override
			public void onBody(HttpChunk chunk)
			{
				System.out.println("####Recv chunk:"
				        + chunk.getContent().readableBytes());

			}

			@Override
			public void onComplete(HttpResponse res)
			{
				System.out.println("####onResponseComplete");

			}
		});
		Thread.sleep(2000);
		client.doGet("http://www.qq.com/path/asdf", new FutureCallback()
		{
			@Override
			public void onResponse(HttpResponse res)
			{
				System.out.println("####Recv response:" + res);
			}

			@Override
			public void onError(String error)
			{
				System.out.println("####Recv error:" + error);
			}

			@Override
			public void onBody(HttpChunk chunk)
			{
				System.out.println("####Recv chunk:"
				        + chunk.getContent().readableBytes());

			}

			@Override
			public void onComplete(HttpResponse res)
			{
				System.out.println("####onResponseComplete");

			}
		});

		Thread.sleep(2000);
	}

}

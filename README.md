httpclient
===========
httpclient is an async httpclient built on netty3.

## Use httpclient
Usage:

        HttpClient client = new HttpClient(null);
		client.doGet("http://www.google.com/", new FutureCallback()
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
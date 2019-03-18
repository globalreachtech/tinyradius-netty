/**
 * $Id: TestProxy.java,v 1.1 2005/09/07 22:19:01 wuttke Exp $
 * Created on 07.09.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.1 $
 */
package org.tinyradius.test.netty;

import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.dictionary.DictionaryParser;
import org.tinyradius.dictionary.MemoryDictionary;
import org.tinyradius.dictionary.WritableDictionary;
import org.tinyradius.netty.RadiusProxy;
import org.tinyradius.netty.RadiusServer;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Test proxy server.
 * Listens on localhost:1812 and localhost:1813. Proxies every access request
 * to localhost:10000 and every accounting request to localhost:10001.
 * You can use TestClient to ask this TestProxy and TestServer
 * with the parameters 10000 and 10001 as the target server.
 * Uses "testing123" as the shared secret for the communication with the
 * target server (localhost:10000/localhost:10001) and "proxytest" as the
 * shared secret for the communication with connecting clients.
 */
public class TestProxy<T extends DatagramChannel> extends RadiusProxy<T> {

	public TestProxy(EventExecutorGroup executorGroup, ChannelFactory<T> factory, Timer timer) {
		super(executorGroup, factory, timer);
	}

	public TestProxy(Dictionary dictionary, EventExecutorGroup executorGroup, ChannelFactory<T> factory, Timer timer) {
		super(dictionary, executorGroup, factory, timer);
	}

	public RadiusEndpoint getProxyServer(RadiusPacket packet,
			RadiusEndpoint client) {
		// always proxy
		try {
			InetAddress address = InetAddress.getByAddress(new byte[]{127,0,0,1});
			int port = 1812;
			if (packet instanceof AccountingRequest)
				port = 1813;
			return new RadiusEndpoint(new InetSocketAddress(address, port), "testing123");
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			return null;
		}
	}
	
	public String getSharedSecret(InetSocketAddress client) {
		if (client.getPort() == 1812 || client.getPort() == 1813)
			return "testing123";
		else if (client.getAddress().getHostAddress().equals("127.0.0.1"))
			return "proxytest";
		else
			return null;
	}
	
	public String getUserPassword(String userName) {
		// not used because every request is proxied
		return null;
	}

	public static void main(String[] args) throws IOException, Exception {

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		final NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);

		Dictionary dictionary = new MemoryDictionary();
		DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
				(WritableDictionary) dictionary);

		final TestProxy<NioDatagramChannel> proxy = new TestProxy<NioDatagramChannel>(dictionary,
				new DefaultEventExecutorGroup(4),
				new NioDatagramChannelFactory(),
				new HashedWheelTimer());

		proxy.setAuthPort(11812);
		proxy.setAcctPort(11813);
		proxy.setProxyPort(11814);

		Future<RadiusServer<NioDatagramChannel>> future =
				proxy.start(eventGroup, true, true, true);
		future.addListener(new GenericFutureListener<Future<? super RadiusServer<NioDatagramChannel>>>() {
		   public void operationComplete(Future<? super RadiusServer<NioDatagramChannel>> future) throws Exception {
			   if (future.isSuccess()) {
				   System.out.println("Server started.");
			   } else {
				   System.out.println("Failed to start server");
				   future.cause().printStackTrace();
			   }
		   	}
		});

		System.in.read();

		proxy.stop();

		eventGroup.shutdownGracefully()
				.awaitUninterruptibly();
	}

	private static class NioDatagramChannelFactory implements ChannelFactory<NioDatagramChannel> {
		public NioDatagramChannel newChannel() {
			return new NioDatagramChannel();
		}
	}
}

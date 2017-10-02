/**
 * $Id: RadiusClient.java,v 1.7 2005/11/10 10:20:21 wuttke Exp $
 * Created on 09.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.7 $
 */
package org.tinyradius.netty;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other. This object
 * is thread safe, but only opens a single socket so operations using this
 * socket are synchronized to avoid confusion with the mapping of request
 * and result packets.
 */
public class RadiusClient<T extends DatagramChannel> {

	private int retryCount = 3;
	private static Log logger = LogFactory.getLog(RadiusClient.class);

	private ChannelFactory<T> factory;
	private T channel = null;
	private EventLoopGroup eventGroup;
	private RadiusQueue queue = new RadiusQueueImpl();
	private Dictionary dictionary;

	/**
	 * Creates a new Radius client object for a special Radius server.
	 *
	 * @param dictionary
	 * @param eventGroup
	 * @param factory
	 */
	public RadiusClient(Dictionary dictionary, EventLoopGroup eventGroup, ChannelFactory<T> factory) {
		setChannelFactory(factory);
		setEventGroup(eventGroup);
		this.dictionary = dictionary;
	}


	/**
	 * Creates a new Radius client object for a special Radius server.
	 *
	 * @param eventGroup
	 * @param factory
	 */
	public RadiusClient(EventLoopGroup eventGroup, ChannelFactory<T> factory) {
		this(DefaultDictionary.getDefaultDictionary(), eventGroup, factory);
	}

	/**
	 * Closes the socket of this client.
	 */
	public void close() {
		if (channel != null)
			channel.close();
	}

	/**
	 *
	 * @param factory
	 */
	public void setChannelFactory(ChannelFactory<T> factory) {
		if (factory == null)
			throw new NullPointerException("factory cannot be null");
		this.factory = factory;
	}

	/**
	 *
	 * @param eventGroup
	 */
	public void setEventGroup(EventLoopGroup eventGroup) {
		if (eventGroup == null)
			throw new NullPointerException("group cannot be null");
		this.eventGroup = eventGroup;
	}

	/**
	 *
	 * @return
	 */
	public EventLoopGroup getEventGroup() {
		return eventGroup;
	}

	/**
	 *
	 * @return
	 */
	public ChannelFactory<T> getChannelFactory() {
		return this.factory;
	}

	/**
	 * Returns the retry count for failed transmissions.
	 * @return retry count
	 */
	public int getRetryCount() {
		return retryCount;
	}
	
	/**
	 * Sets the retry count for failed transmissions.
	 * @param retryCount retry count, >0
	 */
	public void setRetryCount(int retryCount) {
		if (retryCount < 1)
			throw new IllegalArgumentException("retry count must be positive");
		this.retryCount = retryCount;
	}

	/**
	 * Sends a Radius packet to the server and awaits an answer.
	 * @param request packet to be sent
	 * @param endpoint server endpoint
	 * @return response Radius packet
	 * @exception RadiusException malformed packet
	 * @exception IOException communication error (after getRetryCount()
	 * retries)
	 */
	public RadiusRequestPromise communicate(RadiusPacket request, RadiusEndpoint endpoint)
			throws IOException, RadiusException {

		RadiusQueueEntry queued = null;
		RadiusRequestPromise promise = null;

		try {
			RadiusRequestContextImpl context =
					new RadiusRequestContextImpl(request, endpoint);

			queued = queue.queue(context);
			promise = new DefaultRadiusRequestPromise(context, eventGroup.next());
			context.setPromise(promise);

			DatagramPacket packetOut = makeDatagramPacket(context.request(),
					context.endpoint());
			T channel = getChannel(); /* XXX: find available channel to send request */
			ChannelFuture f = channel.writeAndFlush(packetOut);

			if (logger.isInfoEnabled())
				logger.info(context.request().toString());

		} catch (Exception e) {
			if (queued != null)
				queue.dequeue(queued);
			promise.setFailure(e);
		}

		return promise;
	}

	/**
	 * Returns the socket used for the server communication. It is
	 * bound to an arbitrary free local port number.
	 * @return local socket
	 * @throws ChannelException
	 */
	protected T getChannel() throws ChannelException {
		if (channel == null) {
			channel = factory.newChannel();
			ChannelFuture future = eventGroup.register(channel);
			if (future.cause() != null)
				throw new ChannelException(future.cause());
			future.syncUninterruptibly();
			channel.bind(new InetSocketAddress(0));
			channel.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {

				public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
					RadiusQueueEntry queued = queue.lookup(packet);
					if (queued == null) {
						logger.info("Request context not found for received packet, ignoring...");
					} else {
						RadiusRequestContextImpl context =
								(RadiusRequestContextImpl)queued.context();
						try {
							context.setResponse(RadiusPacket.decodeResponsePacket(dictionary,
									new ByteBufInputStream(packet.content()),
									context.endpoint().getSharedSecret(), context.request()));

							if (logger.isInfoEnabled())
								logger.info(String.format("Received response: %s\nFor request: %s",
										context.response().toString(),
										context.request().toString()));

							context.promise().setSuccess(null);

						} catch (IOException ioe) {
							context.promise().setFailure(ioe);
						} catch (RadiusException re) {
							context.promise().setFailure(re);
						}
					}
				}

				public void channelReadComplete(ChannelHandlerContext ctx) {
					ctx.flush();
				}

			});
		}

		return channel;
	}
	
	/**
	 * Creates a datagram packet from a RadiusPacket to be send. 
	 * @param packet RadiusPacket
	 * @param endpoint destination port number
	 * @return new datagram packet
	 * @throws IOException
	 */
	protected DatagramPacket makeDatagramPacket(RadiusPacket packet, RadiusEndpoint endpoint)
	throws IOException {

		ByteBufOutputStream bos = new ByteBufOutputStream(Unpooled.buffer(
				RadiusPacket.MAX_PACKET_LENGTH, RadiusPacket.MAX_PACKET_LENGTH));
		packet.encodeRequestPacket(bos, endpoint.getSharedSecret());

		return new DatagramPacket(bos.buffer(), endpoint.getEndpointAddress());
	}

	private class RadiusRequestContextImpl implements RadiusRequestContext {

		private RadiusPacket request;
		private RadiusPacket response;
		private RadiusEndpoint endpoint;
		private RadiusRequestPromise promise;

		public RadiusRequestContextImpl(RadiusPacket request, RadiusEndpoint endpoint) {
			if (request == null)
				throw new NullPointerException("request cannot be null");
			if (endpoint == null)
				throw new NullPointerException("endpoint cannot be null");
			this.request = request;
			this.endpoint = endpoint;
		}

		public RadiusPacket request() {
			return request;
		}

		public RadiusPacket response() {
			return response;
		}

		public void setResponse(RadiusPacket response) {
			this.response = response;
		}

		public void setPromise(RadiusRequestPromise promise) {
			this.promise = promise;
		}

		public RadiusEndpoint endpoint() {
			return endpoint;
		}

		public RadiusRequestPromise promise() {
			return promise;
		}
	}
}

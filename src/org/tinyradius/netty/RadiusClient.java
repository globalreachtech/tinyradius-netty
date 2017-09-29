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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tinyradius.packet.AccessRequest;
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

	private ChannelFactory<T> factory;
	private T channel = null;
	private EventLoopGroup eventGroup;

	/**
	 * Creates a new Radius client object for a special Radius server.
	 *
	 * @param eventGroup
	 * @param factory
	 */
	public RadiusClient(EventLoopGroup eventGroup, ChannelFactory<T> factory) {
		setChannelFactory(factory);
		setEventGroup(eventGroup);
	}

	/**
	 * Authenticates a user.
	 *
	 * @param userName user name
	 * @param password password
	 * @throws RadiusException malformed packet
	 * @throws IOException     communication error (after getRetryCount()
	 *                         retries)
	 */
	public synchronized void authenticate(String userName, String password, RadiusEndpoint endpoint, CallbackHandler handler)
			throws IOException, RadiusException {

		AccessRequest request = new AccessRequest(userName, password);

		if (logger.isInfoEnabled())
			logger.info("send Access-Request packet: " + request);

		RadiusPacket response = communicate(request, endpoint, handler);
		if (logger.isInfoEnabled())
			logger.info("received packet: " + response);
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
	public RadiusPacket communicate(RadiusPacket request, RadiusEndpoint endpoint, CallbackHandler handler)
			throws IOException, RadiusException {

		DatagramPacket packetOut = makeDatagramPacket(request, endpoint);

		System.out.println(request.toString());

		T channel = getChannel(); /* XXX: find available channel to send request */
		ChannelFuture f = channel.writeAndFlush(packetOut);

		//f.syncUninterruptibly();

		/*
		for (int i = 1; i <= getRetryCount(); i++) {
			try {
				channel.write(packetOut);
				socket.receive(packetIn);
				return makeRadiusPacket(packetIn, request);
			} catch (IOException ioex) {
				if (i == getRetryCount()) {
					if (logger.isErrorEnabled()) {
						if (ioex instanceof SocketTimeoutException)
							logger.error("communication failure (timeout), no more retries");
						else
							logger.error("communication failure, no more retries", ioex);
					}
					throw ioex;
				}
				if (logger.isInfoEnabled())
					logger.info("communication failure, retry " + i);
				// TODO increase Acct-Delay-Time by getSocketTimeout()/1000
				// this changes the packet authenticator and requires packetOut to be
				// calculated again (call makeDatagramPacket)
            }
        }
        */
		
		return null;
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
					try {

						/* XXX: lookup shared secret for received packet */

						RadiusPacket recevied = makeRadiusPacket(packet, "testing123"); /* XXX: Change hard coded */
						System.out.println(recevied.toString());

					} catch (RadiusException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
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
	
	/**
	 * Creates a RadiusPacket from a received datagram packet.
	 * @param packet received datagram
	 * @param request Radius request packet
	 * @return RadiusPacket object
	 */
	protected RadiusPacket makeRadiusPacket(DatagramPacket packet, RadiusPacket request, String sharedSecret)
			throws IOException, RadiusException {
		return RadiusPacket.decodeResponsePacket(
				new ByteBufInputStream(packet.content()), sharedSecret, request);
	}

	/**
	 * Creates a RadiusPacket from a received datagram packet.
	 * @param packet received datagram
	 * @return RadiusPacket object
	 */
	protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret)
			throws IOException, RadiusException {
		return RadiusPacket.decodeRequestPacket(
				new ByteBufInputStream(packet.content()), sharedSecret);
	}

	private int retryCount = 3;
	private static Log logger = LogFactory.getLog(RadiusClient.class);

	/**
	 *
	 */
	public interface CallbackHandler {
		/**
		 *
		 * @param response
		 */
		public void response(RadiusPacket response);

		/**
		 *
		 * @param e
		 */
		public void error(Exception e);
	}
}

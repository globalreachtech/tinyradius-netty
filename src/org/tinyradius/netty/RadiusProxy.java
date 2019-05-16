/**
 * $Id: RadiusProxy.java,v 1.1 2005/09/07 22:19:01 wuttke Exp $
 * Created on 07.09.2005
 * @author glanz, Matthias Wuttke
 * @version $Revision: 1.1 $
 */
package org.tinyradius.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;
import io.netty.util.concurrent.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.proxy.RadiusProxyConnection;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements a Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * You have to override the method getRadiusProxyConnection() which
 * identifies the Radius proxy connection a Radius packet belongs to.
 */
public abstract class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> {

    /**
     * {@inheritDoc}
	 */
	public RadiusProxy(EventExecutorGroup executorGroup, ChannelFactory<T> factory, Timer timer) {
		super(executorGroup, factory, timer);
	}

	/**
	 * {@inheritDoc}
	 */
	public RadiusProxy(Dictionary dictionary, EventExecutorGroup executorGroup, ChannelFactory<T> factory, Timer timer) {
		super(dictionary, executorGroup, factory, timer);
	}

	/**
	 * Starts the Radius proxy. Listens on the proxy port.
	 */
	public Future<RadiusServer<T>> start(EventLoopGroup eventGroup, boolean listenProxy) {

		final Promise<RadiusServer<T>> promise =
				new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

		Future<RadiusServer<T>> future = super.start(eventGroup);
		future.addListener(future1 -> {
			if (!future1.isSuccess()) {
				promise.setFailure(future1.cause());
			} else {
				listenProxy().addListeners((ChannelFutureListener) channelFuture -> {
					if (!channelFuture.isSuccess()) {
						promise.setFailure(channelFuture.cause());
					} else {
						promise.setSuccess(RadiusProxy.this);
					}
				});
			}
		});

		return promise;
	}
	
    /**
     * Stops the proxy and closes the socket.
     */
    public void stop() {
    	logger.info("stopping Radius proxy");
    	if (proxySocket != null)
    		proxySocket.close();
    	super.stop();
    }

	/**
	 * Listens on the proxy port (blocks the current thread).
	 * Returns when stop() is called.
	 * @return ChannelFuture
	 *
	 */
	protected ChannelFuture listenProxy() {
		logger.info("starting RadiusProxyListener on port " + getProxyPort());
		return listen(getProxySocket(), new InetSocketAddress(getListenAddress(), getProxyPort()));
	}

    /**
     * This method must be implemented to return a RadiusEndpoint
     * if the given packet is to be proxied. The endpoint represents the
     * Radius server the packet should be proxied to.
     * @param packet the packet in question
     * @param client the client endpoint the packet originated from
     * (containing the address, port number and shared secret)
     * @return a RadiusEndpoint or null if the packet should not be
     * proxied
     */
    public abstract RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client);
	
    /**
	 * Returns the proxy port this server listens to.
	 * Defaults to 1814.
	 * @return proxy port
	 */
	public int getProxyPort() {
		return proxyPort;
	}
	
	/**
	 * Sets the proxy port this server listens to.
	 * Please call before start().
	 * @param proxyPort proxy port
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
		this.proxySocket = null;
	}

	/**
	 * Returns a socket bound to the proxy port.
	 * @return socket
	 */
	protected T getProxySocket() {
		if (proxySocket == null) {
			proxySocket = factory().newChannel();
		}
		return proxySocket;
	}
	
	/**
	 * Handles packets coming in on the proxy port. Decides whether
	 * packets coming in on Auth/Acct ports should be proxied.
	 */
	protected RadiusPacket handlePacket(InetSocketAddress localAddress, InetSocketAddress remoteAddress, RadiusPacket request, String sharedSecret)
	throws RadiusException, IOException {
		// handle incoming proxy packet
		if (localAddress.getPort() == getProxyPort()) {
			proxyPacketReceived(request, remoteAddress);
			return null;
		}
		
		// handle auth/acct packet
		RadiusEndpoint radiusClient = new RadiusEndpoint(remoteAddress, sharedSecret);
		RadiusEndpoint radiusServer = getProxyServer(request, radiusClient);
		if (radiusServer != null) {
			// proxy incoming packet to other radius server
			RadiusProxyConnection proxyConnection = new RadiusProxyConnection(radiusServer, radiusClient, request, localAddress.getPort());
			logger.info("proxy packet to " + proxyConnection.getRadiusServer().getEndpointAddress());
			proxyPacket(request, proxyConnection);
			return null;
		} else
			// normal processing
			return super.handlePacket(localAddress, remoteAddress, request, sharedSecret);
	}
    
    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     * @param packet packet to be sent back
     * @param remote the server the packet arrived from
     * @throws IOException
     */
    protected void proxyPacketReceived(RadiusPacket packet, InetSocketAddress remote) throws IOException, RadiusException {
    	// retrieve my Proxy-State attribute (the last)
    	List<RadiusAttribute> proxyStates = packet.getAttributes(33);
    	if (proxyStates == null || proxyStates.size() == 0)
    		throw new RadiusException("proxy packet without Proxy-State attribute");
    	RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);
    	
    	// retrieve proxy connection from cache 
    	String state = new String(proxyState.getAttributeData());
        RadiusProxyConnection proxyConnection = proxyConnections.remove(state);
    	if (proxyConnection == null) {
    		logger.warn("received packet on proxy port without saved proxy connection - duplicate?");
    		return;
    	}
    	
    	// retrieve client
    	RadiusEndpoint client = proxyConnection.getRadiusClient();
       	if (logger.isInfoEnabled()) {
       		logger.info("received proxy packet: " + packet);
       		logger.info("forward packet to " + client.getEndpointAddress().toString() + " with secret " + client.getSharedSecret());
       	}
       	
       	// remove only own Proxy-State (last attribute)
       	packet.removeLastAttribute(33);

       	// re-encode answer packet with authenticator of the original packet
       	RadiusPacket answer = new RadiusPacket(packet.getPacketType(), packet.getPacketIdentifier(), packet.getAttributes());

		DatagramPacket datagram = makeDatagramPacket(answer, client.getSharedSecret(), client.getEndpointAddress(), proxyConnection.getPacket());
        
       	// send back using correct socket
       	T socket;
       	if (proxyConnection.getPort() == getAuthPort())
       		socket = getAuthSocket();
        else
        	socket = getAcctSocket();
       	socket.writeAndFlush(datagram);
    }

    /**
     * Proxies the given packet to the server given in the proxy connection.
     * Stores the proxy connection object in the cache with a key that
     * is added to the packet in the "Proxy-State" attribute.
     * @param packet the packet to proxy
     * @param proxyConnection the RadiusProxyConnection for this packet
     * @throws IOException
     */
    protected void proxyPacket(RadiusPacket packet, RadiusProxyConnection proxyConnection)
    throws IOException, RadiusException {
		// add Proxy-State attribute
		String proxyIndexStr = Integer.toString(proxyIndex.getAndIncrement());
		packet.addAttribute(new RadiusAttribute(33, proxyIndexStr.getBytes()));

		// store RadiusProxyConnection object
		proxyConnections.put(proxyIndexStr, proxyConnection);

        // get server address
        InetAddress serverAddress = proxyConnection.getRadiusServer().getEndpointAddress().getAddress();
        int serverPort = proxyConnection.getRadiusServer().getEndpointAddress().getPort();
        String serverSecret = proxyConnection.getRadiusServer().getSharedSecret();

        // save request authenticator (will be calculated new)
    	byte[] auth = packet.getAuthenticator();

    	// encode new packet (with new authenticator)
		ByteBuf buf = Unpooled.buffer(RadiusPacket.MAX_PACKET_LENGTH,
				RadiusPacket.MAX_PACKET_LENGTH);
		ByteBufOutputStream bos = new ByteBufOutputStream(buf);
		packet.encodeRequestPacket(bos, serverSecret);

		DatagramPacket datagram = new DatagramPacket(buf, new InetSocketAddress(serverAddress, serverPort));

        // restore original authenticator
        packet.setAuthenticator(auth);

		// send packet
    	T proxySocket = getProxySocket();
        proxySocket.writeAndFlush(datagram);
    }

	/**
	 * Index for Proxy-State attribute.
	 */
	private AtomicInteger proxyIndex = new AtomicInteger(1);
	
	/**
	 * Cache for Radius proxy connections belonging to sent packets
	 * without a received response.
	 * Key: Proxy Index (String), Value: RadiusProxyConnection
	 */ 
	private Map<String, RadiusProxyConnection> proxyConnections =
			new ConcurrentHashMap<>();

	private int proxyPort = 1814;
	private T proxySocket = null;
	private static Log logger = LogFactory.getLog(RadiusProxy.class);
	
}

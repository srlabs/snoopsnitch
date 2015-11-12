package de.srlabs.snoopsnitch.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Vector;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ForceTLSSocketFactory extends SSLSocketFactory {
	SSLSocketFactory delegate = null;
	public ForceTLSSocketFactory(SSLSocketFactory delegate) {
		super();
		this.delegate = delegate;
	}
	private Socket forceTLS(SSLSocket s){
		return new SSLSocketWrapper(s);
	}
	class SSLSocketWrapper extends SSLSocket{
		SSLSocket delegate;

		public SSLSocketWrapper(SSLSocket delegate) {
			super();
			this.delegate = delegate;
		}

		public void shutdownInput() throws IOException {
			delegate.shutdownInput();
		}

		public void shutdownOutput() throws IOException {
			delegate.shutdownOutput();
		}

		public String[] getSupportedCipherSuites() {
			return delegate.getSupportedCipherSuites();
		}

		public String[] getEnabledCipherSuites() {
			return delegate.getEnabledCipherSuites();
		}

		public void setEnabledCipherSuites(String[] suites) {
			delegate.setEnabledCipherSuites(suites);
		}

		public String[] getSupportedProtocols() {
			return delegate.getSupportedProtocols();
		}

		public String[] getEnabledProtocols() {
			return delegate.getEnabledProtocols();
		}

		public void setEnabledProtocols(String[] protocols) {
			delegate.setEnabledProtocols(protocols);
		}

		public SSLSession getSession() {
			return delegate.getSession();
		}

		public void addHandshakeCompletedListener(
				HandshakeCompletedListener listener) {
			delegate.addHandshakeCompletedListener(listener);
		}

		public void removeHandshakeCompletedListener(
				HandshakeCompletedListener listener) {
			delegate.removeHandshakeCompletedListener(listener);
		}

		public boolean equals(Object o) {
			return delegate.equals(o);
		}

		/**
		 * Force the protocol to TLS directly before doing the handshake so that
		 * the Android system has no chance of doing a fallback to SSLv3 (some
		 * phones do that if the TLS connection before fails e.g. due to a
		 * timeout).
		 */
		public void startHandshake() throws IOException {
			Vector<String> protocolsToEnable = new Vector<String>();
			for(String protocol: delegate.getSupportedProtocols()){
				if(protocol.contains("TLS"))
					protocolsToEnable.add(protocol);
			}
			if(protocolsToEnable.isEmpty()) // Just in case getSupportedProtocols doesn't return anything
				protocolsToEnable.add("TLSv1");
			delegate.setEnabledProtocols(protocolsToEnable.toArray(new String[protocolsToEnable.size()]));
			delegate.startHandshake();
		}

		public void setUseClientMode(boolean mode) {
			delegate.setUseClientMode(mode);
		}

		public boolean getUseClientMode() {
			return delegate.getUseClientMode();
		}

		public void setNeedClientAuth(boolean need) {
			delegate.setNeedClientAuth(need);
		}

		public void setWantClientAuth(boolean want) {
			delegate.setWantClientAuth(want);
		}

		public boolean getNeedClientAuth() {
			return delegate.getNeedClientAuth();
		}

		public boolean getWantClientAuth() {
			return delegate.getWantClientAuth();
		}

		public void setEnableSessionCreation(boolean flag) {
			delegate.setEnableSessionCreation(flag);
		}

		public boolean getEnableSessionCreation() {
			return delegate.getEnableSessionCreation();
		}

		public SSLParameters getSSLParameters() {
			return delegate.getSSLParameters();
		}

		public void setSSLParameters(SSLParameters p) {
			delegate.setSSLParameters(p);
		}

		public void close() throws IOException {
			delegate.close();
		}

		public InetAddress getInetAddress() {
			return delegate.getInetAddress();
		}

		public InputStream getInputStream() throws IOException {
			return delegate.getInputStream();
		}

		public boolean getKeepAlive() throws SocketException {
			return delegate.getKeepAlive();
		}

		public InetAddress getLocalAddress() {
			return delegate.getLocalAddress();
		}

		public int getLocalPort() {
			return delegate.getLocalPort();
		}

		public OutputStream getOutputStream() throws IOException {
			return delegate.getOutputStream();
		}

		public int getPort() {
			return delegate.getPort();
		}

		public int getSoLinger() throws SocketException {
			return delegate.getSoLinger();
		}

		public int getReceiveBufferSize() throws SocketException {
			return delegate.getReceiveBufferSize();
		}

		public int getSendBufferSize() throws SocketException {
			return delegate.getSendBufferSize();
		}

		public int getSoTimeout() throws SocketException {
			return delegate.getSoTimeout();
		}

		public boolean getTcpNoDelay() throws SocketException {
			return delegate.getTcpNoDelay();
		}

		public void setKeepAlive(boolean keepAlive) throws SocketException {
			delegate.setKeepAlive(keepAlive);
		}

		public void setSendBufferSize(int size) throws SocketException {
			delegate.setSendBufferSize(size);
		}

		public void setReceiveBufferSize(int size) throws SocketException {
			delegate.setReceiveBufferSize(size);
		}

		public void setSoLinger(boolean on, int timeout) throws SocketException {
			delegate.setSoLinger(on, timeout);
		}

		public void setSoTimeout(int timeout) throws SocketException {
			delegate.setSoTimeout(timeout);
		}

		public void setTcpNoDelay(boolean on) throws SocketException {
			delegate.setTcpNoDelay(on);
		}

		public String toString() {
			return delegate.toString();
		}

		public SocketAddress getLocalSocketAddress() {
			return delegate.getLocalSocketAddress();
		}

		public SocketAddress getRemoteSocketAddress() {
			return delegate.getRemoteSocketAddress();
		}

		public boolean isBound() {
			return delegate.isBound();
		}

		public boolean isConnected() {
			return delegate.isConnected();
		}

		public boolean isClosed() {
			return delegate.isClosed();
		}

		public void bind(SocketAddress localAddr) throws IOException {
			delegate.bind(localAddr);
		}

		public void connect(SocketAddress remoteAddr) throws IOException {
			delegate.connect(remoteAddr);
		}

		public void connect(SocketAddress remoteAddr, int timeout)
				throws IOException {
			delegate.connect(remoteAddr, timeout);
		}

		public boolean isInputShutdown() {
			return delegate.isInputShutdown();
		}

		public boolean isOutputShutdown() {
			return delegate.isOutputShutdown();
		}

		public void setReuseAddress(boolean reuse) throws SocketException {
			delegate.setReuseAddress(reuse);
		}

		public boolean getReuseAddress() throws SocketException {
			return delegate.getReuseAddress();
		}

		public void setOOBInline(boolean oobinline) throws SocketException {
			delegate.setOOBInline(oobinline);
		}

		public boolean getOOBInline() throws SocketException {
			return delegate.getOOBInline();
		}

		public void setTrafficClass(int value) throws SocketException {
			delegate.setTrafficClass(value);
		}

		public int getTrafficClass() throws SocketException {
			return delegate.getTrafficClass();
		}

		public void sendUrgentData(int value) throws IOException {
			delegate.sendUrgentData(value);
		}

		public SocketChannel getChannel() {
			return delegate.getChannel();
		}

		public void setPerformancePreferences(int connectionTime, int latency,
				int bandwidth) {
			delegate.setPerformancePreferences(connectionTime, latency,
					bandwidth);
		}
		
	}
	public Socket createSocket() throws IOException {
		return forceTLS((SSLSocket) delegate.createSocket());
	}

	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		return forceTLS((SSLSocket) delegate.createSocket(host, port));
	}

	public String[] getDefaultCipherSuites() {
		return delegate.getDefaultCipherSuites();
	}

	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		return forceTLS((SSLSocket) delegate.createSocket(host, port, localHost, localPort));
	}

	public String[] getSupportedCipherSuites() {
		return delegate.getSupportedCipherSuites();
	}

	public Socket createSocket(Socket s, String host, int port,
			boolean autoClose) throws IOException {
		return forceTLS((SSLSocket) delegate.createSocket(s, host, port, autoClose));
	}

	public Socket createSocket(InetAddress host, int port) throws IOException {
		return forceTLS((SSLSocket) delegate.createSocket(host, port));
	}

	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		return forceTLS((SSLSocket) delegate.createSocket(address, port, localAddress, localPort));
	}
}

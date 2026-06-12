package dev.krotname.networkchat.network;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Socket factory helpers for plain TCP and optional JSSE TLS mode. */
public final class ChatSockets {
  private static final String TLS_PROTOCOL = "TLS";

  private ChatSockets() {}

  public static ServerSocket openServerSocket(ChatServerConfig config) throws IOException {
    if (!config.tls().enabled()) {
      return new ServerSocket(config.port());
    }
    try {
      ServerSocketFactory factory = serverSslContext(config.tls()).getServerSocketFactory();
      return factory.createServerSocket(config.port());
    } catch (GeneralSecurityException ex) {
      throw new IOException("Unable to initialize TLS server socket", ex);
    }
  }

  public static Socket openClientSocket(String host, int port, TlsClientConfig tlsConfig)
      throws IOException {
    if (!tlsConfig.enabled()) {
      return new Socket(host, port);
    }
    try {
      SocketFactory factory = clientSslContext(tlsConfig).getSocketFactory();
      return factory.createSocket(host, port);
    } catch (GeneralSecurityException ex) {
      throw new IOException("Unable to initialize TLS client socket", ex);
    }
  }

  private static SSLContext serverSslContext(TlsServerConfig config)
      throws IOException, GeneralSecurityException {
    KeyStore keyStore = loadKeyStore(config.keyStoreFile(), config.keyStorePassword());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, config.keyPassword().toCharArray());
    SSLContext context = SSLContext.getInstance(TLS_PROTOCOL);
    context.init(keyManagerFactory.getKeyManagers(), null, null);
    return context;
  }

  private static SSLContext clientSslContext(TlsClientConfig config)
      throws IOException, GeneralSecurityException {
    SSLContext context = SSLContext.getInstance(TLS_PROTOCOL);
    context.init(null, trustManagers(config), null);
    return context;
  }

  private static TrustManager[] trustManagers(TlsClientConfig config)
      throws IOException, GeneralSecurityException {
    if (config.trustAllCertificates()) {
      return new TrustManager[] {new InsecureTrustAllManager()};
    }
    if (config.trustStoreFile() == null) {
      return null;
    }
    KeyStore trustStore = loadKeyStore(config.trustStoreFile(), config.trustStorePassword());
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    return trustManagerFactory.getTrustManagers();
  }

  private static KeyStore loadKeyStore(Path file, String password)
      throws IOException, GeneralSecurityException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream input = Files.newInputStream(file)) {
      keyStore.load(input, password.toCharArray());
    }
    return keyStore;
  }

  @SuppressFBWarnings(
      value = "WEAK_TRUST_MANAGER",
      justification = "Explicit opt-in development mode; production docs require a truststore.")
  private static final class InsecureTrustAllManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[0];
    }
  }
}

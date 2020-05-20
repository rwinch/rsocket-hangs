package sample.rsocket;

import java.net.InetSocketAddress;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatCode;

class RsocketApplicationTests {

	SocketAcceptorInterceptor interceptor = rsocketInterceptor();

	private CloseableChannel server;

	private RSocket rsocket;

	@BeforeEach
	public void setup() {
		this.server = RSocketFactory.receive()
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.addSocketAcceptorPlugin(this.interceptor)
				.acceptor((acceptor, sendingSocket) -> Mono.empty())
				.transport(TcpServerTransport.create("localhost", 0))
				.start()
				.block();
	}

	@AfterEach
	public void dispose() {
		this.rsocket.dispose();
		this.server.dispose();
	}

	@RepeatedTest(1000)
	public void connectWhenNotAuthenticated() {
		System.out.println("connectWhenNotAuthenticated");
		InetSocketAddress address = this.server.address();
		TcpClientTransport transport = TcpClientTransport.create(address.getHostName(), address.getPort());
		this.rsocket = RSocketConnector.create().connectWith(transport)
				.block();

		System.out.println("Requester");
		assertThatCode(() -> this.rsocket.requestResponse(DefaultPayload.create("Hello"))
				.block())
				.isNotNull()
				.hasMessage("access denied");
	}

	SocketAcceptorInterceptor rsocketInterceptor() {
		return socketAcceptor ->
				(SocketAcceptor) (setup, sendingSocket) ->
						Mono.error(() -> new RuntimeException("access denied"));
	}
}

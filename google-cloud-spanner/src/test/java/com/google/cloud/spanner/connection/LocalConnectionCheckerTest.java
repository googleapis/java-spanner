package com.google.cloud.spanner.connection;

import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1;
import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1_RESULTSET;

import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalConnectionCheckerTest {

  private MockSpannerServiceImpl mockSpanner;
  private Server server;
  private InetSocketAddress address;

  @Before
  public void setUp() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    address = new InetSocketAddress("localhost", 0);
    server = NettyServerBuilder.forAddress(address).addService(mockSpanner).build();
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
    server.awaitTermination();
  }

  @Test
  public void localConnectionCheckerWorksWithMockSpanner() {
    final String uri =
        String.format(
            "cloudspanner://localhost:%d/projects/proj/instances/inst/databases/db?usePlainText=true",
            server.getPort());
    final ConnectionOptions connectionOptions = ConnectionOptions.newBuilder().setUri(uri).build();
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.query(SELECT1, SELECT1_RESULTSET));

    try (Connection connection = connectionOptions.getConnection();
        ResultSet resultSet = connection.executeQuery(SELECT1)) {
      while (resultSet.next()) {}
    }
  }
}

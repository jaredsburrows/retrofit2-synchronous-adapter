package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.net.HttpURLConnection;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.helpers.StringConverterFactory;
import retrofit2.http.GET;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test is to valid the code in the readme.
 *
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
@SuppressWarnings("ConstantConditions")
public final class ReadmeExampleTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private Service example;

  interface Service {
    @GET("/") String getString();
    @GET("/") ResponseBody getBody();
    @GET("/") Response<String> getStringResponse();
    @GET("/") Response<ResponseBody> getResponseBodyResponse();
  }

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    example = retrofit.create(Service.class);
  }

  @Test public void string() throws IOException {
    server.enqueue(new MockResponse().setBody("Hi"));

    String response = example.getString();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void body() throws IOException {
    server.enqueue(new MockResponse().setBody("Hi"));

    ResponseBody response = example.getBody();
    assertThat(response.string()).isEqualTo("Hi");
  }

  @Test public void stringResponse() throws IOException {
    server.enqueue(new MockResponse().setBody("1234"));

    Response<String> response = example.getStringResponse();
    assertThat(response.body()).isEqualTo("1234");
    assertThat(response.code()).isEqualTo(HTTP_OK);
  }

  @Test public void responseBodyResponse() throws IOException {
    server.enqueue(new MockResponse().setBody("1234"));

    Response<ResponseBody> response = example.getResponseBodyResponse();
    assertThat(response.body().string()).isEqualTo("1234");
    assertThat(response.code()).isEqualTo(HTTP_OK);
  }
}

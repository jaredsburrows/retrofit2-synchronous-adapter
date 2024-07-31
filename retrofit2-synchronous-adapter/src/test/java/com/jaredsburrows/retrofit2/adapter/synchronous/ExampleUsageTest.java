package com.jaredsburrows.retrofit2.adapter.synchronous;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.annotations.SerializedName;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

/**
 * This test is to valid the code in the readme.
 */
public final class ExampleUsageTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private Service example;

  interface Service {
    @GET("/") TestDto returnDto();

    @GET("/") Response<TestDto> responseDto();

    @GET("/") Void returnVoid();

    @GET("/") Response<Void> responseVoid();

    @GET("/") ResponseBody returnResponseBody();

    @GET("/") Response<ResponseBody> responseResponseBody();
  }

  static class TestDto {
    @SerializedName("name") String name;
  }

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(server.url("/"))
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .addConverterFactory(GsonConverterFactory.create())
      .build();
    example = retrofit.create(Service.class);
  }

  @Test public void testGsonDtoType() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    TestDto response = example.returnDto();
    assertThat(response.name).isEqualTo("value");
  }

  @Test public void testResponseOfGsonDto() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    Response<TestDto> response = example.responseDto();
    assertThat(response.body().name).isEqualTo("value");
  }

  @Test public void testVoidResponse() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    Void response = example.returnVoid();
    assertThat(response).isNull();
  }

  @Test public void testVoidResponseNoResponse() {
    server.enqueue(new MockResponse());
    Void response = example.returnVoid();
    assertThat(response).isNull();

    server.enqueue(new MockResponse().setBody(""));
    Void response2 = example.returnVoid();
    assertThat(response2).isNull();
  }

  @Test public void testResponseOfVoid() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    Response<Void> response = example.responseVoid();
    assertThat(response.body()).isNull();
  }

  @Test public void testResponseOfVoidNoResponse() {
    server.enqueue(new MockResponse());
    Response<Void> response = example.responseVoid();
    assertThat(response.body()).isNull();

    server.enqueue(new MockResponse().setBody(""));
    Response<Void> response2 = example.responseVoid();
    assertThat(response2.body()).isNull();
  }

  @Test public void testResponseBody() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    ResponseBody response = example.returnResponseBody();
    assertThat(response).isNotNull();
  }

  @Test public void testResponseBodyNoResponse() {
    server.enqueue(new MockResponse());
    ResponseBody response = example.returnResponseBody();
    assertThat(response).isNotNull();

    server.enqueue(new MockResponse().setBody(""));
    ResponseBody response2 = example.returnResponseBody();
    assertThat(response2).isNotNull();
  }

  @Test public void testResponseOfResponseBody() {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));
    Response<ResponseBody> response = example.responseResponseBody();
    assertThat(response.body()).isNotNull();
  }

  @Test public void testResponseOfResponseBodyNoResponse() {
    server.enqueue(new MockResponse());
    Response<ResponseBody> response = example.responseResponseBody();
    assertThat(response.body()).isNotNull(); // empty but non-null

    server.enqueue(new MockResponse().setBody(""));
    Response<ResponseBody> response2 = example.responseResponseBody();
    assertThat(response2.body()).isNotNull(); // empty but non-null
  }
}

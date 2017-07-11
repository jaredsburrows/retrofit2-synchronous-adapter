package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Converter;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.helpers.StringConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * This test does not use {@link retrofit2.Call} and uses the {@link SynchronousCallAdapterFactory} instead.
 *
 * Based off of
 *  - https://github.com/square/retrofit/blob/master/retrofit/src/test/java/retrofit2/CallTest.java.
 *  - https://github.com/square/retrofit/blob/master/retrofit-adapters/java8/src/test/java/retrofit2/adapter/java8/CompletableFutureTest.java
 *
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
@SuppressWarnings("ConstantConditions")
public final class SynchronousCallTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") String getString();
    @GET("/") ResponseBody getBody();
    @GET("/") @Streaming ResponseBody getStreamingBody();
    @POST("/") String postString(@Body String body);
  }

  @Test public void http200Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    String response = example.getString();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void http404Sync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    try {
      String response = example.getString();
      assertThat(response).isEqualTo("Hi");
      fail();
    } catch (Exception e) {
      assertThat(e)
          .isInstanceOf(HttpException.class)
          .hasMessage("HTTP 404 Client Error");
    }
  }

  @Test public void transportProblemSync() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    try {
      example.getString();
      fail();
    } catch (Exception ignored) {
    }
  }

  @Test public void conversionProblemOutgoingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory() {
          @Override
          public Converter<?, RequestBody> requestBodyConverter(Type type,
              Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
              Retrofit retrofit) {
            return new Converter<String, RequestBody>() {
              @Override public RequestBody convert(String value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    try {
      example.postString("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingSync() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.postString("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingMaskedByConverterIsUnwrapped() throws IOException {
    // MWS has no way to trigger IOExceptions during the response body so use an interceptor.
    OkHttpClient client = new OkHttpClient.Builder() //
        .addInterceptor(new Interceptor() {
          @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response response = chain.proceed(chain.request());
            ResponseBody body = response.body();
            BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
              @Override public long read(Buffer sink, long byteCount) throws IOException {
                throw new IOException("cause");
              }
            });
            body = ResponseBody.create(body.contentType(), body.contentLength(), source);
            return response.newBuilder().body(body).build();
          }
        }).build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(new StringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                try {
                  return value.string();
                } catch (IOException e) {
                  // Some serialization libraries mask transport problems in runtime exceptions. Bad!
                  throw new RuntimeException("wrapper", e);
                }
              }
            };
          }
        })
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.getString();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessageContaining("cause");
    }
  }

  @Test public void http204SkipsConverter() throws IOException {
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return converter;
          }
        })
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

    String response = example.getString();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() throws IOException {
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return converter;
          }
        })
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 205 Nothin"));

    String response = example.getString();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));

    String response = example.getString();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void responseBody() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234"));

    ResponseBody response = example.getBody();
    assertThat(response.string()).isEqualTo("1234");
  }

  @Test public void responseBodyBuffers() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse()
        .setBody("1234")
        .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

    // When buffering we will detect all socket problems before returning the Response.
    try {
      example.getBody();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessageContaining("unexpected end of stream");
    }
  }

  @Test public void responseBodyStreams() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse()
        .setBody("1234")
        .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

    // When streaming we only detect socket problems as the ResponseBody is read.
    try {
      example.getStreamingBody().string();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("unexpected end of stream");
    }
  }

  @Test public void emptyResponse() throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous call adapter
        .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "text/stringy"));

    String response = example.getString();
    assertThat(response).isEmpty();
  }
}

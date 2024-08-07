package com.jaredsburrows.retrofit2.adapter.synchronous;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.MediaType;
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
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.helpers.StringConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * This test does not use {@link retrofit2.Call} and uses the {@link SynchronousCallAdapterFactory}
 * instead.
 * From: https://github.com/square/retrofit/blob/d51805b9af79d631b43b5e8b85d12581989b1d49/retrofit/java-test/src/test/java/retrofit2/CallTest.java#L53
 */
public final class SynchronousCallTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") String getString();

    @GET("/") ResponseBody getBody();

    @GET("/") @Streaming ResponseBody getStreamingBody();

    @POST("/") String postString(@Body String body);

    @POST("/{a}") String postRequestBody(@Path("a") Object a);

    @GET("/") Response<String> getStringResponse();

    @GET("/") Response<ResponseBody> getResponseBodyResponse();
  }

  @Test public void http200Sync() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    String response = example.getString();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void http404Sync() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    try {
      String response = example.getString();
      assertThat(response).isEqualTo("Hi");
      fail();
    } catch (Exception e) {
      assertThat(e)
        .isInstanceOf(HttpException.class);
      assertThat(e)
        .hasMessageThat().isEqualTo("HTTP 404 Client Error");
    }
  }

  @Test public void transportProblemSync() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    try {
      example.getString();
      fail();
    } catch (Exception ignored) {
    }
  }

  @Test public void conversionProblemOutgoingSync() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory() {
        @Override
        public Converter<?, RequestBody> requestBodyConverter(Type type,
          Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
          Retrofit retrofit) {
          return (Converter<String, RequestBody>) value -> {
            throw new UnsupportedOperationException("I am broken!");
          };
        }
      })
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    try {
      example.postString("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageThat().isEqualTo("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingSync() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory() {
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type,
          Annotation[] annotations, Retrofit retrofit) {
          return (Converter<ResponseBody, String>) value -> {
            throw new UnsupportedOperationException("I am broken!");
          };
        }
      })
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.postString("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageThat().isEqualTo("I am broken!");
    }
  }

  @Test public void requestBeforeExecuteCreates() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        return "Hello";
      }
    };

    service.postRequestBody(a);
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestThrowingBeforeExecuteFailsExecute() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };

    try {
      service.postRequestBody(a);
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessageThat().isEqualTo("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void requestAfterExecuteThrowingAlsoThrows() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service service = retrofit.create(Service.class);

    server.enqueue(new MockResponse());

    AtomicInteger writeCount = new AtomicInteger();
    Object a = new Object() {
      @Override public String toString() {
        writeCount.incrementAndGet();
        throw new RuntimeException("Broken!");
      }
    };

    try {
      service.postRequestBody(a);
      fail();
    } catch (RuntimeException e) {
      assertThat(e).hasMessageThat().isEqualTo("Broken!");
    }
    assertThat(writeCount.get()).isEqualTo(1);
  }

  @Test public void conversionProblemIncomingMaskedByConverterIsUnwrapped() {
    // MWS has no way to trigger IOExceptions during the response body so use an interceptor.
    OkHttpClient client = new OkHttpClient.Builder() //
      .addInterceptor(chain -> {
        okhttp3.Response response = chain.proceed(chain.request());
        ResponseBody body = response.body();
        BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
          @Override public long read(@Nonnull Buffer sink, long byteCount) throws IOException {
            throw new IOException("cause");
          }
        });
        body = create(body.contentType(), body.contentLength(), source);
        return response.newBuilder().body(body).build();
      }).build();

    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .client(client)
      .addConverterFactory(new StringConverterFactory() {
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type,
          Annotation[] annotations, Retrofit retrofit) {
          return (Converter<ResponseBody, String>) value -> {
            try {
              return value.string();
            } catch (IOException e) {
              // Some serialization libraries mask transport problems in runtime exceptions. Bad!
              throw new RuntimeException("wrapper", e);
            }
          };
        }
      })
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("Hi"));

    try {
      example.getString();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessageThat().contains("cause");
    }
  }

  @Test public void http204SkipsConverter() {
    Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(@Nonnull ResponseBody value) throws IOException {
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
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

    String response = example.getString();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() {
    Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(@Nonnull ResponseBody value) throws IOException {
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
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setStatus("HTTP/1.1 205 Nothin"));

    String response = example.getString();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
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
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("1234"));

    ResponseBody response = example.getBody();
    assertThat(response.string()).isEqualTo("1234");
  }

  @Test public void responseBodyBuffers() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
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
      assertThat(e).hasMessageThat().contains("unexpected end of stream");
    }
  }

  @Test public void responseBodyStreams() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
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
      assertThat(e).hasMessageThat().contains("unexpected end of stream");
    }
  }

  @Test public void emptyResponse() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
      .build();
    Service example = retrofit.create(Service.class);

    server.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "text/stringy"));

    String response = example.getString();
    assertThat(response).isEmpty();
  }

  @SuppressWarnings("deprecation")
  private static ResponseBody create(@Nullable MediaType mediaType, Long length,
    BufferedSource source) {
    return ResponseBody.create(mediaType, length, source);
  }
}

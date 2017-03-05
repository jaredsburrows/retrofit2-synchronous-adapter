package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.helpers.ToStringConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
public final class SynchronousCallAdapterFactoryTest {
  @Rule public final MockWebServer server = new MockWebServer();

  private interface Service {
    @GET("/") String string();
    @GET("/") ResponseBody body();
    @GET("/") @Streaming ResponseBody streamingBody();
    @POST("/") String string(@Body String body);
  }

  @Test public void http200Sync() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setResponseCode(HTTP_OK)
        .setBody("Hi"));

    // Assert
    String response = example.string();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void http404Sync() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setResponseCode(HTTP_NOT_FOUND)
        .setBody("Not Found"));

    // Assert
    String response = example.string();
    assertThat(response).isEqualTo("Not Found");
  }

  @Test public void transportProblemSync() {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    // Assert
    try {
      example.string();
      fail();
    } catch (Exception ignored) {
    }
  }

  @Test public void conversionProblemOutgoingSync() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
              Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
              Retrofit retrofit) {
            return new Converter<String, RequestBody>() {
              @Override public RequestBody convert(String value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act and Assert
    try {
      example.string("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void conversionProblemIncomingSync() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return new Converter<ResponseBody, String>() {
              @Override public String convert(ResponseBody value) throws IOException {
                throw new UnsupportedOperationException("I am broken!");
              }
            };
          }
        })
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse().setBody("Hi"));

    // Assert
    try {
      example.string("Hi");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("I am broken!");
    }
  }

  @Test public void http204SkipsConverter() throws IOException {
    // Arrange
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return converter;
          }
        })
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setStatus("HTTP/1.1 204 Nothing"));

    // Assert
    String response = example.string();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void http205SkipsConverter() throws IOException {
    // Arrange
    final Converter<ResponseBody, String> converter = spy(new Converter<ResponseBody, String>() {
      @Override public String convert(ResponseBody value) throws IOException {
        return value.string();
      }
    });
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory() {
          @Override
          public Converter<ResponseBody, ?> responseBodyConverter(Type type,
              Annotation[] annotations, Retrofit retrofit) {
            return converter;
          }
        })
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setStatus("HTTP/1.1 205 Nothing"));

    // Assert
    String response = example.string();
    assertThat(response).isNull();
    verifyNoMoreInteractions(converter);
  }

  @Test public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setBody("Hi")
        .removeHeader("Content-Type"));

    // Assert
    String response = example.string();
    assertThat(response).isEqualTo("Hi");
  }

  @Test public void responseBody() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse().setBody("1234"));

    // Assert
    ResponseBody response = example.body();
    assertThat(response.string()).isEqualTo("1234");
  }

  @Test public void responseBodyStreams() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setBody("1234")
        .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

    // Assert
    try {
      example.streamingBody().string();
      fail();
    } catch (IOException e) {
      assertThat(e).hasMessage("unexpected end of stream");
    }
  }

  @Test public void emptyResponse() throws IOException {
    // Arrange
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ToStringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory())
        .build();
    Service example = retrofit.create(Service.class);

    // Act
    server.enqueue(new MockResponse()
        .setBody("")
        .addHeader("Content-Type", "text/stringy"));

    // Assert
    String response = example.string();
    assertThat(response).isEmpty();
  }
}

package com.jaredsburrows.retrofit2.adapter.synchronous;

import com.google.gson.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.helpers.StringConverterFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test does not use {@link retrofit2.Call} and uses the {@link SynchronousCallAdapterFactory} instead.
 *
 * Based off of: https://github.com/square/retrofit/blob/master/retrofit-adapters/guava/src/test/java/retrofit2/adapter/guava/GuavaCallAdapterFactoryTest.java
 *
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
@SuppressWarnings("ConstantConditions")
public final class SynchronousCallAdapterFactoryTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];
  private final CallAdapter.Factory factory = SynchronousCallAdapterFactory.create();
  private Retrofit retrofit;

  @Before public void setUp() {
    retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(new SynchronousCallAdapterFactory()) // Add synchronous call adapter
        .addCallAdapterFactory(factory)
        .build();
  }

  @Test public void responseType() {
    Type bodyClass = new TypeToken<String>() {}.getType();
    assertThat(factory.get(bodyClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(String.class);
    Type bodyWildcard = new TypeToken<List<? extends String>>() {}.getType();
    assertThat(factory.get(bodyWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<List<? extends String>>() {}.getType());
    Type bodyGeneric = new TypeToken<List<String>>() {}.getType();
    assertThat(factory.get(bodyGeneric, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<List<String>>() {}.getType());
    Type responseClass = new TypeToken<Response<String>>() {}.getType();
    assertThat(factory.get(responseClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<Response<String>>() {}.getType());
    Type responseWildcard = new TypeToken<Response<? extends String>>() {}.getType();
    assertThat(factory.get(responseWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<Response<? extends String>>() {}.getType());
    Type resultClass = new TypeToken<Response<String>>() {}.getType();
    assertThat(factory.get(resultClass, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<Response<String>>() {}.getType());
    Type resultWildcard = new TypeToken<Response<? extends String>>() {}.getType();
    assertThat(factory.get(resultWildcard, NO_ANNOTATIONS, retrofit).responseType())
        .isEqualTo(new TypeToken<Response<? extends String>>() {}.getType());
  }

  @Test public void rawTypeReturnsNull() {
    // Act and Assert
    assertThat(factory.get(Call.class, NO_ANNOTATIONS, retrofit)).isNull();
  }
}

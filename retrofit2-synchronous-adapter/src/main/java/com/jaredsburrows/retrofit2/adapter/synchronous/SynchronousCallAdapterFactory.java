package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A synchronous {@link CallAdapter.Factory} that uses the same thread for both I/O and
 * application-level callbacks.
 * <p>
 * Adding this class to {@link Retrofit} allows you to return direct deserialized type from service
 * methods:
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   User getUser()
 * }
 * </code></pre>
 * or allows you to return deserialized type wrapped in {@link Response}:
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   Response&lt;User&gt; getUser()
 * }
 * </code></pre>
 * {@link CallAdapter.Factory} returns the deserialized body for 2XX responses, sets {@link
 * retrofit2.HttpException} errors for non-2XX responses, and for network errors.
 */
public final class SynchronousCallAdapterFactory extends CallAdapter.Factory {
  private SynchronousCallAdapterFactory() {
  }

  public static CallAdapter.Factory create() {
    return new SynchronousCallAdapterFactory();
  }

  @Override @Nullable public CallAdapter<?, ?> get(
    Type returnType, Annotation[] annotations, Retrofit retrofit) {
    // Prevent the Async calls via Call class
    if (getRawType(returnType) == Call.class) {
      return null;
    }

    // Return type is not Response<T>. Use it for body-only adapter.
    if (getRawType(returnType) != Response.class) {
      return new SynchronousBodyCallAdapter<>(returnType);
    }

    // Make sure Response<T> is parameterized
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
        "Response must be parameterized as Response<Foo> or Response<? extends Foo>");
    }

    // Handle Response<T> return types
    Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
    return new SynchronousResponseCallAdapter<>(responseType);
  }
}

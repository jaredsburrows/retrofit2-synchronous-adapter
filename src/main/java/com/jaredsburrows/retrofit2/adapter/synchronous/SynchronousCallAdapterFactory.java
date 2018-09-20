package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Creates a synchronous call adapters for that uses the same thread for both I/O and
 * application-level callbacks.
 */
public final class SynchronousCallAdapterFactory extends CallAdapter.Factory {
  private SynchronousCallAdapterFactory() {
  }

  public static CallAdapter.Factory create() {
    return new SynchronousCallAdapterFactory();
  }

  @Override public @Nullable CallAdapter<?, ?> get(@Nonnull Type returnType,
                                                   @Nonnull Annotation[] annotations,
                                                   @Nonnull Retrofit retrofit) {
    // Prevent the Async calls via Call class
    if (getRawType(returnType) == Call.class) return null;

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
    final Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
    return new SynchronousResponseCallAdapter<>(responseType);
  }
}

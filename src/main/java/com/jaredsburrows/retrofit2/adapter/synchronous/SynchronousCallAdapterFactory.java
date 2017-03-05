package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Creates a synchronous call adapters for that uses the same thread for both I/O and
 * application-level callbacks.
 *
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
public final class SynchronousCallAdapterFactory extends CallAdapter.Factory {
  public static CallAdapter.Factory create() {
    return new SynchronousCallAdapterFactory();
  }

  @Override
  public CallAdapter<?, ?> get(final Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) == Call.class) return null;

    return new CallAdapter<Object, Object>() {
      @Override public Type responseType() {
        return returnType;
      }

      @Override public Object adapt(Call<Object> call) {
        final Response<Object> response;
        try {
          response = call.execute();

          // Stop here if something goes wrong
          if (response == null) return null;

          // If successful, return the response
          if (response.body() != null) return response.body();

          // If an error occurs, return the error response body
          if (response.errorBody() != null) return response.errorBody().string();

          return null;
        } catch (IOException e) {
          throw new RuntimeException();
        }
      }
    };
  }
}

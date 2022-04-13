package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * {@link CallAdapter} allows you to return deserialized type:
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   User getUser()
 * }
 * </code></pre>
 */
final class SynchronousBodyCallAdapter<R> implements CallAdapter<R, Object> {
  private final Type responseType;

  SynchronousBodyCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override @Nullable public Object adapt(Call<R> call) {
    Response<R> response;

    // Make the initial call
    try {
      response = call.execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // If successful(200 OK), return the response with body
    if (response.isSuccessful()) {
      return response.body();
    }

    // If an error occurs, return HttpException including response
    throw new HttpException(response);
  }
}

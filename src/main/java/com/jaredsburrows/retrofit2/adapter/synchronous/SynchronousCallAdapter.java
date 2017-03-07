package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
 */
final class SynchronousCallAdapter<R> implements CallAdapter<R, Object> {
  private final Type responseType;

  SynchronousCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override public Object adapt(Call<R> call) {
    Response<R> response;

    // Make the initial call.
    try {
      response = call.execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Stop here if something goes wrong.
    if (response == null) return null;

    // If successful, return the response.
    if (response.body() != null) return response.body();

    // If an error occurs, return the error response body.
    if (response.errorBody() != null) {
      try {
        return response.errorBody().string();
      } catch (IOException e) {
        throw new RuntimeException(new HttpException(response));
      }
    }

    return null;
  }
}

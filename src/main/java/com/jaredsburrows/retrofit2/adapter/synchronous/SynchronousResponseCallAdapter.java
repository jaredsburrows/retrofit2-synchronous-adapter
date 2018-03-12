package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.annotation.Nonnull;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

@SuppressWarnings("ConstantConditions")
final class SynchronousResponseCallAdapter<R> implements CallAdapter<R, Response<R>> {
  private final Type responseType;

  SynchronousResponseCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override public Response<R> adapt(@Nonnull Call<R> call) {
    Response<R> response;

    // Make the initial call
    try {
      response = call.execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Stop here if something goes wrong
    if (response == null) return null;

    // If successful(200 OK) and Response<T> type, return the response with body
    if (response.isSuccessful()) return Response.success(response.body(), response.raw());

    // If unsuccessful(non 200 OK) and Response<T> type, return the response with body
    return Response.error(response.code(), response.errorBody());
  }
}

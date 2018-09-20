package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.annotation.Nonnull;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

@SuppressWarnings("ConstantConditions")
final class SynchronousResponseCallAdapter<R> implements CallAdapter<R, Response<R>> {
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("text/plain");
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

    if (response.isSuccessful()) {
      // If successful(200 OK) and Response<T> type, return the response with body
      return response.raw() == null
        ? Response.success(response.body())
        : Response.success(response.body(), response.raw());
    } else {
      // If unsuccessful(non 200 OK) and Response<T> type, return the response with body
      final ResponseBody errorBody = response.errorBody();
      if (errorBody == null) {
        return Response.error(ResponseBody.create(DEFAULT_MEDIA_TYPE, ""), response.raw());
      } else {
        return response.raw() == null
          ? Response.<R>error(response.code(), errorBody)
          : Response.<R>error(errorBody, response.raw());
      }
    }
  }
}

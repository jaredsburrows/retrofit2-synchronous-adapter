package com.jaredsburrows.retrofit2.adapter.synchronous;

import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

/**
 * {@link CallAdapter} allows you to return deserialized type wrapped in {@link Response}:
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   Response&lt;User&gt; getUser()
 * }
 * </code></pre>
 */
final class SynchronousResponseCallAdapter<R> implements CallAdapter<R, Response<R>> {
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("text/plain");
  private final Type responseType;

  SynchronousResponseCallAdapter(Type responseType) {
    this.responseType = responseType;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override public Response<R> adapt(Call<R> call) {
    Response<R> response;

    // Make the initial call
    try {
      response = call.execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (response.isSuccessful()) {
      // If successful(200 OK) and Response<T> type, return the response with body
      return response.raw() == null
        ? Response.success(response.body())
        : Response.success(response.body(), response.raw());
    } else {
      // If unsuccessful(non 200 OK) and Response<T> type, return the response with body
      ResponseBody errorBody = response.errorBody();
      if (errorBody == null) {
        return Response.error(ResponseBody.create(DEFAULT_MEDIA_TYPE, ""), response.raw());
      } else {
        return response.raw() == null
          ? Response.error(response.code(), errorBody)
          : Response.error(errorBody, response.raw());
      }
    }
  }
}

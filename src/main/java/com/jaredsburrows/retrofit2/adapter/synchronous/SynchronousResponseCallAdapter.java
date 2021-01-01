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
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.get("text/plain");
  private static final String DEFAULT_EMPTY_CONTENT = "";
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

    // If successful(200 OK) and Response<T> type, return the response with body
    if (response.isSuccessful()) {
      return Response.success(response.body(), response.raw());
    }

    // If unsuccessful(non 200 OK) and Response<T> type, return the response with body
    ResponseBody errorBody = response.errorBody();
    okhttp3.Response raw = response.raw();
    if (errorBody == null) {
      return Response.error(ResponseBody.create(DEFAULT_MEDIA_TYPE, DEFAULT_EMPTY_CONTENT), raw);
    } else {
      return Response.error(errorBody, raw);
    }
  }
}

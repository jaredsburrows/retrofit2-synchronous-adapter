package retrofit2.helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * From: https://github.com/square/retrofit/blob/master/retrofit-adapters/guava/src/test/java/retrofit2/adapter/guava/StringConverterFactory.java
 */
public class StringConverterFactory extends Converter.Factory {
  private static final MediaType MEDIA_TYPE = MediaType.get("text/plain");

  @Nullable @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit) {
    if (String.class.equals(type)) {
      return (Converter<ResponseBody, String>) ResponseBody::string;
    }
    return null;
  }

  @Nullable @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (String.class.equals(type)) {
      return (Converter<String, RequestBody>) value -> RequestBody.create(MEDIA_TYPE, value);
    }
    return null;
  }
}

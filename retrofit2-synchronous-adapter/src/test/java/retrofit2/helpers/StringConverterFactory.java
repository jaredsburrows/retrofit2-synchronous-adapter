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
 * From: https://github.com/square/retrofit/blob/d51805b9af79d631b43b5e8b85d12581989b1d49/retrofit-adapters/guava/src/test/java/retrofit2/adapter/guava/StringConverterFactory.java#L26
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
      return (Converter<String, RequestBody>) StringConverterFactory::create;
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  private static RequestBody create(String value) {
    return RequestBody.create(MEDIA_TYPE, value);
  }
}

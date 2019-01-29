package com.jaredsburrows.retrofit2.adapter.synchronous;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test does not use {@link retrofit2.Call} and uses the {@link SynchronousCallAdapterFactory} instead.
 *
 * Based off of: https://github.com/square/retrofit/blob/master/retrofit-converters/gson/src/test/java/retrofit2/converter/gson/GsonConverterFactoryTest.java.
 */
public final class SynchronousGsonConverterFactoryTest {
  interface AnInterface {
    String getName();
  }

  static class AnImplementation implements AnInterface {
    private final String theName;

    AnImplementation(String name) {
      theName = name;
    }

    @Override public String getName() {
      return theName;
    }
  }

  static class AnInterfaceAdapter extends TypeAdapter<AnInterface> {
    @Override public void write(JsonWriter jsonWriter, AnInterface anInterface) throws IOException {
      jsonWriter.beginObject();
      jsonWriter.name("name").value(anInterface.getName());
      jsonWriter.endObject();
    }

    @Override public AnInterface read(JsonReader jsonReader) throws IOException {
      jsonReader.beginObject();

      String name = null;
      while (jsonReader.peek() != JsonToken.END_OBJECT) {
        if ("name".equals(jsonReader.nextName())) {
          name = jsonReader.nextString();
        }
      }

      jsonReader.endObject();
      return new AnImplementation(name);
    }
  }

  interface Service {
    @POST("/") AnImplementation anImplementation(@Body AnImplementation impl);
    @POST("/") AnInterface anInterface(@Body AnInterface impl);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before public void setUp() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(AnInterface.class, new AnInterfaceAdapter())
        .setLenient()
        .create();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(SynchronousCallAdapterFactory.create()) // Add synchronous adapter
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void anInterface() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

    AnInterface response = service.anInterface(new AnImplementation("value"));
    assertThat(response.getName()).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void anImplementation() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    AnImplementation response = service.anImplementation(new AnImplementation("value"));
    assertThat(response.theName).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void serializeUsesConfiguration() throws Exception {
    server.enqueue(new MockResponse().setBody("{}"));

    service.anImplementation(new AnImplementation(null));

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void deserializeUsesConfiguration() {
    server.enqueue(new MockResponse().setBody("{/* a comment! */}"));

    AnImplementation response = service.anImplementation(new AnImplementation("value"));
    assertThat(response.getName()).isNull();
  }
}

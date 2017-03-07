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
 * This test uses a {@link Service} that does not use {@link retrofit2.Call} as well as uses the
 * synchronous call adapter with a {@link Gson} converter to ensure correct serialization.
 *
 * Based off of: https://github.com/square/retrofit/blob/master/retrofit-converters/gson/src/test/java/retrofit2/converter/gson/GsonConverterFactoryTest.java.
 *
 * @author <a href="mailto:jaredsburrows@gmail.com">Jared Burrows</a>
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
        switch (jsonReader.nextName()) {
          case "name":
            name = jsonReader.nextString();
            break;
        }
      }

      jsonReader.endObject();
      return new AnImplementation(name);
    }
  }

  // Raw return types
  private interface Service {
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
        .addCallAdapterFactory(
            SynchronousCallAdapterFactory.create())  // Add synchronous call adapter
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void anInterface() throws IOException, InterruptedException {
    // Act
    server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

    // Assert
    AnInterface response = service.anInterface(new AnImplementation("value"));
    assertThat(response.getName()).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void anImplementation() throws IOException, InterruptedException {
    // Act
    server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

    // Assert
    AnImplementation response = service.anImplementation(new AnImplementation("value"));
    assertThat(response.theName).isEqualTo("value");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{\"theName\":\"value\"}");
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void serializeUsesConfiguration() throws IOException, InterruptedException {
    // Act
    server.enqueue(new MockResponse().setBody("{}"));

    // Assert
    service.anImplementation(new AnImplementation(null));

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void deserializeUsesConfiguration() throws IOException, InterruptedException {
    // Act
    server.enqueue(new MockResponse().setBody("{/* a comment! */}"));

    // Assert
    AnImplementation response = service.anImplementation(new AnImplementation("value"));
    assertThat(response.getName()).isNull();
  }
}

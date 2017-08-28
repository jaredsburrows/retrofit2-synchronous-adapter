# Retrofit 2 Synchronous Adapter

[![License](https://img.shields.io/badge/license-apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/jaredsburrows/retrofit2-synchronous-adapter.svg?branch=master)](https://travis-ci.org/jaredsburrows/retrofit2-synchronous-adapter)
[![Coverage Status](https://coveralls.io/repos/github/jaredsburrows/retrofit2-synchronous-adapter/badge.svg?branch=master)](https://coveralls.io/github/jaredsburrows/retrofit2-synchronous-adapter?branch=master)
[![Twitter Follow](https://img.shields.io/twitter/follow/jaredsburrows.svg?style=social)](https://twitter.com/jaredsburrows)


A synchronous `CallAdapter.Factory` implementation for Retrofit 2.

This project brings Retrofit 1's synchronous usage to Retrofit 2.


## Usage

```java
// Setup retrofit
Retrofit retrofit = new Retrofit.Builder()
  .baseUrl("https://api.example.com")
  .addCallAdapterFactory(SynchronousCallAdapterFactory.create())
  .build();

// Create your service
interface Service {
  @GET("/") String getString();                               // Return type directly
  @GET("/") ResponseBody getBody();                           // Return generic type directly
  @GET("/") Response<String> getStringResponse();             // Return Response information with type
  @GET("/") Response<ResponseBody> getResponseBodyResponse(); // Return Response information with generic type
}

// Initiate the service
Service service = retrofit.create(Service.class);

// Make your HTTP request
String string = service.getString();
ResponseBody body = service.getBody();
Response<String> response = service.getStringResponse();
Response<ResponseBody> response = service.getResponseBodyResponse();

```


## Download

Gradle:
```groovy
repositories {
  jcenter()
}
  
compile "com.jaredsburrows.retrofit:retrofit2-synchronous-adapter:0.3.0"
```

Snapshot versions are available in the JFrog Artifactory repository: https://oss.jfrog.org/webapp/#/builds/retrofit2-synchronous-adapter

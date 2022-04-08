# Retrofit 2 Synchronous Adapter

[![License](https://img.shields.io/badge/license-apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build](https://github.com/jaredsburrows/retrofit2-synchronous-adapter/actions/workflows/build.yml/badge.svg)](https://github.com/jaredsburrows/retrofit2-synchronous-adapter/actions/workflows/build.yml)
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
  @GET("/") ApiResponse response();                 // Return type directly
  @GET("/") Response<ApiResponse> responseApi();    // Return Response information with type
  @GET("/") ResponseBody body();                    // Return generic type directly
  @GET("/") Response<ResponseBody> responseBody();  // Return Response information with generic type
}

// Initiate the service
Service example = retrofit.create(Service.class);

// Make your HTTP request
ApiResponse response = example.response();
ResponseBody body = example.body();
Response<ResponseBody> responseBody = example.responseBody();
Response<ApiResponse> responseApi = example.responseApi();
```

## Download

**Release:**
```groovy
repositories {
  mavenCentral()
}

dependencies {
  compile 'com.jaredsburrows.retrofit:retrofit2-synchronous-adapter:0.5.0'
}
```
Release versions are available in the [Sonatype's release repository](https://repo1.maven.org/maven2/com/jaredsburrows/retrofit/).

**Snapshot:**
```groovy
repositories {
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  compile 'com.jaredsburrows.retrofit:retrofit2-synchronous-adapter:0.6.0-SNAPSHOT'
}
```
Snapshot versions are available in the [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/com/jaredsburrows/retrofit/retrofit2-synchronous-adapter/).

## License

```
Copyright (C) 2017 Jared Burrows

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

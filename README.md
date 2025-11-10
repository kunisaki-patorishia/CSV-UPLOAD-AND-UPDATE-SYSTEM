# CSV Upload System

A complete Java Spring Boot application for uploading and processing CSV files with background processing and real-time updates.

## Features

- ✅ CSV file upload with drag & drop interface
- ✅ Idempotent uploads (no duplicate files)
- ✅ UPSERT operations using UNIQUE_KEY
- ✅ Background processing with @Async
- ✅ Real-time status updates
- ✅ UTF-8 character cleaning
- ✅ Responsive design

## Tech Stack

- Java 17
- Spring Boot 3.1
- Spring Data JPA
- H2 Database
- Thymeleaf Templates
- Apache Commons CSV

## Local Development

```bash
./mvnw spring-boot:run

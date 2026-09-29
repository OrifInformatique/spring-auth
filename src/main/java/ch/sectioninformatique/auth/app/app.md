# APP PACKAGE
APP Documentation of Spring-Auth

## *Table of Contents*

* [Errors](#errors)

  * [ErrorDto.java](#errordtojava)

* [Exception](#exception)

  * [AppException.java](#appexceptionjava)
  * [GlobalExceptionHandler.java](#globalexceptionhandlerjava)

    * [handleAppException()](#handleappexception)
    * [handleValidationErrors()](#handlevalidationerrors)
    * [handleUnsupportedMediaType()](#handleunsupportedmediatype)
    * [handleMissingParams()](#handlemissingparams)
    * [handleMalformedJson()](#handlemalformedjson)
    * [errorResponse()](#errorresponse)
    * [buildResponse()](#buildresponse)
    * [msg()](#msg)
    * [resolveAppExceptionMessage()](#resolveappexceptionmessage)
    * [resolveValidationFieldError()](#resolvevalidationfielderror)
  * [MessageKeyProvider.java](#messagekeyproviderjava)

    * [getMessageKey()](#getmessagekey)
    * [getMessageArgs()](#getmessageargs)
    * [NO_ARGS](#no_args)


---
## *Errors*

### ErrorDto.java

`ErrorDto` is a Data Transfer Object used to represent error responses in the REST API.

It provides a standardized structure for returning error messages.

It contains :

* `message`

The `message` field contains a description of what went wrong.

Because `ErrorDto` is a Java `record`, it is immutable and automatically provides its constructor, accessor, `equals()`, `hashCode()`, and `toString()` methods.

---

## *Exception*

### AppException.java

`AppException` is the base exception class used for application-specific errors.

It extends `RuntimeException` and stores the HTTP status that should be returned when the exception is handled by the API.

It contains :

* `status`

The `status` field stores the HTTP status associated with the exception.

The constructor receives the HTTP status when the exception is created.

The `getStatus()` method returns the HTTP status so it can be used when creating the HTTP response.

Other application-specific exceptions can extend `AppException` to provide their own HTTP status.


---

### GlobalExceptionHandler.java

`GlobalExceptionHandler` is a centralized exception handler for the application.

It uses `@ControllerAdvice` to catch exceptions from all controllers and return consistent error responses.

It uses `MessageSource` to provide localized error messages according to the current locale.

It contains :

* `handleAppException()`,
* `handleValidationErrors()`,
* `handleUnsupportedMediaType()`,
* `handleMissingParams()`,
* `handleMalformedJson()`,
* `errorResponse()`,
* `buildResponse()`,
* `msg()`,
* `resolveAppExceptionMessage()`,
* `resolveValidationFieldError()`

### `handleAppException()`

Handles custom `AppException` errors.

It uses the exception's HTTP status and resolves its localized message.

If the exception implements `MessageKeyProvider`, its message key and arguments are used to retrieve the translated message.

Otherwise, the generic `error.unexpected` message is returned.

### `handleValidationErrors()`

Handles validation errors caused by `@Valid`.

It collects the validation errors for each field and stores them in a `fieldErrors` map.

The response uses HTTP `400 BAD_REQUEST` and includes a localized `error.validation.failed` message.

### `handleUnsupportedMediaType()`

Handles requests using an unsupported media type, such as an unsupported `Content-Type`.

It returns HTTP `415 UNSUPPORTED_MEDIA_TYPE`.

### `handleMissingParams()`

Handles requests where a required request parameter is missing.

It returns HTTP `400 BAD_REQUEST`.

### `handleMalformedJson()`

Handles requests where the request body cannot be read or parsed, such as malformed JSON or invalid data types.

It returns HTTP `400 BAD_REQUEST`.

### `errorResponse()`

Creates the common error response structure containing :

* `timestamp`,
* `status`,
* `error`,
* `message`

### `buildResponse()`

Creates a `ResponseEntity` using the provided HTTP status and error message.

It uses `errorResponse()` to create the response body.

### `msg()`

Uses `MessageSource` and `LocaleContextHolder` to retrieve a localized message using its message key and optional arguments.

### `resolveAppExceptionMessage()`

Determines the message to use for an `AppException`.

If the exception implements `MessageKeyProvider`, its message key and arguments are used.

Otherwise, `error.unexpected` is returned.

### `resolveValidationFieldError()`

Retrieves the default validation message for a field error.

If no default message is available, the field name is returned.


---

### MessageKeyProvider.java

`MessageKeyProvider` is an interface used by exceptions to provide a message key and optional arguments for localized error messages.

It allows the global exception handler to retrieve the correct translated message.

It contains :

* `getMessageKey()`,
* `getMessageArgs()`,
* `NO_ARGS`

### `getMessageKey()`

Returns the key used to find the corresponding translated message in the application's message files.

### `getMessageArgs()`

Returns optional arguments used to format the translated message.

By default, it returns the shared `NO_ARGS` array.

### `NO_ARGS`

A shared empty `Object` array used when an exception does not require any message arguments.

Classes implementing `MessageKeyProvider` can override `getMessageArgs()` when their translated message requires additional values.


---
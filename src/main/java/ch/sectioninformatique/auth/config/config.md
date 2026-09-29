# CONFIG PACKAGE
CONFIG Documentation of Spring-Auth


## *Table of Contents*

* [LocaleConfig.java](#localeconfigjava)
  * [messageSource()](#messagesource)
  * [resolveMessageBasenames()](#resolvemessagebasenames)
  * [resolveResourceBasename()](#resolveresourcebasename)
  * [localeResolver()](#localeresolver)
  * [validator()](#validator)
  * [localeChangeInterceptor()](#localechangeinterceptor)
  * [addInterceptors()](#addinterceptors)
* [MapperConfig.java](#mapperconfigjava)
  * [userMapper()](#usermapper)

---

## *LocaleConfig.java*

`LocaleConfig` is a configuration class responsible for managing internationalization and localization in the `spring-auth` service.

It configures the message source, default locale, validation messages, and the `lang` request parameter used to change the language.

It contains :

* `messageSource()`,
* `resolveMessageBasenames()`,
* `resolveResourceBasename()`,
* `localeResolver()`,
* `validator()`,
* `localeChangeInterceptor()`,
* `addInterceptors()`

### `messageSource()`

Configures the `MessageSource` used to load translated messages from `.properties` files.

The application automatically searches for message files inside the `messages` directory and its subdirectories.

The discovered files are converted into Spring message basenames.

The default encoding is `UTF-8`, and messages are cached for **3600 seconds**.

### `resolveMessageBasenames()`

Searches the classpath for all message property files matching the configured resource pattern.

It converts each file into a Spring message basename using `resolveResourceBasename()`.

If no message files are found, the default `messages/messages` basename is used.

### `resolveResourceBasename()`

Converts a message property file path into a Spring message basename.

It removes :

* the path before the `messages` directory,
* the `.properties` extension,
* the locale suffix, such as `_en` or `_en_US`.

This allows files for different languages to use the same message basename.

### `localeResolver()`

Defines the default locale used by the application.

The default locale is French (`Locale.FRANCE`).

The locale can be changed for a request using the `lang` parameter.

### `validator()`

Configures Bean Validation to use the application's `MessageSource`.

This allows validation error messages to be translated according to the current locale.

### `localeChangeInterceptor()`

Creates a `LocaleChangeInterceptor` that checks for the `lang` request parameter.

This parameter is used to change the locale for the current session.

### `addInterceptors()`

Registers the `LocaleChangeInterceptor` with Spring MVC.

This makes the `lang` parameter available for changing the application's language on requests.

Overall, `LocaleConfig` provides the internationalization system used by the `spring-auth` service, including translated messages, validation messages, and language switching.


---

## *MapperConfig.java*

`MapperConfig` is a configuration class used to register the `UserMapper` as a Spring bean.

It provides a fallback configuration for cases where MapStruct's annotation processing did not generate the mapper as a Spring bean.

It contains :

* `userMapper()`

### `userMapper()`

Registers the pre-generated `UserMapper.INSTANCE` as a Spring bean.

This allows the `UserMapper` to be injected into other Spring components even if MapStruct's normal Spring bean generation did not run correctly.

Overall, `MapperConfig` ensures that the `UserMapper` is available in the Spring application context.


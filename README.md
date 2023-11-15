# BandwidthSession & BandwidthUA Integration

This guide will assist developers in integrating the `BandwidthSession` and `BandwidthUA` from Bandwidth services into Android applications.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
  - [Dependencies](#dependencies)
  - [Initialization](#initialization)
- [Usage](#usage)
  - [Making a Call](#making-a-call)
  - [Terminating a Call](#terminating-a-call)
- [Listeners and Implementation](#listeners-and-implementation)
- [Configuring the User Agent](#configuring-the-user-agent)
- [Error Handling](#error-handling)

## Prerequisites

- Experience with Kotlin and Android development.
- Android Studio with the latest SDK.
- The Bandwidth SDK integrated into your project.

## Configuration

The primary source for configurations in the Bandwidth integration is the `assets > config.properties` file. Ensure this file is populated with the necessary values before integrating.

Following this template:
```markdown
account.username=...
account.display-name=...
account.password=...
connection.domain=...
connection.port=...
connection.token=...
connection.header.user=...
connection.header.pass=...
```

## Getting Started

### Dependencies

Ensure that the Bandwidth libraries are part of your project's `build.gradle` file.

### Initialization

Main components:

- **BandwidthSession**: Using `lateinit` ensures the variable is initialized before use.
- **BandwidthUA**: The instance is available from the outset.

## Usage

### Making a Call

Making a call using the Bandwidth services involves a series of steps to ensure the call's proper initiation and management.

1. **Configuration**: 
   Before making the call, extract and apply the necessary configurations from `config.properties`. This ensures that the application interacts correctly with the Bandwidth servers.

2. **Authentication**: 
   Authenticate your application with the Bandwidth service. This is achieved by logging in using the `bandwidthUA` instance:
   
   ```kotlin
   bandwidthUA.login(this)
   ```

3. **Setting the Remote Contact**:
   Define the remote contact you intend to call. For this, use the `RemoteContact` class, and assign the desired number:

   ```kotlin
   val remoteContact = RemoteContact()
   ```

4. **Call Initiation**:
   Start the call by invoking the `call` method on the `bandwidthUA` instance:

   ```kotlin
   bandwidthSession = bandwidthUA.call(remoteContact)
   ```

5. **Error Handling**:
   Implementing try-catch blocks is essential to capture and handle any exceptions that might arise during the call initiation process, providing feedback to the user as necessary.

### Terminating a Call

To end an active call, you need to invoke the `terminate()` method on the `BandwidthSession` instance:

```kotlin
bandwidthSession.terminate()
```

This method is responsible for correctly signaling the termination of the call session. After invoking this method, it's a good practice to handle UI transitions and take any other post-call actions that may be necessary in your application's context.

## Listeners and Implementation

Listeners are pivotal in monitoring and responding to real-time events during the call. 

In the provided code, the `BandwidthSessionEventListener` is used. This listener has multiple callback methods:

- `callTerminated`: Invoked when a call is terminated.
- `callProgress`: Triggered when there's a progress update in the call.

**Implementation**:

To use the listener, you implement it as an anonymous class and provide logic inside each method:

```kotlin
bandwidthSession.addSessionEventListener(object : BandwidthSessionEventListener {
    override fun callTerminated(session: BandwidthSession?, info: TerminationInfo?) {
        // Handle call termination
    }

    override fun callProgress(session: BandwidthSession?) {
        // Handle call progress
    }

    override fun incomingNotify(event: NotifyEvent?, dtmfValue: String?) {
        // Handle other events
    }
})
```

## Configuring the User Agent

`setUserAgentConfig` is a critical method that establishes the settings for the user agent, ensuring correct communication with Bandwidth services.

The method requires:

- **Server Configurations**: 
  - `proxyAddress`: The address of the proxy.
  - `port`: The port number.
  - `domain`: The domain name.
  - `transport`: Type of transport (e.g., `Transport.TLS`).
  - `oAuthTokenUrl`: URL for token authentication.
  - `authHeaderUser`: User header for authentication.
  - `authHeaderPassword`: Password header for authentication.

- **Account Details**:
  - `username`: Account's username.
  - `displayName`: Display name associated with the account.
  - `password`: Account's password.

These values should be fetched from the `config.properties` file, ensuring sensitive information isn't hard-coded.

## Error Handling

Errors, especially in networked operations, are inevitable. Ensure you catch, manage, and inform users about these, fostering a seamless experience.
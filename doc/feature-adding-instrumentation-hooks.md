# Feature: Attempts and timeout per request. Hooks for instrumentation of RadiusClient

## Justification for the usefulness of the feature

The `RadiusClient` object uses a `FixedTimeoutHandler` passed on creation which, as the name implies, uses the same values for `maxAttempts` and `timeoutMillis` for all requests. If using `Tinyradius-netty` as the core of a fully fledged radius server/proxy implementation, this poses a limitation, since it is usually desirable to have different values for those parameters for different upstream radius servers, while wanting to use the same `RadiusClient` instance.

In addition, also for a fully fledged radius server/proxy implementation, it is important to have metrics for packets sent/received/timed-out. The current implementation of `RadiusClient` implements iternally a retry strategy, so that applications using it cannot record the number of times a packet was sent to an upstream radius server.

## Rationale for the proposed implementation

One possiblity would be to change the `TimeoutHandler.onTimeout` interface method to include the value of the timeout, but that would require a relatively major change. An approach that only adds new methods to the `RadiusClient` has been followed, ensuring backwards compatiblity, and not needing to rewrite any tests (only adding new ones).

The modified `RadiusClient` has additional methods to:
- Create internally and use another TimeoutHandler, instead of using the one specified at instantiation time (which is used as default), with parametrizable `maxAttempts` and `timeoutMillis`. 
- Invoke Java functions ("hooks") when a packet is sent, when a packet is received and when a timeout is fired. Those "hooks" may be used by the external application to record radius client metrics. The hook has two parameters: the type (request in case of recording a sending or a timeout, response in case of recording a response), and the destination InetSocketAddress.

## Implementation

An interface is created to encapsulate the hook calls, to avoid having methods with too many parameters (`RadiusClientHooks`)

```java
public interface RadiusClientHooks {
    void preSendHook(int code, InetSocketAddress address);
    void timeoutHook(int code, InetSocketAddress address);
    void postReceiveHook(int code, InetSocketAddress address);
}
```

The signature of the enriched `communicate` method is as follows

```java
public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, int maxAttempts, int timeoutMillis,
            RadiusClientHooks hooks)
```

Where, in `RadiusClientHooks`, `preSendHook` is invoked before sending the radius client request, `timeoutHook` is invoked when a timeout is fired, and `postReceiveHook` is invoked when a response is received, the `type` Byte parameter being the response type.

And also analogous versions for `communicate` for a single Endpoint, such as

```java
public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, int maxAttempts, int timeoutMillis,
            RadiusClientHooks hooks)
```

The implementation is pretty trivial, the only issue being the long list of parameters.

## Note

Happy to work on any other approach you may suggest to implement similar functionality.


## One additional change

Moved from `info` to `debug` 
```java
log.info("Found request for response identifier {}, proxyState requestId '{}'",...)
```

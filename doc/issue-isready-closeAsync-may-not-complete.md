# Issue isReady and closeAsync may not reach completion

## Description

Lines such as to `server.isReady().syncUninterruptibly()` and `closeAsync().sync()` sometimes block forever. The issue is not deterministic, pointing to some kind of race condition.

This is reproducible in the unit tests, which may finish or not for this reason.

In order to force the error, I simply run the tests a hundred times. Sooner or later, the problem arises.

```bash
for i in {1..100}; do ./gradlew clean && ./gradlew test; done
```

## Analysis

The problem seems to be related to the Future returned in `RadiusServer.isReady()` and `RadiusServer.closeAsync()`.

Both of them are generated with the same pattern. A `PromiseCombiner` triggers its completion. In both cases, the code is something like this:

```java
isReady = eventLoopGroup.next().newPromise(); // or isClosed
final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
// ... Generate a list of channel futures ...
combiner.addAll(channelFutures.toArray(ChannelFuture[]::new));
combiner.finish(isReady);
```

This should be ok to me. The unit tests in netty for `PromiseCombiner` also make use of `ImmediateEventExcecutor.INSTANCE`, but there must be some subtelities lurking there. The javadoc for `PromiseCombiner` says `This implementation is NOT thread-safe and all methods must be called from the EventExecutor thread`.

`Warning, speculation here` The code in `PromiseCombiner` (netty project, not tinyradius) makes some effort to ensure that both the `add` and `finish` methods, and the listeners for event completion are executed in the EventLoop, but `ImmediateEventExecutor` behaves in such a way that in reality this is not waranteed (`ImmediateEventExecutor.inEventLoop()` always returns `true`, so that, effectively, the `add` and `finish` methods execute in different threads than the listeners for event completion).

If I don't use ImmediateEventExecutor, but the netty EventLoop, the undeterministing blocking behaviour disapears. The pseudocode is (in `RadiusServer.java`)

```java
EventLoop eventLoop = eventLoopGroup.next();
isReady = eventLoop.newPromise();
// Not using ImmediateEventExecutor
final PromiseCombiner combiner = new PromiseCombiner(eventLoop);

// ... Generate a list of channel futures ...

// As stated in the javadoc for PromiseCombiner, addAll and finsish must run in the EventExecutor thread
eventLoop.execute(() ->{
    combiner.addAll(channelFutures.toArray(ChannelFuture[]::new));
    combiner.finish(isReady);
});
```

## Additional info

Typical result (notice has been running for 7m 21s)

```
Gradle Test Executor 1 STANDARD_OUT
    15:43:55.175 [multiThreadIoEventLoopGroup-9-1] DEBUG org.tinyradius.io.client.RadiusClient - Attempt 1, sending packet to 0.0.0.0/0.0.0.0:2
    15:43:55.270 [pool-12-thread-1] DEBUG org.tinyradius.io.client.RadiusClient - Attempt 2, sending packet to 0.0.0.0/0.0.0.0:2
    15:43:55.370 [multiThreadIoEventLoopGroup-9-1] WARN  org.tinyradius.io.client.RadiusClient - Client send timeout - max attempts reached: 2
<===========--> 88% EXECUTING [7m 21s]
> IDLE
> IDLE
> IDLE
> IDLE
> :test > Executing test org.tinyradius.io.server.RadiusServerTest
> IDLE
> IDLE
> :test > 204 tests completed, 1 failed, 2 skipped
> IDLE
```

Relevant jstack output
```
"ForkJoinPool-1-worker-5" #23 daemon prio=5 os_prio=0 cpu=1101.39ms elapsed=323.27s tid=0x00007f8b04000fe0 nid=0xe74 in Object.wait()  [0x00007f8bc96f7000]
   java.lang.Thread.State: WAITING (on object monitor)
        at java.lang.Object.wait(java.base@17.0.17/Native Method)
        - waiting on <0x00000000e18105b8> (a io.netty.util.concurrent.DefaultPromise)
        at java.lang.Object.wait(java.base@17.0.17/Unknown Source)
        at io.netty.util.concurrent.DefaultPromise.awaitUninterruptibly(DefaultPromise.java:290)
        - locked <0x00000000e18105b8> (a io.netty.util.concurrent.DefaultPromise)
        at io.netty.util.concurrent.DefaultPromise.syncUninterruptibly(DefaultPromise.java:426)
        at io.netty.util.concurrent.DefaultPromise.syncUninterruptibly(DefaultPromise.java:37)
        at org.tinyradius.io.server.RadiusServerTest.serverStartStop(RadiusServerTest.java:40)
        at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(java.base@17.0.17/Native Method)
        at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(java.base@17.0.17/NativeMethodAccessorImpl.java:77)
        at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(java.base@17.0.17/DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(java.base@17.0.17/Method.java:569)
```
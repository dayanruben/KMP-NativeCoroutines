# KMP-NativeCoroutines

A library to use Kotlin Coroutines from Swift code in KMP apps.

## Why this library?

Both KMP and Kotlin Coroutines are amazing but together they have some limitations.

The most important limitation is cancellation support.  
Kotlin suspend functions are exposed to Swift as functions with a completion handler.  
This allows you to easily use them from your Swift code, but it doesn't support cancellation.

> **Note**: while Swift 5.5 brings async functions to Swift, it doesn't solve this issue.  
> For interoperability with ObjC all functions with a completion handler can be called like an async function.  
> This means starting with Swift 5.5 your Kotlin suspend functions will look like Swift async functions.  
> But that's just syntactic sugar, so there's still no cancellation support.

Besides cancellation support, ObjC doesn't support generics on protocols.  
So all the `Flow` interfaces lose their generic value type which make them hard to use.

This library solves both of these limitations 😄.

## Compatibility

> **Note**: version `0.13` and above only support the [new Kotlin Native memory model][new-mm].  
> Previous versions were using the [`-native-mt`][native-mt] versions of the kotlinx.coroutines library.  
> To use the new memory model with older versions you should use the `-new-mm` variant.

[new-mm]: https://github.com/JetBrains/kotlin/blob/0b871d7534a9c8e90fb9ad61cd5345716448d08c/kotlin-native/NEW_MM.md
[native-mt]: https://github.com/kotlin/kotlinx.coroutines/issues/462

The latest version of the library uses Kotlin version `1.7.20`.  
Compatibility versions for older Kotlin versions are also available:

| Version      | Version suffix    |   Kotlin   |   Coroutines    |
|--------------|-------------------|:----------:|:---------------:|
| **_latest_** | **_no suffix_**   | **1.7.20** |    **1.6.4**    |
| 0.13.0       | _no suffix_       |   1.7.10   |      1.6.4      |
| 0.12.6       | -kotlin-1.7.20-RC | 1.7.20-RC  |      1.6.4      |
| 0.12.6       | -new-mm           |   1.7.10   |      1.6.3      |
| 0.12.6       | _no suffix_       |   1.7.10   | 1.6.3-native-mt |
| 0.12.5       | -new-mm           |   1.7.0    |      1.6.3      |
| 0.12.5       | _no suffix_       |   1.7.0    | 1.6.3-native-mt |

You can choose from a couple of Swift implementations.  
Depending on the implementation you can support as low as iOS 9, macOS 10.9, tvOS 9 and watchOS 3:

| Implementation | Swift | iOS  | macOS | tvOS | watchOS |
|----------------|:-----:|:----:|:-----:|:----:|:-------:|
| Async          |  5.5  | 13.0 | 10.15 | 13.0 |   6.0   |
| Combine        |  5.0  | 13.0 | 10.15 | 13.0 |   6.0   |
| RxSwift        |  5.0  | 9.0  | 10.9  | 9.0  |   3.0   |

## Installation

The library consists of a Kotlin and Swift part which you'll need to add to your project.  
The Kotlin part is available on Maven Central and the Swift part can be installed via CocoaPods 
or the Swift Package Manager.

Make sure to always use the same versions for all the libraries!

[![latest release](https://img.shields.io/github/v/release/rickclephas/KMP-NativeCoroutines?label=latest%20release&sort=semver)](https://github.com/rickclephas/KMP-NativeCoroutines/releases)

### Kotlin

For Kotlin just add the plugin to your `build.gradle.kts`:
```kotlin
plugins {
    id("com.rickclephas.kmp.nativecoroutines") version "<version>"
}
```

### Swift (Swift Package Manager)

The Swift implementations are available via the Swift Package Manager.  
Just add it to your `Package.swift` file:
```swift
dependencies: [
    .package(url: "https://github.com/rickclephas/KMP-NativeCoroutines.git", from: "<version>")
]
```

Or add it in Xcode by going to `File` > `Add Packages...` and providing the URL:
`https://github.com/rickclephas/KMP-NativeCoroutines.git`.

> **Note**: the version for the Swift package should not contain the Kotlin version suffix
> (e.g. `-new-mm` or `-kotlin-1.6.0`).

> **Note**: if you only need a single implementation you can also use the SPM specific versions with suffixes
> `-spm-async`, `-spm-combine` and `-spm-rxswift`.

### Swift (CocoaPods)

If you use CocoaPods add one or more of the following libraries to your `Podfile`:
```ruby
pod 'KMPNativeCoroutinesAsync', '<version>'    # Swift 5.5 Async/Await implementation
pod 'KMPNativeCoroutinesCombine', '<version>'  # Combine implementation
pod 'KMPNativeCoroutinesRxSwift', '<version>'  # RxSwift implementation
```
> **Note**: the version for CocoaPods should not contain the Kotlin version suffix (e.g. `-new-mm` or `-kotlin-1.6.0`).

## Usage

Using your Kotlin Coroutines code from Swift is almost as easy as calling the Kotlin code.   
Just use the wrapper functions in Swift to get async functions, AsyncStreams, Publishers or Observables.

### Kotlin

> **Warning**: the Kotlin part of this library consists of helper functions and a Kotlin compiler plugin.  
> Using the plugin removes the boilerplate code from your project, however **Kotlin compiler plugins aren't stable**!
> 
> The plugin is known to cause recursion errors in some scenarios such as in [#4][GH-4] and [#23][GH-23].  
> To prevent such recursion errors it's best to explicitly define the (return) types of public 
> properties and functions.

[GH-4]: https://github.com/rickclephas/KMP-NativeCoroutines/issues/4
[GH-23]: https://github.com/rickclephas/KMP-NativeCoroutines/issues/23

The plugin will automagically generate the necessary code for you! 🔮

Your `Flow` properties/functions get a `Native` version:
```kotlin
class Clock {
    // Somewhere in your Kotlin code you define a Flow property
    val time: StateFlow<Long> // This can be any kind of Flow

    // The plugin will generate this native property for you
    val timeNative
        get() = time.asNativeFlow()
}
```

In case of a `StateFlow` or `SharedFlow` property you also get a `NativeValue` or `NativeReplayCache` property:
```kotlin
// For the StateFlow defined above the plugin will generate this native value property
val timeNativeValue
    get() = time.value

// In case of a SharedFlow the plugin would generate this native replay cache property
val timeNativeReplayCache
    get() = time.replayCache
```

The plugin also generates `Native` versions for all your suspend functions:
```kotlin
class RandomLettersGenerator {
    // Somewhere in your Kotlin code you define a suspend function
    suspend fun getRandomLetters(): String { 
        // Code to generate some random letters
    }

    // The plugin will generate this native function for you
    fun getRandomLettersNative() = 
        nativeSuspend { getRandomLetters() }
}
```

#### Global properties and functions

The plugin is currently unable to generate native versions for global properties and functions.  
In such cases you have to manually create the native versions in your Kotlin native code.

#### Custom suffix

If you don't like the naming of these generated properties/functions, you can easily change the suffix.  
For example add the following to your `build.gradle.kts` to use the suffix `Apple`:
```kotlin
nativeCoroutines {
    suffix = "Apple"
}
```

#### Custom CoroutineScope

For more control you can provide a custom `CoroutineScope` with the `NativeCoroutineScope` annotation:
```kotlin
class Clock {
    @NativeCoroutineScope
    internal val coroutineScope = CoroutineScope(job + Dispatchers.Default)
}
```

If you don't provide a `CoroutineScope` the default scope will be used which is defined as:
```kotlin
internal val defaultCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

#### Ignoring declarations

Use the `NativeCoroutinesIgnore` annotation to tell the plugin to ignore a property or function:
```kotlin
@NativeCoroutinesIgnore
val ignoredFlowProperty: Flow<Int>

@NativeCoroutinesIgnore
suspend fun ignoredSuspendFunction() { }
```

### Swift 5.5 Async/Await

The Async implementation provides some functions to get async Swift functions and `AsyncStream`s.

Use the `asyncFunction(for:)` function to get an async function that can be awaited:
```swift
let handle = Task {
    do {
        let letters = try await asyncFunction(for: randomLettersGenerator.getRandomLettersNative())
        print("Got random letters: \(letters)")
    } catch {
        print("Failed with error: \(error)")
    }
}

// To cancel the suspend function just cancel the async task
handle.cancel()
```

or if you don't like these do-catches you can use the `asyncResult(for:)` function:
```swift
let result = await asyncResult(for: randomLettersGenerator.getRandomLettersNative())
if case let .success(letters) = result {
    print("Got random letters: \(letters)")
}
```

For `Flow`s there is the `asyncSequence(for:)` function to get an `AsyncSequence`:
```swift
let handle = Task {
    do {
        let sequence = asyncSequence(for: randomLettersGenerator.getRandomLettersFlowNative())
        for try await letters in sequence {
            print("Got random letters: \(letters)")
        }
    } catch {
        print("Failed with error: \(error)")
    }
}

// To cancel the flow (collection) just cancel the async task
handle.cancel()
```

### Combine

The Combine implementation provides a couple functions to get an `AnyPublisher` for your Coroutines code.

For your `Flow`s use the `createPublisher(for:)` function:
```swift
// Create an AnyPublisher for your flow
let publisher = createPublisher(for: clock.timeNative)

// Now use this publisher as you would any other
let cancellable = publisher.sink { completion in
    print("Received completion: \(completion)")
} receiveValue: { value in
    print("Received value: \(value)")
}

// To cancel the flow (collection) just cancel the publisher
cancellable.cancel()
```

For the suspend functions you should use the `createFuture(for:)` function:
```swift
// Create a Future/AnyPublisher for the suspend function
let future = createFuture(for: randomLettersGenerator.getRandomLettersNative())

// Now use this future as you would any other
let cancellable = future.sink { completion in
    print("Received completion: \(completion)")
} receiveValue: { value in
    print("Received value: \(value)")
}

// To cancel the suspend function just cancel the future
cancellable.cancel()
```

You can also use the `createPublisher(for:)` function for suspend functions that return a `Flow`:
```swift
let publisher = createPublisher(for: randomLettersGenerator.getRandomLettersFlowNative())
```

> **Note**: these functions create deferred `AnyPublisher`s.  
> Meaning every subscription will trigger the collection of the `Flow` or execution of the suspend function.

### RxSwift

The RxSwift implementation provides a couple functions to get an `Observable` or `Single` for your Coroutines code.

For your `Flow`s use the `createObservable(for:)` function:
```swift
// Create an observable for your flow
let observable = createObservable(for: clock.timeNative)

// Now use this observable as you would any other
let disposable = observable.subscribe(onNext: { value in
    print("Received value: \(value)")
}, onError: { error in
    print("Received error: \(error)")
}, onCompleted: {
    print("Observable completed")
}, onDisposed: {
    print("Observable disposed")
})

// To cancel the flow (collection) just dispose the subscription
disposable.dispose()
```

For the suspend functions you should use the `createSingle(for:)` function:
```swift
// Create a single for the suspend function
let single = createSingle(for: randomLettersGenerator.getRandomLettersNative())

// Now use this single as you would any other
let disposable = single.subscribe(onSuccess: { value in
    print("Received value: \(value)")
}, onFailure: { error in
    print("Received error: \(error)")
}, onDisposed: {
    print("Single disposed")
})

// To cancel the suspend function just dispose the subscription
disposable.dispose()
```

You can also use the `createObservable(for:)` function for suspend functions that return a `Flow`:
```swift
let observable = createObservable(for: randomLettersGenerator.getRandomLettersFlowNative())
```

> **Note**: these functions create deferred `Observable`s and `Single`s.  
> Meaning every subscription will trigger the collection of the `Flow` or execution of the suspend function.

package com.rickclephas.kmp.nativecoroutines

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.isFrozen
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeFlowTests {

    @Test
    fun `ensure frozen`() {
        val flow = flow<RandomValue> {  }
        assertFalse(flow.isFrozen, "Flow shouldn't be frozen yet")
        val nativeFlow = flow.asNativeFlow()
        assertTrue(nativeFlow.isFrozen, "NativeFlow should be frozen")
        assertTrue(flow.isFrozen, "Flow should be frozen")
    }

    @Test
    fun `ensure completion callback is invoked`() = runTest {
        val flow = flow<RandomValue> {  }
        val job = Job()
        val nativeFlow = flow.asNativeFlow(CoroutineScope(job))
        val completionCount = atomic(0)
        nativeFlow({ _, _ -> }, { error, _ ->
            assertNull(error, "Flow should complete without an error")
            completionCount.incrementAndGet()
        })
        job.children.forEach { it.join() } // Waits for the collection to complete
        assertEquals(1, completionCount.value, "Completion callback should be called once")
    }

    @Test
    fun `ensure exceptions are received as errors`() = runTest {
        val exception = RandomException()
        val flow = flow<RandomValue> { throw exception }
        val job = Job()
        val nativeFlow = flow.asNativeFlow(CoroutineScope(job))
        val completionCount = atomic(0)
        nativeFlow({ _, _ -> }, { error, _ ->
            assertNotNull(error, "Flow should complete with an error")
            val kotlinException = error.userInfo["KotlinException"]
            assertSame(exception, kotlinException, "Kotlin exception should be the same exception")
            completionCount.incrementAndGet()
        })
        job.children.forEach { it.join() } // Waits for the collection to complete
        assertEquals(1, completionCount.value, "Completion callback should be called once")
    }

    @Test
    fun `ensure values are received`() = runTest {
        val values = listOf(RandomValue(), RandomValue(), RandomValue(), RandomValue())
        val flow = flow { values.forEach { emit(it) } }
        val job = Job()
        val nativeFlow = flow.asNativeFlow(CoroutineScope(job))
        val receivedValueCount = atomic(0)
        nativeFlow({ value, _ ->
            assertSame(values[receivedValueCount.value], value, "Received incorrect value")
            receivedValueCount.incrementAndGet()
        }, { _, _ -> })
        job.children.forEach { it.join() } // Waits for the collection to complete
        assertEquals(values.size, receivedValueCount.value, "Item callback should be called for every value")
    }

    @Test
    fun `ensure collection is cancelled`() = runTest {
        val flow = MutableSharedFlow<RandomValue>()
        val job = Job()
        val nativeFlow = flow.asNativeFlow(CoroutineScope(job))
        val completionCount = atomic(0)
        val cancel = nativeFlow({ _, _ -> }, { error, _ ->
            assertNotNull(error, "Flow should complete with an error")
            val exception = error.userInfo["KotlinException"]
            assertIs<CancellationException>(exception, "Error should contain CancellationException")
            completionCount.incrementAndGet()
        })
        delay(100) // Gives the collection some time to start
        cancel()
        job.children.forEach { it.join() } // Waits for the collection to complete
        assertEquals(1, completionCount.value, "Completion callback should be called once")
    }
}
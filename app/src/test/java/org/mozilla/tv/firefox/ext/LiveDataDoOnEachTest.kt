package org.mozilla.tv.firefox.ext

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule

class LiveDataDoOnEachTest {

    @get:Rule
    val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var observerSpy: Observer<Int>
    private lateinit var liveData: MutableLiveData<Int>
    private var uninitializedValue: Int? = null

    @Before
    fun setup() {
        liveData = MutableLiveData()
    }

    @Test
    fun `side effects should be executed`() {
        observerSpy = spy(Observer { })

        liveData.doOnEach { uninitializedValue = it }
            .observeForever(observerSpy)

        liveData.value = 1
        assertNotNull(uninitializedValue)
        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `passed value should not be changed`() {
        observerSpy = spy(Observer { assertEquals(1, it) })

        liveData.doOnEach { uninitializedValue = it!! * 5 }
            .observeForever(observerSpy)

        liveData.value = 1
        verify(observerSpy, times(1)).onChanged(any())
    }
}

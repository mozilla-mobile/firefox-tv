package org.mozilla.tv.firefox.utils

import android.arch.lifecycle.MutableLiveData

/**
 * Used in situations where postValue should not be used
 */
class SetOnlyLiveData<T> : MutableLiveData<T>() {

    @Deprecated("postValue should not be used with instances of SetOnlyLiveData", replaceWith = ReplaceWith("setValue"))
    override fun postValue(value: T) {
        throw NotImplementedError("Method 'postValue' called on an instance of SetOnlyLiveData\nUse setValue instead")
    }
}

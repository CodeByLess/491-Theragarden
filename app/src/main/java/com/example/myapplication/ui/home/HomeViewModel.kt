package com.example.myapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    // Mutable LiveData used internally to store text value
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment" // Default text value
    }

    // Exposed LiveData so UI can observe changes safely
    val text: LiveData<String> = _text
}
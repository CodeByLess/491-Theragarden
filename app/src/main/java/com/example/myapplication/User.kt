package com.example.myapplication

data class User(var firstName : String ?= null,
                var lastName : String ?= null,
                var dateOfBirth : String ?= null,
                var country : String ?= null,
                var profileImageUrl: String ?= null
)

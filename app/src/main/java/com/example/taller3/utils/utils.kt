package com.example.taller3.utils

fun validateForm(email:String, password:String):Boolean {
    if (!email.isEmpty() &&
        validEmailAddress(email) &&
        !password.isEmpty() &&
        password.length >= 6){
        return true
    }
    return false
}
private fun validEmailAddress(email:String):Boolean {
    val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
    return email.matches(regex.toRegex())
}
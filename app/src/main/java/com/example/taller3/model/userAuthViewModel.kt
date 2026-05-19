package com.example.taller3.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class UserAuthState(
    val email : String = "",
    val password : String = "",
    val firstName: String = "",
    val lastName: String = "",
    val idNumber: String = "",
    val emailError:String = "",
    val passError : String = ""
)

class UserAuthViewModel : ViewModel(){
    val _user = MutableStateFlow<UserAuthState>(UserAuthState())
    val user = _user.asStateFlow()

    fun updateFirstName(newName: String) {
        _user.value = _user.value.copy(firstName = newName)
    }

    fun updateLastName(newName: String) {
        _user.value = _user.value.copy(lastName = newName)
    }

    fun updateIdNumber(newId: String) {
        _user.value = _user.value.copy(idNumber = newId)
    }

    fun updateEmailClass(newEmail : String){
        _user.value = _user.value.copy(email=newEmail)
    }
    fun updatePassClass(newPass : String){
        _user.value = _user.value.copy(password=newPass)
    }
    fun updateEmailError(error:String){
        _user.value = _user.value.copy(emailError = error)
    }
    fun updatePassError(error:String){
        _user.value = _user.value.copy(passError = error)
    }
}
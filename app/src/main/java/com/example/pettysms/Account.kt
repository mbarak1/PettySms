package com.example.pettysms

import java.io.Serializable

class Account(val id: Int?, var name: String?, var owner: Owner?, var type: String?, var currency: String? = "Kenyan Shilling", var accountNumber: String?, var isDeleted: Boolean = false){

}
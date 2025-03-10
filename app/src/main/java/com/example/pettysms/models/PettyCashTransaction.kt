package com.example.pettysms.models

data class PettyCashTransaction(
    val id: Int? = null,
    val pettyCashNumber: String? = null,
    val date: String? = null,
    val amount: Double? = null,
    val description: String? = null,
    val paymentMode: String? = null,
    val transactorId: Int? = null,
    val transactorName: String? = null,
    val ownerId: Int? = null,
    val ownerName: String? = null,
    val truckId: Int? = null,
    val truckNumber: String? = null,
    val truckIds: String? = null,
    val accountId: Int? = null,
    val accountName: String? = null
) 
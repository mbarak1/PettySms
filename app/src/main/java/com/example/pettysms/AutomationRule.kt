package com.example.pettysms

import java.io.Serializable

data class AutomationRule(
    var id: Int? = null,
    var name: String? = null,
    var transactorId: Int? = null,
    var transactorName: String? = null,
    var accountId: Int? = null,
    var accountName: String? = null,
    var ownerId: Int? = null,
    var ownerName: String? = null,
    var truckId: Int? = null,
    var truckName: String? = null,
    var descriptionPattern: String? = null,
    var minAmount: Double? = null,
    var maxAmount: Double? = null,
    var createdAt: String? = null,
    var updatedAt: String? = null
) : Serializable 
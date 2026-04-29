package com.kyovo.domain.port.secondary

interface TransactionPort
{
    fun <T> executeInTransaction(block: () -> T): T
}

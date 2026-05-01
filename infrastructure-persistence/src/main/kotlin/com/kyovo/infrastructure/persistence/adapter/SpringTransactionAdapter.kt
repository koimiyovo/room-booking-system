package com.kyovo.infrastructure.persistence.adapter

import com.kyovo.domain.port.secondary.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class SpringTransactionAdapter(transactionManager: PlatformTransactionManager) : TransactionPort
{
    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun <T> executeInTransaction(block: () -> T): T
    {
        return transactionTemplate.execute { block() }
            ?: error("Transaction block returned null — block must return a non-null value")
    }
}

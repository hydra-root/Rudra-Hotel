package com.teasupplier.app.domain.engine

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Smart route & sequence prediction algorithm using weighted transition matrices
 * and exponential time-decay for delivery customer optimization.
 */

import com.teasupplier.app.data.local.dao.RouteDao
import com.teasupplier.app.data.local.dao.SaleDao
import com.teasupplier.app.data.local.entity.FirstCustomerHistoryEntity
import com.teasupplier.app.data.local.entity.TransitionEntity
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.utils.DateUtils
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class RoutePredictionEngine @Inject constructor(
    private val routeDao: RouteDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao
) {
    
    // Decay weight by 10% each week
    private fun calculateDecayedWeight(transition: TransitionEntity): Double {
        val now = System.currentTimeMillis()
        val diffMs = now - transition.lastUpdated
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
        
        val decayPeriods = diffDays / 7
        val weight = transition.weight * Math.pow(0.9, decayPeriods.toDouble())
        return maxOf(0.1, weight) // Minimum weight to prevent zeroing out
    }

    suspend fun recordSaleTransition(customerId: Long, timestamp: Long) {
        val startOfDay = DateUtils.getStartOfDay(timestamp)
        val endOfDay = DateUtils.getEndOfDay(timestamp)
        
        // Find previous sale today to create transition
        val todaySales = saleDao.getSalesByDateOrderedBySequence(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        
        // Filter out the one we just saved if it's already in the list
        // Wait, normally recordSaleTransition is called AFTER saving. 
        // So we need to find the sale immediately preceding this one.
        val previousSale = todaySales
            .filter { it.customerId != customerId } // Don't transition to self
            .maxByOrNull { it.sequenceNumber }
            
        if (previousSale == null && todaySales.size <= 1) {
            // First sale of the day
            val firstCustomerHistory = FirstCustomerHistoryEntity(
                date = startOfDay,
                customerId = customerId
            )
            routeDao.insertFirstCustomer(firstCustomerHistory)
        } else if (previousSale != null) {
            // Record transition from previousSale.customerId to customerId
            val existingTransition = routeDao.getTransition(previousSale.customerId, customerId)
            
            val newTransition = if (existingTransition != null) {
                val decayedWeight = calculateDecayedWeight(existingTransition)
                existingTransition.copy(
                    transitionCount = existingTransition.transitionCount + 1,
                    weight = decayedWeight + 1.0, // Add to weight for recent occurrence
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                TransitionEntity(
                    fromCustomerId = previousSale.customerId,
                    toCustomerId = customerId,
                    transitionCount = 1,
                    weight = 1.0,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            routeDao.insertOrUpdateTransition(newTransition)
        }
    }

    suspend fun predictNextCustomer(currentCustomerId: Long): PredictionResult? {
        val transitions = routeDao.getTransitionsFrom(currentCustomerId)
        if (transitions.isEmpty()) return null
        
        // Sort by decayed weight * transition count
        val scoredTransitions = transitions.map {
            val score = calculateDecayedWeight(it) * it.transitionCount
            it to score
        }.sortedByDescending { it.second }
        
        val topPrediction = scoredTransitions.first()
        val totalScore = scoredTransitions.sumOf { it.second }
        val confidence = (topPrediction.second / totalScore) * 100.0
        
        val predictedCustomer = customerDao.getCustomerById(topPrediction.first.toCustomerId)
            ?: return null
            
        val alternatePredictions = scoredTransitions.drop(1).mapNotNull {
            customerDao.getCustomerById(it.first.toCustomerId)
        }

        return PredictionResult(
            predictedCustomer = predictedCustomer,
            confidence = confidence,
            alternatives = alternatePredictions
        )
    }

    suspend fun predictFirstCustomer(): PredictionResult? {
        val frequencies = routeDao.getMostFrequentFirstCustomers()
        if (frequencies.isEmpty()) return null
        
        val topFrequency = frequencies.first()
        val totalFrequency = frequencies.sumOf { it.frequency }
        val confidence = (topFrequency.frequency.toDouble() / totalFrequency) * 100.0
        
        val predictedCustomer = customerDao.getCustomerById(topFrequency.customerId)
            ?: return null
            
        val alternatives = frequencies.drop(1).mapNotNull {
            customerDao.getCustomerById(it.customerId)
        }
        
        return PredictionResult(
            predictedCustomer = predictedCustomer,
            confidence = confidence,
            alternatives = alternatives
        )
    }
}

data class PredictionResult(
    val predictedCustomer: CustomerEntity,
    val confidence: Double,
    val alternatives: List<CustomerEntity> = emptyList()
)

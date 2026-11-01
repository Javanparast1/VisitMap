package com.javanparast.visitmap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.repository.CustomerRepository
import com.javanparast.visitmap.domain.CustomerInput
import com.javanparast.visitmap.domain.CustomerValidation
import com.javanparast.visitmap.domain.CustomerValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val query: String = "",
    val selectedCustomer: CustomerEntity? = null,
    val message: String? = null,
    val isBusy: Boolean = false
)

class CustomerViewModel(private val repository: CustomerRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCustomers().collect { rows ->
                _state.value = _state.value.copy(customers = rows)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query)
        viewModelScope.launch {
            repository.searchCustomers(query).collect { rows ->
                _state.value = _state.value.copy(customers = rows)
            }
        }
    }

    fun select(customer: CustomerEntity?) {
        _state.value = _state.value.copy(selectedCustomer = customer, message = null)
    }

    fun save(input: CustomerInput) {
        val validation = CustomerValidation.validate(input)
        if (validation is CustomerValidationResult.Invalid) {
            _state.value = _state.value.copy(message = validation.message)
            return
        }
        val valid = (validation as CustomerValidationResult.Valid).input
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            try {
                val now = System.currentTimeMillis()
                val selected = _state.value.selectedCustomer
                if (selected == null) {
                    repository.registerCustomer(
                        CustomerEntity(
                            name = valid.name,
                            customerType = valid.customerType,
                            phone = valid.phone,
                            contactName = valid.contactName,
                            description = valid.description,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    _state.value = _state.value.copy(message = "فروشگاه ثبت شد.")
                } else {
                    val updated = selected.copy(
                        name = valid.name,
                        customerType = valid.customerType,
                        phone = valid.phone,
                        contactName = valid.contactName,
                        description = valid.description,
                        updatedAt = now
                    )
                    if (repository.updateCustomer(updated) == 1) {
                        _state.value = _state.value.copy(
                            selectedCustomer = updated,
                            message = "اطلاعات فروشگاه به‌روزرسانی شد."
                        )
                    } else {
                        _state.value = _state.value.copy(message = "رکورد فروشگاه پیدا نشد؛ فهرست را تازه‌سازی کنید.")
                    }
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(message = "ذخیره اطلاعات انجام نشد: ${error.message ?: "خطای نامشخص"}")
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun setActive(active: Boolean) {
        val selected = _state.value.selectedCustomer ?: return
        viewModelScope.launch {
            val updated = selected.copy(
                status = if (active) CustomerEntity.STATUS_ACTIVE else CustomerEntity.STATUS_INACTIVE,
                updatedAt = System.currentTimeMillis()
            )
            try {
                if (repository.updateCustomer(updated) == 1) {
                    _state.value = _state.value.copy(
                        selectedCustomer = updated,
                        message = if (active) "فروشگاه دوباره فعال شد." else "فروشگاه غیرفعال شد."
                    )
                } else {
                    _state.value = _state.value.copy(message = "تغییر وضعیت انجام نشد؛ رکورد پیدا نشد.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(message = "تغییر وضعیت انجام نشد: ${error.message ?: "خطای نامشخص"}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}

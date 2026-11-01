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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val query: String = "",
    val selectedCustomer: CustomerEntity? = null,
    val message: String? = null,
    val isBusy: Boolean = false,
    val saveCompleted: Boolean = false,
    val saveCommitted: Boolean = false,
    val deleteCompleted: Boolean = false
)

class CustomerViewModel(private val repository: CustomerRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            query.flatMapLatest { text ->
                if (text.isBlank()) repository.observeCustomers()
                else repository.searchCustomers(text)
            }.collect { rows -> _state.update { it.copy(customers = rows) } }
        }
    }

    fun search(value: String) {
        _state.update { it.copy(query = value) }
        query.value = value
    }

    fun select(customer: CustomerEntity?) {
        _state.update { it.copy(selectedCustomer = customer, message = null, saveCompleted = false, saveCommitted = false, deleteCompleted = false) }
    }


    fun save(input: CustomerInput) {
        if (_state.value.saveCommitted) return
        val validation = CustomerValidation.validate(input)
        if (validation is CustomerValidationResult.Invalid) {
            _state.update { it.copy(message = validation.message) }
            return
        }
        val valid = (validation as CustomerValidationResult.Valid).input

        // Acquire the save gate synchronously, before launching a coroutine. This
        // prevents rapid repeated taps from scheduling multiple inserts/updates.
        while (true) {
            val current = _state.value
            if (current.isBusy) return
            if (_state.compareAndSet(
                    current,
                    current.copy(isBusy = true, message = null, saveCompleted = false)
                )
            ) break
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val selected = _state.value.selectedCustomer
                if (selected == null) {
                    val customer = CustomerEntity(
                        name = valid.name,
                        customerType = valid.customerType,
                        phone = valid.phone,
                        contactName = valid.contactName,
                        description = valid.description,
                        createdAt = now,
                        updatedAt = now
                    )
                    repository.registerCustomer(customer)
                    _state.update {
                        it.copy(
                            selectedCustomer = customer,
                            message = "فروشگاه ثبت شد.",
                            saveCompleted = true,
                            saveCommitted = true
                        )
                    }
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
                        _state.update {
                            it.copy(
                                selectedCustomer = updated,
                                message = "اطلاعات فروشگاه به‌روزرسانی شد.",
                                saveCompleted = true,
                                saveCommitted = true
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(message = "رکورد فروشگاه پیدا نشد؛ فهرست را تازه‌سازی کنید.")
                        }
                    }
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(message = "ذخیره اطلاعات انجام نشد: ${error.message ?: "خطای نامشخص"}")
                }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun deletePermanently() {
        val selected = _state.value.selectedCustomer ?: return
        while (true) {
            val current = _state.value
            if (current.isBusy) return
            if (_state.compareAndSet(current, current.copy(isBusy = true, message = null))) break
        }
        viewModelScope.launch {
            try {
                if (repository.deleteCustomerPermanently(selected.customerId) == 1) {
                    _state.update { it.copy(selectedCustomer = null, message = null, deleteCompleted = true) }
                } else {
                    _state.update { it.copy(message = "رکورد فروشگاه پیدا نشد؛ حذف انجام نشد.") }
                }
            } catch (error: Exception) {
                _state.update { it.copy(message = "حذف انجام نشد: ${error.message ?: "خطای نامشخص"}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
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
                    _state.update {
                        it.copy(
                            selectedCustomer = updated,
                            message = if (active) "فروشگاه دوباره فعال شد." else "فروشگاه غیرفعال شد."
                        )
                    }
                } else {
                    _state.update { it.copy(message = "تغییر وضعیت انجام نشد؛ رکورد پیدا نشد.") }
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(message = "تغییر وضعیت انجام نشد: ${error.message ?: "خطای نامشخص"}")
                }
            }
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}

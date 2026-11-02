package com.javanparast.visitmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.javanparast.visitmap.data.database.VisitMapDatabase
import com.javanparast.visitmap.data.model.CustomerEntity
import com.javanparast.visitmap.data.repository.CustomerRepository
import com.javanparast.visitmap.domain.CustomerInput
import com.javanparast.visitmap.ui.CustomerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CustomerRepository(VisitMapDatabase.getInstance(applicationContext).visitMapDao())
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
                    return CustomerViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        setContent {
            val customerViewModel: CustomerViewModel = viewModel(factory = factory)
            VisitMapApp(customerViewModel)
        }
    }
}

private enum class Screen { LIST, FORM, DETAIL }

@Composable
private fun VisitMapApp(viewModel: CustomerViewModel) {
    val state by viewModel.state.collectAsState()
    var screen by remember { mutableStateOf(Screen.LIST) }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) screen = Screen.DETAIL
    }
    LaunchedEffect(state.deleteCompleted) {
        if (state.deleteCompleted) {
            viewModel.select(null)
            screen = Screen.LIST
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("ویزیت‌مپ", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    when (screen) {
                        Screen.LIST -> CustomerList(
                            state = state,
                            onSearch = viewModel::search,
                            onAdd = { viewModel.select(null); screen = Screen.FORM },
                            onSelect = { viewModel.select(it); screen = Screen.DETAIL }
                        )
                        Screen.FORM -> CustomerForm(
                            customer = state.selectedCustomer,
                            busy = state.isBusy,
                            message = state.message,
                            onBack = { viewModel.clearMessage(); screen = if (state.selectedCustomer == null) Screen.LIST else Screen.DETAIL },
                            onSave = {
                                viewModel.save(it)
                                // Successful saves navigate to the saved customer's detail screen.
                            }
                        )
                        Screen.DETAIL -> state.selectedCustomer?.let { customer ->
                            CustomerDetail(
                                customer = customer,
                                message = state.message,
                                busy = state.isBusy,
                                onDelete = viewModel::deletePermanently,
                                onBack = { viewModel.select(null); screen = Screen.LIST },
                                onEdit = { viewModel.select(customer); screen = Screen.FORM },
                                onToggleActive = { viewModel.setActive(it) }
                            )
                        } ?: run { screen = Screen.LIST }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerList(
    state: com.javanparast.visitmap.ui.CustomerUiState,
    onSearch: (String) -> Unit,
    onAdd: () -> Unit,
    onSelect: (CustomerEntity) -> Unit
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onSearch,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("جست‌وجوی نام فروشگاه، مسئول یا شماره تماس") },
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ثبت فروشگاه جدید") }
    Spacer(Modifier.height(8.dp))
    if (state.customers.isEmpty()) {
        Text(if (state.query.isBlank()) "هنوز فروشگاهی ثبت نشده است." else "نتیجه‌ای پیدا نشد.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.customers, key = { it.customerId }) { customer ->
                Card(Modifier.fillMaxWidth().clickable { onSelect(customer) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        Text(customerTypeLabel(customer.customerType))
                        Text(if (customer.status == CustomerEntity.STATUS_ACTIVE) "فعال" else "غیرفعال")
                        customer.contactName?.let { Text("مسئول: $it") }
                        customer.phone?.let { Text("تماس: $it") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerForm(
    customer: CustomerEntity?,
    busy: Boolean,
    message: String?,
    onBack: () -> Unit,
    onSave: (CustomerInput) -> Unit
) {
    var name by remember(customer?.customerId) { mutableStateOf(customer?.name.orEmpty()) }
    var type by remember(customer?.customerId) { mutableStateOf(customer?.customerType ?: CustomerEntity.TYPE_SUPERMARKET) }
    var phone by remember(customer?.customerId) { mutableStateOf(customer?.phone.orEmpty()) }
    var contact by remember(customer?.customerId) { mutableStateOf(customer?.contactName.orEmpty()) }
    var description by remember(customer?.customerId) { mutableStateOf(customer?.description.orEmpty()) }
    Text(if (customer == null) "ثبت فروشگاه" else "ویرایش اطلاعات فروشگاه", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("نام فروشگاه *") }, singleLine = true)
    Spacer(Modifier.height(6.dp))
    Text("نوع فعالیت")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { type = CustomerEntity.TYPE_SUPERMARKET }) {
            Text(if (type == CustomerEntity.TYPE_SUPERMARKET) "✓ سوپرمارکت" else "سوپرمارکت")
        }
        OutlinedButton(onClick = { type = CustomerEntity.TYPE_TOBACCONIST }) {
            Text(if (type == CustomerEntity.TYPE_TOBACCONIST) "✓ دخانیاتی" else "دخانیاتی")
        }
    }
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("شماره تماس (اختیاری)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth(), label = { Text("نام مسئول (اختیاری)") }, singleLine = true)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("توضیحات (اختیاری)") }, minLines = 2)
    Spacer(Modifier.height(8.dp))
    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = !busy,
            onClick = { onSave(CustomerInput(name, type, phone, contact, description)) }
        ) { Text(if (busy) "در حال ذخیره…" else "ذخیره") }
        OutlinedButton(enabled = !busy, onClick = onBack) { Text("بازگشت") }
    }
}

@Composable
private fun CustomerDetail(
    customer: CustomerEntity,
    message: String?,
    busy: Boolean,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit
){
    var confirmDelete by remember(customer.customerId) { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text("حذف دائمی فروشگاه") },
            text = { Text("این کار اطلاعات فروشگاه و تمام سوابق مکانی آن را برای همیشه از گوشی پاک می‌کند و قابل بازگردانی نیست. ادامه می‌دهید؟") },
            confirmButton = {
                Button(enabled = !busy, onClick = onDelete) { Text(if (busy) "در حال حذف…" else "حذف دائمی") }
            },
            dismissButton = { OutlinedButton(enabled = !busy, onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
    Text(customer.name, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text("نوع فعالیت: ${customerTypeLabel(customer.customerType)}")
    Text("وضعیت: ${if (customer.status == CustomerEntity.STATUS_ACTIVE) "فعال" else "غیرفعال"}")
    Text("مسئول: ${customer.contactName ?: "ثبت نشده"}")
    Text("شماره تماس: ${customer.phone ?: "ثبت نشده"}")
    Text("توضیحات: ${customer.description ?: "ثبت نشده"}")
    Spacer(Modifier.height(12.dp))
    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    Button(enabled = !busy, onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("ویرایش اطلاعات") }
    OutlinedButton(
        enabled = !busy,
        onClick = { onToggleActive(customer.status != CustomerEntity.STATUS_ACTIVE) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (customer.status == CustomerEntity.STATUS_ACTIVE) "غیرفعال‌سازی (بدون حذف اطلاعات)" else "فعال‌سازی مجدد") }
    OutlinedButton(enabled = !busy, onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به فهرست") }
    OutlinedButton(enabled = !busy, onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("حذف دائمی فروشگاه") }
}

private fun customerTypeLabel(type: String): String = when (type) {
    CustomerEntity.TYPE_SUPERMARKET -> "سوپرمارکت"
    CustomerEntity.TYPE_TOBACCONIST -> "دخانیاتی"
    else -> "نامشخص"
}

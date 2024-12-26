package com.example.tokoplastik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.data.repository.AddProductPricesRepository
import com.example.tokoplastik.data.repository.AddProductRepository
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.data.repository.BaseRepository
import com.example.tokoplastik.data.repository.CheckoutRepository
import com.example.tokoplastik.data.repository.CustomerRepository
import com.example.tokoplastik.data.repository.DashboardRepository
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.repository.StockRepository

class ViewModelFactory (
    private val repository: BaseRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository as AuthRepository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository as ProductRepository) as T
            modelClass.isAssignableFrom(ProductViewModel::class.java) -> ProductViewModel(repository as ProductRepository) as T
            modelClass.isAssignableFrom(ProductDetailViewModel::class.java) -> ProductDetailViewModel(repository as ProductRepository) as T
            modelClass.isAssignableFrom(AddProductViewModel::class.java) -> AddProductViewModel(repository as AddProductRepository) as T
            modelClass.isAssignableFrom(AddProductPricesViewModel::class.java) -> AddProductPricesViewModel(repository as AddProductPricesRepository) as T
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> TransactionViewModel(repository as CustomerRepository) as T
            modelClass.isAssignableFrom(CheckoutViewModel::class.java) -> CheckoutViewModel(repository as CheckoutRepository) as T
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> CustomerViewModel(repository as CustomerRepository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository as DashboardRepository) as T
            modelClass.isAssignableFrom(StockViewModel::class.java) -> StockViewModel(repository as StockRepository) as T
            else -> throw IllegalArgumentException("ViewModelClass not found")
        }
    }
}
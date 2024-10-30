package com.example.tokoplastik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tokoplastik.data.repository.AddProductRepository
import com.example.tokoplastik.data.repository.AuthRepository
import com.example.tokoplastik.data.repository.BaseRepository
import com.example.tokoplastik.data.repository.ProductRepository

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
            else -> throw IllegalArgumentException("ViewModelClass not found")
        }
    }
}
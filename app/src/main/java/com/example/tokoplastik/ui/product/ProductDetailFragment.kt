package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.example.tokoplastik.R
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.databinding.FragmentProductDetailBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.ProductDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ProductDetailFragment : BaseFragment<ProductDetailViewModel, FragmentProductDetailBinding, ProductRepository>() {

    private val args: ProductDetailFragmentArgs by navArgs()

    override fun getViewModel(): Class<ProductDetailViewModel> = ProductDetailViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProductDetailBinding = FragmentProductDetailBinding.inflate(inflater, container, false)

    override fun getFragmentRepository(): ProductRepository {
        val token = runBlocking { userPreferences.authToken.first() }
        val api = remoteDataSource.buildApi(GetProductApi::class.java, token)
        return ProductRepository(api)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val productId = args.productId
        // Observe product details
        viewModel.getProductDetail(productId)
        viewModel.productDetail.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    it.data?.data.let { product ->
                        binding.apply {
                            textProductName.text = product?.name
                            textProductPrice.text = "Rp ${product?.capitalPrice}"
                            textProductDescription.text = product?.supplier
                            // Add more UI bindings as needed
                        }
                    }
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    handleApiError(it)
                }
            }
        })
    }
}
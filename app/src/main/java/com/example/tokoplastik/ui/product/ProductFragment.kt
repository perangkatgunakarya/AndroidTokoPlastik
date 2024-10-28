package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokoplastik.adapter.ProductAdapter
import com.example.tokoplastik.data.network.GetProductApi
import com.example.tokoplastik.data.repository.ProductRepository
import com.example.tokoplastik.data.responses.GetProduct
import com.example.tokoplastik.databinding.FragmentProductBinding
import com.example.tokoplastik.ui.base.BaseFragment
import com.example.tokoplastik.util.Resource
import com.example.tokoplastik.util.handleApiError
import com.example.tokoplastik.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductFragment : BaseFragment<ProductViewModel, FragmentProductBinding, ProductRepository> () {

    private lateinit var adapter: ProductAdapter
    private lateinit var getProduct: List<GetProduct>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getProduct = listOf()

        viewModel.product.observe(viewLifecycleOwner, Observer {
            when(it) {
                is Resource.Success -> {
                    lifecycleScope.launch {
                        withContext (Dispatchers.Main) {
                            binding.productRecycler.apply {
                                adapter = ProductAdapter(getProduct)
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                        }
                        //
                        val product = getProduct
                        Toast.makeText(requireContext(), product.toString(), Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Failure -> {
                    handleApiError(it)
                }
                is Resource.Loading -> {
                }
            }
        })
    }

    override fun getViewModel() = ProductViewModel::class.java

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProductBinding.inflate(inflater, container, false)

    override fun getFragmentRepository() = ProductRepository(remoteDataSource.buildApi(GetProductApi::class.java))
}
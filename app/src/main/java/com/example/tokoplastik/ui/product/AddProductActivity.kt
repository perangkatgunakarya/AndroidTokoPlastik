package com.example.tokoplastik.ui.product

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.tokoplastik.HomeActivity
import com.example.tokoplastik.R

class AddProductActivity : AppCompatActivity() {

    private lateinit var homeActivity: HomeActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_product)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
            title = "Tambah Produk"
        }

        // Get a reference to the HomeActivity
        homeActivity = HomeActivity()

        // Set the back button click listener
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            homeActivity.navigateToProductFragment()
        }
    }
}
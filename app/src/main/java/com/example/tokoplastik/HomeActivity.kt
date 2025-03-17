package com.example.tokoplastik

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.tokoplastik.ui.transaction.CheckoutFragment
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                if (insets.isVisible(WindowInsetsCompat.Type.navigationBars())) 0 else systemBars.bottom
            )
            insets
        }

        bottomNav = findViewById(R.id.bottom_navigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView2) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.detailProductFragment -> {
                    bottomNav.visibility = View.GONE
                }

                R.id.stockFragment -> {
                    bottomNav.visibility = View.GONE
                }

                R.id.checkoutFragment -> {
                    bottomNav.visibility = View.GONE
                }

                R.id.detailHistoryFragment -> {
                    bottomNav.visibility = View.GONE
                }
                R.id.addCustomerFragment  -> {
                    bottomNav.visibility = View.GONE
                }
                R.id.customerFragment -> {
                    bottomNav.visibility = View.GONE
                }
                R.id.updateCustomerFragment -> {
                    bottomNav.visibility = View.GONE
                }

                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }

        NavigationUI.setupWithNavController(bottomNav, navController)
        bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                super.onFragmentResumed(fm, f)
                when (f) {
                    is CheckoutFragment -> disableBottomNavScroll()
                    else -> enableBottomNavScroll()
                }
            }
        }, true)
    }

    private fun enableBottomNavScroll() {
        val params = bottomNav.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = HideBottomViewOnScrollBehavior<BottomNavigationView>()
        bottomNav.layoutParams = params
    }

    private fun disableBottomNavScroll() {
        val params = bottomNav.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = null
        bottomNav.layoutParams = params
//        bottomNav.visibility = View.VISIBLE
    }
}
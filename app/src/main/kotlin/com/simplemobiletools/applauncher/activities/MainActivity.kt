package com.simplemobiletools.applauncher.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.simplemobiletools.applauncher.BuildConfig
import com.simplemobiletools.applauncher.LauncherAdapterUpdateListener
import com.simplemobiletools.applauncher.R
import com.simplemobiletools.applauncher.adapters.LaunchersAdapter
import com.simplemobiletools.applauncher.databinding.ActivityMainBinding
import com.simplemobiletools.applauncher.dialogs.AddLaunchersDialog
import com.simplemobiletools.applauncher.dialogs.ChangeSortingDialog
import com.simplemobiletools.applauncher.extensions.config
import com.simplemobiletools.applauncher.extensions.dbHelper
import com.simplemobiletools.applauncher.extensions.getAllLaunchers
import com.simplemobiletools.applauncher.extensions.isAPredefinedApp
import com.simplemobiletools.applauncher.models.AppLauncher
import com.simplemobiletools.applauncher.voyah.*
import com.simplemobiletools.applauncher.voyah.PERMISSION_REQUEST_CODE
import com.simplemobiletools.applauncher.voyah.SYSTEM_ALERT_WINDOW_REQUEST_CODE
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView


class MainActivity : SimpleActivity(), LauncherAdapterUpdateListener {
    companion object {
        private const val MAX_COLUMN_COUNT = 15
    }

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private var launchersIgnoringSearch = ArrayList<AppLauncher>()
    private var allLaunchers: ArrayList<AppLauncher>? = null
    private var zoomListener: MyRecyclerView.MyZoomListener? = null

    private var mStoredPrimaryColor = 0
    private var mStoredTextColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        updateMaterialActivityViews(binding.mainCoordinator, binding.launchersGrid, useTransparentNavigation = true, useTopSearchMenu = true)

        setupEmptyView()
        setupLaunchers()
        checkWhatsNewDialog()
        storeStateVariables()
        setupGridLayoutManager()

        binding.fab.setOnClickListener {
            fabClicked()
        }
        checkAndRequestPermissions(this)
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
        if (mStoredTextColor != getProperTextColor()) {
            getGridAdapter()?.updateTextColor(getProperTextColor())
        }

        val properPrimaryColor = getProperPrimaryColor()
        if (mStoredPrimaryColor != properPrimaryColor) {
            getGridAdapter()?.apply {
                updatePrimaryColor()
                notifyDataSetChanged()
            }
        }

        binding.apply {
            updateTextColors(coordinatorLayout)
            noItemsPlaceholder2.setTextColor(properPrimaryColor)
            launchersFastscroller.updateColors(properPrimaryColor)
            (fab.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = navigationBarHeight + resources.getDimension(R.dimen.activity_margin).toInt()

        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onBackPressed() {
        if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionResult", "Permission granted: ${permissions[i]}")
                } else {
                    Log.e("PermissionResult", "Permission denied: ${permissions[i]}")
                }
            }
            val serviceIntent = Intent(this, VoyahFloatingButtonService::class.java)
            startForegroundService(serviceIntent)
            checkSpecialPermissions(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SYSTEM_ALERT_WINDOW_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("OverlayPermission", "SYSTEM_ALERT_WINDOW permission granted")
            } else {
                Log.e("OverlayPermission", "SYSTEM_ALERT_WINDOW permission denied")
            }
        }
    }

    private fun refreshMenuItems() {
        binding.mainMenu.getToolbar().menu.apply {
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.apply {
            getToolbar().inflateMenu(R.menu.menu)
            toggleHideOnScroll(false)
            setupMenu()

            onSearchTextChangedListener = { text ->
                searchTextChanged(text)
            }

            getToolbar().setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sort -> showSortingDialog()
                    R.id.toggle_app_name -> toggleAppName()
                    R.id.column_count -> changeColumnCount()
                    R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                    R.id.settings -> launchSettings()
                    R.id.about -> launchAbout()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun searchTextChanged(text: String) {
        val launchers = launchersIgnoringSearch.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<AppLauncher>
        setupAdapter(launchers)
    }

    private fun updateMenuColors() {
        updateStatusbarColor(getProperBackgroundColor())
        binding.mainMenu.updateColors()
    }

    private fun getGridAdapter() = binding.launchersGrid.adapter as? LaunchersAdapter

    private fun setupLaunchers() {
        launchersIgnoringSearch = dbHelper.getLaunchers()
        checkInvalidApps()
        initZoomListener()
        setupAdapter(launchersIgnoringSearch)
    }

    private fun setupAdapter(launchers: ArrayList<AppLauncher>) {
        AppLauncher.sorting = config.sorting
        launchers.sort()

        LaunchersAdapter(
            activity = this,
            launchers = launchers,
            listener = this,
            recyclerView = binding.launchersGrid,
        ) {
            hideKeyboard()
            val launchIntent = packageManager.getLaunchIntentForPackage((it as AppLauncher).packageName)
            if (launchIntent != null) {
                try {
                    startActivity(launchIntent)
                    if (config.closeApp) {
                        finish()
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            } else {
                try {
                    launchViewIntent("market://details?id=${it.packageName}")
                } catch (ignored: Exception) {
                    launchViewIntent("https://play.google.com/store/apps/details?id=${it.packageName}")
                }
            }
        }.apply {
            setupZoomListener(zoomListener)
            binding.launchersGrid.adapter = this
        }

        maybeShowEmptyView()
        ensureBackgroundThread {
            allLaunchers = getAllLaunchers()
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            setupAdapter(launchersIgnoringSearch)
        }
    }

    private fun toggleAppName() {
        config.showAppName = !config.showAppName
        setupAdapter(launchersIgnoringSearch)
    }

    private fun changeColumnCount() {
        val items = ArrayList<RadioItem>()
        for (i in 1..MAX_COLUMN_COUNT) {
            items.add(RadioItem(i, resources.getQuantityString(R.plurals.column_counts, i, i)))
        }

        val currentColumnCount = (binding.launchersGrid.layoutManager as MyGridLayoutManager).spanCount
        RadioGroupDialog(this, items, currentColumnCount) {
            val newColumnCount = it as Int
            if (currentColumnCount != newColumnCount) {
                (binding.launchersGrid.layoutManager as MyGridLayoutManager).spanCount = newColumnCount
                if (portrait) {
                    config.portraitColumnCnt = newColumnCount
                } else {
                    config.landscapeColumnCnt = newColumnCount
                }
                columnCountChanged()
            }
        }
    }

    private fun increaseColumnCount() {
        val newColumnCount = ++(binding.launchersGrid.layoutManager as MyGridLayoutManager).spanCount
        if (portrait) {
            config.portraitColumnCnt = newColumnCount
        } else {
            config.landscapeColumnCnt = newColumnCount
        }
        columnCountChanged()
    }

    private fun reduceColumnCount() {
        val newColumnCount = --(binding.launchersGrid.layoutManager as MyGridLayoutManager).spanCount
        if (portrait) {
            config.portraitColumnCnt = newColumnCount
        } else {
            config.landscapeColumnCnt = newColumnCount
        }
        columnCountChanged()
    }

    private fun columnCountChanged() {
        refreshMenuItems()
        getGridAdapter()?.apply {
            calculateIconWidth()
            notifyItemRangeChanged(0, launchers.size)
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.launchersGrid.layoutManager as MyGridLayoutManager
        if (portrait) {
            layoutManager.spanCount = config.portraitColumnCnt
        } else {
            layoutManager.spanCount = config.landscapeColumnCnt
        }
    }

    private fun initZoomListener() {
        val layoutManager = binding.launchersGrid.layoutManager as MyGridLayoutManager
        zoomListener = object : MyRecyclerView.MyZoomListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getGridAdapter()?.finishActMode()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                    increaseColumnCount()
                    getGridAdapter()?.finishActMode()
                }
            }
        }
    }

    private fun checkInvalidApps() {
        val invalidIds = ArrayList<String>()
        for ((id, name, packageName) in launchersIgnoringSearch) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null && !packageName.isAPredefinedApp()) {
                invalidIds.add(id.toString())
            }
        }

        dbHelper.deleteLaunchers(invalidIds)
        launchersIgnoringSearch = launchersIgnoringSearch.filter { !invalidIds.contains(it.id.toString()) } as ArrayList<AppLauncher>
    }

    private fun storeStateVariables() {
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredTextColor = getProperTextColor()
    }

    override fun refreshItems() {
        binding.mainMenu.closeSearch()
        setupLaunchers()
    }

    override fun refetchItems() {
        launchersIgnoringSearch = dbHelper.getLaunchers()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(7, R.string.release_7))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }

    private fun fabClicked() {
        if (allLaunchers != null) {
            val shownLaunchers = launchersIgnoringSearch
            AddLaunchersDialog(this, allLaunchers!!, shownLaunchers) {
                setupLaunchers()
            }
        }
    }

    private fun setupEmptyView() {
        binding.noItemsPlaceholder2.apply {
            val properPrimaryColor = getProperPrimaryColor()
            underlineText()
            setTextColor(properPrimaryColor)
            setOnClickListener {
                fabClicked()
            }
        }
    }

    private fun maybeShowEmptyView() {
        binding.apply {
            if (getGridAdapter()?.launchers?.isEmpty() == true) {
                launchersFastscroller.beGone()
                noItemsPlaceholder2.beVisibleIf(mainMenu.getCurrentQuery().isEmpty())
                noItemsPlaceholder.beVisible()
            } else {
                noItemsPlaceholder2.beGone()
                noItemsPlaceholder.beGone()
                launchersFastscroller.beVisible()
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = 0L

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
        )

        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
            faqItems.add(FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, false)
    }
}

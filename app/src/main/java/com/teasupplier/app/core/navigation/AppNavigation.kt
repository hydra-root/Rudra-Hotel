package com.teasupplier.app.core.navigation

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Central navigation graph defining destination routes, bottom navigation bar
 * for quick access (Home & Bulk Entry), and seamless screen transitions.
 */

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val DASHBOARD = "dashboard"
    const val TABLE_ORDER = "table_order/{tableNumber}"
    const val CUSTOMER_LIST = "customer_list"
    const val ADD_EDIT_CUSTOMER = "add_edit_customer/{customerId}"
    const val SALE_ENTRY = "sale_entry"
    const val BILLING = "billing"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val REPORTS_DAILY = "reports_daily"
    const val REPORTS_MONTHLY = "reports_monthly"
    const val REPORTS_CUSTOMER = "reports_customer"
    const val LEDGER = "ledger"
    const val PDF_VIEWER = "pdf_viewer/{encodedPath}"
    const val PRODUCT_MANAGEMENT = "product_management"
    const val BULK_ORDER = "bulk_order"
    const val BUSINESS_PROFILE = "business_profile"
    const val EXPENSE = "expense"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Routes.DASHBOARD,
        Routes.BULK_ORDER
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Routes.DASHBOARD,
                        onClick = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Bulk Entry") },
                        label = { Text("Bulk Entry") },
                        selected = currentRoute == Routes.BULK_ORDER,
                        onClick = {
                            navController.navigate(Routes.BULK_ORDER) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) {
                com.teasupplier.app.feature.dashboard.DashboardScreen(
                    onNavigateToTableOrder = { tableNumber ->
                        navController.navigate("table_order/$tableNumber")
                    },
                    onNavigateToBulkOrder = { navController.navigate(Routes.BULK_ORDER) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                    onNavigateToBilling = { navController.navigate(Routes.BILLING) },
                    onNavigateToAddCustomer = {
                        navController.navigate(Routes.ADD_EDIT_CUSTOMER.replace("{customerId}", "-1"))
                    },
                    onNavigateToCustomers = { navController.navigate(Routes.CUSTOMER_LIST) },
                    onNavigateToExpense = { navController.navigate(Routes.EXPENSE) },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(
                route = Routes.TABLE_ORDER,
                arguments = listOf(
                    navArgument("tableNumber") {
                        type = NavType.IntType
                        defaultValue = 1
                    }
                )
            ) {
                com.teasupplier.app.feature.tableorder.TableOrderScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(Routes.CUSTOMER_LIST) {
                com.teasupplier.app.feature.customer.CustomerListScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAddCustomer = { 
                        navController.navigate(Routes.ADD_EDIT_CUSTOMER.replace("{customerId}", "-1")) 
                    },
                    onNavigateToCustomerProfile = { customerId ->
                        navController.navigate(Routes.ADD_EDIT_CUSTOMER.replace("{customerId}", customerId.toString()))
                    }
                )
            }
            composable(Routes.ADD_EDIT_CUSTOMER) {
                com.teasupplier.app.feature.customer.AddEditCustomerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SALE_ENTRY) {
                com.teasupplier.app.feature.sale.SaleEntryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToBulkOrder = { navController.navigate(Routes.BULK_ORDER) }
                )
            }
            composable(Routes.BILLING) {
                com.teasupplier.app.feature.billing.BillingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(Routes.REPORTS) {
                com.teasupplier.app.feature.reports.ReportsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDaily = { navController.navigate(Routes.REPORTS_DAILY) },
                    onNavigateToMonthly = { navController.navigate(Routes.REPORTS_MONTHLY) },
                    onNavigateToCustomer = { navController.navigate(Routes.REPORTS_CUSTOMER) }
                )
            }
            composable(Routes.REPORTS_DAILY) {
                com.teasupplier.app.feature.reports.daily.DailyReportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(Routes.REPORTS_MONTHLY) {
                com.teasupplier.app.feature.reports.monthly.MonthlyReportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(Routes.REPORTS_CUSTOMER) {
                com.teasupplier.app.feature.reports.customer.CustomerReportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
            composable(Routes.SETTINGS) {
                com.teasupplier.app.feature.settings.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProducts = { navController.navigate(Routes.PRODUCT_MANAGEMENT) },
                    onNavigateToBusinessProfile = { navController.navigate(Routes.BUSINESS_PROFILE) }
                )
            }
            composable(Routes.LEDGER) {
                com.teasupplier.app.feature.ledger.LedgerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PDF_VIEWER) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("encodedPath") ?: ""
                val filePath = android.net.Uri.decode(encodedPath)
                com.teasupplier.app.feature.pdfviewer.PdfViewerScreen(
                    filePath = filePath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PRODUCT_MANAGEMENT) {
                com.teasupplier.app.feature.product.ProductManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BULK_ORDER) {
                com.teasupplier.app.feature.bulkorder.BulkOrderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BUSINESS_PROFILE) {
                com.teasupplier.app.feature.settings.BusinessProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EXPENSE) {
                com.teasupplier.app.feature.expense.ExpenseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdfViewer = { encodedPath ->
                        navController.navigate(Routes.PDF_VIEWER.replace("{encodedPath}", encodedPath))
                    }
                )
            }
        }
    }
}

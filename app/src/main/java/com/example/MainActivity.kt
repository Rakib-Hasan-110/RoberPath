package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOutUser
import com.example.data.model.AgencySettings
import com.example.data.model.RoberPathCatalog
import com.example.data.repository.AgencyRepository
import com.example.ui.components.RoberPathTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.admin.AdminAgencyHubScreen
import com.example.ui.screens.admin.AdminCrmBookingsScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminFinanceScreen
import com.example.ui.screens.admin.AdminOperationsScreen
import com.example.ui.screens.customer.CustomerHomeScreen
import com.example.ui.screens.customer.CustomerPaymentsScreen
import com.example.ui.screens.customer.PackagesScreen
import com.example.ui.screens.customer.PilgrimsAndVisaScreen
import com.example.ui.screens.customer.SafarItineraryScreen
import com.example.ui.theme.RoberPathTheme
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.PortalMode
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      RoberPathTheme {
        AppNavigation()
      }
    }
  }
}

private data class NavDestination(
  val label: String,
  val icon: ImageVector,
  val testTag: String,
)

@Composable
fun AppNavigation() {
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      val viewModel: AgencyViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) {
              "APPLICATION_KEY missing from CreationExtras"
            }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            AgencyViewModel(AgencyRepository(db, auth), user.uid)
          }
        },
      )
      RoberPathWorkspace(
        viewModel = viewModel,
        fallbackDisplayName = user.displayName ?: "Pilgrim",
      )
    }
  }
}

@Composable
private fun RoberPathWorkspace(
  viewModel: AgencyViewModel,
  fallbackDisplayName: String,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  val portalMode by viewModel.portalMode.collectAsStateWithLifecycle()
  val currency by viewModel.currency.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
  val settingsState by viewModel.agencySettingsState.collectAsStateWithLifecycle()
  val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
  val pilgrimsState by viewModel.pilgrimsState.collectAsStateWithLifecycle()
  val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
  val inquiriesState by viewModel.inquiriesState.collectAsStateWithLifecycle()
  val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
  val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
  val suppliersState by viewModel.suppliersState.collectAsStateWithLifecycle()
  val staffState by viewModel.staffState.collectAsStateWithLifecycle()
  val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()

  val userProfile = (userProfileState as? UiState.Success)?.data
  val packages = (packagesState as? UiState.Success)?.data ?: RoberPathCatalog.defaultPackages
  val settings = (settingsState as? UiState.Success)?.data ?: AgencySettings()
  val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()
  val pilgrims = (pilgrimsState as? UiState.Success)?.data ?: emptyList()
  val payments = (paymentsState as? UiState.Success)?.data ?: emptyList()
  val inquiries = (inquiriesState as? UiState.Success)?.data ?: emptyList()
  val groups = (groupsState as? UiState.Success)?.data ?: emptyList()
  val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
  val suppliers = (suppliersState as? UiState.Success)?.data ?: emptyList()
  val staff = (staffState as? UiState.Success)?.data ?: emptyList()
  val auditLogs = (auditLogsState as? UiState.Success)?.data ?: emptyList()

  var selectedTab by rememberSaveable(portalMode) { mutableIntStateOf(0) }

  LaunchedEffect(feedback) {
    val msg = feedback ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearFeedback()
  }

  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  val destinations = if (portalMode == PortalMode.ADMIN && viewModel.isAdminUser) {
    listOf(
      NavDestination("Dashboard", Icons.Default.Dashboard, "nav_admin_dashboard"),
      NavDestination("CRM", Icons.Default.BookOnline, "nav_admin_crm"),
      NavDestination("Ops/Visa", Icons.Default.FlightTakeoff, "nav_admin_ops"),
      NavDestination("Finance", Icons.Default.AccountBalanceWallet, "nav_admin_finance"),
      NavDestination("Agency", Icons.Default.BusinessCenter, "nav_admin_hub"),
    )
  } else {
    listOf(
      NavDestination("Home", Icons.Default.Home, "nav_customer_home"),
      NavDestination("Packages", Icons.Default.Mosque, "nav_customer_packages"),
      NavDestination("Safar", Icons.Default.EventNote, "nav_customer_safar"),
      NavDestination("Pilgrims", Icons.Default.Badge, "nav_customer_pilgrims"),
      NavDestination("Pay", Icons.Default.Payments, "nav_customer_payments"),
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 700.dp

    Scaffold(
      topBar = {
        RoberPathTopBar(
          title = if (portalMode == PortalMode.ADMIN) "Rober Path Admin" else "Rober Path",
          subtitle = if (portalMode == PortalMode.ADMIN) "Agency Management • Dhaka HQ" else "Hajj & Umrah Customer Portal",
          currency = currency,
          isAdminUser = viewModel.isAdminUser,
          portalMode = portalMode,
          onToggleCurrency = viewModel::toggleCurrency,
          onTogglePortalMode = {
            val next = if (portalMode == PortalMode.ADMIN) PortalMode.CUSTOMER else PortalMode.ADMIN
            viewModel.setPortalMode(next)
          },
          onSignOut = {
            signOutUser(credentialManager, scope)
          },
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (!useRail) {
          NavigationBar {
            destinations.forEachIndexed { index, dest ->
              NavigationBarItem(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            destinations.forEachIndexed { index, dest ->
              NavigationRailItem(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f)) {
          val displayName = userProfile?.displayName?.ifBlank { fallbackDisplayName } ?: fallbackDisplayName
          if (portalMode == PortalMode.ADMIN && viewModel.isAdminUser) {
            when (selectedTab) {
              0 -> AdminDashboardScreen(
                bookings = bookings,
                pilgrims = pilgrims,
                payments = payments,
                expenses = expenses,
                inquiries = inquiries,
                groups = groups,
                currency = currency,
                settings = settings,
                onNavigateTab = { selectedTab = it },
              )
              1 -> AdminCrmBookingsScreen(
                bookings = bookings,
                inquiries = inquiries,
                pilgrims = pilgrims,
                groups = groups,
                currency = currency,
                settings = settings,
                onUpdateBookingAdmin = viewModel::updateBookingAdmin,
                onSubmitInquiry = viewModel::submitInquiry,
                onUpdateInquiryStatus = viewModel::updateInquiryStatus,
                onSaveTourGroup = viewModel::saveTourGroup,
              )
              2 -> AdminOperationsScreen(
                pilgrims = pilgrims,
                groups = groups,
                settings = settings,
                onUpdatePilgrimOps = viewModel::updatePilgrimOps,
              )
              3 -> AdminFinanceScreen(
                bookings = bookings,
                payments = payments,
                expenses = expenses,
                currency = currency,
                settings = settings,
                onVerifyPayment = viewModel::verifyPayment,
                onAddExpense = viewModel::addExpense,
                onProcessRefund = viewModel::processRefund,
                onFeedback = viewModel::showMessage,
              )
              else -> AdminAgencyHubScreen(
                packages = packages,
                staffMembers = staff,
                suppliers = suppliers,
                auditLogs = auditLogs,
                settings = settings,
                currency = currency,
                onSavePackage = viewModel::savePackage,
                onAddStaff = viewModel::addStaffMember,
                onUpdateAttendance = viewModel::updateStaffAttendance,
                onAddSupplier = viewModel::saveSupplier,
                onSaveSettings = viewModel::updateAgencySettings,
              )
            }
          } else {
            when (selectedTab) {
              0 -> CustomerHomeScreen(
                displayName = displayName,
                bookings = bookings,
                pilgrims = pilgrims,
                currency = currency,
                settings = settings,
                onNavigateTab = { selectedTab = it },
              )
              1 -> PackagesScreen(
                packages = packages,
                currency = currency,
                settings = settings,
                defaultCustomerName = displayName,
                onCreateBooking = viewModel::createBooking,
                onSubmitInquiry = viewModel::submitInquiry,
              )
              2 -> SafarItineraryScreen(activeBooking = bookings.firstOrNull())
              3 -> PilgrimsAndVisaScreen(
                pilgrims = pilgrims,
                bookings = bookings,
                settings = settings,
                onSavePilgrim = viewModel::savePilgrim,
                onDeletePilgrim = viewModel::deletePilgrim,
              )
              else -> CustomerPaymentsScreen(
                bookings = bookings,
                payments = payments,
                currency = currency,
                settings = settings,
                onSubmitPayment = viewModel::submitPayment,
                onFeedback = viewModel::showMessage,
              )
            }
          }
        }
      }
    }
  }
}

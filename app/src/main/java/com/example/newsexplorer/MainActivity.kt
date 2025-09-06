package com.example.newsexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.newsexplorer.viewmodel.NewsViewModel
import com.example.newsexplorer.ui.theme.NewsExplorerTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.net.URLDecoder
import com.example.newsexplorer.ui.settings.SettingsScreen
import com.example.newsexplorer.viewmodel.ThemeViewModel
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeVm: ThemeViewModel = viewModel()
            val isDark = themeVm.isDarkMode.collectAsState(initial = false).value
            NewsExplorerTheme(darkTheme = isDark) {
                AppNavigation(themeVm)
            }
        }
    }
}

// --- Navigation Host ---
@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    val route = backStack.value?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val vm: NewsViewModel = viewModel()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Categories", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                val items = listOf(
                    "All" to null,
                    "Business" to "business",
                    "Entertainment" to "entertainment",
                    "General" to "general",
                    "Health" to "health",
                    "Science" to "science",
                    "Sports" to "sports",
                    "Technology" to "technology"
                )
                items.forEach { (label, value) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = false,
                        onClick = {
                            vm.setCategory(value)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = route?.startsWith("home") == true,
                    onClick = { navController.navigate("home") },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = route?.startsWith("settings") == true,
                    onClick = { navController.navigate("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {

            composable("home") {
                HomeScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } }) { title, description, imageUrl ->
                    val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                    val encodedDesc = URLEncoder.encode(description ?: "", StandardCharsets.UTF_8.toString())
                    val encodedImage = URLEncoder.encode(imageUrl ?: "", StandardCharsets.UTF_8.toString())
                    navController.navigate("detail/$encodedTitle/$encodedDesc/$encodedImage")
                }
            }

            composable(
                route = "detail/{title}/{description}/{imageUrl}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("description") { type = NavType.StringType },
                    navArgument("imageUrl") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
                val description = backStackEntry.arguments?.getString("description")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                val imageUrl = backStackEntry.arguments?.getString("imageUrl")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                DetailScreen(title, description, imageUrl)
            }

            composable("settings") {
                SettingsScreen(themeViewModel)
            }
        }
    }
    }
}

// --- Home Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    newsViewModel: NewsViewModel = viewModel<NewsViewModel>(),
    onOpenDrawer: () -> Unit,
    onArticleClick: (String, String?, String?) -> Unit
) {
    val articles = newsViewModel.articles.collectAsState().value
    val currentCategory = newsViewModel.category.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCategory?.replaceFirstChar { it.titlecase() } ?: "News Explorer") },
                navigationIcon = {
                    if (currentCategory != null) {
                        IconButton(onClick = { newsViewModel.setCategory(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Article, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(articles) { article ->
                NewsCard(article.title, article.description, article.urlToImage) {
                    onArticleClick(article.title, article.description, article.urlToImage)
                }
            }
        }
    }
}

// --- News Card ---
@Composable
fun NewsCard(
    title: String,
    description: String?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (!imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- Detail Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(title: String, description: String?, imageUrl: String?) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Article Details") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (!imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

package processing.app.ui

import java.awt.Desktop
import java.net.URI
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.input.pointer.pointerMoveFilter

const val SKETCHBOOK_PATH = "/Users/tonz/Documents/Processing/sketchbook/"

enum class SortOption(val label: String) {
    RECENTLY_UPDATED("Recently Updated"),
    ALPHABETICAL("Alphabetical");

    override fun toString(): String = label
}

data class SketchItem(
    val name: String,
    //val isFolder: Boolean,
    val path: String,
    val lastModified: Long = 0L // default for folders
)

data class SketchCollection(
    val name: String,
    val path: String,
    val lastModified: Long,
    val subcollections: List<SketchCollection>,
    val sketches: List<SketchItem>
)

data class DisplayItem(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val indentLevel: Int
)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sketchbook Prototype",
        state = WindowState(width = 800.dp, height = 600.dp, position = WindowPosition(Alignment.Center))
    ) {
        Sketchbook()
    }
}

fun scanCollection(dir: File): SketchCollection {
    val allSubDirs = dir.listFiles()
        ?.filter { it.isDirectory }
        ?: emptyList()

    val (sketchFolders, normalSubcollections) = allSubDirs.partition { containsPdeFile(it) }

    val sketches = sketchFolders.map { folder ->
        SketchItem(
            name = folder.name,
            path = folder.absolutePath,
            lastModified = folder.lastModified()
        )
    }

    val subcollections = normalSubcollections.map { scanCollection(it) }

    return SketchCollection(
        name = dir.name,
        path = dir.absolutePath,
        lastModified = dir.lastModified(),
        subcollections = subcollections,
        sketches = sketches
    )
}

fun containsPdeFile(folder: File): Boolean {
    return folder.listFiles()?.any { it.isFile && it.extension.equals("pde", true) } == true
}

fun flattenSketchesForGridDisplay(collection: SketchCollection): List<SketchItem> {
    val childSketches = collection.subcollections.flatMap { flattenSketchesForGridDisplay(it) }
    return collection.sketches + childSketches
}

fun flattenHierarchyForSidebarDisplay(collection: SketchCollection, indentLevel: Int = 0): List<DisplayItem> {
    val items = mutableListOf<DisplayItem>()

    items.add(DisplayItem(collection.name, collection.path, isFolder = true, indentLevel = indentLevel))

    collection.subcollections.forEach {
        items.addAll(flattenHierarchyForSidebarDisplay(it, indentLevel + 1))
    }

    return items
}

@Composable
fun Sketchbook() {
    var selectedFolder by remember { mutableStateOf<SketchCollection?>(null) }

    val sketchbookFolder = File(SKETCHBOOK_PATH);

    var sketchHierarchy = SketchCollection(
        name = sketchbookFolder.name,
        path = sketchbookFolder.absolutePath,
        lastModified = sketchbookFolder.lastModified(),
        subcollections = emptyList(),
        sketches = emptyList<SketchItem>()
    )

    if (sketchbookFolder.exists() && sketchbookFolder.isDirectory) {
        sketchHierarchy = scanCollection(sketchbookFolder);
    }

    var sortOption by remember { mutableStateOf(SortOption.RECENTLY_UPDATED) }
    var searchQuery by remember { mutableStateOf("") }

    val flattenedSketchesForGrid = if (selectedFolder != null) {
        flattenSketchesForGridDisplay(selectedFolder!!)
    } else {
        flattenSketchesForGridDisplay(sketchHierarchy)
    }

    val filteredSketchesForGrid = when {
        searchQuery.isBlank() -> flattenedSketchesForGrid
        else -> flattenedSketchesForGrid.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val sortedSketchesForGrid = when (sortOption) {
        SortOption.ALPHABETICAL -> filteredSketchesForGrid.sortedBy { it.name.lowercase() }
        SortOption.RECENTLY_UPDATED -> filteredSketchesForGrid.sortedByDescending { it.lastModified }
        else -> filteredSketchesForGrid
    }

    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Sidebar(sketchHierarchy
                , onSearchQueryChange = { newQuery ->
                searchQuery = newQuery
            }
            , onFolderClick = { folderItem ->
                selectedFolder = findFolderByPath(sketchHierarchy, folderItem.path)
            })
        }

        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            MainSketchGrid(
                sketches = sortedSketchesForGrid,
                selectedFolder = selectedFolder?.name,
                searchQuery = searchQuery,
                sortOption = sortOption,
                onSortChange = { sortOption = it }
            )
        }
    }
}

@Composable
fun Sidebar(sketchHierarchy: SketchCollection, onSearchQueryChange: (String) -> Unit, onFolderClick: (DisplayItem) -> Unit) {
    val flatItems = remember(sketchHierarchy) {
        flattenHierarchyForSidebarDisplay(sketchHierarchy)
    }

    Column(
        modifier = Modifier.fillMaxHeight().width(350.dp)
    ) {
        SidebarSearchbar(onSearchQueryChange)

        Box(modifier = Modifier.weight(1f)) {
            SidebarHierarchy(flatItems, onFolderClick)
        }

        SidebarBottom()
    }
}

fun findFolderByPath(collection: SketchCollection, path: String): SketchCollection? {
    if (collection.path == path) return collection
    for (sub in collection.subcollections) {
        val found = findFolderByPath(sub, path)
        if (found != null) return found
    }
    return null
}

@Composable
fun SidebarSearchbar(onSearchQueryChange: (String) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column() {
            Text(
                text = "Sketchbook",
                style = MaterialTheme.typography.h4,
            )
            TextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    onSearchQueryChange(it)
                },
                placeholder = { Text("Search...") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SidebarBottom() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { println("Clicked add folder")  },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            contentPadding = PaddingValues(
                start = 8.dp,  // less left padding (default is 16.dp)
                top = 12.dp,
                end = 16.dp,
                bottom = 12.dp
            )
        ) {
            Image(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Folder")
        }
        Button(onClick = { println("Clicked create folder") },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            contentPadding = PaddingValues(
                start = 8.dp,  // less left padding (default is 16.dp)
                top = 12.dp,
                end = 16.dp,
                bottom = 12.dp
            )
        ) {
            Image(
                imageVector = Icons.Outlined.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Folder")
        }
    }
}

@Composable
fun SidebarHierarchy(items: List<DisplayItem>, onFolderClick: (DisplayItem) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(items) { item ->
            SidebarItemRow(item.name, item.isFolder, item.indentLevel, onFolderClick = {
                if (item.isFolder) {
                    onFolderClick(item)
                }
            })
        }

    }
}

@Composable
fun SidebarItemRow(name: String, isFolder: Boolean, indentLevel: Int = 0, onFolderClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showContextMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHovered || showContextMenu) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .hoverable(interactionSource)
            .padding(start = (indentLevel * 16).dp, top = 4.dp, bottom = 4.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { println("Clicked on ${name}"); onFolderClick(); },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val folderIcon: ImageVector = if (isFolder) Icons.Outlined.MailOutline else Icons.Outlined.Face

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = folderIcon,
                contentDescription = "Folder Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = name)
        }

        Box() {
            if (isHovered || showContextMenu) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent)
                        .clickable(interactionSource = remember { MutableInteractionSource() },
                            indication = null){ showContextMenu = true }
                )
            } else {
                // Optionally reserve space so text doesn't shift
                Spacer(modifier = Modifier.size(24.dp)) // match IconButton size
            }
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(onClick = {
                    println("Open in Explorer: $name")
                    showContextMenu = false
                }) {
                    Text("Open in Explorer")
                }
                if (isFolder && indentLevel == 0) { //TODO: CHECK IF ITS USER ADDED
                    DropdownMenuItem(onClick = {
                        println("Remove Top Level Folder: $name")
                        showContextMenu = false
                    }) {
                        Text("Remove Folder")
                    }
                }
            }
        }
    }
}

@Composable
fun MainSketchGrid(
    sketches: List<SketchItem>,
    selectedFolder: String?,
    searchQuery: String,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    val displayedHeader = when {
        selectedFolder != null && searchQuery.isNotBlank() ->
            "$selectedFolder — Search results for \"$searchQuery\""
        selectedFolder != null ->
            selectedFolder
        searchQuery.isNotBlank() ->
            "Search results for \"$searchQuery\""
        else ->
            "All Sketches"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = displayedHeader,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box {
            Button(onClick = { expanded = true }) {
                Text("Sort by: $sortOption")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    onSortChange(SortOption.RECENTLY_UPDATED)
                    expanded = false
                }) {
                    Text(SortOption.RECENTLY_UPDATED.label)
                }
                DropdownMenuItem(onClick = {
                    onSortChange(SortOption.ALPHABETICAL)
                    expanded = false
                }) {
                    Text(SortOption.ALPHABETICAL.label)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AddNewSketchCard()
            }
            items(sketches) { sketch ->
                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Open in Explorer") {
                                println("Show File ${sketch.name}")
                            },
                            ContextMenuItem("Favorite Sketch") {
                                println("Favorite ${sketch.name}")
                            },
                        )
                    }
                ) {
                    SketchCard(sketch)
                }
            }
        }
    }
}

@Composable
fun AddNewSketchCard() {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            // Build the pde:// URI and open it
            val pdeUri = URI("pde://sketch/new")
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(pdeUri)
            } else {
                println("Desktop API not supported. Could not open new sketch.")
            }
            println("Clicked to Add New Sketch")
        },//.aspectRatio(1f), // square
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    ,
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.DarkGray)
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add New",
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add New", style = MaterialTheme.typography.body1)
            }
        }
    }
}

@Composable
fun SketchCard(sketch: SketchItem) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),//.aspectRatio(1f), // square
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.LightGray)
                    .clickable {
                        // Build the pde:// URI and open it
                        val pdeUri = URI("pde://${sketch.path}/${sketch.name}.pde")
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(pdeUri)
                        } else {
                            println("Desktop API not supported. Could not open sketch: ${sketch.name}")
                        }
                        println("Clicked open ${sketch.name}")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Thumbnail", color = Color.DarkGray)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(sketch.name, style = MaterialTheme.typography.body1)
                /*Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Sketch Info",
                    modifier = Modifier.size(20.dp).clickable {
                        println("Clicked info for ${sketch.name}")
                    }
                )*/
            }
        }
    }
}
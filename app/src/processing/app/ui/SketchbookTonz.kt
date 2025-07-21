package processing.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.outlined.Info
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

data class SketchItem(
    val name: String,
    val isFolder: Boolean,
    val lastModified: Long = 0L // default for folders
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

@Composable
fun Sketchbook() {
    // TODO: Replace dummy data with reading from folder.
    val sketchList = listOf(
            SketchItem("Fun", false, 1680000000000),
            SketchItem("Cute", false, 1660000000000),
            SketchItem("MyFolder", true),
            SketchItem("Silly", false, 1670000000000),
        )

    var sortOption by remember { mutableStateOf("Recently Updated") }

    val sortedSketches = when (sortOption) {
            "Alphabetical" -> sketchList.sortedBy { it.name.lowercase() }
            "Recently Updated" -> sketchList.sortedByDescending { it.lastModified }
            else -> sketchList
    }


    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Sidebar(sketchList)
        }

        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            MainSketchGrid(
                sketches = sortedSketches.filter { !it.isFolder },
                selectedFolder = null,
                searchQuery = "",
                sortOption = sortOption,
                onSortChange = { sortOption = it }
            )
        }
    }
}

@Composable
fun Sidebar(sketches: List<SketchItem>) {
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxHeight().width(350.dp)
    ) {
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
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(8.dp)
        ) {

            items(sketches) { sketch ->
                SketchRow(sketch)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { println("Clicked track folder")  },
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
                Text("Track Folder")
            }
            Button(onClick = { println("Clicked add folder") },
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
                Text("New Folder")
            }
        }
    }
}

@Composable
fun SketchRow(sketch: SketchItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { println("Clicked on ${sketch.name}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon: ImageVector = if (sketch.isFolder) Icons.Outlined.MailOutline else Icons.Outlined.Face
        Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = sketch.name)
    }
}

@Composable
fun MainSketchGrid(
    sketches: List<SketchItem>,
    selectedFolder: String?,
    searchQuery: String,
    sortOption: String,
    onSortChange: (String) -> Unit
) {
    val displayedHeader = when {
        selectedFolder != null -> selectedFolder
        searchQuery.isNotBlank() -> "Search results for \"$searchQuery\""
        else -> "All Sketches"
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
                    onSortChange("Recently Updated")
                    expanded = false
                }) {
                    Text("Recently Updated")
                }
                DropdownMenuItem(onClick = {
                    onSortChange("Alphabetical")
                    expanded = false
                }) {
                    Text("Alphabetical")
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
                SketchCard(sketch)
            }
        }
    }
}

@Composable
fun AddNewSketchCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),//.aspectRatio(1f), // square
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp).clickable {
                        println("Clicked to Add New Sketch")
                    },
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
                    .background(Color.LightGray).clickable {
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
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Sketch Info",
                    modifier = Modifier.size(20.dp).clickable {
                        println("Clicked info for ${sketch.name}")
                    }
                )
            }
        }
    }
}
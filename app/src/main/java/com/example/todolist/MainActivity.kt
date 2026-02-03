package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.example.todolist.ui.theme.TodoListTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val todoDao by lazy { database.todoDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoListTheme {
                TodoApp(todoDao, this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(todoDao: TodoDao, context: Context) {
    val todoItems by todoDao.getAllItems().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var itemToMoveId by remember { mutableStateOf<Int?>(null) }

    var newTaskTitle by remember { mutableStateOf("") }
    var editTaskTitle by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val completedCount = todoItems.count { it.isCompleted }
    val totalCount = todoItems.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    // Trigger widget update
    fun updateWidget() {
        scope.launch {
            TodoWidget().updateAll(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Journey",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (totalCount > 0) {
                            Text(
                                "$completedCount of $totalCount tasks completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("New Task") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                if (todoItems.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todoItems, key = { it.id }) { item ->
                            TodoRow(
                                item = item,
                                isMoveMode = itemToMoveId == item.id,
                                onToggle = {
                                    if (itemToMoveId == item.id) {
                                        itemToMoveId = null
                                    } else {
                                        scope.launch {
                                            todoDao.update(item.copy(isCompleted = !item.isCompleted))
                                            updateWidget()
                                        }
                                    }
                                },
                                onEdit = {
                                    editingItem = item
                                    editTaskTitle = item.title
                                },
                                onDelete = {
                                    scope.launch {
                                        todoDao.delete(item)
                                        updateWidget()
                                    }
                                },
                                onLongClick = {
                                    itemToMoveId = if (itemToMoveId == item.id) null else item.id
                                },
                                onMoveUp = {
                                    val currentItem = todoItems.find { it.id == item.id }
                                    currentItem?.let { activeItem ->
                                        val index = todoItems.indexOf(activeItem)
                                        if (index > 0) {
                                            val otherItem = todoItems[index - 1]
                                            if (!otherItem.isCompleted) {
                                                scope.launch {
                                                    val pos1 = activeItem.position
                                                    val pos2 = otherItem.position
                                                    todoDao.updateAll(listOf(
                                                        activeItem.copy(position = pos2),
                                                        otherItem.copy(position = pos1)
                                                    ))
                                                    updateWidget()
                                                }
                                            }
                                        }
                                    }
                                },
                                onMoveDown = {
                                    val currentItem = todoItems.find { it.id == item.id }
                                    currentItem?.let { activeItem ->
                                        val index = todoItems.indexOf(activeItem)
                                        if (index < todoItems.size - 1) {
                                            val otherItem = todoItems[index + 1]
                                            if (!otherItem.isCompleted) {
                                                scope.launch {
                                                    val pos1 = activeItem.position
                                                    val pos2 = otherItem.position
                                                    todoDao.updateAll(listOf(
                                                        activeItem.copy(position = pos2),
                                                        otherItem.copy(position = pos1)
                                                    ))
                                                    updateWidget()
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            TaskDialog(
                title = "What's next?",
                taskTitle = newTaskTitle,
                onTitleChange = { newTaskTitle = it },
                onDismiss = {
                    showAddDialog = false
                    newTaskTitle = ""
                },
                onConfirm = {
                    if (newTaskTitle.isNotBlank()) {
                        scope.launch {
                            val maxPos = todoDao.getMaxPosition() ?: 0
                            todoDao.insert(TodoItem(title = newTaskTitle, position = maxPos + 1))
                            updateWidget()
                            newTaskTitle = ""
                            showAddDialog = false
                        }
                    }
                },
                confirmLabel = "Add to Queue"
            )
        }

        editingItem?.let { item ->
            TaskDialog(
                title = "Edit Task",
                taskTitle = editTaskTitle,
                onTitleChange = { editTaskTitle = it },
                onDismiss = {
                    editingItem = null
                    editTaskTitle = ""
                },
                onConfirm = {
                    if (editTaskTitle.isNotBlank()) {
                        scope.launch {
                            todoDao.update(item.copy(title = editTaskTitle))
                            updateWidget()
                            editingItem = null
                            editTaskTitle = ""
                        }
                    }
                },
                confirmLabel = "Update Task"
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your list is clear",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add a task to start your journey",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoRow(
    item: TodoItem,
    isMoveMode: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val view = LocalView.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggle,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isMoveMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isMoveMode) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Complete",
                    tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            )

            if (isMoveMode) {
                Row {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, "Move Up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, "Move Down")
                    }
                }
            } else {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    taskTitle: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Task description") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

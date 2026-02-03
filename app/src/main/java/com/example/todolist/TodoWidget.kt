package com.example.todolist

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TodoWidget : GlanceAppWidget() {

    // 10 Pleasant colors for round-robin
    private val taskColors = listOf(
        Color(0xFFFFCDD2), // Light Red
        Color(0xFFFFF9C4), // Light Yellow
        Color(0xFFC8E6C9), // Light Green
        Color(0xFFBBDEFB), // Light Blue
        Color(0xFFE1BEE7), // Light Purple
        Color(0xFFFFE0B2), // Light Orange
        Color(0xFFB2EBF2), // Light Cyan
        Color(0xFFF8BBD0), // Light Pink
        Color(0xFFB2DFDB), // Light Teal
        Color(0xFFC5CAE9)  // Light Indigo
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val todoDao = database.todoDao()

        provideContent {
            val items by todoDao.getAllItems().collectAsState(initial = emptyList())
            
            GlanceTheme {
                WidgetContent(items)
            }
        }
    }

    @Composable
    private fun WidgetContent(items: List<TodoItem>) {
        // Filter out completed tasks
        val activeItems = items.filter { !it.isCompleted }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                text = "My Journey",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.primary
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp, start = 8.dp)
            )

            if (activeItems.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active tasks",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    itemsIndexed(activeItems) { index, item ->
                        TodoWidgetItem(item, index)
                    }
                }
            }
        }
    }

    @Composable
    private fun TodoWidgetItem(item: TodoItem, index: Int) {
        val backgroundColor = taskColors[index % taskColors.size]

        // Outer Box providing the "margin" space between boxes
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            // Inner colored card with 12dp internal padding for a pleasant look
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(backgroundColor))
                    .cornerRadius(12.dp)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    // Subtle indicator bar
                    Box(
                        modifier = GlanceModifier
                            .width(3.dp)
                            .height(14.dp)
                            .background(ColorProvider(Color.Black.copy(alpha = 0.15f)))
                            .cornerRadius(1.5.dp)
                    ) {}

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Text(
                        text = item.title,
                        style = TextStyle(
                            color = ColorProvider(Color.Black),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

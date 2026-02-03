package com.example.todolist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, position ASC, createdAt ASC")
    fun getAllItems(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TodoItem)

    @Update
    suspend fun update(item: TodoItem)

    @Update
    suspend fun updateAll(items: List<TodoItem>)

    @Delete
    suspend fun delete(item: TodoItem)

    @Query("SELECT MAX(position) FROM todo_items")
    suspend fun getMaxPosition(): Int?
}

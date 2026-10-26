package com.dxlr8r.notifyme.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.Prefs
import com.dxlr8r.notifyme.data.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    val prefs = Prefs(app)

    /** null until the database has answered once, so we never flash an empty state. */
    val tasks: StateFlow<List<Item>?> = repo.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<Item>> = repo.events
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(item: Item, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch { onSaved(repo.save(item)) }
    }

    fun setDone(id: Long, done: Boolean) {
        viewModelScope.launch { repo.setDone(id, done) }
    }

    fun delete(id: Long, onDeleted: (Item) -> Unit = {}) {
        viewModelScope.launch { repo.delete(id)?.let(onDeleted) }
    }

    fun restore(item: Item) {
        viewModelScope.launch { repo.restore(item) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repo.clearCompleted() }
    }
}

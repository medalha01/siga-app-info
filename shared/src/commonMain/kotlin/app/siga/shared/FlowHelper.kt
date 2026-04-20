package app.siga.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlowWrapper<T>(private val flow: StateFlow<T>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun collect(onEach: (T) -> Unit) {
        scope.launch {
            flow.collectLatest { onEach(it) }
        }
    }
}

package com.ravi.samstudioapp.presentation.screens.daily

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.domain.model.DailyLog
import com.ravi.samstudioapp.presentation.screens.daily.DailyViewModel
import com.ravi.samstudioapp.ui.components.DarkGray
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: DailyViewModel = VmInjector.getDailyViewModel(
        LocalContext.current as Activity,
        LocalContext.current as ViewModelStoreOwner
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collect state from ViewModel
    val selectedDate by viewModel.selectedDate.collectAsState()
    val logData by viewModel.logData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val scrollState = rememberScrollState()
    
    SamStudioAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header with date
                DateHeaderCard(
                    selectedDate = selectedDate,
                    onTodayClick = {
                        coroutineScope.launch {
                            viewModel.selectToday()
                        }
                    },
                    onYesterdayClick = {
                        coroutineScope.launch {
                            viewModel.selectYesterday()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loading or Log Content
                if (isLoading) {
                    LoadingCard()
                } else {
                    logData?.let { log ->
                        DailyLogCard(log = log)
                    } ?: EmptyLogCard(
                        selectedDate = selectedDate,
                        onCreateLog = {
                            // TODO: Implement log creation
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DateHeaderCard(
    selectedDate: Long,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = formatDate(selectedDate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onTodayClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedButton(
                    onClick = onYesterdayClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yesterday")
                }
            }
        }
    }
}

@Composable
private fun DailyLogCard(log: DailyLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mood Section
            LogSection(
                title = "Mood",
                content = log.mood,
                emoji = "😊"
            )
            
            // Sleep Section
            LogSection(
                title = "Sleep",
                content = "${log.sleepHours} hours",
                emoji = "😴"
            )
            
            // Water Intake Section
            LogSection(
                title = "Water Intake",
                content = "${log.waterGlasses} glasses",
                emoji = "💧"
            )
            
            // Exercise Section
            LogSection(
                title = "Exercise",
                content = "${log.exerciseMinutes} minutes",
                emoji = "🏃"
            )
            
            // Energy Level Section
            LogSection(
                title = "Energy Level",
                content = "${log.energyLevel}/10",
                emoji = "⚡"
            )
            
            // Notes Section
            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Gratitude Section
            if (log.gratitude.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🙏 Gratitude",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.gratitude,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogSection(
    title: String,
    content: String,
    emoji: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLogCard(
    selectedDate: Long,
    onCreateLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📝",
                style = MaterialTheme.typography.displayMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No log entry for ${formatDate(selectedDate)}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create a new log entry to track your daily activities, mood, and thoughts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onCreateLog,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Create Log Entry")
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun DailyScreenPreview() {
    val mockLog = DailyLog(
        date = System.currentTimeMillis(),
        mood = "😊 Happy",
        sleepHours = 7.5,
        notes = "Had a productive day at work. Feeling accomplished!",
        waterGlasses = 6,
        exerciseMinutes = 30,
        energyLevel = 8,
        gratitude = "Grateful for good health and supportive friends"
    )
    
    SamStudioAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                DateHeaderCard(
                    selectedDate = System.currentTimeMillis(),
                    onTodayClick = {},
                    onYesterdayClick = {}
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DailyLogCard(log = mockLog)
            }
        }
    }
} 
package com.prasad.daylatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasad.daylatch.data.HabitType

private val BG = Color(0xFFFEFEFE)
private val FG = Color(0xFF423D53)
private val PRIMARY = Color(0xFFA5EDA8)
private val PRIMARY_TEXT = Color(0xFF3F8445)
private val SECONDARY = Color(0xFFEAE8F4)
private val TERTIARY = Color(0xFFD0EAED)
private val MUTED = Color(0xFFF5F4F8)
private val MUTED_FG = Color(0xFF6E687C)
private val ACCENT = Color(0xFFFEDFE9)
private val ACCENT_TEXT = Color(0xFFB45974)
private val CARD = Color(0xFFFFFFFF)
private val BORDER = Color(0xFFD7D2E1)

enum class Persona { STUDENT, WORKER, RETIREE }

data class SuggestedHabit(val name: String, val type: HabitType, val target: Int, val window: Int)

fun suggestedHabitsFor(persona: Persona): List<SuggestedHabit> {
    return when (persona) {
        Persona.STUDENT -> listOf(
            SuggestedHabit("Study 2 hours", HabitType.DAILY, 1, 1),
            SuggestedHabit("Read 20 pages", HabitType.DAILY, 1, 1),
            SuggestedHabit("Exercise 30 min", HabitType.DAILY, 1, 1),
            SuggestedHabit("Drink 2L water", HabitType.DAILY, 1, 1),
            SuggestedHabit("Review notes", HabitType.DAILY, 1, 1),
            SuggestedHabit("Sleep by 11 PM", HabitType.DAILY, 1, 1),
            SuggestedHabit("Practice coding", HabitType.QUOTA, 5, 7),
            SuggestedHabit("Meditate 10 min", HabitType.DAILY, 1, 1)
        )
        Persona.WORKER -> listOf(
            SuggestedHabit("Morning routine", HabitType.DAILY, 1, 1),
            SuggestedHabit("Exercise 30 min", HabitType.QUOTA, 4, 7),
            SuggestedHabit("Read 15 min", HabitType.DAILY, 1, 1),
            SuggestedHabit("Meal prep", HabitType.QUOTA, 3, 7),
            SuggestedHabit("Walk 8000 steps", HabitType.DAILY, 1, 1),
            SuggestedHabit("Sleep 7+ hours", HabitType.DAILY, 1, 1),
            SuggestedHabit("No phone after 10 PM", HabitType.DAILY, 1, 1),
            SuggestedHabit("Plan next day", HabitType.DAILY, 1, 1)
        )
        Persona.RETIREE -> listOf(
            SuggestedHabit("Morning walk", HabitType.DAILY, 1, 1),
            SuggestedHabit("Drink water", HabitType.DAILY, 1, 1),
            SuggestedHabit("Read 30 min", HabitType.DAILY, 1, 1),
            SuggestedHabit("Light stretching", HabitType.DAILY, 1, 1),
            SuggestedHabit("Call a friend", HabitType.QUOTA, 3, 7),
            SuggestedHabit("Garden / hobby", HabitType.QUOTA, 4, 7),
            SuggestedHabit("Meditate 15 min", HabitType.DAILY, 1, 1),
            SuggestedHabit("Evening walk", HabitType.DAILY, 1, 1)
        )
    }
}

@Composable
fun OnboardingScreen(onComplete: (List<SuggestedHabit>) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selectedPersona by rememberSaveable { mutableStateOf<Persona?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalDaylatchColors.current.bg)
            .padding(20.dp)
    ) {
        when (page) {
            0 -> {
                // Page 0: Welcome
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Daylatch",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalDaylatchColors.current.fg
                    )
                    Text(
                        text = "Small steps, every day",
                        fontSize = 14.sp,
                        color = LocalDaylatchColors.current.mutedFg
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Track your habits with zero cloud dependency. Your data stays on your device, always.",
                        fontSize = 14.sp,
                        color = LocalDaylatchColors.current.fg,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { page = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalDaylatchColors.current.primary,
                            contentColor = LocalDaylatchColors.current.primaryText
                        )
                    ) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Privacy-first · Offline-only · No accounts",
                        fontSize = 11.sp,
                        color = LocalDaylatchColors.current.mutedFg
                    )
                }
            }
            1 -> {
                // Page 1: Persona Selection
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("WHO ARE YOU?", fontSize = 10.sp, color = LocalDaylatchColors.current.mutedFg, fontWeight = FontWeight.Bold)
                    Text("Let's personalize your journey", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg)
                    Text("We'll suggest habits that fit your lifestyle", fontSize = 13.sp, color = LocalDaylatchColors.current.mutedFg)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    PersonaCard(
                        icon = "📚",
                        title = "Student",
                        subtitle = "Focus on learning & growth",
                        isSelected = selectedPersona == Persona.STUDENT,
                        selectedColor = LocalDaylatchColors.current.tertiary,
                        onClick = { selectedPersona = Persona.STUDENT }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PersonaCard(
                        icon = "💼",
                        title = "Working Professional",
                        subtitle = "Balance work & wellness",
                        isSelected = selectedPersona == Persona.WORKER,
                        selectedColor = LocalDaylatchColors.current.primary,
                        onClick = { selectedPersona = Persona.WORKER }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PersonaCard(
                        icon = "🌿",
                        title = "Retired / Senior",
                        subtitle = "Gentle routines & wellbeing",
                        isSelected = selectedPersona == Persona.RETIREE,
                        selectedColor = LocalDaylatchColors.current.accent,
                        onClick = { selectedPersona = Persona.RETIREE }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { page = 2 },
                        enabled = selectedPersona != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalDaylatchColors.current.primary,
                            contentColor = LocalDaylatchColors.current.primaryText,
                            disabledContainerColor = LocalDaylatchColors.current.muted,
                            disabledContentColor = LocalDaylatchColors.current.mutedFg
                        )
                    ) {
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            2 -> {
                // Page 2: Suggested Habits
                var selectedHabits by remember { 
                    mutableStateOf(suggestedHabitsFor(selectedPersona!!).toSet()) 
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("SUGGESTED FOR YOU", fontSize = 10.sp, color = LocalDaylatchColors.current.mutedFg, fontWeight = FontWeight.Bold)
                    Text("Pick your habits", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg)
                    Text("Tap to select · you can always add more later", fontSize = 13.sp, color = LocalDaylatchColors.current.mutedFg)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val habits = suggestedHabitsFor(selectedPersona!!)
                        items(habits) { habit ->
                            val isSelected = selectedHabits.contains(habit)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LocalDaylatchColors.current.card, RoundedCornerShape(12.dp))
                                    .border(1.dp, LocalDaylatchColors.current.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedHabits = if (isSelected) {
                                            selectedHabits - habit
                                        } else {
                                            selectedHabits + habit
                                        }
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(if (isSelected) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.muted, RoundedCornerShape(8.dp))
                                        .border(1.dp, LocalDaylatchColors.current.border, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", color = LocalDaylatchColors.current.primaryText, fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = habit.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalDaylatchColors.current.fg,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(LocalDaylatchColors.current.secondary, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val tagText = if (habit.type == HabitType.DAILY) "Daily" else "${habit.target}x/week"
                                    Text(tagText, fontSize = 11.sp, color = LocalDaylatchColors.current.fg)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onComplete(selectedHabits.toList()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalDaylatchColors.current.primary,
                            contentColor = LocalDaylatchColors.current.primaryText
                        )
                    ) {
                        Text("Start with ${selectedHabits.size} habits", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { onComplete(emptyList()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Skip — I'll add my own", color = LocalDaylatchColors.current.mutedFg)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonaCard(
    icon: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp)
            .background(if (isSelected) selectedColor else LocalDaylatchColors.current.card, RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.border,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg)
                Text(text = subtitle, fontSize = 13.sp, color = LocalDaylatchColors.current.mutedFg)
            }
        }
    }
}

package com.aura.app.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.data.local.entities.UserProfile
import com.aura.app.ui.components.AdaptiveCard
import com.aura.app.ui.components.XPProgressBar
import com.aura.app.ui.components.avatar.LivingAura
import com.aura.app.ui.theme.AuraTheme
import com.aura.app.util.XPManager
import java.time.LocalTime

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    latestMood: String? = null,
    onNavigateToSocialLab: () -> Unit,
    onNavigateToMissions: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToMirror: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
) {
    val ageGroup = AuraTheme.ageGroup
    val profile = userProfile ?: return

    val greeting = when {
        LocalTime.now().hour < 12 -> "Good Morning"
        LocalTime.now().hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Theme Toggle Button
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Avatar mini preview (Dynamic miniature Aura)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                LivingAura(
                    stage = profile.avatarStage,
                    ageGroup = ageGroup,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // ── XP Progress ──
        AdaptiveCard {
            Text(
                text = "Level ${profile.level} • ${XPManager.getStageName(profile.avatarStage)} ${XPManager.getStageEmoji(profile.avatarStage)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            XPProgressBar(currentXP = profile.xp)
        }

        // ── Quick Stats ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatChip(
                emoji = "🔥",
                value = "${profile.streakDays}",
                label = "Streak",
                modifier = Modifier.weight(1f),
            )
            StatChip(
                emoji = "⭐",
                value = "${profile.level}",
                label = "Level",
                modifier = Modifier.weight(1f),
            )
            StatChip(
                emoji = "📊",
                value = "${profile.totalSessions}",
                label = "Sessions",
                modifier = Modifier.weight(1f),
            )
        }

        // Daily Mood Challenge
        AdaptiveCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🌱 Daily Mood Challenge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val challengeText = when (latestMood?.lowercase()) {
                    "sad" -> "Take 2 minutes to write 3 things you are grateful for."
                    "anxious" -> "Try 1 minute of slow breathing and relax your mind."
                    "happy" -> "Write one positive moment from today."
                    "excited" -> "Write one goal you feel excited to achieve."
                    "calm" -> "Spend 2 minutes enjoying your peaceful surroundings."
                    "angry" -> "Take 3 deep breaths and write what triggered you."
                    "neutral" -> "Reflect on one thing you learned today."
                    else -> "Select a mood in your journal to get a challenge."
                }

                Text(
                    text = challengeText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── Feature Cards ──
        Text(
            text = "What would you like to do?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        FeatureCard(
            icon = Icons.Filled.Psychology,
            emoji = "🧪",
            title = "Social Lab",
            description = "Practice real-life conversations with AI",
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            ),
            onClick = onNavigateToSocialLab,
        )

        FeatureCard(
            icon = Icons.Filled.EmojiEvents,
            emoji = "🎯",
            title = "Missions",
            description = "Complete tasks to earn XP and grow",
            gradientColors = listOf(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            ),
            onClick = onNavigateToMissions,
        )

        FeatureCard(
            icon = Icons.Filled.Person,
            emoji = "🧬",
            title = "My Avatar",
            description = "Watch yourself evolve — Stage ${profile.avatarStage}: ${XPManager.getStageName(profile.avatarStage)}",
            gradientColors = listOf(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            ),
            onClick = onNavigateToAvatar,
        )

        FeatureCard(
            icon = Icons.Filled.CameraAlt,
            emoji = "🪞",
            title = "Mirror-Tech",
            description = "Analyze your body language with AI in real-time",
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            ),
            onClick = onNavigateToMirror,
        )

        // ── Daily Tip ──
        AdaptiveCard {
            val tip = getDailyTip(ageGroup)
            Text(
                text = "💡 Tip of the Day",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 22.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatChip(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    AdaptiveCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    emoji: String,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    val ageGroup = AuraTheme.ageGroup

    AdaptiveCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center,
            ) {
                if (ageGroup == "kids") {
                    Text(text = emoji, fontSize = 24.sp)
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

private fun getDailyTip(ageGroup: String): String {
    val tips = when (ageGroup) {
        "kids" -> listOf(
            "Try smiling at someone new today — it might make their day! 😊",
            "If a friend feels sad, just listening can help a lot. 💛",
            "It's okay to feel nervous — being brave means doing it anyway! 💪",
            "Saying 'please' and 'thank you' makes everyone feel good! ✨",
            "When you feel angry, try counting to 10 before you speak. 🧘",
        )
        "teens" -> listOf(
            "Active listening means putting your phone down and really hearing someone.",
            "Confidence isn't about being perfect — it's about being comfortable with imperfection.",
            "Your body language speaks louder than your words. Stand tall!",
            "It's okay to disagree — just do it respectfully.",
            "Taking deep breaths before a tough conversation can change the outcome.",
        )
        else -> listOf(
            "In negotiations, the person who listens more usually wins more.",
            "Emotional intelligence is the strongest predictor of workplace success.",
            "Reframing feedback as 'perspective sharing' reduces defensiveness in others.",
            "Micro-expressions reveal true feelings — practice reading them.",
            "The 10-second pause before responding transforms reactive into reflective.",
        )
    }
    val dayOfYear = java.time.LocalDate.now().dayOfYear
    return tips[dayOfYear % tips.size]
}

private fun getAvatarEmoji(stage: Int, ageGroup: String): String {
    return when (ageGroup) {
        "kids" -> when (stage) {
            1 -> "🌱" // Seed
            2 -> "🌿" // Sprout
            3 -> "🌸" // Bloom
            4 -> "✨" // Radiant
            5 -> "🦸" // Hero
            else -> "🧒"
        }
        "teens" -> when (stage) {
            1 -> "🌑" // Seed
            2 -> "🌙" // Sprout
            3 -> "🌕" // Bloom
            4 -> "☄️" // Radiant
            5 -> "🔥" // Master
            else -> "😶"
        }
        else -> when (stage) {
            1 -> "💎" // Seed
            2 -> "💠" // Sprout
            3 -> "🔱" // Bloom
            4 -> "👑" // Radiant
            5 -> "🌌" // Visionary
            else -> "🙂"
        }
    }
}

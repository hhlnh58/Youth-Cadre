package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Modern clean white atmosphere with subtle, airy executive ambient blue top glow.
 * Eliminates all muddy or dirty shadows for a crisp, high-end feel.
 */
@Composable
fun LuminousAtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CleanCanvas)
            .drawBehind {
                // Subtle top ambient blue tint radiating gently
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x161E3A8A), Color(0x0C38BDF8), Color.Transparent),
                        center = Offset(size.width * 0.5f, -size.height * 0.05f),
                        radius = size.width * 1.1f
                    ),
                    center = Offset(size.width * 0.5f, -size.height * 0.05f),
                    radius = size.width * 1.1f
                )
            },
        content = content
    )
}

/**
 * Modern Clean White Card with hairline border
 * Replaces blurry, muddy drop shadows with razor-sharp borders and crisp white surfaces.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 18.dp,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9)
        )
    ),
    backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            PureWhite,
            Color(0xFFFAFCFF)
        )
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(shapeRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = PureWhite,
        border = BorderStroke(1.dp, borderBrush),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Modern Deep Navy Gradient Hero Card
 * Provides the user-requested "gradient xanh đậm" look with crisp white typography.
 */
@Composable
fun DeepNavyCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(shapeRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = NavyDeep,
        border = BorderStroke(1.dp, Color(0x3538BDF8)),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(DeepNavyGradient)
                .padding(18.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Sleek Section Header with punchy title & clean action/pill
 */
@Composable
fun SleekSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.1.sp
                ),
                color = TextPrimary
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "$count",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = NavyDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Modern Youth Cadre Emblem
 * Uses the user-provided high-res circular logo (R.drawable.logo_yc)
 */
@Composable
fun ModernYouthCadreLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val cornerRadius = size * 0.22f
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF1B1464))
            .border(1.dp, Color(0x18000000), shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_yc),
            contentDescription = "Youth Cadre Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        )
    }
}

/**
 * Clean Minimalist Metric Counter Card
 * High contrast, razor-sharp hairline borders, crisp typography
 */
@Composable
fun MinimalMetricCard(
    count: String,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = PureWhite,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(PureWhite, accentColor.copy(alpha = 0.04f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = count,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyHeroDark,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    SleekPriorityBadge(priority = priority, modifier = modifier)
}

/**
 * Compact Priority Badge with glowing dot
 */
@Composable
fun SleekPriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (color, text) = when (priority.uppercase()) {
        "P1" -> Pair(CoralUrgent, "P1 • Khẩn cấp")
        "P2" -> Pair(ChampagneGold, "P2 • Trọng tâm")
        "P3" -> Pair(NavyMedium, "P3 • Tiêu chuẩn")
        else -> Pair(TextSecondary, "P4")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Clean Status Pill
 */
@Composable
fun SleekStatusPill(status: String, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        "TODO" -> Pair(TextMuted, "Chưa làm")
        "IN_PROGRESS" -> Pair(NavyMedium, "Đang xử lý")
        "WAITING_FOR_OTHERS" -> Pair(ChampagneGold, "Chờ duyệt")
        "SUBMITTED_FOR_APPROVAL" -> Pair(LuminousCyan, "Đã trình")
        "COMPLETED" -> Pair(EmeraldSuccess, "Hoàn tất")
        else -> Pair(TextMuted, "Tạm dừng")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * Modern Filter Pill
 */
@Composable
fun FilterBadge(
    label: String,
    isActive: Boolean,
    accentColor: Color = NavyDeep,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) NavyDeep else PureWhite,
        border = BorderStroke(
            1.dp,
            if (isActive) NavyDeep else BorderLight
        ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

package com.malayspeak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.malayspeak.app.ui.navigation.Screen
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Floating glassmorphism navigation bar built with the shared GlassCard.
 */
@Composable
fun BottomNavBar(
    items: List<Screen>,
    currentRoute: String?,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val glass = MalaySpeakTheme.glass

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xs),
        shape = glass.navBarShape,
        elevated = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            items.forEach { screen ->
                key(screen.route) {
                    val onItemClick = remember(onSelect, screen) { { onSelect(screen) } }
                    NavItem(
                        screen = screen,
                        selected = currentRoute == screen.route,
                        onClick = onItemClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val label = stringResource(screen.labelRes)
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = sizes.minTouchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
            },
        shape = styles.navItemShape,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.xs, horizontal = spacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = null,
                modifier = Modifier.size(sizes.iconMd)
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Box(
                modifier = Modifier
                    .width(sizes.iconMd)
                    .height(sizes.borderWidth * 2)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (selected) content else Color.Transparent)
            )
        }
    }
}

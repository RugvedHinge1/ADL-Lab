package com.scholr.app.ui.clay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading

/**
 * Labelled input sitting in a clay well. Used across Sign In / Sign Up and
 * anywhere else a form appears.
 */
@Composable
fun ClayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var revealed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextHeading,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .clayWell(cornerRadius = ClayRadius.Button, offset = 4.dp, blur = 11.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = ScholrPurple.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(13.dp))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextBody.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextHeading),
                    cursorBrush = SolidColor(ScholrPurple),
                    visualTransformation = if (isPassword && !revealed) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isPassword) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = if (revealed) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                    contentDescription = if (revealed) "Hide password" else "Show password",
                    tint = TextBody,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { revealed = !revealed }
                )
            }
        }
    }
}

/** A hairline clay divider with a centred label, e.g. "or continue with". */
@Composable
fun ClayDividerLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .clayWell(cornerRadius = 1.dp, offset = 1.dp, blur = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextBody,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .clayWell(cornerRadius = 1.dp, offset = 1.dp, blur = 2.dp)
        )
    }
}

/** Small circular social-auth button in clay. */
@Composable
fun ClaySocialButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = ScholrPurple
) {
    Box(
        modifier = modifier
            .size(58.dp)
            .claySurface(cornerRadius = 20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
    }
}

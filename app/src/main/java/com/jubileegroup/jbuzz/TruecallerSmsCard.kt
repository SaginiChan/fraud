package com.jubileegroup.jbuzz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme

// Design canvas in dp: just the card. In the reference screenshot (720 px wide = 360 dp) it measures
// 342 x 187 dp.
private const val DesignWidth = 342f
private const val DesignHeight = 187f

/** Width divided by height of [TruecallerSmsCard]: the height follows from the width it is given. */
const val SmsCardAspectRatio = DesignWidth / DesignHeight

private val FrameTop = Color(0xFF0B6BFD)
private val FrameBottom = Color(0xFF0B41A5)
private val FooterColor = Color(0xFFE9F4FE)
private val DividerColor = Color(0xFFD0E1F2)
private val GrayText = Color(0xFF5F6368)
private val AmountColor = Color(0xFFE8690F)
private val ActionColor = Color(0xFF1666E1)

private val CardShape = RoundedCornerShape(15.dp)
private val PanelShape = RoundedCornerShape(9.dp)

// The values shown in the reference screenshot.
const val ReferenceTime = "09:09 PM"
const val ReferenceAmount = "- KSH 50"
const val ReferencePaidTo = "Vincent Momanyi"

/** The header line of the card, e.g. "SMS • 09:09 PM". */
fun smsMeta(time: String): String = "SMS • $time"

/** "Paid to <name>", with "Paid" and the name in bold, as in the reference screenshot. */
fun paidToMessage(name: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Paid") }
    append(" to ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
}

private val DefaultMessage = paidToMessage(ReferencePaidTo)

/**
 * Truecaller-style SMS card, used as the drop-down notification. Defaults reproduce the reference
 * screenshot.
 *
 * This is the card alone: no margin, shadow or backdrop around it, so where it sits is up to the
 * caller. It is laid out on a 342 dp wide design canvas and scaled to the width it is given, so give
 * it a bounded width (e.g. `Modifier.fillMaxWidth()`); the height follows automatically.
 */
@Composable
fun TruecallerSmsCard(
    modifier: Modifier = Modifier,
    meta: String = smsMeta(ReferenceTime),
    sender: String = "MPESA",
    senderLogo: Painter? = painterResource(R.drawable.mpesa_logo),
    amount: String = ReferenceAmount,
    message: AnnotatedString = DefaultMessage,
    primaryAction: String = "Mark as read",
    secondaryAction: String = "View profile",
    onClose: () -> Unit = {},
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {}
) {
    BoxWithConstraints(modifier) {
        val system = LocalDensity.current
        // 1 design dp -> maxWidth / 342 real dp. Font scale is pinned so text keeps the design proportions.
        val design = Density(
            density = system.density * (maxWidth.value / DesignWidth),
            fontScale = 1f
        )
        CompositionLocalProvider(LocalDensity provides design) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(DesignWidth / DesignHeight)
                    .background(Brush.verticalGradient(listOf(FrameTop, FrameBottom)), CardShape)
            ) {
                Header(meta = meta, onClose = onClose)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 7.dp, top = 34.dp, end = 7.dp, bottom = 7.5.dp)
                        .clip(PanelShape)
                ) {
                    Body(
                        sender = sender,
                        senderLogo = senderLogo,
                        amount = amount,
                        message = message,
                        modifier = Modifier.weight(1f)
                    )
                    Footer(
                        primary = primaryAction,
                        secondary = secondaryAction,
                        onPrimary = onPrimaryAction,
                        onSecondary = onSecondaryAction
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(meta: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(31.dp)) {
        Image(
            painter = painterResource(R.drawable.truecaller_wordmark),
            contentDescription = "Truecaller",
            modifier = Modifier
                .offset(x = 15.dp, y = 9.5.dp)
                .size(width = 62.25.dp, height = 12.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = meta,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 3.5.dp)
                // Stays clear of the wordmark and the close button if the text is long.
                .widthIn(max = 188.dp)
                .paddingFromBaseline(top = 20.5.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 32dp touch target around the 11dp icon.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 5.25.dp)
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            CloseIcon(Modifier.size(11.dp))
        }
    }
}

@Composable
private fun CloseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val inset = stroke / 2f
        val path = Path().apply {
            moveTo(inset, inset)
            lineTo(size.width - inset, size.height - inset)
            moveTo(size.width - inset, inset)
            lineTo(inset, size.height - inset)
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun Body(
    sender: String,
    senderLogo: Painter?,
    amount: String,
    message: AnnotatedString,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().background(Color.White)) {
        if (senderLogo != null) {
            Image(
                painter = senderLogo,
                contentDescription = sender,
                modifier = Modifier
                    .offset(x = 9.75.dp, y = 16.5.dp)
                    .size(width = 27.75.dp, height = 14.dp),
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = sender,
            modifier = Modifier
                .padding(start = 47.dp, end = 16.dp)
                .paddingFromBaseline(top = 29.dp),
            color = GrayText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = amount,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .paddingFromBaseline(top = 69.5.dp),
            color = AmountColor,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = message,
            modifier = Modifier
                .padding(start = 15.5.dp, end = 16.dp)
                .paddingFromBaseline(top = 93.dp),
            color = GrayText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Footer(
    primary: String,
    secondary: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(FooterColor)) {
        Row(modifier = Modifier.fillMaxSize()) {
            FooterAction(text = primary, onClick = onPrimary, modifier = Modifier.weight(1f))
            FooterAction(text = secondary, onClick = onSecondary, modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .height(30.dp)
                .background(DividerColor)
        )
    }
}

@Composable
private fun FooterAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ActionColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

// The card alone, at its design size (342 dp wide).
@Preview(widthDp = 342)
@Composable
private fun TruecallerSmsCardPreview() {
    JBuzzTheme {
        TruecallerSmsCard(modifier = Modifier.fillMaxWidth())
    }
}


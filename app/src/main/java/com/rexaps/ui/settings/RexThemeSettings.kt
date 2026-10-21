package com.rexaps.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rexaps.ui.theme.*

@Composable
fun RexThemeSettings(manager: RexThemeManager, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var selectedId by remember { mutableStateOf(manager.getCurrentTheme().id) }
    var showDetail by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(manager.motionSettings) }
    val theme = manager.getCurrentTheme()
    val categories = listOf("Semua","Glass","Dark","Light","Neon","Cosmic","Elemental")
    val filtered = manager.getThemesByCategory().values.flatten().filter {
        (category == "Semua" || it.category.equals(category, true)) &&
            (it.name.contains(query, true) || it.category.contains(query, true))
    }
    Column(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(theme.background),Color(theme.backgroundAlt)))).padding(16.dp)) {
        Text("Tema & tampilan", style = MaterialTheme.typography.headlineSmall, color = Color(theme.text), fontWeight = FontWeight.Bold)
        Text("30 tema • preview langsung • pengaturan tersimpan", color = Color(theme.textMuted), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),
            label={Text("Cari tema")},singleLine=true)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.fillMaxWidth()) {
            categories.forEach { c -> FilterChip(selected=category==c,onClick={category=c},label={Text(c)}) }
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(columns=GridCells.Adaptive(minSize=150.dp), modifier=Modifier.weight(1f),
            contentPadding=PaddingValues(bottom=12.dp), horizontalArrangement=Arrangement.spacedBy(10.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(filtered,key={it.id}) { item ->
                val active = selectedId == item.id
                Column(Modifier.clip(RoundedCornerShape(16.dp)).border(if(active) 2.dp else 1.dp,
                    Color(if(active) item.primary else item.border),RoundedCornerShape(16.dp))
                    .background(Color(item.surface)).clickable {
                        selectedId=item.id; manager.previewTheme(item.id)
                    }.padding(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(item.gradientPrimary.map { Color(it) }))) {
                        Row(Modifier.align(Alignment.BottomStart).padding(7.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf(item.primary,item.secondary,item.accent).forEach { swatch ->
                                Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(Color(swatch)))
                            }
                        }
                        if(item.id == RexThemeRegistry.default.id) Text("Default",Modifier.align(Alignment.TopEnd).padding(5.dp),
                            color=Color(item.onPrimary),style=MaterialTheme.typography.labelSmall)
                        if(active) Text("✓",Modifier.align(Alignment.TopEnd).padding(5.dp),color=Color(item.onPrimary),fontWeight=FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.name,color=Color(item.text),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                    Text(item.category,color=Color(item.textMuted),style=MaterialTheme.typography.labelSmall)
                    TextButton(onClick={selectedId=item.id;manager.previewTheme(item.id);showDetail=true},contentPadding=PaddingValues(0.dp)) { Text("Lihat detail") }
                }
            }
        }
        AnimatedVisibility(showDetail) {
            val detail=RexThemeRegistry.get(selectedId)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(detail.surface)).padding(12.dp)) {
                Text(detail.name,color=Color(detail.text),fontWeight=FontWeight.Bold)
                Text("Warna: primary #${detail.primary.toString(16).takeLast(6).uppercase()}, secondary #${detail.secondary.toString(16).takeLast(6).uppercase()}, accent #${detail.accent.toString(16).takeLast(6).uppercase()}",color=Color(detail.textMuted))
                Text("Karakter: ${detail.category.lowercase()} — ${detail.signature}.",color=Color(detail.text))
                Text("Efek visual: blur tematik, glow terkontrol, gradient berlapis, noise ${detail.noiseIntensity}.",color=Color(detail.textMuted))
                Text("Animasi background: ${detail.backgroundMotion}.",color=Color(detail.textMuted))
                Text("Animasi transisi: ${detail.transitionMotion}.",color=Color(detail.textMuted))
                Text("Animasi hover/tap: ${detail.interactionMotion}.",color=Color(detail.textMuted))
                Text("Animasi loading: ${detail.loadingMotion}.",color=Color(detail.textMuted))
                Text("Animasi notifikasi: ${detail.notificationMotion}.",color=Color(detail.textMuted))
                Text("Signature: ${detail.signature}.",color=Color(detail.text))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tingkat animasi",color=Color(theme.text),fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            RexAnimationLevel.values().forEach { level -> FilterChip(selected=settings.level==level,
                onClick={settings=settings.copy(level=level);manager.updateMotion(settings)},label={Text(when(level){RexAnimationLevel.FULL->"Penuh";RexAnimationLevel.BALANCED->"Seimbang";RexAnimationLevel.SAVER->"Hemat";RexAnimationLevel.OFF->"Mati"})}) }
        }
        Text("Intensitas efek: ${settings.effectIntensity}%",color=Color(theme.text))
        Slider(value=settings.effectIntensity.toFloat(),onValueChange={settings=settings.copy(effectIntensity=it.toInt());manager.updateMotion(settings)},valueRange=0f..100f)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Ikuti reduce motion sistem",modifier=Modifier.weight(1f),color=Color(theme.text))
            Switch(checked=settings.followSystemReduceMotion,onCheckedChange={settings=settings.copy(followSystemReduceMotion=it);manager.updateMotion(settings)})
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            OutlinedButton(onClick={manager.cancelPreview();selectedId=manager.getCurrentTheme().id},modifier=Modifier.weight(1f)) { Text("Batal") }
            Button(onClick={manager.commitPreview();selectedId=manager.getCurrentTheme().id},modifier=Modifier.weight(1f)) { Text("Terapkan") }
            TextButton(onClick={manager.setTheme(RexThemeRegistry.default.id);selectedId=RexThemeRegistry.default.id;settings=RexMotionSettings();manager.updateMotion(settings)} ) { Text("Reset") }
        }
    }
}

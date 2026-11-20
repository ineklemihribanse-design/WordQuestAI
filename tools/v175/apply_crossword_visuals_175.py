#!/usr/bin/env python3
"""Visual-only upgrade to the existing real crossword board and clue controls."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
p=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"
s=p.read_text("utf-8")
def one(a,b,label):
    global s
    if s.count(a)!=1: raise AssertionError(f"{label}: {s.count(a)}")
    s=s.replace(a,b,1)

one("import androidx.compose.ui.draw.clip",
    "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.shadow",
    "card shadow")
one("import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush",
    "gradient import")
one('''    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {''',
'''    Column(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(23.dp))
        .clip(RoundedCornerShape(23.dp))
        .background(Brush.verticalGradient(listOf(
            Color(0xFFFBFFF8), Color(0xFFEDF7EA))))
        .border(1.dp,Color(0xFFD9E8D9),RoundedCornerShape(23.dp))
        .padding(horizontal=13.dp,vertical=12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {''',
"premium score header")
one('''        Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(game.title, color = Ink, fontFamily = Serif, fontSize = 33.sp, lineHeight = 36.sp)''',
'''        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Wq175GlossIcon("puzzle",Modifier.size(45.dp))
            Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(game.path, color = Forest, fontFamily = Sans,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(game.title, color = Ink, fontFamily = Serif,
                    fontSize = 28.sp, lineHeight = 31.sp)
            }
        }''',"crossword icon next to title")
one('''    Column(
        Modifier.fillMaxWidth().clip(shape).background(LightLime).clickable(enabled = !solved, role = Role.Button, onClick = type)
            .padding(horizontal = 13.dp, vertical = if (compact) 7.dp else 10.dp),''',
'''    Column(
        Modifier.fillMaxWidth().shadow(3.dp,shape).clip(shape)
            .background(Brush.horizontalGradient(listOf(
                Color(0xFFEAF7CE),Color(0xFFF7FBE9))))
            .border(1.dp,Color(0xFFD4E8AB),shape)
            .clickable(enabled = !solved, role = Role.Button, onClick = type)
            .padding(horizontal = 13.dp, vertical = if (compact) 10.dp else 14.dp),''',
"premium active clue card")
one('''                onClick = check, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White),''',
'''                onClick = check, modifier = Modifier.weight(1f).heightIn(min=50.dp),
                shape=RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White),''',
"bigger check button")
one('''            OutlinedButton(onClick = reveal, enabled = !view.complete && view.wordIndex !in view.solvedIndices, modifier = Modifier.weight(1f)) {''',
'''            OutlinedButton(onClick = reveal,
                enabled = !view.complete && view.wordIndex !in view.solvedIndices,
                modifier = Modifier.weight(1f).heightIn(min=50.dp),
                shape=RoundedCornerShape(16.dp)) {''',"accessible hint button")
one('''            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (index == view.wordIndex) LightLime else White)
                    .border(1.dp, Line, shape).clickable(role = Role.Button) { select(index) }
                    .padding(horizontal = 9.dp, vertical = 9.dp),''',
'''            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (index == view.wordIndex) Color(0xFFEAF7CD) else White)
                    .border(1.dp, if(index == view.wordIndex)
                        Color(0xFF8DBD64) else Line, shape)
                    .clickable(role = Role.Button) { select(index) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),''',"comfortable clue selection")
p.write_text(s,"utf-8")
print("PASS: premium green crossword board chrome, selected clue, larger answer controls.")
